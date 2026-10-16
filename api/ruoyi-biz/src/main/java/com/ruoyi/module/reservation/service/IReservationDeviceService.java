package com.ruoyi.module.reservation.service;

import java.util.List;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.IService;
import com.ruoyi.module.reservation.domain .ReservationDevice;

/**
 * 预约设备关联Service接口
 *
 * @author itjialu
 * @date 2026-10-16
 */
public interface IReservationDeviceService extends IService<ReservationDevice> {
    public IPage<ReservationDevice> pageReservationDevice(Page<ReservationDevice> page, QueryWrapper<ReservationDevice> queryWrapper);

    /**
     * 查询预约设备关联
     *
     * @param id 预约设备关联主键
     * @return 预约设备关联
     */
    public ReservationDevice selectReservationDeviceById(Long id);

    /**
     * 新增预约设备关联
     *
     * @param reservationDevice 预约设备关联
     * @return 结果
     */
    public int insertReservationDevice(ReservationDevice reservationDevice);

    /**
     * 修改预约设备关联
     *
     * @param reservationDevice 预约设备关联
     * @return 结果
     */
    public int updateReservationDevice(ReservationDevice reservationDevice);

    /**
     * 批量删除预约设备关联
     *
     * @param ids 需要删除的预约设备关联主键集合
     * @return 结果
     */
    public int deleteReservationDeviceByIds(Long[] ids);

    /**
     * 删除预约设备关联信息
     *
     * @param id 预约设备关联主键
     * @return 结果
     */
    public int deleteReservationDeviceById(Long id);

    /**
     * 导入预约设备关联数据
     *
     * @param list            数据列表
     * @param isUpdateSupport 是否更新支持，如果已存在，则进行更新数据
     * @param operName        操作用户
     * @return 结果
     */
    public String importReservationDevice(
    List<ReservationDevice> list, int titleNum, Boolean
    isUpdateSupport,
    String operName);
}
