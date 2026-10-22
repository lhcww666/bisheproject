package com.ruoyi.module.reservation.domain;

import lombok.Data;

/**
 * 预约提交时的设备使用数量。
 *
 * @author itjialu
 */
@Data
public class ReservationDeviceUsage
{
    /** 设备主键 */
    private Long deviceId;

    /** 本次预约使用数量 */
    private Long quantity;
}
