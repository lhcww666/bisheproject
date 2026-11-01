package com.ruoyi.module.agent.controller;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.ruoyi.common.core.domain.AjaxResult;
import com.ruoyi.module.agent.domain.AgentContextClaims;
import com.ruoyi.module.agent.service.AgentContextService;
import com.ruoyi.module.agent.service.AgentToolAuditService;
import com.ruoyi.module.agent.service.AgentToolPolicyService;
import com.ruoyi.module.reservation.domain.Reservation;
import com.ruoyi.module.reservation.domain.ReservationView;
import com.ruoyi.module.reservation.service.IReservationService;
import org.junit.jupiter.api.Test;

import java.util.List;

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
        AgentToolController controller = new AgentToolController(
                contextService,
                policyService,
                auditService,
                reservationService);
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
}
