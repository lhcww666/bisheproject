package com.ruoyi.module.stats.domain;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.util.Date;

import org.junit.jupiter.api.Test;

/**
 * 统计计算工具测试。
 *
 * @author itjialu
 */
class StatsCalculatorTest
{
    @Test
    void overlapHoursClipsToRange()
    {
        Date dayStart = new Date(1_000_000L);
        Date dayEnd = new Date(1_000_000L + 4 * 3_600_000L);
        Date bookingStart = new Date(1_000_000L + 3 * 3_600_000L);
        Date bookingEnd = new Date(1_000_000L + 6 * 3_600_000L);
        assertEquals(1D, StatsCalculator.overlapHours(bookingStart, bookingEnd, dayStart, dayEnd), 0.001D);
    }

    @Test
    void percentageHandlesZeroTotal()
    {
        assertEquals(0D, StatsCalculator.percentage(3, 0), 0.001D);
        assertEquals(50D, StatsCalculator.percentage(1, 2), 0.001D);
    }
}
