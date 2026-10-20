package com.ruoyi.module.stats.domain;

import lombok.Data;

/**
 * 看板概览指标。
 *
 * @author itjialu
 */
@Data
public class StatsOverview
{
    private String scopeLabel;

    private Long totalReservations;

    private Long todayReservations;

    private Long pendingReservations;

    private Long approvedReservations;

    private Long completedReservations;

    private Long noShowReservations;

    private Long violationCount;

    private Long totalRooms;

    private Double noShowRate;

    private Double examPassRate;
}
