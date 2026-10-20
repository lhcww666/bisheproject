package com.ruoyi.module.stats.service;

import com.ruoyi.module.stats.domain.StatsDashboard;

/**
 * 实验室数据看板Service。
 *
 * @author itjialu
 */
public interface IStatsService
{
    StatsDashboard getDashboard(int days, String startDate, String endDate);
}
