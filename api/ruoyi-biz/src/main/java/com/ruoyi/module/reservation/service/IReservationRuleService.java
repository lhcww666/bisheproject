package com.ruoyi.module.reservation.service;

import java.util.Date;
import java.util.List;

import com.ruoyi.module.reservation.domain.Reservation;
import com.ruoyi.module.room.domain.Room;

/**
 * 实验室预约业务规则校验Service接口
 *
 * <p>把"能不能约"的规则集中在这里，供普通接口与后续智能助手的受控工具共同复用，
 * 避免同一个规则在多处实现、出现口径不一致。</p>
 *
 * @author itjialu
 */
public interface IReservationRuleService
{
    /**
     * 校验一次预约申请是否满足全部业务规则
     *
     * @param userId      申请人（取自登录上下文，不信任请求参数）
     * @param reservation 预约申请
     * @return 预约对应的实验室
     */
    public Room checkApply(Long userId, Reservation reservation);

    /**
     * 校验预约是否可以取消
     *
     * @param reservation 预约
     * @param userId      当前登录用户
     */
    public void checkCancel(Reservation reservation, Long userId);

    /**
     * 校验实验室在指定时段是否已被占用
     *
     * @param roomId    实验室主键
     * @param startTime 开始时间
     * @param endTime   结束时间
     * @param excludeId 需要排除的预约主键（修改、审核时排除自身）
     */
    public void checkConflict(Long roomId, Date startTime, Date endTime, Long excludeId);

    /**
     * 审核通过前的冲突校验：只与"已通过"的预约比较
     *
     * <p>申请阶段会与"待审核 + 已通过"比较，避免同一条时段被多次排队；
     * 审核阶段若也把待审核算作占用，两条排队申请就会互相阻塞，因此这里只比已通过的预约。</p>
     *
     * @param roomId    实验室主键
     * @param startTime 开始时间
     * @param endTime   结束时间
     * @param excludeId 需要排除的预约主键
     */
    public void checkApprovedConflict(Long roomId, Date startTime, Date endTime, Long excludeId);

    /**
     * 查询指定时段可用的实验室
     *
     * @param startTime        开始时间
     * @param endTime          结束时间
     * @param participantCount 参与人数，可为空
     * @return 满足开放时间、状态、容量与冲突条件的实验室
     */
    public List<Room> selectAvailableRooms(Date startTime, Date endTime, Long participantCount);

    /**
     * 实验室是否需要教师审核：安全等级达到中风险及以上时为 true
     *
     * @param room 实验室
     * @return 是否需要审核
     */
    public boolean isReviewRequired(Room room);

    /**
     * 查询用户当前信用分
     *
     * @param userId 用户主键
     * @return 当前信用分
     */
    public long currentCreditScore(Long userId);
}
