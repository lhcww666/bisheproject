package com.ruoyi.module.reservation.service;

import java.util.List;
import java.util.Date;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.IService;
import com.ruoyi.module.reservation.domain .Reservation;
import com.ruoyi.module.reservation.domain.ReservationView;
import com.ruoyi.module.room.domain.Room;

/**
 * 实验室预约Service接口
 *
 * @author itjialu
 * @date 2026-10-16
 */
public interface IReservationService extends IService<Reservation> {
    public IPage<Reservation> pageReservation(Page<Reservation> page, QueryWrapper<Reservation> queryWrapper);

    /**
     * 提交预约申请（走完整业务规则校验，申请人取自登录上下文）
     *
     * @param reservation 预约申请
     * @return 创建后的预约（含预约编号、状态、是否需要审核）
     */
    public Reservation applyReservation(Reservation reservation);

    /**
     * 取消本人预约
     *
     * @param id 预约主键
     * @return 结果
     */
    public int cancelReservation(Long id);

    /**
     * 分页查询本人预约
     *
     * @param page      分页参数
     * @param status    预约状态，可为空
     * @param startTime 开始时间下限，可为空
     * @param endTime   结束时间上限，可为空
     * @return 分页结果
     */
    public IPage<ReservationView> pageMyReservation(Page<Reservation> page, String status, Date startTime, Date endTime);

    /**
     * 查询指定时段可用的实验室
     *
     * @param startTime        开始时间
     * @param endTime          结束时间
     * @param participantCount 参与人数，可为空
     * @return 可用实验室列表
     */
    public List<Room> selectAvailableRooms(Date startTime, Date endTime, Long participantCount);

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
