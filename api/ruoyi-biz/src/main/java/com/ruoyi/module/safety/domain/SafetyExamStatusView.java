package com.ruoyi.module.safety.domain;

import java.util.Date;

import com.fasterxml.jackson.annotation.JsonFormat;

import lombok.Data;

/**
 * 学生安全考试状态。
 *
 * @author itjialu
 */
@Data
public class SafetyExamStatusView
{
    private Boolean hasValidPass;

    private Integer latestScore;

    private Boolean latestPassed;

    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private Date latestExamTime;

    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private Date latestValidUntil;

    private Long inProgressRecordId;

    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private Date inProgressDeadline;

    private Boolean canStart;

    private Integer passScore;

    private Integer durationMinutes;
}
