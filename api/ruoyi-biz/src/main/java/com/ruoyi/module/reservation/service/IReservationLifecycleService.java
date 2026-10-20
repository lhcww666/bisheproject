package com.ruoyi.module.reservation.service;

import java.util.List;

import com.ruoyi.module.reservation.domain.ReservationStatusLog;

/**
 * 预约生命周期服务：签到、完成、爽约和状态日志。
 *
 * @author itjialu
 */
public interface IReservationLifecycleService
{
    void checkIn(Long reservationId);

    void finish(Long reservationId, Long actualParticipantCount, String remark);

    /**
     * 将单条已通过但未签到的预约标记为爽约。
     */
    void markNoShow(Long reservationId);

    /**
     * 定时任务入口：批量处理超时未签到的预约。
     *
     * @return 处理的预约数量
     */
    int markNoShowBatch();

    List<ReservationStatusLog> listStatusLogs(Long reservationId);
}
