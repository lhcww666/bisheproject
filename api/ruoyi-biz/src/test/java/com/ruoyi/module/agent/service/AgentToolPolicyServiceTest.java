package com.ruoyi.module.agent.service;

import org.junit.jupiter.api.Test;
import com.ruoyi.common.exception.ServiceException;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class AgentToolPolicyServiceTest
{
    private final AgentToolPolicyService service = new AgentToolPolicyService();

    @Test
    void studentCanCreateDraftButTeacherCannot()
    {
        assertTrue(service.allowedTools("STUDENT").contains("createReservationDraft"));
        assertFalse(service.allowedTools("TEACHER").contains("createReservationDraft"));
    }

    @Test
    void unknownRoleUsesRestrictedPublicTools()
    {
        assertTrue(service.allowedTools("UNKNOWN").contains("queryAvailableRooms"));
        assertFalse(service.allowedTools("UNKNOWN").contains("queryMyReservations"));
    }

    @Test
    void requireAllowedRejectsToolOutsideRolePolicy()
    {
        org.junit.jupiter.api.Assertions.assertThrows(
                ServiceException.class,
                () -> service.requireAllowed("TEACHER", "createReservationDraft"));
    }
}
