package com.ruoyi.module.stats.domain;

import lombok.Data;

/**
 * 预约状态分布。
 *
 * @author itjialu
 */
@Data
public class StatusCount
{
    private String status;

    private String label;

    private Long count;
}
