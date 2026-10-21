package com.ruoyi.module.agent.draft.service;

import java.util.List;

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

    /**
     * 查询某个会话中尚未确认且未过期的草稿。
     */
    List<ReservationDraftView> listPendingDrafts(Long userId, Long sessionId);
}
