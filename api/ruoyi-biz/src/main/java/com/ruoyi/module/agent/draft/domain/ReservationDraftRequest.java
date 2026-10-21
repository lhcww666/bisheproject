package com.ruoyi.module.agent.draft.domain;

import java.util.List;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Data;

/**
 * 创建预约草稿的请求。
 *
 * @author itjialu
 */
@Data
public class ReservationDraftRequest
{
    @NotNull(message = "实验室不能为空")
    private Long roomId;

    @NotBlank(message = "实验名称不能为空")
    @Size(max = 100, message = "实验名称不能超过100个字符")
    private String experimentName;

    @NotBlank(message = "开始时间不能为空")
    private String startTime;

    @NotBlank(message = "结束时间不能为空")
    private String endTime;

    @NotNull(message = "参与人数不能为空")
    private Integer participantCount;

    private List<Long> deviceIds;

    @Size(max = 500, message = "实验说明不能超过500个字符")
    private String remark;
}
