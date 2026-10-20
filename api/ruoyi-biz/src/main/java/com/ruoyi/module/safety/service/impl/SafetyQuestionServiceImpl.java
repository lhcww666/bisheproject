package com.ruoyi.module.safety.service.impl;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.concurrent.ThreadLocalRandom;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.ruoyi.common.exception.ServiceException;
import com.ruoyi.common.utils.DateUtils;
import com.ruoyi.common.utils.SecurityUtils;
import com.ruoyi.common.utils.StringUtils;
import com.ruoyi.module.safety.domain.SafetyQuestion;
import com.ruoyi.module.safety.domain.SafetyQuestionRequest;
import com.ruoyi.module.safety.domain.SafetyQuestionView;
import com.ruoyi.module.safety.domain.SafetyExamAnswer;
import com.ruoyi.module.safety.mapper.SafetyQuestionMapper;
import com.ruoyi.module.safety.mapper.SafetyExamAnswerMapper;
import com.ruoyi.module.safety.service.ISafetyQuestionService;

import lombok.RequiredArgsConstructor;

/**
 * 安全题库Service实现。
 *
 * @author itjialu
 */
@Service
@RequiredArgsConstructor
public class SafetyQuestionServiceImpl implements ISafetyQuestionService
{
    private static final String TYPE_SINGLE = "SINGLE";
    private static final String TYPE_MULTIPLE = "MULTIPLE";
    private static final String TYPE_JUDGE = "JUDGE";

    private final SafetyQuestionMapper safetyQuestionMapper;

    private final ObjectMapper objectMapper;

    private final SafetyExamAnswerMapper safetyExamAnswerMapper;

    @Override
    public IPage<SafetyQuestionView> pageQuestion(Page<?> page, QueryWrapper<?> queryWrapper)
    {
        @SuppressWarnings("unchecked")
        Page<SafetyQuestion> questionPage = (Page<SafetyQuestion>) page;
        @SuppressWarnings("unchecked")
        QueryWrapper<SafetyQuestion> questionQuery = (QueryWrapper<SafetyQuestion>) queryWrapper;
        IPage<SafetyQuestion> pageData = safetyQuestionMapper.selectPage(questionPage, questionQuery);
        Page<SafetyQuestionView> viewPage = new Page<>(pageData.getCurrent(), pageData.getSize(), pageData.getTotal());
        List<SafetyQuestionView> views = new ArrayList<>();
        for (SafetyQuestion question : pageData.getRecords())
        {
            views.add(toView(question));
        }
        viewPage.setRecords(views);
        return viewPage;
    }

    @Override
    public SafetyQuestionView selectQuestionView(Long id)
    {
        SafetyQuestion question = safetyQuestionMapper.selectById(id);
        return question == null ? null : toView(question);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public int insertQuestion(SafetyQuestionRequest request)
    {
        validate(request);
        SafetyQuestion question = toEntity(request);
        question.setQuestionCode(StringUtils.isEmpty(request.getQuestionCode())
                ? generateQuestionCode() : request.getQuestionCode().trim());
        question.setStatus(StringUtils.isEmpty(request.getStatus()) ? "0" : request.getStatus());
        question.setCreateBy(SecurityUtils.getUsername());
        question.setCreateTime(DateUtils.getNowDate());
        return safetyQuestionMapper.insert(question);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public int updateQuestion(SafetyQuestionRequest request)
    {
        if (request.getId() == null)
        {
            throw new ServiceException("题目ID不能为空");
        }
        validate(request);
        SafetyQuestion existing = safetyQuestionMapper.selectById(request.getId());
        if (existing == null)
        {
            throw new ServiceException("题目不存在");
        }
        SafetyQuestion question = toEntity(request);
        question.setId(request.getId());
        question.setQuestionCode(StringUtils.isEmpty(request.getQuestionCode())
                ? existing.getQuestionCode() : request.getQuestionCode().trim());
        question.setStatus(StringUtils.isEmpty(request.getStatus()) ? existing.getStatus() : request.getStatus());
        question.setUpdateBy(SecurityUtils.getUsername());
        question.setUpdateTime(DateUtils.getNowDate());
        return safetyQuestionMapper.updateById(question);
    }

    @Override
    public int deleteQuestionByIds(Long[] ids)
    {
        if (ids == null || ids.length == 0)
        {
            throw new ServiceException("请选择要删除的题目");
        }
        for (Long id : ids)
        {
            QueryWrapper<SafetyExamAnswer> query = new QueryWrapper<>();
            query.eq("question_id", id);
            if (safetyExamAnswerMapper.selectCount(query) > 0)
            {
                throw new ServiceException("题目已被考试使用，不能删除；可以改为停用");
            }
        }
        return safetyQuestionMapper.deleteByIds(List.of(ids));
    }

    private void validate(SafetyQuestionRequest request)
    {
        if (request == null)
        {
            throw new ServiceException("题目不能为空");
        }
        String type = request.getQuestionType() == null ? "" : request.getQuestionType().toUpperCase(Locale.ROOT);
        if (!TYPE_SINGLE.equals(type) && !TYPE_MULTIPLE.equals(type) && !TYPE_JUDGE.equals(type))
        {
            throw new ServiceException("题型只能是单选、多选或判断");
        }
        if (request.getOptions() == null || request.getOptions().size() < 2)
        {
            throw new ServiceException("至少需要两个选项");
        }
        for (String option : request.getOptions())
        {
            if (StringUtils.isEmpty(option))
            {
                throw new ServiceException("选项内容不能为空");
            }
        }
        if (request.getAnswer() == null || request.getAnswer().isEmpty())
        {
            throw new ServiceException("请选择正确答案");
        }
        if ((TYPE_SINGLE.equals(type) || TYPE_JUDGE.equals(type)) && request.getAnswer().size() != 1)
        {
            throw new ServiceException("单选题和判断题只能有一个正确答案");
        }
        for (String answer : request.getAnswer())
        {
            String normalized = answer == null ? "" : answer.trim().toUpperCase(Locale.ROOT);
            if (normalized.length() != 1)
            {
                throw new ServiceException("答案必须是选项 A/B/C/D 中的值");
            }
            int index = normalized.charAt(0) - 'A';
            if (index < 0 || index >= request.getOptions().size())
            {
                throw new ServiceException("答案必须是选项 A/B/C/D 中的值");
            }
        }
        if (request.getScore() == null || request.getScore() <= 0)
        {
            throw new ServiceException("分值必须大于 0");
        }
        if (request.getSafetyLevel() == null || request.getSafetyLevel() < 1 || request.getSafetyLevel() > 3)
        {
            throw new ServiceException("安全等级只能是 1、2、3");
        }
    }

    private SafetyQuestion toEntity(SafetyQuestionRequest request)
    {
        SafetyQuestion question = new SafetyQuestion();
        question.setQuestionType(request.getQuestionType().toUpperCase(Locale.ROOT));
        question.setContent(request.getContent());
        question.setOptionsJson(writeJson(request.getOptions()));
        question.setAnswerJson(writeJson(normalizeAnswers(request.getAnswer())));
        question.setScore(request.getScore());
        question.setSafetyLevel(request.getSafetyLevel());
        question.setRemark(request.getRemark());
        return question;
    }

    private SafetyQuestionView toView(SafetyQuestion question)
    {
        SafetyQuestionView view = new SafetyQuestionView();
        view.setId(question.getId());
        view.setQuestionCode(question.getQuestionCode());
        view.setQuestionType(question.getQuestionType());
        view.setContent(question.getContent());
        view.setOptions(readStringList(question.getOptionsJson()));
        view.setAnswer(readStringList(question.getAnswerJson()));
        view.setScore(question.getScore());
        view.setSafetyLevel(question.getSafetyLevel());
        view.setStatus(question.getStatus());
        view.setRemark(question.getRemark());
        view.setCreateTime(question.getCreateTime());
        return view;
    }

    private List<String> normalizeAnswers(List<String> answers)
    {
        List<String> result = new ArrayList<>();
        for (String answer : answers)
        {
            result.add(answer.trim().toUpperCase(Locale.ROOT));
        }
        return result;
    }

    private String writeJson(Object value)
    {
        try
        {
            return objectMapper.writeValueAsString(value);
        }
        catch (Exception e)
        {
            throw new ServiceException("题目选项格式不正确");
        }
    }

    private List<String> readStringList(String json)
    {
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

    private String generateQuestionCode()
    {
        return "Q" + System.currentTimeMillis() + ThreadLocalRandom.current().nextInt(100, 1000);
    }
}
