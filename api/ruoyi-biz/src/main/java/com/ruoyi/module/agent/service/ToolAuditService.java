package com.ruoyi.module.agent.service;

import java.util.Date;

import org.springframework.stereotype.Service;

import com.ruoyi.module.agent.domain.ToolCallLog;
import com.ruoyi.module.agent.domain.ToolCallView;
import com.ruoyi.module.agent.mapper.ToolCallLogMapper;

import lombok.RequiredArgsConstructor;

/**
 * Agent 工具调用审计。
 *
 * <p>每次工具调用都会写入 tool_call_log，记录工具名、参数、结果、耗时和成功状态。
 * 参数和结果会做长度限制，避免大结果撑爆数据库；前端也会收到一份摘要用于展开查看。</p>
 *
 * @author itjialu
 */
@Service
@RequiredArgsConstructor
public class ToolAuditService
{
    private final ToolCallLogMapper toolCallLogMapper;

    /**
     * 记录一次工具调用。
     *
     * @param toolName  模型选择的工具名
     * @param arguments 模型传入的参数摘要
     * @param result    工具返回结果摘要
     * @param success   是否成功
     * @param costMs    工具执行耗时
     */
    public void record(String toolName, String arguments, String result, boolean success, long costMs)
    {
        AgentContext.Context context = AgentContext.current();
        ToolCallLog log = new ToolCallLog();
        log.setSessionId(context == null ? null : context.getSessionId());
        log.setUserId(context == null ? null : context.getUserId());
        log.setToolName(toolName);
        // 限制审计字段长度，避免模型或异常把超长文本直接写入数据库。
        log.setArguments(truncate(arguments, 4000));
        log.setResult(truncate(result, 8000));
        log.setSuccess(success ? "1" : "0");
        log.setCostMs((int) Math.min(Integer.MAX_VALUE, costMs));
        log.setCreateTime(new Date());
        toolCallLogMapper.insert(log);

        ToolCallView view = new ToolCallView();
        view.setToolName(toolName);
        view.setArguments(log.getArguments());
        view.setResult(log.getResult());
        view.setSuccess(success);
        view.setCostMs(log.getCostMs());
        // 同步调用时 Context 一定存在；如果未来改成流式调用，仍可用数据库日志追溯。
        if (context != null)
        {
            context.getToolCalls().add(view);
            context.getToolCallLogIds().add(log.getId());
        }
    }

    private String truncate(String value, int max)
    {
        if (value == null || value.length() <= max)
        {
            return value;
        }
        return value.substring(0, max) + "...";
    }
}
