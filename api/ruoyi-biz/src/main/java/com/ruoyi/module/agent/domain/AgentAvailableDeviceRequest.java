package com.ruoyi.module.agent.domain;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import lombok.Data;

/**
 * Agent 查询实验室可用设备请求。
 */
@Data
public class AgentAvailableDeviceRequest
{
    @NotNull(message = "实验室ID不能为空")
    @Positive(message = "实验室ID必须大于0")
    private Long roomId;
}
