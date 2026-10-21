package com.ruoyi.module.agent.eval.domain;

import java.util.Date;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import com.fasterxml.jackson.annotation.JsonFormat;

import lombok.Data;

/**
 * Agent 离线评测用例。
 *
 * @author itjialu
 */
@Data
@TableName("agent_eval_case")
public class AgentEvalCase
{
    @TableId(type = IdType.AUTO)
    private Long id;

    private String caseCode;

    private String category;

    private String question;

    private String expectedIntent;

    private String expectedTool;

    /** RAG 用例期望命中的关键词，逗号分隔。 */
    private String expectedKeywords;

    private String enabled;

    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private Date createTime;
}
