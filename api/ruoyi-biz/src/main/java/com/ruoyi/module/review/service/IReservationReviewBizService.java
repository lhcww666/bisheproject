package com.ruoyi.module.review.service;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;

import com.ruoyi.module.reservation.domain.Reservation;
import com.ruoyi.module.review.domain.ReservationReviewRequest;

/**
 * 预约审核流程Service接口
 *
 * @author itjialu
 */
public interface IReservationReviewBizService
{
    /**
     * 审核预约：通过或驳回，并写入审核记录、流转预约状态
     *
     * @param reservationId 预约主键
     * @param request       审核结果与意见
     */
    public void review(Long reservationId, ReservationReviewRequest request);

    /**
     * 分页查询待审核预约
     *
     * @param page 分页参数
     * @return 待审核预约
     */
    public IPage<Reservation> pagePending(Page<Reservation> page);
}
