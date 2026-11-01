package com.ruoyi.module.agent.domain;

import lombok.AllArgsConstructor;
import lombok.Data;

/**
 * 提供给 Agent 的设备摘要。
 */
@Data
@AllArgsConstructor
public class AgentDeviceSummary
{
    private Long id;

    private String deviceCode;

    private String deviceName;

    private String deviceType;

    private Long quantity;

    private String requiresTraining;
}
