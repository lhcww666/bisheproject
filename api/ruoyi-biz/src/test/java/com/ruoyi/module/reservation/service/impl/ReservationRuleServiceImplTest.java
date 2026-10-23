package com.ruoyi.module.reservation.service.impl;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.Date;
import java.util.List;

import org.junit.jupiter.api.Test;

import com.ruoyi.common.exception.ServiceException;
import com.ruoyi.module.classrep.service.IClassRepresentativeService;
import com.ruoyi.module.credit.service.ICreditService;
import com.ruoyi.module.reservation.domain.Reservation;
import com.ruoyi.module.reservation.domain.ReservationStatus;
import com.ruoyi.module.reservation.domain.ReservationType;
import com.ruoyi.module.reservation.mapper.ReservationMapper;
import com.ruoyi.module.room.domain.Room;
import com.ruoyi.module.room.mapper.RoomMapper;
import com.ruoyi.module.roomaccess.service.IRoomAccessService;
import com.ruoyi.module.safety.mapper.SafetyExamRecordMapper;

/**
 * 实验室预约核心业务规则测试。
 *
 * @author itjialu
 */
class ReservationRuleServiceImplTest
{
    @Test
    void validApplicationPassesAllBusinessRules()
    {
        Fixture fixture = fixture(80L);
        when(fixture.roomMapper.selectById(1L)).thenReturn(fixture.room);
        when(fixture.reservationMapper.selectList(any())).thenReturn(List.of());
        when(fixture.creditService.currentScore(42L)).thenReturn(80);

        Room checked = fixture.service.checkApply(42L, reservation());

        assertSame(fixture.room, checked);
    }

    @Test
    void lowCreditBlocksApplication()
    {
        Fixture fixture = fixture(40L);
        when(fixture.roomMapper.selectById(1L)).thenReturn(fixture.room);
        when(fixture.reservationMapper.selectList(any())).thenReturn(List.of());
        when(fixture.creditService.currentScore(42L)).thenReturn(40);

        ServiceException exception = assertThrows(ServiceException.class,
                () -> fixture.service.checkApply(42L, reservation()));

        assertTrue(exception.getMessage().contains("信用分不足"));
    }

    @Test
    void overlappingReservationIsRejectedWithConflictNumber()
    {
        Fixture fixture = fixture(80L);
        Reservation conflict = new Reservation();
        conflict.setReservationNo("RES20261024-001");
        when(fixture.reservationMapper.selectList(any())).thenReturn(List.of(conflict));
        Reservation request = reservation();

        ServiceException exception = assertThrows(ServiceException.class,
                () -> fixture.service.checkConflict(1L, request.getStartTime(), request.getEndTime(), null));

        assertTrue(exception.getMessage().contains("RES20261024-001"));
    }

    private Fixture fixture(long creditScore)
    {
        RoomMapper roomMapper = mock(RoomMapper.class);
        IRoomAccessService roomAccessService = mock(IRoomAccessService.class);
        IClassRepresentativeService classRepresentativeService = mock(IClassRepresentativeService.class);
        ReservationMapper reservationMapper = mock(ReservationMapper.class);
        SafetyExamRecordMapper safetyExamRecordMapper = mock(SafetyExamRecordMapper.class);
        ICreditService creditService = mock(ICreditService.class);
        when(creditService.currentScore(42L)).thenReturn((int) creditScore);
        Room room = new Room();
        room.setId(1L);
        room.setRoomCode("A101");
        room.setRoomName("有机化学实验室");
        room.setStatus("0");
        room.setCapacity(24L);
        room.setSafetyLevel(1L);
        room.setOpenStartTime("08:00:00");
        room.setOpenEndTime("22:00:00");
        ReservationRuleServiceImpl service = new ReservationRuleServiceImpl(roomMapper, roomAccessService,
                classRepresentativeService, reservationMapper, safetyExamRecordMapper, creditService);
        return new Fixture(service, roomMapper, reservationMapper, creditService, room);
    }

    private Reservation reservation()
    {
        LocalDateTime start = LocalDate.now().plusDays(1).atTime(10, 0);
        Reservation reservation = new Reservation();
        reservation.setRoomId(1L);
        reservation.setExperimentName("有机溶剂萃取");
        reservation.setParticipantCount(6L);
        reservation.setReservationType(ReservationType.INDIVIDUAL);
        reservation.setStatus(ReservationStatus.PENDING);
        reservation.setStartTime(toDate(start));
        reservation.setEndTime(toDate(start.plusHours(2)));
        return reservation;
    }

    private Date toDate(LocalDateTime time)
    {
        return Date.from(time.atZone(ZoneId.systemDefault()).toInstant());
    }

    private record Fixture(ReservationRuleServiceImpl service, RoomMapper roomMapper,
            ReservationMapper reservationMapper, ICreditService creditService, Room room)
    {
    }
}
