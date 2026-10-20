package com.ruoyi.module.safety.service;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.ruoyi.module.safety.domain.SafetyQuestionRequest;
import com.ruoyi.module.safety.domain.SafetyQuestionView;

/**
 * 安全题库Service。
 *
 * @author itjialu
 */
public interface ISafetyQuestionService
{
    IPage<SafetyQuestionView> pageQuestion(Page<?> page, QueryWrapper<?> queryWrapper);

    SafetyQuestionView selectQuestionView(Long id);

    int insertQuestion(SafetyQuestionRequest request);

    int updateQuestion(SafetyQuestionRequest request);

    int deleteQuestionByIds(Long[] ids);
}
