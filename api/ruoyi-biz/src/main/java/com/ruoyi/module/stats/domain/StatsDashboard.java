package com.ruoyi.module.stats.domain;

import java.util.List;

import lombok.Data;

/**
 * 数据看板聚合结果。
 *
 * @author itjialu
 */
@Data
public class StatsDashboard
{
    private StatsOverview overview;

    private List<ReservationTrendPoint> reservationTrend;

    private List<StatusCount> statusDistribution;

    private List<RoomUsage> roomUsage;

    private List<ViolationTrendPoint> violationTrend;
}
