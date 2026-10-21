package com.ruoyi.module.agent.audit.domain;

import java.util.Date;

import com.fasterxml.jackson.annotation.JsonFormat;

import lombok.Data;

/**
 * Agent 审计会话视图。
 *
 * @author itjialu
 */
@Data
public class AgentAuditSessionView
{
    private Long id;

    private Long userId;

    private String userName;

    private String title;

    private Long messageCount;

    private Long toolCallCount;

    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private Date createTime;

    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private Date updateTime;
}
