package com.ruoyi.module.safety.domain;

import java.util.Date;
import java.util.List;

import com.fasterxml.jackson.annotation.JsonFormat;

import lombok.Data;

/**
 * 开始/继续考试返回的试卷信息。
 *
 * @author itjialu
 */
@Data
public class SafetyExamStartView
{
    private Long recordId;

    private String paperNo;

    private Integer passScore;

    private Integer durationMinutes;

    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private Date startedAt;

    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private Date deadline;

    private List<SafetyExamQuestionView> questions;
}
