package com.ruoyi.module.agent.domain;

import lombok.AllArgsConstructor;
import lombok.Data;

/**
 * Agent Gateway 状态。
 */
@Data
@AllArgsConstructor
public class AgentStatusView
{
    private boolean enabled;

    private String engine;

    private boolean pythonConfigured;

    private boolean pythonAvailable;

    private boolean difyConfigured;

    private String role;
}
