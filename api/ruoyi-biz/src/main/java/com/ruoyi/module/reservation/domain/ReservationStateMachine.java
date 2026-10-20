package com.ruoyi.module.reservation.domain;

import java.util.Map;
import java.util.Set;

/**
 * 预约状态机。只负责状态与动作是否匹配，具体时间、角色和业务规则由 Service 校验。
 *
 * @author itjialu
 */
public final class ReservationStateMachine
{
    private static final Map<String, Map<String, String>> TRANSITIONS = Map.of(
            ReservationStatusAction.APPROVE,
            Map.of(ReservationStatus.PENDING, ReservationStatus.APPROVED),
            ReservationStatusAction.REJECT,
            Map.of(ReservationStatus.PENDING, ReservationStatus.REJECTED),
            ReservationStatusAction.UNAUDIT,
            Map.of(ReservationStatus.APPROVED, ReservationStatus.PENDING),
            ReservationStatusAction.CANCEL,
            Map.of(ReservationStatus.PENDING, ReservationStatus.CANCELED,
                    ReservationStatus.APPROVED, ReservationStatus.CANCELED),
            ReservationStatusAction.CHECK_IN,
            Map.of(ReservationStatus.APPROVED, ReservationStatus.CHECKED_IN),
            ReservationStatusAction.FINISH,
            Map.of(ReservationStatus.CHECKED_IN, ReservationStatus.COMPLETED),
            ReservationStatusAction.NO_SHOW,
            Map.of(ReservationStatus.APPROVED, ReservationStatus.NO_SHOW));

    private ReservationStateMachine()
    {
    }

    public static boolean canTransit(String fromStatus, String action, String toStatus)
    {
        Map<String, String> targets = TRANSITIONS.get(action);
        return targets != null && toStatus.equals(targets.get(fromStatus));
    }

    public static Set<String> actionsFrom(String fromStatus)
    {
        return TRANSITIONS.entrySet().stream()
                .filter(entry -> entry.getValue().containsKey(fromStatus))
                .map(Map.Entry::getKey)
                .collect(java.util.stream.Collectors.toSet());
    }
}
