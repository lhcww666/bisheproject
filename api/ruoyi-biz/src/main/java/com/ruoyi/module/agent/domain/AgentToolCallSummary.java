package com.ruoyi.module.agent.domain;

import lombok.Data;

/**
 * 返回给浏览器的工具执行摘要。
 */
@Data
public class AgentToolCallSummary
{
    private String toolName;

    private String status;

    private String error;
}
