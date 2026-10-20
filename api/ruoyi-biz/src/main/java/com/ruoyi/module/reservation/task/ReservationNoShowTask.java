package com.ruoyi.module.reservation.task;

import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import com.ruoyi.module.reservation.service.IReservationLifecycleService;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

/**
 * 预约爽约扫描任务。每 5 分钟检查一次已通过但超过签到时限的预约。
 *
 * @author itjialu
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class ReservationNoShowTask
{
    private final IReservationLifecycleService reservationLifecycleService;

    @Scheduled(initialDelay = 60_000L, fixedDelay = 300_000L)
    public void scanNoShowReservations()
    {
        try
        {
            int count = reservationLifecycleService.markNoShowBatch();
            if (count > 0)
            {
                log.info("预约爽约扫描完成，处理 {} 条预约", count);
            }
        }
        catch (Exception e)
        {
            log.error("预约爽约扫描失败", e);
        }
    }
}
