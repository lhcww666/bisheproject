package com.ruoyi.module.reservation.domain;

import jakarta.validation.constraints.NotNull;
import lombok.Data;

/**
 * 预约审核/弃审请求。
 *
 * @author itjialu
 */
@Data
public class ReservationStatusRequest
{
    @NotNull(message = "预约ID不能为空")
    private Long id;

    /** APPROVE / REJECT / UNAUDIT */
    private String action;

    private String comment;
}
