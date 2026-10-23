package com.ruoyi.module.agent.eval.service.impl;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Date;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ThreadLocalRandom;
import java.util.stream.Collectors;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.ruoyi.common.utils.SecurityUtils;
import com.ruoyi.common.utils.StringUtils;
import com.ruoyi.common.exception.ServiceException;
import com.ruoyi.module.agent.eval.domain.AgentEvalCase;
import com.ruoyi.module.agent.eval.domain.AgentEvalResult;
import com.ruoyi.module.agent.eval.domain.AgentEvalRun;
import com.ruoyi.module.agent.eval.domain.AgentEvalRunView;
import com.ruoyi.module.agent.eval.domain.LocalIntentRouter;
import com.ruoyi.module.agent.eval.mapper.AgentEvalCaseMapper;
import com.ruoyi.module.agent.eval.mapper.AgentEvalResultMapper;
import com.ruoyi.module.agent.eval.mapper.AgentEvalRunMapper;
import com.ruoyi.module.agent.eval.service.IAgentEvalService;
import com.ruoyi.module.agent.knowledge.domain.KnowledgeSearchResult;
import com.ruoyi.module.agent.knowledge.service.IKnowledgeSearchService;
import com.ruoyi.module.agent.domain.ToolCallView;
import com.ruoyi.module.agent.role.AgentRole;
import com.ruoyi.module.agent.role.AgentRoleService;
import com.ruoyi.module.agent.service.AgentChatClientRouter;
import com.ruoyi.module.agent.service.AgentContext;
import com.ruoyi.module.agent.service.IAgentService;

import lombok.RequiredArgsConstructor;

/**
 * Agent 评测实现。
 *
 * <p>OFFLINE 模式使用确定性意图路由和 RAG 检索，适合稳定回归；
 * LLM 模式调用真实 ChatClient，并以实际工具调用和回答内容生成报告。</p>
 *
 * @author itjialu
 */
@Service
@RequiredArgsConstructor
public class AgentEvalServiceImpl implements IAgentEvalService
{
    private final AgentEvalCaseMapper caseMapper;

    private final AgentEvalRunMapper runMapper;

    private final AgentEvalResultMapper resultMapper;

    private final IKnowledgeSearchService knowledgeSearchService;

    private final AgentChatClientRouter chatClientRouter;

    private final AgentRoleService agentRoleService;

    private final IAgentService agentService;

    @Override
    public List<AgentEvalCase> listCases()
    {
        return caseMapper.selectList(new QueryWrapper<AgentEvalCase>()
                .eq("enabled", "1").orderByAsc("id"));
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public AgentEvalRunView runOffline()
    {
        List<AgentEvalCase> cases = listCases();
        AgentEvalRun run = new AgentEvalRun();
        run.setRunNo("EVAL" + new SimpleDateFormat("yyyyMMddHHmmssSSS").format(new Date())
                + ThreadLocalRandom.current().nextInt(100, 1000));
        run.setMode("OFFLINE");
        run.setTotal(cases.size());
        run.setPassed(0);
        run.setIntentAccuracy(BigDecimal.ZERO);
        run.setToolAccuracy(BigDecimal.ZERO);
        run.setRagHitRate(BigDecimal.ZERO);
        run.setPassRate(BigDecimal.ZERO);
        run.setCreateBy(SecurityUtils.getUsername());
        run.setCreateTime(new Date());
        runMapper.insert(run);

        int passed = 0;
        int intentHits = 0;
        int toolHits = 0;
        int ragCases = 0;
        int ragHits = 0;
        List<AgentEvalResult> results = new ArrayList<>();
        for (AgentEvalCase item : cases)
        {
            long started = System.currentTimeMillis();
            String actualIntent = LocalIntentRouter.route(item.getQuestion());
            String actualTool = LocalIntentRouter.toolFor(actualIntent);
            boolean intentHit = item.getExpectedIntent().equals(actualIntent);
            boolean toolHit = item.getExpectedTool().equals(actualTool);
            boolean keywordHit = true;
            String answer = "离线路由：" + actualTool;
            if (LocalIntentRouter.SAFETY.equals(item.getExpectedIntent()))
            {
                ragCases++;
                List<KnowledgeSearchResult> searchResults = knowledgeSearchService.search(item.getQuestion(), 3);
                toolHit = toolHit && !searchResults.isEmpty();
                answer = searchResults.isEmpty() ? "未检索到知识片段"
                        : searchResults.get(0).getDocumentTitle() + "：" + searchResults.get(0).getContent();
                keywordHit = hitKeyword(item.getExpectedKeywords(), searchResults);
                if (keywordHit)
                {
                    ragHits++;
                }
            }
            boolean casePassed = intentHit && toolHit && keywordHit;
            if (intentHit)
            {
                intentHits++;
            }
            if (toolHit)
            {
                toolHits++;
            }
            if (casePassed)
            {
                passed++;
            }

            AgentEvalResult result = new AgentEvalResult();
            result.setRunId(run.getId());
            result.setCaseId(item.getId());
            result.setCategory(item.getCategory());
            result.setQuestion(item.getQuestion());
            result.setExpectedIntent(item.getExpectedIntent());
            result.setExpectedTool(item.getExpectedTool());
            result.setActualIntent(actualIntent);
            result.setActualTool(actualTool);
            result.setPassed(casePassed ? "1" : "0");
            result.setToolSuccess(toolHit ? "1" : "0");
            result.setActualAnswer(truncate(answer, 1000));
            result.setDurationMs((int) (System.currentTimeMillis() - started));
            result.setCreateTime(new Date());
            resultMapper.insert(result);
            results.add(result);
        }

        run.setPassed(passed);
        run.setIntentAccuracy(percent(intentHits, cases.size()));
        run.setToolAccuracy(percent(toolHits, cases.size()));
        run.setRagHitRate(percent(ragHits, ragCases));
        run.setPassRate(percent(passed, cases.size()));
        runMapper.updateById(run);

        AgentEvalRunView view = new AgentEvalRunView();
        copyRun(run, view);
        view.setResults(results);
        return view;
    }

    @Override
    public AgentEvalRunView runLlm()
    {
        AgentRole role = agentRoleService.currentRole();
        Set<String> allowedTools = allowedTools(role);
        List<AgentEvalCase> cases = listCases().stream()
                .filter(item -> "unknown".equals(item.getExpectedTool())
                        || allowedTools.contains(item.getExpectedTool()))
                .collect(Collectors.toList());
        if (cases.isEmpty())
        {
            throw new ServiceException("当前角色没有可执行的真实模型评测用例");
        }

        Map<String, Object> connection = agentService.checkModelConnection();
        if (!Boolean.TRUE.equals(connection.get("ok")))
        {
            throw new ServiceException("真实模型不可用：" + connection.get("message"));
        }

        AgentEvalRun run = createRun("LLM", cases.size());
        int passed = 0;
        int intentHits = 0;
        int toolHits = 0;
        int ragCases = 0;
        int ragHits = 0;
        List<AgentEvalResult> results = new ArrayList<>();
        Long userId = SecurityUtils.getUserId();
        for (AgentEvalCase item : cases)
        {
            long started = System.currentTimeMillis();
            String answer = "";
            String actualIntent = LocalIntentRouter.UNKNOWN;
            String actualTool = "unknown";
            String errorMessage = null;
            boolean intentHit = false;
            boolean toolHit = false;
            boolean keywordHit = true;
            List<ToolCallView> calls = Collections.emptyList();
            AgentContext.open(null, userId);
            try
            {
                answer = chatClientRouter.current().prompt().user(item.getQuestion()).call().content();
                AgentContext.Context context = AgentContext.current();
                calls = context == null ? Collections.emptyList() : new ArrayList<>(context.getToolCalls());
                Set<String> calledTools = calls.stream().map(ToolCallView::getToolName)
                        .collect(Collectors.toCollection(LinkedHashSet::new));
                actualTool = calledTools.isEmpty() ? "unknown" : String.join(",", calledTools);
                actualIntent = calledTools.isEmpty() ? LocalIntentRouter.UNKNOWN
                        : LocalIntentRouter.intentForTool(calledTools.iterator().next());
                intentHit = item.getExpectedIntent().equals(actualIntent);
                toolHit = "unknown".equals(item.getExpectedTool()) ? calls.isEmpty()
                        : calls.stream().anyMatch(call -> item.getExpectedTool().equals(call.getToolName())
                                && Boolean.TRUE.equals(call.getSuccess()));
                if (LocalIntentRouter.SAFETY.equals(item.getExpectedIntent()))
                {
                    ragCases++;
                    String evidence = answer + " " + calls.stream()
                            .map(ToolCallView::getResult)
                            .filter(StringUtils::isNotEmpty)
                            .collect(Collectors.joining(" "));
                    keywordHit = hitKeyword(item.getExpectedKeywords(), evidence);
                    if (keywordHit)
                    {
                        ragHits++;
                    }
                }
            }
            catch (Exception e)
            {
                errorMessage = e.getMessage() == null ? e.getClass().getSimpleName() : e.getMessage();
            }
            finally
            {
                AgentContext.close();
            }

            boolean casePassed = errorMessage == null && StringUtils.isNotEmpty(answer)
                    && intentHit && toolHit && keywordHit;
            if (intentHit)
            {
                intentHits++;
            }
            if (toolHit)
            {
                toolHits++;
            }
            if (casePassed)
            {
                passed++;
            }

            AgentEvalResult result = buildResult(run.getId(), item, actualIntent, truncate(actualTool, 80),
                    casePassed, toolHit, truncate(answer, 10000), elapsed(started), truncate(errorMessage, 500));
            resultMapper.insert(result);
            results.add(result);
        }

        run.setPassed(passed);
        run.setIntentAccuracy(percent(intentHits, cases.size()));
        run.setToolAccuracy(percent(toolHits, cases.size()));
        run.setRagHitRate(percent(ragHits, ragCases));
        run.setPassRate(percent(passed, cases.size()));
        runMapper.updateById(run);

        AgentEvalRunView view = new AgentEvalRunView();
        copyRun(run, view);
        view.setResults(results);
        return view;
    }

    @Override
    public List<AgentEvalRun> listRuns()
    {
        return runMapper.selectList(new QueryWrapper<AgentEvalRun>()
                .orderByDesc("id").last("limit 20"));
    }

    @Override
    public AgentEvalRunView getRun(Long runId)
    {
        AgentEvalRun run = runMapper.selectById(runId);
        if (run == null)
        {
            return null;
        }
        AgentEvalRunView view = new AgentEvalRunView();
        copyRun(run, view);
        view.setResults(resultMapper.selectList(new QueryWrapper<AgentEvalResult>()
                .eq("run_id", runId).orderByAsc("id")));
        return view;
    }

    private AgentEvalRun createRun(String mode, int total)
    {
        AgentEvalRun run = new AgentEvalRun();
        run.setRunNo(mode + new SimpleDateFormat("yyyyMMddHHmmssSSS").format(new Date())
                + ThreadLocalRandom.current().nextInt(100, 1000));
        run.setMode(mode);
        run.setTotal(total);
        run.setPassed(0);
        run.setIntentAccuracy(BigDecimal.ZERO);
        run.setToolAccuracy(BigDecimal.ZERO);
        run.setRagHitRate(BigDecimal.ZERO);
        run.setPassRate(BigDecimal.ZERO);
        run.setCreateBy(SecurityUtils.getUsername());
        run.setCreateTime(new Date());
        runMapper.insert(run);
        return run;
    }

    private Set<String> allowedTools(AgentRole role)
    {
        Set<String> tools = new HashSet<>(List.of("unknown", "queryAvailableRooms",
                "queryAvailableRoomsNow", "queryAvailableDevices", "searchSafetyKnowledge"));
        if (role == AgentRole.TEACHER || role == AgentRole.ADMIN)
        {
            tools.addAll(List.of("queryPendingReservations", "queryStudentQualification",
                    "queryReservationForReview", "queryClassStudentStatistics"));
        }
        if (role == AgentRole.STUDENT)
        {
            tools.addAll(List.of("checkSafetyExamStatus", "queryMyCredit",
                    "checkMyReservationEligibility", "queryMyReservations",
                    "queryReservationReview", "createReservationDraft", "confirmReservation"));
        }
        return tools;
    }

    private AgentEvalResult buildResult(Long runId, AgentEvalCase item, String actualIntent, String actualTool,
            boolean passed, boolean toolSuccess, String answer, int durationMs, String errorMessage)
    {
        AgentEvalResult result = new AgentEvalResult();
        result.setRunId(runId);
        result.setCaseId(item.getId());
        result.setCategory(item.getCategory());
        result.setQuestion(item.getQuestion());
        result.setExpectedIntent(item.getExpectedIntent());
        result.setExpectedTool(item.getExpectedTool());
        result.setActualIntent(actualIntent);
        result.setActualTool(actualTool);
        result.setPassed(passed ? "1" : "0");
        result.setToolSuccess(toolSuccess ? "1" : "0");
        result.setActualAnswer(answer);
        result.setDurationMs(durationMs);
        result.setErrorMessage(errorMessage);
        result.setCreateTime(new Date());
        return result;
    }

    private boolean hitKeyword(String expectedKeywords, List<KnowledgeSearchResult> results)
    {
        if (StringUtils.isEmpty(expectedKeywords) || results.isEmpty())
        {
            return false;
        }
        String text = results.get(0).getDocumentTitle() + " " + results.get(0).getContent();
        return Arrays.stream(expectedKeywords.split("[,，]"))
                .map(String::trim)
                .filter(StringUtils::isNotEmpty)
                .anyMatch(text::contains);
    }

    private boolean hitKeyword(String expectedKeywords, String text)
    {
        if (StringUtils.isEmpty(expectedKeywords) || StringUtils.isEmpty(text))
        {
            return false;
        }
        return Arrays.stream(expectedKeywords.split("[,，]"))
                .map(String::trim)
                .filter(StringUtils::isNotEmpty)
                .anyMatch(text::contains);
    }

    private int elapsed(long started)
    {
        return (int) Math.min(Integer.MAX_VALUE, System.currentTimeMillis() - started);
    }

    private BigDecimal percent(int part, int total)
    {
        if (total <= 0)
        {
            return BigDecimal.ZERO.setScale(2, RoundingMode.HALF_UP);
        }
        return BigDecimal.valueOf(part * 100D / total).setScale(2, RoundingMode.HALF_UP);
    }

    private void copyRun(AgentEvalRun source, AgentEvalRunView target)
    {
        target.setId(source.getId());
        target.setRunNo(source.getRunNo());
        target.setMode(source.getMode());
        target.setTotal(source.getTotal());
        target.setPassed(source.getPassed());
        target.setIntentAccuracy(source.getIntentAccuracy());
        target.setToolAccuracy(source.getToolAccuracy());
        target.setRagHitRate(source.getRagHitRate());
        target.setPassRate(source.getPassRate());
        target.setCreateBy(source.getCreateBy());
        target.setCreateTime(source.getCreateTime());
    }

    private String truncate(String value, int max)
    {
        return value == null || value.length() <= max ? value : value.substring(0, max) + "...";
    }
}
