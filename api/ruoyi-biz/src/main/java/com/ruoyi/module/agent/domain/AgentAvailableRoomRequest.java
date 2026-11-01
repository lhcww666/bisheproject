package com.ruoyi.module.agent.domain;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import lombok.Data;

/**
 * Agent 查询可用实验室请求。
 */
@Data
public class AgentAvailableRoomRequest
{
    @NotBlank(message = "开始时间不能为空")
    private String startTime;

    @NotBlank(message = "结束时间不能为空")
    private String endTime;

    @Min(value = 1, message = "参与人数必须大于0")
    private Long participantCount;
}
