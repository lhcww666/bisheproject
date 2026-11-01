package com.ruoyi.module.agent.service;

import com.ruoyi.module.agent.domain.AgentReservationDraftCreateRequest;
import com.ruoyi.module.agent.domain.ReservationDraftView;

/**
 * Agent 预约草稿服务。
 */
public interface IReservationDraftService
{
    ReservationDraftView create(
            Long userId,
            String idempotencyKey,
            AgentReservationDraftCreateRequest request);

    ReservationDraftView get(Long draftId, Long userId);

    ReservationDraftView cancel(Long draftId, Long userId);
}
