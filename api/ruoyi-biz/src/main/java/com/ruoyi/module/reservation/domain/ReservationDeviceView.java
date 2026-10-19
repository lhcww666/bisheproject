package com.ruoyi.module.reservation.domain;

import lombok.Data;

/**
 * 预约设备明细。
 */
@Data
public class ReservationDeviceView
{
    private Long id;

    private Long deviceId;

    private String deviceCode;

    private String deviceName;

    private Long quantity;
}
