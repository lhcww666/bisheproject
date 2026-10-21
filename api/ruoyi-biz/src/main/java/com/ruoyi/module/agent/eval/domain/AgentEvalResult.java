package com.ruoyi.module.agent.eval.domain;

import java.util.Date;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import com.fasterxml.jackson.annotation.JsonFormat;

import lombok.Data;

/**
 * Agent 单条评测结果。
 *
 * @author itjialu
 */
@Data
@TableName("agent_eval_result")
public class AgentEvalResult
{
    @TableId(type = IdType.AUTO)
    private Long id;

    private Long runId;

    private Long caseId;

    private String category;

    private String question;

    private String expectedIntent;

    private String expectedTool;

    private String actualIntent;

    private String actualTool;

    private String passed;

    private String toolSuccess;

    private String actualAnswer;

    private Integer durationMs;

    private String errorMessage;

    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private Date createTime;
}
