package com.ruoyi.module.stats.domain;

import lombok.Data;

/**
 * 违规趋势点。
 *
 * @author itjialu
 */
@Data
public class ViolationTrendPoint
{
    private String date;

    private Long count;

    private Long creditChange;
}
