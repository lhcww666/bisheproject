package com.ruoyi.module.safety.domain;

import java.util.List;

import lombok.Data;

/**
 * 考试题目视图，不包含正确答案。
 *
 * @author itjialu
 */
@Data
public class SafetyExamQuestionView
{
    private Long id;

    private String questionType;

    private String content;

    private List<String> options;

    private Integer score;

    private Integer safetyLevel;
}
