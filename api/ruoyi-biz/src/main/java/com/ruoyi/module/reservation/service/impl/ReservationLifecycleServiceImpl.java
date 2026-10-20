package com.ruoyi.module.reservation.service.impl;

import java.util.Date;
import java.util.List;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.ruoyi.common.exception.ServiceException;
import com.ruoyi.common.utils.DateUtils;
import com.ruoyi.common.utils.SecurityUtils;
import com.ruoyi.common.utils.StringUtils;
import com.ruoyi.module.common.service.ModuleDataScopeService;
import com.ruoyi.module.credit.service.ICreditService;
import com.ruoyi.module.reservation.domain.Reservation;
import com.ruoyi.module.reservation.domain.ReservationStateMachine;
import com.ruoyi.module.reservation.domain.ReservationStatus;
import com.ruoyi.module.reservation.domain.ReservationStatusAction;
import com.ruoyi.module.reservation.domain.ReservationStatusLog;
import com.ruoyi.module.reservation.mapper.ReservationMapper;
import com.ruoyi.module.reservation.mapper.ReservationStatusLogMapper;
import com.ruoyi.module.reservation.service.IReservationLifecycleService;
import com.ruoyi.module.violation.domain.ViolationType;
import com.ruoyi.module.violation.service.IViolationRecordService;

import lombok.RequiredArgsConstructor;

/**
 * 预约生命周期服务实现。
 *
 * @author itjialu
 */
@Service
@RequiredArgsConstructor
public class ReservationLifecycleServiceImpl implements IReservationLifecycleService
{
    /** 允许提前签到的时间 */
    private static final int CHECK_IN_EARLY_MINUTES = 30;

    /** 允许迟到签到的时间 */
    private static final int CHECK_IN_LATE_MINUTES = 30;

    private static final int LATE_CANCEL_MINUTES = 120;

    private static final int NO_SHOW_CREDIT_CHANGE = -5;

    private static final int COMPLETED_CREDIT_CHANGE = 1;

    private final ReservationMapper reservationMapper;

    private final ReservationStatusLogMapper statusLogMapper;

    private final ModuleDataScopeService moduleDataScopeService;

    private final IViolationRecordService violationRecordService;

    private final ICreditService creditService;

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void checkIn(Long reservationId)
    {
        Reservation reservation = requireReservation(reservationId);
        checkOperator(reservation);
        assertTransition(reservation.getStatus(), ReservationStatusAction.CHECK_IN, ReservationStatus.CHECKED_IN,
                "只有已通过的预约可以签到");
        Date now = new Date();
        Date earliest = plusMinutes(reservation.getStartTime(), -CHECK_IN_EARLY_MINUTES);
        Date latest = plusMinutes(reservation.getStartTime(), CHECK_IN_LATE_MINUTES);
        if (now.before(earliest))
        {
            throw new ServiceException("还未到签到时间，开始前 " + CHECK_IN_EARLY_MINUTES + " 分钟开放签到");
        }
        if (now.after(latest))
        {
            throw new ServiceException("已超过签到时间，预约将按爽约处理");
        }
        reservation.setStatus(ReservationStatus.CHECKED_IN);
        reservation.setCheckInTime(now);
        updateReservation(reservation);
        log(reservation, ReservationStatus.APPROVED, ReservationStatus.CHECKED_IN,
                ReservationStatusAction.CHECK_IN, "学生签到");
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void finish(Long reservationId, Long actualParticipantCount, String remark)
    {
        Reservation reservation = requireReservation(reservationId);
        checkOperator(reservation);
        assertTransition(reservation.getStatus(), ReservationStatusAction.FINISH, ReservationStatus.COMPLETED,
                "只有已签到的预约可以结束实验");
        Date now = new Date();
        if (now.before(reservation.getStartTime()))
        {
            throw new ServiceException("实验尚未开始，不能结束");
        }
        Long actualCount = actualParticipantCount == null ? reservation.getParticipantCount()
                : actualParticipantCount;
        if (actualCount == null || actualCount < 1)
        {
            throw new ServiceException("实际参与人数必须大于 0");
        }
        reservation.setStatus(ReservationStatus.COMPLETED);
        reservation.setFinishTime(now);
        reservation.setActualParticipantCount(actualCount);
        if (StringUtils.isNotEmpty(remark))
        {
            reservation.setRemark(remark);
        }
        updateReservation(reservation);
        log(reservation, ReservationStatus.CHECKED_IN, ReservationStatus.COMPLETED,
                ReservationStatusAction.FINISH, "实验结束");
        // 按时完成实验给予少量信用分奖励，鼓励学生规范使用
        creditServiceChange(reservation, COMPLETED_CREDIT_CHANGE, "按时完成实验");
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void markNoShow(Long reservationId)
    {
        Reservation reservation = requireReservation(reservationId);
        doMarkNoShow(reservation);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public int markNoShowBatch()
    {
        QueryWrapper<Reservation> query = new QueryWrapper<>();
        query.eq("status", ReservationStatus.APPROVED);
        query.isNull("check_in_time");
        query.lt("start_time", plusMinutes(new Date(), -CHECK_IN_LATE_MINUTES));
        List<Reservation> reservations = reservationMapper.selectList(query);
        for (Reservation reservation : reservations)
        {
            doMarkNoShow(reservation);
        }
        return reservations.size();
    }

    @Override
    public List<ReservationStatusLog> listStatusLogs(Long reservationId)
    {
        Reservation reservation = requireReservation(reservationId);
        moduleDataScopeService.checkStudentRecordAccess(reservation.getApplicantId());
        QueryWrapper<ReservationStatusLog> query = new QueryWrapper<>();
        query.eq("reservation_id", reservationId).orderByAsc("id");
        return statusLogMapper.selectList(query);
    }

    private void doMarkNoShow(Reservation reservation)
    {
        assertTransition(reservation.getStatus(), ReservationStatusAction.NO_SHOW, ReservationStatus.NO_SHOW,
                "只有已通过的预约可以标记爽约");
        if (reservation.getCheckInTime() != null)
        {
            throw new ServiceException("该预约已经签到，不能标记爽约");
        }
        Date now = new Date();
        if (now.before(plusMinutes(reservation.getStartTime(), CHECK_IN_LATE_MINUTES)))
        {
            throw new ServiceException("还未超过签到时限");
        }
        reservation.setStatus(ReservationStatus.NO_SHOW);
        updateReservation(reservation);
        log(reservation, ReservationStatus.APPROVED, ReservationStatus.NO_SHOW,
                ReservationStatusAction.NO_SHOW, "超时未签到，系统标记爽约");
        violationRecordService.recordSystemViolation(reservation.getApplicantId(), reservation.getId(),
                ViolationType.NO_SHOW, "预约开始后 " + CHECK_IN_LATE_MINUTES + " 分钟未签到",
                NO_SHOW_CREDIT_CHANGE);
    }

    private Reservation requireReservation(Long reservationId)
    {
        if (reservationId == null)
        {
            throw new ServiceException("预约ID不能为空");
        }
        Reservation reservation = reservationMapper.selectById(reservationId);
        if (reservation == null)
        {
            throw new ServiceException("预约不存在");
        }
        return reservation;
    }

    private void checkOperator(Reservation reservation)
    {
        Long userId = SecurityUtils.getUserId();
        if (!userId.equals(reservation.getApplicantId()) && !moduleDataScopeService.isAdmin())
        {
            throw new ServiceException("只有预约申请人或管理员可以执行该操作");
        }
    }

    private void assertTransition(String fromStatus, String action, String toStatus, String message)
    {
        if (!ReservationStateMachine.canTransit(fromStatus, action, toStatus))
        {
            throw new ServiceException(message);
        }
    }

    private void updateReservation(Reservation reservation)
    {
        reservation.setUpdateBy(currentOperator());
        reservation.setUpdateTime(DateUtils.getNowDate());
        reservationMapper.updateById(reservation);
    }

    private void log(Reservation reservation, String fromStatus, String toStatus, String action, String reason)
    {
        ReservationStatusLog log = new ReservationStatusLog();
        log.setReservationId(reservation.getId());
        log.setFromStatus(fromStatus);
        log.setToStatus(toStatus);
        log.setAction(action);
        log.setOperatorId(currentOperatorId());
        log.setOperatorName(currentOperator());
        log.setReason(reason);
        log.setCreateTime(DateUtils.getNowDate());
        statusLogMapper.insert(log);
    }

    private void creditServiceChange(Reservation reservation, int delta, String reason)
    {
        creditService.changeScore(reservation.getApplicantId(), delta, reason, "RESERVATION",
                reservation.getId());
    }

    private Date plusMinutes(Date date, int minutes)
    {
        return new Date(date.getTime() + minutes * 60_000L);
    }

    private Long currentOperatorId()
    {
        try
        {
            return SecurityUtils.getUserId();
        }
        catch (Exception e)
        {
            return 0L;
        }
    }

    private String currentOperator()
    {
        try
        {
            String username = SecurityUtils.getUsername();
            return StringUtils.isEmpty(username) ? "system" : username;
        }
        catch (Exception e)
        {
            return "system";
        }
    }
}
