package com.ruoyi.module.safety.domain;

import java.util.Date;

import com.fasterxml.jackson.annotation.JsonFormat;

import lombok.Data;

/**
 * 交卷结果。
 *
 * @author itjialu
 */
@Data
public class SafetyExamResultView
{
    private Long recordId;

    private Integer score;

    private Boolean passed;

    private Integer correctCount;

    private Integer totalQuestions;

    private Integer passScore;

    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private Date validUntil;
}
