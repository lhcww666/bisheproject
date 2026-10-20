package com.ruoyi.module.stats.service.impl;

import java.time.Duration;
import java.time.LocalDate;
import java.time.LocalTime;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.Date;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import org.springframework.stereotype.Service;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.ruoyi.module.common.service.ModuleDataScopeService;
import com.ruoyi.module.reservation.domain.Reservation;
import com.ruoyi.module.reservation.domain.ReservationStatus;
import com.ruoyi.module.reservation.mapper.ReservationMapper;
import com.ruoyi.module.room.domain.Room;
import com.ruoyi.module.room.mapper.RoomMapper;
import com.ruoyi.module.safety.domain.SafetyExamRecord;
import com.ruoyi.module.safety.mapper.SafetyExamRecordMapper;
import com.ruoyi.module.stats.domain.ReservationTrendPoint;
import com.ruoyi.module.stats.domain.RoomUsage;
import com.ruoyi.module.stats.domain.StatsCalculator;
import com.ruoyi.module.stats.domain.StatsDashboard;
import com.ruoyi.module.stats.domain.StatsOverview;
import com.ruoyi.module.stats.domain.StatusCount;
import com.ruoyi.module.stats.domain.ViolationTrendPoint;
import com.ruoyi.module.stats.service.IStatsService;
import com.ruoyi.module.violation.domain.ViolationRecord;
import com.ruoyi.module.violation.mapper.ViolationRecordMapper;

import lombok.RequiredArgsConstructor;

/**
 * 实验室数据看板Service实现。
 *
 * <p>当前模拟数据规模下在 Java 中聚合，避免为演示系统引入额外统计表；
 * 后续数据量上来后可以把同样的指标迁移到 SQL 聚合或定时汇总表。</p>
 *
 * @author itjialu
 */
@Service
@RequiredArgsConstructor
public class StatsServiceImpl implements IStatsService
{
    private static final DateTimeFormatter TIME_WITH_SECOND = DateTimeFormatter.ofPattern("HH:mm:ss");

    private static final DateTimeFormatter TIME_WITHOUT_SECOND = DateTimeFormatter.ofPattern("HH:mm");

    private final ReservationMapper reservationMapper;

    private final RoomMapper roomMapper;

    private final ViolationRecordMapper violationRecordMapper;

    private final SafetyExamRecordMapper safetyExamRecordMapper;

    private final ModuleDataScopeService moduleDataScopeService;

    @Override
    public StatsDashboard getDashboard(int days, String startDate, String endDate)
    {
        DateRange range = resolveRange(days, startDate, endDate);
        List<Reservation> reservations = loadScopedReservations();
        List<ViolationRecord> violations = loadScopedViolations();
        List<SafetyExamRecord> exams = loadScopedExams();
        List<Room> rooms = roomMapper.selectList(null);

        StatsDashboard dashboard = new StatsDashboard();
        dashboard.setOverview(buildOverview(reservations, violations, exams, rooms));
        dashboard.setReservationTrend(buildReservationTrend(reservations, range));
        dashboard.setStatusDistribution(buildStatusDistribution(reservations));
        dashboard.setRoomUsage(buildRoomUsage(rooms, reservations, range));
        dashboard.setViolationTrend(buildViolationTrend(violations, range));
        return dashboard;
    }

    private List<Reservation> loadScopedReservations()
    {
        QueryWrapper<Reservation> query = new QueryWrapper<>();
        moduleDataScopeService.applyStudentRecordScope(query, "applicant_id");
        return reservationMapper.selectList(query);
    }

    private List<ViolationRecord> loadScopedViolations()
    {
        QueryWrapper<ViolationRecord> query = new QueryWrapper<>();
        moduleDataScopeService.applyStudentRecordScope(query, "user_id");
        return violationRecordMapper.selectList(query);
    }

    private List<SafetyExamRecord> loadScopedExams()
    {
        QueryWrapper<SafetyExamRecord> query = new QueryWrapper<>();
        moduleDataScopeService.applyStudentRecordScope(query, "student_id");
        return safetyExamRecordMapper.selectList(query);
    }

    private StatsOverview buildOverview(List<Reservation> reservations, List<ViolationRecord> violations,
            List<SafetyExamRecord> exams, List<Room> rooms)
    {
        StatsOverview overview = new StatsOverview();
        overview.setScopeLabel(scopeLabel());
        overview.setTotalReservations((long) reservations.size());
        LocalDate today = LocalDate.now();
        overview.setTodayReservations(reservations.stream()
                .filter(item -> item.getStartTime() != null && today.equals(StatsCalculator.toLocalDate(item.getStartTime())))
                .count());
        overview.setPendingReservations(countStatus(reservations, ReservationStatus.PENDING));
        overview.setApprovedReservations(reservations.stream()
                .filter(item -> isActiveStatus(item.getStatus())).count());
        overview.setCompletedReservations(countStatus(reservations, ReservationStatus.COMPLETED));
        overview.setNoShowReservations(countStatus(reservations, ReservationStatus.NO_SHOW));
        overview.setViolationCount((long) violations.size());
        overview.setTotalRooms((long) rooms.size());
        overview.setNoShowRate(StatsCalculator.percentage(overview.getNoShowReservations(),
                overview.getTotalReservations()));

        long attempts = exams.stream().filter(item -> item.getPassed() != null).count();
        Date now = new Date();
        long passed = exams.stream()
                .filter(item -> "1".equals(item.getPassed()))
                .filter(item -> item.getValidUntil() == null || !item.getValidUntil().before(now))
                .count();
        overview.setExamPassRate(StatsCalculator.percentage(passed, attempts));
        return overview;
    }

    private List<ReservationTrendPoint> buildReservationTrend(List<Reservation> reservations, DateRange range)
    {
        Map<LocalDate, ReservationTrendPoint> points = new LinkedHashMap<>();
        for (LocalDate date = range.startDate; date.isBefore(range.endDateExclusive); date = date.plusDays(1))
        {
            points.put(date, newPoint(date));
        }
        for (Reservation reservation : reservations)
        {
            LocalDate date = StatsCalculator.toLocalDate(reservation.getCreateTime());
            if (date == null || !points.containsKey(date))
            {
                continue;
            }
            ReservationTrendPoint point = points.get(date);
            point.setTotal(point.getTotal() + 1);
            if (isActiveStatus(reservation.getStatus()))
            {
                point.setApproved(point.getApproved() + 1);
            }
            if (ReservationStatus.COMPLETED.equals(reservation.getStatus()))
            {
                point.setCompleted(point.getCompleted() + 1);
            }
            if (ReservationStatus.NO_SHOW.equals(reservation.getStatus()))
            {
                point.setNoShow(point.getNoShow() + 1);
            }
        }
        return new ArrayList<>(points.values());
    }

    private List<StatusCount> buildStatusDistribution(List<Reservation> reservations)
    {
        Map<String, Long> counts = new LinkedHashMap<>();
        for (String status : new String[] { ReservationStatus.PENDING, ReservationStatus.APPROVED,
                ReservationStatus.REJECTED, ReservationStatus.CANCELED, ReservationStatus.COMPLETED,
                ReservationStatus.CHECKED_IN, ReservationStatus.NO_SHOW })
        {
            counts.put(status, 0L);
        }
        for (Reservation reservation : reservations)
        {
            if (reservation.getStatus() != null)
            {
                counts.merge(reservation.getStatus(), 1L, Long::sum);
            }
        }
        List<StatusCount> result = new ArrayList<>();
        for (Map.Entry<String, Long> entry : counts.entrySet())
        {
            StatusCount item = new StatusCount();
            item.setStatus(entry.getKey());
            item.setLabel(statusLabel(entry.getKey()));
            item.setCount(entry.getValue());
            result.add(item);
        }
        return result;
    }

    private List<RoomUsage> buildRoomUsage(List<Room> rooms, List<Reservation> reservations, DateRange range)
    {
        long days = ChronoUnit.DAYS.between(range.startDate, range.endDateExclusive);
        List<RoomUsage> result = new ArrayList<>();
        for (Room room : rooms)
        {
            LocalTime openStart = parseTime(room.getOpenStartTime());
            LocalTime openEnd = parseTime(room.getOpenEndTime());
            if (openStart == null || openEnd == null || !openEnd.isAfter(openStart))
            {
                continue;
            }
            double dailyHours = Duration.between(openStart, openEnd).toMinutes() / 60D;
            double openHours = dailyHours * Math.max(1, days);
            double bookedHours = 0D;
            long reservationCount = 0;
            for (Reservation reservation : reservations)
            {
                if (!room.getId().equals(reservation.getRoomId()) || !isUsageStatus(reservation.getStatus()))
                {
                    continue;
                }
                double overlap = StatsCalculator.overlapHours(reservation.getStartTime(), reservation.getEndTime(),
                        range.start, range.end);
                if (overlap > 0)
                {
                    bookedHours += overlap;
                    reservationCount++;
                }
            }
            if (moduleDataScopeService.isStudent() && reservationCount == 0)
            {
                continue;
            }
            RoomUsage usage = new RoomUsage();
            usage.setRoomCode(room.getRoomCode());
            usage.setRoomName(room.getRoomName());
            usage.setBookedHours(round(bookedHours));
            usage.setOpenHours(round(openHours));
            usage.setUsageRate(StatsCalculator.percentage(bookedHours, openHours));
            usage.setReservationCount(reservationCount);
            result.add(usage);
        }
        result.sort(Comparator.comparing(RoomUsage::getUsageRate).reversed());
        return result;
    }

    private List<ViolationTrendPoint> buildViolationTrend(List<ViolationRecord> violations, DateRange range)
    {
        Map<LocalDate, ViolationTrendPoint> points = new LinkedHashMap<>();
        for (LocalDate date = range.startDate; date.isBefore(range.endDateExclusive); date = date.plusDays(1))
        {
            ViolationTrendPoint point = new ViolationTrendPoint();
            point.setDate(date.toString());
            point.setCount(0L);
            point.setCreditChange(0L);
            points.put(date, point);
        }
        for (ViolationRecord violation : violations)
        {
            LocalDate date = StatsCalculator.toLocalDate(violation.getCreateTime());
            if (date == null || !points.containsKey(date))
            {
                continue;
            }
            ViolationTrendPoint point = points.get(date);
            point.setCount(point.getCount() + 1);
            point.setCreditChange(point.getCreditChange()
                    + (violation.getCreditChange() == null ? 0 : violation.getCreditChange()));
        }
        return new ArrayList<>(points.values());
    }

    private ReservationTrendPoint newPoint(LocalDate date)
    {
        ReservationTrendPoint point = new ReservationTrendPoint();
        point.setDate(date.toString());
        point.setTotal(0L);
        point.setApproved(0L);
        point.setCompleted(0L);
        point.setNoShow(0L);
        return point;
    }

    private long countStatus(List<Reservation> reservations, String status)
    {
        return reservations.stream().filter(item -> status.equals(item.getStatus())).count();
    }

    private boolean isActiveStatus(String status)
    {
        return ReservationStatus.APPROVED.equals(status) || ReservationStatus.CHECKED_IN.equals(status)
                || ReservationStatus.COMPLETED.equals(status);
    }

    private boolean isUsageStatus(String status)
    {
        return ReservationStatus.APPROVED.equals(status) || ReservationStatus.CHECKED_IN.equals(status)
                || ReservationStatus.COMPLETED.equals(status);
    }

    private String statusLabel(String status)
    {
        if (ReservationStatus.PENDING.equals(status)) return "待审核";
        if (ReservationStatus.APPROVED.equals(status)) return "已通过";
        if (ReservationStatus.REJECTED.equals(status)) return "已驳回";
        if (ReservationStatus.CANCELED.equals(status)) return "已取消";
        if (ReservationStatus.COMPLETED.equals(status)) return "已完成";
        if (ReservationStatus.CHECKED_IN.equals(status)) return "已签到";
        if (ReservationStatus.NO_SHOW.equals(status)) return "已爽约";
        return "其他";
    }

    private String scopeLabel()
    {
        if (moduleDataScopeService.isAdmin()) return "全平台";
        if (moduleDataScopeService.isTeacher()) return "本班";
        return "我的";
    }

    private DateRange resolveRange(int days, String startDate, String endDate)
    {
        LocalDate start;
        LocalDate endExclusive;
        if (startDate != null && !startDate.isEmpty() && endDate != null && !endDate.isEmpty())
        {
            start = LocalDate.parse(startDate);
            endExclusive = LocalDate.parse(endDate).plusDays(1);
        }
        else
        {
            int safeDays = Math.max(1, Math.min(365, days));
            start = LocalDate.now().minusDays(safeDays - 1L);
            endExclusive = LocalDate.now().plusDays(1);
        }
        if (!endExclusive.isAfter(start))
        {
            throw new com.ruoyi.common.exception.ServiceException("结束日期必须晚于开始日期");
        }
        DateRange range = new DateRange();
        range.startDate = start;
        range.endDateExclusive = endExclusive;
        range.start = toDate(start);
        range.end = toDate(endExclusive);
        return range;
    }

    private LocalTime parseTime(String text)
    {
        if (text == null || text.isEmpty())
        {
            return null;
        }
        try
        {
            return text.length() > 5 ? LocalTime.parse(text, TIME_WITH_SECOND)
                    : LocalTime.parse(text, TIME_WITHOUT_SECOND);
        }
        catch (Exception e)
        {
            return null;
        }
    }

    private Date toDate(LocalDate date)
    {
        return Date.from(date.atStartOfDay(ZoneId.systemDefault()).toInstant());
    }

    private double round(double value)
    {
        return Math.round(value * 100D) / 100D;
    }

    private static class DateRange
    {
        private LocalDate startDate;
        private LocalDate endDateExclusive;
        private Date start;
        private Date end;
    }
}
