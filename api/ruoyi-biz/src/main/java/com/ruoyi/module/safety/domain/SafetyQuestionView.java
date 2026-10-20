package com.ruoyi.module.safety.domain;

import java.util.Date;
import java.util.List;

import com.fasterxml.jackson.annotation.JsonFormat;

import lombok.Data;

/**
 * 题库展示对象。
 *
 * @author itjialu
 */
@Data
public class SafetyQuestionView
{
    private Long id;

    private String questionCode;

    private String questionType;

    private String content;

    private List<String> options;

    private List<String> answer;

    private Integer score;

    private Integer safetyLevel;

    private String status;

    private String remark;

    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private Date createTime;
}
