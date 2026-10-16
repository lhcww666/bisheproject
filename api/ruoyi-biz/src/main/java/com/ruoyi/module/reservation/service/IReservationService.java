package com.ruoyi.module.reservation.service;

import java.util.List;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.IService;
import com.ruoyi.module.reservation.domain .Reservation;

/**
 * 实验室预约Service接口
 *
 * @author itjialu
 * @date 2026-10-16
 */
public interface IReservationService extends IService<Reservation> {
    public IPage<Reservation> pageReservation(Page<Reservation> page, QueryWrapper<Reservation> queryWrapper);

    /**
     * 查询实验室预约
     *
     * @param id 实验室预约主键
     * @return 实验室预约
     */
    public Reservation selectReservationById(Long id);

    /**
     * 新增实验室预约
     *
     * @param reservation 实验室预约
     * @return 结果
     */
    public int insertReservation(Reservation reservation);

    /**
     * 修改实验室预约
     *
     * @param reservation 实验室预约
     * @return 结果
     */
    public int updateReservation(Reservation reservation);

    /**
     * 批量删除实验室预约
     *
     * @param ids 需要删除的实验室预约主键集合
     * @return 结果
     */
    public int deleteReservationByIds(Long[] ids);

    /**
     * 删除实验室预约信息
     *
     * @param id 实验室预约主键
     * @return 结果
     */
    public int deleteReservationById(Long id);

    /**
     * 导入实验室预约数据
     *
     * @param list            数据列表
     * @param isUpdateSupport 是否更新支持，如果已存在，则进行更新数据
     * @param operName        操作用户
     * @return 结果
     */
    public String importReservation(
    List<Reservation> list, int titleNum, Boolean
    isUpdateSupport,
    String operName);
}
