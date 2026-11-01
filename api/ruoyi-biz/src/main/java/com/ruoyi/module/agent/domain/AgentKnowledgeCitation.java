package com.ruoyi.module.agent.domain;

import lombok.Data;

/**
 * 安全知识回答引用。
 */
@Data
public class AgentKnowledgeCitation
{
    private String documentId;

    private String title;

    private String source;

    private String snippet;

    private Double score;
}
