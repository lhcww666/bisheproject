package com.ruoyi.module.reservation.domain;

import lombok.Data;

/**
 * 结束实验请求。
 *
 * @author itjialu
 */
@Data
public class ReservationFinishRequest
{
    /** 实际参与人数，可为空 */
    private Long actualParticipantCount;

    /** 结束说明，可为空 */
    private String remark;
}
