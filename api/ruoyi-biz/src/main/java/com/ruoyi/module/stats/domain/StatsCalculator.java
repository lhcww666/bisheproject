package com.ruoyi.module.stats.domain;

import java.time.LocalDate;
import java.time.ZoneId;
import java.util.Date;

/**
 * 统计计算工具，便于脱离数据库做单元测试。
 *
 * @author itjialu
 */
public final class StatsCalculator
{
    private StatsCalculator()
    {
    }

    /**
     * 计算两个时间区间重叠的小时数。
     */
    public static double overlapHours(Date firstStart, Date firstEnd, Date secondStart, Date secondEnd)
    {
        if (firstStart == null || firstEnd == null || secondStart == null || secondEnd == null)
        {
            return 0D;
        }
        long start = Math.max(firstStart.getTime(), secondStart.getTime());
        long end = Math.min(firstEnd.getTime(), secondEnd.getTime());
        if (end <= start)
        {
            return 0D;
        }
        return (end - start) / 3_600_000D;
    }

    public static double percentage(double part, double total)
    {
        if (total <= 0)
        {
            return 0D;
        }
        return Math.round(part * 10_000D / total) / 100D;
    }

    public static LocalDate toLocalDate(Date date)
    {
        return date == null ? null : date.toInstant().atZone(ZoneId.systemDefault()).toLocalDate();
    }
}
