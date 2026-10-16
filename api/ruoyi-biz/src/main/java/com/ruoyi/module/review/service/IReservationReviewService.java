package com.ruoyi.module.review.service;

import java.util.List;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.IService;
import com.ruoyi.module.review.domain .ReservationReview;

/**
 * 预约审核Service接口
 *
 * @author itjialu
 * @date 2026-10-16
 */
public interface IReservationReviewService extends IService<ReservationReview> {
    public IPage<ReservationReview> pageReservationReview(Page<ReservationReview> page, QueryWrapper<ReservationReview> queryWrapper);

    /**
     * 查询预约审核
     *
     * @param id 预约审核主键
     * @return 预约审核
     */
    public ReservationReview selectReservationReviewById(Long id);

    /**
     * 新增预约审核
     *
     * @param reservationReview 预约审核
     * @return 结果
     */
    public int insertReservationReview(ReservationReview reservationReview);

    /**
     * 修改预约审核
     *
     * @param reservationReview 预约审核
     * @return 结果
     */
    public int updateReservationReview(ReservationReview reservationReview);

    /**
     * 批量删除预约审核
     *
     * @param ids 需要删除的预约审核主键集合
     * @return 结果
     */
    public int deleteReservationReviewByIds(Long[] ids);

    /**
     * 删除预约审核信息
     *
     * @param id 预约审核主键
     * @return 结果
     */
    public int deleteReservationReviewById(Long id);

    /**
     * 导入预约审核数据
     *
     * @param list            数据列表
     * @param isUpdateSupport 是否更新支持，如果已存在，则进行更新数据
     * @param operName        操作用户
     * @return 结果
     */
    public String importReservationReview(
    List<ReservationReview> list, int titleNum, Boolean
    isUpdateSupport,
    String operName);
}
