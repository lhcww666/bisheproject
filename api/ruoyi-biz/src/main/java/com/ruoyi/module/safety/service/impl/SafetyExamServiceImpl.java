package com.ruoyi.module.safety.service.impl;

import java.math.BigDecimal;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ThreadLocalRandom;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.ruoyi.common.exception.ServiceException;
import com.ruoyi.common.utils.DateUtils;
import com.ruoyi.common.utils.SecurityUtils;
import com.ruoyi.common.utils.StringUtils;
import com.ruoyi.module.safety.domain.SafetyExamAnswer;
import com.ruoyi.module.safety.domain.SafetyExamConfig;
import com.ruoyi.module.safety.domain.SafetyExamGrading;
import com.ruoyi.module.safety.domain.SafetyExamQuestionView;
import com.ruoyi.module.safety.domain.SafetyExamRecord;
import com.ruoyi.module.safety.domain.SafetyExamResultView;
import com.ruoyi.module.safety.domain.SafetyExamStartView;
import com.ruoyi.module.safety.domain.SafetyExamStatusView;
import com.ruoyi.module.safety.domain.SafetyExamSubmitRequest;
import com.ruoyi.module.safety.domain.SafetyQuestion;
import com.ruoyi.module.safety.mapper.SafetyExamAnswerMapper;
import com.ruoyi.module.safety.mapper.SafetyExamConfigMapper;
import com.ruoyi.module.safety.mapper.SafetyExamRecordMapper;
import com.ruoyi.module.safety.mapper.SafetyQuestionMapper;
import com.ruoyi.module.safety.service.ISafetyExamService;

import lombok.RequiredArgsConstructor;

/**
 * 安全考试Service实现。
 *
 * @author itjialu
 */
@Service
@RequiredArgsConstructor
public class SafetyExamServiceImpl implements ISafetyExamService
{
    private static final String STATUS_IN_PROGRESS = "0";
    private static final String STATUS_SUBMITTED = "1";
    private static final String STATUS_EXPIRED = "2";

    private final SafetyExamRecordMapper safetyExamRecordMapper;

    private final SafetyExamConfigMapper safetyExamConfigMapper;

    private final SafetyQuestionMapper safetyQuestionMapper;

    private final SafetyExamAnswerMapper safetyExamAnswerMapper;

    private final ObjectMapper objectMapper;

    @Override
    public SafetyExamStatusView getStatus(Long studentId)
    {
        SafetyExamConfig config = findActiveConfig();
        SafetyExamRecord latest = findLatestRecord(studentId);
        SafetyExamRecord validPass = findValidPass(studentId);
        SafetyExamRecord inProgress = findInProgress(studentId);
        if (inProgress != null && deadline(inProgress, config).before(new Date()))
        {
            expireRecord(inProgress);
            inProgress = null;
        }

        SafetyExamStatusView view = new SafetyExamStatusView();
        view.setHasValidPass(validPass != null);
        view.setLatestScore(latest == null || latest.getScore() == null ? null : latest.getScore().intValue());
        view.setLatestPassed(latest == null ? null : "1".equals(latest.getPassed()));
        view.setLatestExamTime(latest == null ? null : latest.getExamTime());
        view.setLatestValidUntil(validPass == null ? null : validPass.getValidUntil());
        view.setInProgressRecordId(inProgress == null ? null : inProgress.getId());
        view.setInProgressDeadline(inProgress == null || config == null ? null : deadline(inProgress, config));
        // 无进行中考试可以新开；有进行中考试可以继续；配置存在即可进入考试
        view.setCanStart(config != null);
        view.setPassScore(config == null ? 80 : config.getPassScore());
        view.setDurationMinutes(config == null ? 30 : config.getDurationMinutes());
        return view;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public SafetyExamStartView startExam(Long studentId)
    {
        SafetyExamConfig config = findActiveConfig();
        if (config == null)
        {
            throw new ServiceException("安全考试尚未配置，请联系管理员");
        }
        SafetyExamRecord inProgress = findInProgress(studentId);
        if (inProgress != null)
        {
            if (deadline(inProgress, config).after(new Date()))
            {
                return buildStartView(inProgress, config);
            }
            expireRecord(inProgress);
        }

        QueryWrapper<SafetyQuestion> countQuery = new QueryWrapper<>();
        countQuery.eq("status", "0");
        long available = safetyQuestionMapper.selectCount(countQuery);
        if (available == 0)
        {
            throw new ServiceException("题库为空，请联系管理员录入题目");
        }
        int limit = Math.min(config.getQuestionCount() == null ? 10 : config.getQuestionCount(), (int) available);
        QueryWrapper<SafetyQuestion> questionQuery = new QueryWrapper<>();
        questionQuery.eq("status", "0").last("ORDER BY RAND() LIMIT " + limit);
        List<SafetyQuestion> questions = safetyQuestionMapper.selectList(questionQuery);
        if (questions.isEmpty())
        {
            throw new ServiceException("暂无可用于考试的题目");
        }

        Date now = new Date();
        SafetyExamRecord record = new SafetyExamRecord();
        record.setStudentId(studentId);
        record.setScore(BigDecimal.ZERO);
        record.setPassed("0");
        record.setExamTime(now);
        record.setConfigId(config.getId());
        record.setPaperNo(generatePaperNo());
        record.setStatus(STATUS_IN_PROGRESS);
        record.setCorrectCount(0);
        record.setCreateBy(operator());
        record.setCreateTime(now);
        safetyExamRecordMapper.insert(record);

        for (SafetyQuestion question : questions)
        {
            SafetyExamAnswer answer = new SafetyExamAnswer();
            answer.setRecordId(record.getId());
            answer.setQuestionId(question.getId());
            answer.setCreateBy(operator());
            answer.setCreateTime(now);
            safetyExamAnswerMapper.insert(answer);
        }
        return buildStartView(record, config);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public SafetyExamResultView submitExam(Long studentId, SafetyExamSubmitRequest request)
    {
        if (request == null || request.getRecordId() == null)
        {
            throw new ServiceException("考试记录ID不能为空");
        }
        SafetyExamRecord record = safetyExamRecordMapper.selectById(request.getRecordId());
        if (record == null || !studentId.equals(record.getStudentId()))
        {
            throw new ServiceException("考试记录不存在");
        }
        if (STATUS_SUBMITTED.equals(record.getStatus()))
        {
            throw new ServiceException("本次考试已经交卷，不能重复提交");
        }
        if (STATUS_EXPIRED.equals(record.getStatus()))
        {
            throw new ServiceException("本次考试已过期，请重新开始");
        }
        SafetyExamConfig config = configById(record.getConfigId());
        if (config == null)
        {
            throw new ServiceException("考试配置不存在");
        }
        if (new Date().after(deadline(record, config)))
        {
            expireRecord(record);
            throw new ServiceException("考试已超时，本次成绩无效");
        }

        List<SafetyExamAnswer> answerRows = safetyExamAnswerMapper.selectList(
                new QueryWrapper<SafetyExamAnswer>().eq("record_id", record.getId()));
        Map<Long, SafetyExamAnswer> rowByQuestion = new HashMap<>();
        for (SafetyExamAnswer row : answerRows)
        {
            rowByQuestion.put(row.getQuestionId(), row);
        }
        Map<Long, List<String>> submitted = new LinkedHashMap<>();
        if (request.getAnswers() != null)
        {
            for (SafetyExamSubmitRequest.AnswerItem item : request.getAnswers())
            {
                if (item == null || item.getQuestionId() == null)
                {
                    continue;
                }
                if (!rowByQuestion.containsKey(item.getQuestionId()))
                {
                    throw new ServiceException("提交的题目不属于本次考试");
                }
                submitted.put(item.getQuestionId(), item.getAnswer());
            }
        }

        int totalScore = 0;
        int correctCount = 0;
        Date now = new Date();
        for (SafetyExamAnswer row : answerRows)
        {
            SafetyQuestion question = safetyQuestionMapper.selectById(row.getQuestionId());
            if (question == null)
            {
                continue;
            }
            List<String> correctAnswers = readStringList(question.getAnswerJson());
            List<String> studentAnswers = submitted.getOrDefault(question.getId(), List.of());
            boolean correct = SafetyExamGrading.matches(correctAnswers, studentAnswers);
            int score = correct ? (question.getScore() == null ? 0 : question.getScore()) : 0;
            row.setAnswerJson(writeJson(studentAnswers));
            row.setIsCorrect(correct ? "1" : "0");
            row.setScore(score);
            row.setUpdateBy(operator());
            row.setUpdateTime(now);
            safetyExamAnswerMapper.updateById(row);
            totalScore += score;
            if (correct)
            {
                correctCount++;
            }
        }

        boolean passed = totalScore >= (config.getPassScore() == null ? 80 : config.getPassScore());
        record.setScore(BigDecimal.valueOf(totalScore));
        record.setPassed(passed ? "1" : "0");
        record.setCorrectCount(correctCount);
        record.setSubmitTime(now);
        record.setStatus(STATUS_SUBMITTED);
        record.setValidUntil(passed ? new Date(now.getTime() + config.getValidDays() * 86_400_000L) : null);
        record.setUpdateBy(operator());
        record.setUpdateTime(now);
        safetyExamRecordMapper.updateById(record);

        SafetyExamResultView result = new SafetyExamResultView();
        result.setRecordId(record.getId());
        result.setScore(totalScore);
        result.setPassed(passed);
        result.setCorrectCount(correctCount);
        result.setTotalQuestions(answerRows.size());
        result.setPassScore(config.getPassScore());
        result.setValidUntil(record.getValidUntil());
        return result;
    }

    private SafetyExamStartView buildStartView(SafetyExamRecord record, SafetyExamConfig config)
    {
        List<SafetyExamAnswer> rows = safetyExamAnswerMapper.selectList(
                new QueryWrapper<SafetyExamAnswer>().eq("record_id", record.getId()));
        List<SafetyExamQuestionView> questions = new ArrayList<>();
        for (SafetyExamAnswer row : rows)
        {
            SafetyQuestion question = safetyQuestionMapper.selectById(row.getQuestionId());
            if (question != null)
            {
                questions.add(toQuestionView(question));
            }
        }
        SafetyExamStartView view = new SafetyExamStartView();
        view.setRecordId(record.getId());
        view.setPaperNo(record.getPaperNo());
        view.setPassScore(config.getPassScore());
        view.setDurationMinutes(config.getDurationMinutes());
        view.setStartedAt(record.getExamTime());
        view.setDeadline(deadline(record, config));
        view.setQuestions(questions);
        return view;
    }

    private SafetyExamQuestionView toQuestionView(SafetyQuestion question)
    {
        SafetyExamQuestionView view = new SafetyExamQuestionView();
        view.setId(question.getId());
        view.setQuestionType(question.getQuestionType());
        view.setContent(question.getContent());
        view.setOptions(readStringList(question.getOptionsJson()));
        view.setScore(question.getScore());
        view.setSafetyLevel(question.getSafetyLevel());
        return view;
    }

    private SafetyExamConfig findActiveConfig()
    {
        QueryWrapper<SafetyExamConfig> query = new QueryWrapper<>();
        query.eq("status", "0").orderByDesc("id").last("limit 1");
        return safetyExamConfigMapper.selectOne(query);
    }

    private SafetyExamConfig configById(Long id)
    {
        if (id == null)
        {
            return findActiveConfig();
        }
        return safetyExamConfigMapper.selectById(id);
    }

    private SafetyExamRecord findLatestRecord(Long studentId)
    {
        QueryWrapper<SafetyExamRecord> query = new QueryWrapper<>();
        query.eq("student_id", studentId).orderByDesc("id").last("limit 1");
        return safetyExamRecordMapper.selectOne(query);
    }

    private SafetyExamRecord findValidPass(Long studentId)
    {
        QueryWrapper<SafetyExamRecord> query = new QueryWrapper<>();
        query.eq("student_id", studentId).eq("passed", "1");
        query.and(wrapper -> wrapper.isNull("valid_until").or().ge("valid_until", new Date()));
        query.orderByDesc("id").last("limit 1");
        return safetyExamRecordMapper.selectOne(query);
    }

    private SafetyExamRecord findInProgress(Long studentId)
    {
        QueryWrapper<SafetyExamRecord> query = new QueryWrapper<>();
        query.eq("student_id", studentId).eq("status", STATUS_IN_PROGRESS).orderByDesc("id").last("limit 1");
        return safetyExamRecordMapper.selectOne(query);
    }

    private Date deadline(SafetyExamRecord record, SafetyExamConfig config)
    {
        int minutes = config == null || config.getDurationMinutes() == null ? 30 : config.getDurationMinutes();
        return new Date(record.getExamTime().getTime() + minutes * 60_000L);
    }

    private void expireRecord(SafetyExamRecord record)
    {
        record.setStatus(STATUS_EXPIRED);
        record.setUpdateBy(operator());
        record.setUpdateTime(DateUtils.getNowDate());
        safetyExamRecordMapper.updateById(record);
    }

    private String generatePaperNo()
    {
        return "EXAM" + new SimpleDateFormat("yyyyMMddHHmmss").format(new Date())
                + ThreadLocalRandom.current().nextInt(100, 1000);
    }

    private String writeJson(Object value)
    {
        try
        {
            return objectMapper.writeValueAsString(value);
        }
        catch (Exception e)
        {
            throw new ServiceException("答案格式不正确");
        }
    }

    private List<String> readStringList(String json)
    {
        if (StringUtils.isEmpty(json))
        {
            return List.of();
        }
        try
        {
            return objectMapper.readValue(json, new TypeReference<List<String>>()
            {
            });
        }
        catch (Exception e)
        {
            return List.of();
        }
    }

    private String operator()
    {
        try
        {
            return SecurityUtils.getUsername();
        }
        catch (Exception e)
        {
            return "system";
        }
    }
}
