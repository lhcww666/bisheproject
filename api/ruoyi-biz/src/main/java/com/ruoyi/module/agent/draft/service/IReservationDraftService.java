package com.ruoyi.module.agent.draft.service;

import com.ruoyi.module.agent.draft.domain.ReservationDraftRequest;
import com.ruoyi.module.agent.draft.domain.ReservationDraftView;

/**
 * Agent 预约草稿服务。
 *
 * @author itjialu
 */
public interface IReservationDraftService
{
    ReservationDraftView createDraft(Long userId, Long sessionId, ReservationDraftRequest request);

    ReservationDraftView confirmDraft(Long draftId, Long userId);

    ReservationDraftView cancelDraft(Long draftId, Long userId);

    ReservationDraftView getDraft(Long draftId, Long userId);
}
