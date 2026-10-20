package com.ruoyi.module.stats.domain;

import lombok.Data;

/**
 * 预约趋势点。
 *
 * @author itjialu
 */
@Data
public class ReservationTrendPoint
{
    private String date;

    private Long total;

    private Long approved;

    private Long completed;

    private Long noShow;
}
