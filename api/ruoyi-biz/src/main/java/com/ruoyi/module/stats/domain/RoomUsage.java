package com.ruoyi.module.stats.domain;

import lombok.Data;

/**
 * 实验室使用率。
 *
 * @author itjialu
 */
@Data
public class RoomUsage
{
    private String roomCode;

    private String roomName;

    private Double bookedHours;

    private Double openHours;

    private Double usageRate;

    private Long reservationCount;
}
