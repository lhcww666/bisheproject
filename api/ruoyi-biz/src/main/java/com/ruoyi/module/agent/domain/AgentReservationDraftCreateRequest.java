package com.ruoyi.module.agent.domain;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;
import lombok.Data;

import java.util.List;

/**
 * Agent 创建预约草稿请求。
 */
@Data
public class AgentReservationDraftCreateRequest
{
    @NotNull(message = "会话ID不能为空")
    private Long sessionId;

    @NotNull(message = "实验室不能为空")
    @Positive(message = "实验室ID必须大于0")
    private Long roomId;

    @NotBlank(message = "实验名称不能为空")
    @Size(max = 100, message = "实验名称不能超过100个字符")
    private String experimentName;

    @NotBlank(message = "开始时间不能为空")
    private String startTime;

    @NotBlank(message = "结束时间不能为空")
    private String endTime;

    @NotNull(message = "参与人数不能为空")
    @Min(value = 1, message = "参与人数必须大于0")
    private Long participantCount;

    private List<Long> deviceIds;

    @Size(max = 500, message = "备注不能超过500个字符")
    private String remark;
}
