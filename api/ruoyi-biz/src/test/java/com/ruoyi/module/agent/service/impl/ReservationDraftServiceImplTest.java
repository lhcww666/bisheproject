package com.ruoyi.module.agent.service.impl;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.ruoyi.common.core.domain.entity.SysUser;
import com.ruoyi.common.core.domain.model.LoginUser;
import com.ruoyi.module.agent.config.AgentProperties;
import com.ruoyi.module.agent.domain.AgentReservationDraftCreateRequest;
import com.ruoyi.module.agent.domain.ReservationDraft;
import com.ruoyi.module.agent.domain.ReservationDraftView;
import com.ruoyi.module.agent.mapper.ReservationDraftMapper;
import com.ruoyi.module.reservation.service.IReservationService;
import com.ruoyi.module.room.domain.Room;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;

import java.util.Date;
import java.util.List;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class ReservationDraftServiceImplTest
{
    private ReservationDraftMapper draftMapper;

    private IReservationService reservationService;

    private ReservationDraftServiceImpl service;

    @BeforeEach
    void setUp()
    {
        SysUser user = new SysUser(7L);
        user.setUserName("student");
        LoginUser loginUser = new LoginUser(7L, 1L, user, Set.of());
        SecurityContextHolder.getContext().setAuthentication(
                new UsernamePasswordAuthenticationToken(loginUser, null, loginUser.getAuthorities()));
        draftMapper = mock(ReservationDraftMapper.class);
        reservationService = mock(IReservationService.class);
        AgentProperties properties = new AgentProperties();
        properties.setDraftTtlMinutes(30);
        service = new ReservationDraftServiceImpl(
                draftMapper,
                reservationService,
                properties,
                new ObjectMapper());
    }

    @AfterEach
    void tearDown()
    {
        SecurityContextHolder.clearContext();
    }

    @Test
    void createsDraftAfterAvailabilityChecks()
    {
        Room room = new Room();
        room.setId(12L);
        when(reservationService.selectAvailableRooms(
                any(),
                any(Date.class),
                any(Date.class),
                any())).thenReturn(List.of(room));
        doAnswer(invocation -> {
            ReservationDraft draft = invocation.getArgument(0);
            draft.setId(70L);
            return 1;
        }).when(draftMapper).insert(any(ReservationDraft.class));

        ReservationDraftView view = service.create(
                7L,
                "idem-1",
                request());

        assertEquals(70L, view.getId());
        assertEquals("PENDING", view.getStatus());
        assertEquals(12L, view.getRequest().getRoomId());
        ArgumentCaptor<ReservationDraft> captor = ArgumentCaptor.forClass(ReservationDraft.class);
        verify(draftMapper).insert(captor.capture());
        assertEquals(7L, captor.getValue().getUserId());
        assertEquals("idem-1", captor.getValue().getIdempotencyKey());
    }

    @Test
    void returnsExistingDraftForSameIdempotencyKey()
    {
        ReservationDraft existing = new ReservationDraft();
        existing.setId(71L);
        existing.setUserId(7L);
        existing.setSessionId(101L);
        existing.setStatus("PENDING");
        existing.setExpiresAt(new Date(System.currentTimeMillis() + 60_000));
        try
        {
            existing.setRequestJson(new ObjectMapper().writeValueAsString(request()));
        }
        catch (Exception exception)
        {
            throw new RuntimeException(exception);
        }
        when(draftMapper.selectOne(any())).thenReturn(existing);

        ReservationDraftView view = service.create(7L, "idem-1", request());

        assertEquals(71L, view.getId());
    }

    private AgentReservationDraftCreateRequest request()
    {
        AgentReservationDraftCreateRequest request = new AgentReservationDraftCreateRequest();
        request.setSessionId(101L);
        request.setRoomId(12L);
        request.setExperimentName("有机溶剂萃取");
        request.setStartTime("2026-11-08 14:00:00");
        request.setEndTime("2026-11-08 16:00:00");
        request.setParticipantCount(2L);
        return request;
    }
}
