package com.ruoyi.module.agent.controller;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.ruoyi.common.core.domain.AjaxResult;
import com.ruoyi.module.agent.domain.AgentContextClaims;
import com.ruoyi.module.agent.domain.AgentAvailableDeviceRequest;
import com.ruoyi.module.agent.domain.AgentAvailableRoomRequest;
import com.ruoyi.module.agent.service.AgentContextService;
import com.ruoyi.module.agent.service.AgentToolAuditService;
import com.ruoyi.module.agent.service.AgentToolPolicyService;
import com.ruoyi.module.agent.service.IReservationDraftService;
import com.ruoyi.module.credit.domain.CreditSummary;
import com.ruoyi.module.credit.service.ICreditService;
import com.ruoyi.module.device.domain.Device;
import com.ruoyi.module.reservation.domain.Reservation;
import com.ruoyi.module.reservation.domain.ReservationView;
import com.ruoyi.module.reservation.service.IReservationService;
import com.ruoyi.module.safety.domain.SafetyExamStatusView;
import com.ruoyi.module.safety.service.ISafetyExamService;
import com.ruoyi.module.room.domain.Room;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Date;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class AgentToolControllerTest
{
    @Test
    void queriesReservationsForContextUser()
    {
        AgentContextService contextService = mock(AgentContextService.class);
        AgentToolPolicyService policyService = mock(AgentToolPolicyService.class);
        AgentToolAuditService auditService = mock(AgentToolAuditService.class);
        IReservationService reservationService = mock(IReservationService.class);
        ISafetyExamService safetyExamService = mock(ISafetyExamService.class);
        ICreditService creditService = mock(ICreditService.class);
        IReservationDraftService draftService = mock(IReservationDraftService.class);
        AgentToolController controller = new AgentToolController(
                contextService,
                policyService,
                auditService,
                reservationService,
                safetyExamService,
                creditService,
                draftService);
        AgentContextClaims context = new AgentContextClaims();
        context.setUserId(7L);
        context.setRole("STUDENT");
        context.setTraceId("trace-1");
        when(contextService.verify("token", "trace-1")).thenReturn(context);

        ReservationView reservation = new ReservationView();
        reservation.setId(501L);
        reservation.setReservationNo("R-501");
        reservation.setRoomName("化学实验室 A101");
        reservation.setStatus("1");
        Page<ReservationView> page = new Page<>(1, 20);
        page.setRecords(List.of(reservation));
        page.setTotal(1);
        when(reservationService.pageMyReservationForUser(
                any(),
                eq(7L),
                eq(null),
                eq(null),
                eq(null))).thenReturn(page);

        AjaxResult result = controller.myReservations("token", "trace-1", null, 1, 20);

        assertEquals(200, result.get(AjaxResult.CODE_TAG));
        verify(policyService).requireAllowed("STUDENT", "queryMyReservations");
        verify(auditService).success(
                eq(context),
                eq("trace-1"),
                eq("queryMyReservations"),
                any(),
                any(),
                anyLong());
        verify(reservationService).pageMyReservationForUser(
                any(),
                eq(7L),
                eq(null),
                eq(null),
                eq(null));
    }

    @Test
    void queriesSafetyExamStatusForContextUser()
    {
        AgentContextService contextService = mock(AgentContextService.class);
        AgentToolPolicyService policyService = mock(AgentToolPolicyService.class);
        AgentToolAuditService auditService = mock(AgentToolAuditService.class);
        IReservationService reservationService = mock(IReservationService.class);
        ISafetyExamService safetyExamService = mock(ISafetyExamService.class);
        ICreditService creditService = mock(ICreditService.class);
        IReservationDraftService draftService = mock(IReservationDraftService.class);
        AgentToolController controller = new AgentToolController(
                contextService,
                policyService,
                auditService,
                reservationService,
                safetyExamService,
                creditService,
                draftService);
        AgentContextClaims context = studentContext();
        when(contextService.verify("token", "trace-1")).thenReturn(context);
        SafetyExamStatusView status = new SafetyExamStatusView();
        status.setHasValidPass(true);
        when(safetyExamService.getStatus(7L)).thenReturn(status);

        AjaxResult result = controller.safetyExamStatus("token", "trace-1");

        assertEquals(200, result.get(AjaxResult.CODE_TAG));
        verify(policyService).requireAllowed("STUDENT", "checkSafetyExamStatus");
        verify(safetyExamService).getStatus(7L);
    }

    @Test
    void queriesCurrentCreditForContextUser()
    {
        AgentContextService contextService = mock(AgentContextService.class);
        AgentToolPolicyService policyService = mock(AgentToolPolicyService.class);
        AgentToolAuditService auditService = mock(AgentToolAuditService.class);
        IReservationService reservationService = mock(IReservationService.class);
        ISafetyExamService safetyExamService = mock(ISafetyExamService.class);
        ICreditService creditService = mock(ICreditService.class);
        IReservationDraftService draftService = mock(IReservationDraftService.class);
        AgentToolController controller = new AgentToolController(
                contextService,
                policyService,
                auditService,
                reservationService,
                safetyExamService,
                creditService,
                draftService);
        AgentContextClaims context = studentContext();
        when(contextService.verify("token", "trace-1")).thenReturn(context);
        CreditSummary summary = new CreditSummary();
        summary.setScore(100);
        summary.setMinScore(60);
        summary.setCanBook(true);
        when(creditService.getSummary(7L)).thenReturn(summary);

        AjaxResult result = controller.currentCredit("token", "trace-1");

        assertEquals(200, result.get(AjaxResult.CODE_TAG));
        verify(policyService).requireAllowed("STUDENT", "queryMyCredit");
        verify(creditService).getSummary(7L);
    }

    @Test
    void queriesAvailableRoomsForContextUser()
    {
        AgentContextService contextService = mock(AgentContextService.class);
        AgentToolPolicyService policyService = mock(AgentToolPolicyService.class);
        AgentToolAuditService auditService = mock(AgentToolAuditService.class);
        IReservationService reservationService = mock(IReservationService.class);
        ISafetyExamService safetyExamService = mock(ISafetyExamService.class);
        ICreditService creditService = mock(ICreditService.class);
        IReservationDraftService draftService = mock(IReservationDraftService.class);
        AgentToolController controller = new AgentToolController(
                contextService,
                policyService,
                auditService,
                reservationService,
                safetyExamService,
                creditService,
                draftService);
        AgentContextClaims context = studentContext();
        when(contextService.verify("token", "trace-1")).thenReturn(context);
        Room room = new Room();
        room.setId(12L);
        room.setRoomCode("CHEM-A101");
        room.setRoomName("化学实验室 A101");
        when(reservationService.selectAvailableRooms(
                eq(7L),
                any(Date.class),
                any(Date.class),
                eq(2L))).thenReturn(List.of(room));
        AgentAvailableRoomRequest request = new AgentAvailableRoomRequest();
        request.setStartTime("2026-11-08 14:00:00");
        request.setEndTime("2026-11-08 16:00:00");
        request.setParticipantCount(2L);

        AjaxResult result = controller.availableRooms("token", "trace-1", request);

        assertEquals(200, result.get(AjaxResult.CODE_TAG));
        verify(policyService).requireAllowed("STUDENT", "queryAvailableRooms");
        verify(reservationService).selectAvailableRooms(
                eq(7L),
                any(Date.class),
                any(Date.class),
                eq(2L));
    }

    @Test
    void queriesAvailableDevices()
    {
        AgentContextService contextService = mock(AgentContextService.class);
        AgentToolPolicyService policyService = mock(AgentToolPolicyService.class);
        AgentToolAuditService auditService = mock(AgentToolAuditService.class);
        IReservationService reservationService = mock(IReservationService.class);
        ISafetyExamService safetyExamService = mock(ISafetyExamService.class);
        ICreditService creditService = mock(ICreditService.class);
        IReservationDraftService draftService = mock(IReservationDraftService.class);
        AgentToolController controller = new AgentToolController(
                contextService,
                policyService,
                auditService,
                reservationService,
                safetyExamService,
                creditService,
                draftService);
        AgentContextClaims context = studentContext();
        when(contextService.verify("token", "trace-1")).thenReturn(context);
        Device device = new Device();
        device.setId(3L);
        device.setDeviceCode("DEV-003");
        device.setDeviceName("离心机");
        when(reservationService.selectAvailableDevices(12L)).thenReturn(List.of(device));
        AgentAvailableDeviceRequest request = new AgentAvailableDeviceRequest();
        request.setRoomId(12L);

        AjaxResult result = controller.availableDevices("token", "trace-1", request);

        assertEquals(200, result.get(AjaxResult.CODE_TAG));
        verify(policyService).requireAllowed("STUDENT", "queryAvailableDevices");
        verify(reservationService).selectAvailableDevices(12L);
    }

    private AgentContextClaims studentContext()
    {
        AgentContextClaims context = new AgentContextClaims();
        context.setUserId(7L);
        context.setRole("STUDENT");
        context.setTraceId("trace-1");
        return context;
    }
}
