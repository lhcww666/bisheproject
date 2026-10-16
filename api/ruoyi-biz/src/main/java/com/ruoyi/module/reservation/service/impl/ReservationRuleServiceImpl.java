package com.ruoyi.module.reservation.service.impl;

import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

import org.springframework.stereotype.Service;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.ruoyi.common.exception.ServiceException;
import com.ruoyi.common.utils.StringUtils;
import com.ruoyi.module.credit.domain.CreditRecord;
import com.ruoyi.module.credit.mapper.CreditRecordMapper;
import com.ruoyi.module.reservation.domain.Reservation;
import com.ruoyi.module.reservation.domain.ReservationStatus;
import com.ruoyi.module.reservation.mapper.ReservationMapper;
import com.ruoyi.module.reservation.service.IReservationRuleService;
import com.ruoyi.module.room.domain.Room;
import com.ruoyi.module.room.mapper.RoomMapper;
import com.ruoyi.module.safety.domain.SafetyExamRecord;
import com.ruoyi.module.safety.mapper.SafetyExamRecordMapper;

import lombok.RequiredArgsConstructor;

/**
 * 实验室预约业务规则校验Service业务层处理
 *
 * @author itjialu
 */
@Service
@RequiredArgsConstructor
public class ReservationRuleServiceImpl implements IReservationRuleService
{
    /** 实验室状态：正常 */
    private static final String ROOM_STATUS_NORMAL = "0";

    /** 安全考试通过标识 */
    private static final String EXAM_PASSED = "1";

    /** 允许预约的最低信用分 */
    private static final long MIN_CREDIT_SCORE = 60L;

    /** 没有信用分流水时使用的初始信用分 */
    private static final long DEFAULT_CREDIT_SCORE = 100L;

    /** 达到该安全等级（含）的实验室需要安全考试与教师审核 */
    private static final long REVIEW_SAFETY_LEVEL = 2L;

    private static final DateTimeFormatter TIME_WITH_SECOND = DateTimeFormatter.ofPattern("HH:mm:ss");

    private static final DateTimeFormatter TIME_WITHOUT_SECOND = DateTimeFormatter.ofPattern("HH:mm");

    private final RoomMapper roomMapper;

    private final ReservationMapper reservationMapper;

    private final SafetyExamRecordMapper safetyExamRecordMapper;

    private final CreditRecordMapper creditRecordMapper;

    @Override
    public Room checkApply(Long userId, Reservation reservation)
    {
        if (reservation == null)
        {
            throw new ServiceException("预约信息不能为空");
        }
        if (reservation.getRoomId() == null)
        {
            throw new ServiceException("请选择实验室");
        }
        if (StringUtils.isEmpty(reservation.getExperimentName()))
        {
            throw new ServiceException("请填写实验名称");
        }
        if (reservation.getParticipantCount() == null || reservation.getParticipantCount() < 1L)
        {
            throw new ServiceException("参与人数必须大于 0");
        }
        checkTimeRange(reservation.getStartTime(), reservation.getEndTime());

        Room room = roomMapper.selectById(reservation.getRoomId());
        if (room == null)
        {
            throw new ServiceException("实验室不存在");
        }
        if (!ROOM_STATUS_NORMAL.equals(room.getStatus()))
        {
            throw new ServiceException("实验室当前不可预约（维护中或已停用）");
        }
        checkCapacity(room, reservation.getParticipantCount());
        checkOpenTime(room, reservation.getStartTime(), reservation.getEndTime());
        checkConflict(room.getId(), reservation.getStartTime(), reservation.getEndTime(), null);
        checkCredit(userId);
        if (isReviewRequired(room))
        {
            checkSafetyExam(userId);
        }
        return room;
    }

    @Override
    public void checkCancel(Reservation reservation, Long userId)
    {
        if (!reservation.getApplicantId().equals(userId))
        {
            throw new ServiceException("只能取消本人提交的预约");
        }
        if (!ReservationStatus.isOccupied(reservation.getStatus()))
        {
            throw new ServiceException("当前状态不允许取消，只有待审核或已通过的预约可以取消");
        }
    }

    @Override
    public void checkConflict(Long roomId, Date startTime, Date endTime, Long excludeId)
    {
        checkConflict(roomId, startTime, endTime, excludeId, ReservationStatus.PENDING, ReservationStatus.APPROVED);
    }

    @Override
    public void checkApprovedConflict(Long roomId, Date startTime, Date endTime, Long excludeId)
    {
        checkConflict(roomId, startTime, endTime, excludeId, ReservationStatus.APPROVED);
    }

    private void checkConflict(Long roomId, Date startTime, Date endTime, Long excludeId, String... statuses)
    {
        QueryWrapper<Reservation> queryWrapper = new QueryWrapper<>();
        queryWrapper.eq("room_id", roomId);
        queryWrapper.in("status", (Object[]) statuses);
        queryWrapper.lt("start_time", endTime);
        queryWrapper.gt("end_time", startTime);
        if (excludeId != null)
        {
            queryWrapper.ne("id", excludeId);
        }
        List<Reservation> conflicts = reservationMapper.selectList(queryWrapper);
        if (!conflicts.isEmpty())
        {
            Reservation conflict = conflicts.get(0);
            throw new ServiceException("该实验室在所选时段已被占用，冲突预约编号：" + conflict.getReservationNo());
        }
    }

    @Override
    public List<Room> selectAvailableRooms(Date startTime, Date endTime, Long participantCount)
    {
        checkTimeRange(startTime, endTime);
        QueryWrapper<Room> queryWrapper = new QueryWrapper<>();
        queryWrapper.eq("status", ROOM_STATUS_NORMAL);
        List<Room> rooms = roomMapper.selectList(queryWrapper);
        List<Room> available = new ArrayList<>();
        for (Room room : rooms)
        {
            try
            {
                if (participantCount != null)
                {
                    checkCapacity(room, participantCount);
                }
                checkOpenTime(room, startTime, endTime);
                checkConflict(room.getId(), startTime, endTime, null);
                available.add(room);
            }
            catch (ServiceException ignored)
            {
                // 不满足条件的实验室不出现在候选列表里
            }
        }
        return available;
    }

    /**
     * 安全等级达到中风险及以上的实验室，需要安全考试与教师审核
     */
    public boolean isReviewRequired(Room room)
    {
        return room.getSafetyLevel() != null && room.getSafetyLevel() >= REVIEW_SAFETY_LEVEL;
    }

    /**
     * 查询用户当前信用分：取最近一条流水的变动后分值，没有流水时按初始分计算
     */
    public long currentCreditScore(Long userId)
    {
        QueryWrapper<CreditRecord> queryWrapper = new QueryWrapper<>();
        queryWrapper.eq("user_id", userId);
        queryWrapper.orderByDesc("id");
        queryWrapper.last("limit 1");
        List<CreditRecord> records = creditRecordMapper.selectList(queryWrapper);
        if (records.isEmpty() || records.get(0).getBalanceScore() == null)
        {
            return DEFAULT_CREDIT_SCORE;
        }
        return records.get(0).getBalanceScore();
    }

    private void checkTimeRange(Date startTime, Date endTime)
    {
        if (startTime == null || endTime == null)
        {
            throw new ServiceException("请选择开始时间与结束时间");
        }
        if (!startTime.before(endTime))
        {
            throw new ServiceException("开始时间必须早于结束时间");
        }
        if (startTime.before(new Date()))
        {
            throw new ServiceException("开始时间不能早于当前时间");
        }
    }

    private void checkCapacity(Room room, Long participantCount)
    {
        if (room.getCapacity() != null && participantCount > room.getCapacity())
        {
            throw new ServiceException("参与人数超过实验室容量（最多 " + room.getCapacity() + " 人）");
        }
    }

    private void checkOpenTime(Room room, Date startTime, Date endTime)
    {
        LocalTime openStart = parseLocalTime(room.getOpenStartTime());
        LocalTime openEnd = parseLocalTime(room.getOpenEndTime());
        if (openStart == null || openEnd == null)
        {
            return;
        }
        LocalTime start = toLocalDateTime(startTime).toLocalTime();
        LocalTime end = toLocalDateTime(endTime).toLocalTime();
        if (start.isBefore(openStart) || end.isAfter(openEnd))
        {
            throw new ServiceException("预约时间需在实验室开放时段内（" + room.getOpenStartTime() + " - " + room.getOpenEndTime()
                    + "）");
        }
    }

    private void checkCredit(Long userId)
    {
        long score = currentCreditScore(userId);
        if (score < MIN_CREDIT_SCORE)
        {
            throw new ServiceException("信用分不足，当前 " + score + " 分，低于预约门槛 " + MIN_CREDIT_SCORE + " 分");
        }
    }

    private void checkSafetyExam(Long userId)
    {
        QueryWrapper<SafetyExamRecord> queryWrapper = new QueryWrapper<>();
        queryWrapper.eq("student_id", userId);
        queryWrapper.eq("passed", EXAM_PASSED);
        queryWrapper.and(wrapper -> wrapper.isNull("valid_until").or().ge("valid_until", new Date()));
        queryWrapper.orderByDesc("id");
        queryWrapper.last("limit 1");
        List<SafetyExamRecord> records = safetyExamRecordMapper.selectList(queryWrapper);
        if (records.isEmpty())
        {
            throw new ServiceException("尚未通过安全考试或考试成绩已过期，请先完成安全考试");
        }
    }

    private LocalTime parseLocalTime(String text)
    {
        if (StringUtils.isEmpty(text))
        {
            return null;
        }
        try
        {
            return text.length() > 5 ? LocalTime.parse(text, TIME_WITH_SECOND) : LocalTime.parse(text, TIME_WITHOUT_SECOND);
        }
        catch (Exception e)
        {
            return null;
        }
    }

    private LocalDateTime toLocalDateTime(Date date)
    {
        return LocalDateTime.ofInstant(date.toInstant(), ZoneId.systemDefault());
    }
}
