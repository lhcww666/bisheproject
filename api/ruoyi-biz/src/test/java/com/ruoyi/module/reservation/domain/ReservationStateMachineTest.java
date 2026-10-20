package com.ruoyi.module.reservation.domain;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

/**
 * 预约状态机测试。
 *
 * @author itjialu
 */
class ReservationStateMachineTest
{
    @Test
    void pendingCanBeApprovedOrRejected()
    {
        assertTrue(ReservationStateMachine.canTransit(ReservationStatus.PENDING,
                ReservationStatusAction.APPROVE, ReservationStatus.APPROVED));
        assertTrue(ReservationStateMachine.canTransit(ReservationStatus.PENDING,
                ReservationStatusAction.REJECT, ReservationStatus.REJECTED));
    }

    @Test
    void pendingCannotCheckInOrFinish()
    {
        assertFalse(ReservationStateMachine.canTransit(ReservationStatus.PENDING,
                ReservationStatusAction.CHECK_IN, ReservationStatus.CHECKED_IN));
        assertFalse(ReservationStateMachine.canTransit(ReservationStatus.PENDING,
                ReservationStatusAction.FINISH, ReservationStatus.COMPLETED));
    }

    @Test
    void approvedFollowsCheckInThenFinish()
    {
        assertTrue(ReservationStateMachine.canTransit(ReservationStatus.APPROVED,
                ReservationStatusAction.CHECK_IN, ReservationStatus.CHECKED_IN));
        assertTrue(ReservationStateMachine.canTransit(ReservationStatus.CHECKED_IN,
                ReservationStatusAction.FINISH, ReservationStatus.COMPLETED));
    }

    @Test
    void approvedCanTimeoutOrBeCanceled()
    {
        assertTrue(ReservationStateMachine.canTransit(ReservationStatus.APPROVED,
                ReservationStatusAction.NO_SHOW, ReservationStatus.NO_SHOW));
        assertTrue(ReservationStateMachine.canTransit(ReservationStatus.APPROVED,
                ReservationStatusAction.CANCEL, ReservationStatus.CANCELED));
    }

    @Test
    void checkedInCannotBeCanceledOrNoShow()
    {
        assertFalse(ReservationStateMachine.canTransit(ReservationStatus.CHECKED_IN,
                ReservationStatusAction.CANCEL, ReservationStatus.CANCELED));
        assertFalse(ReservationStateMachine.canTransit(ReservationStatus.CHECKED_IN,
                ReservationStatusAction.NO_SHOW, ReservationStatus.NO_SHOW));
    }

    @Test
    void approvedCanBeUnaudited()
    {
        assertTrue(ReservationStateMachine.canTransit(ReservationStatus.APPROVED,
                ReservationStatusAction.UNAUDIT, ReservationStatus.PENDING));
    }
}
