package com.ruoyi.module.agent.service;

import com.ruoyi.common.constant.HttpStatus;
import com.ruoyi.common.exception.ServiceException;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Map;

/**
 * Agent 工具角色白名单。
 */
@Service
public class AgentToolPolicyService
{
    private static final Map<String, List<String>> TOOLS_BY_ROLE = Map.of(
            "STUDENT", List.of(
                    "queryMyReservations",
                    "checkSafetyExamStatus",
                    "queryMyCredit",
                    "queryAvailableRooms",
                    "queryAvailableDevices",
                    "queryReservationReview",
                    "searchSafetyKnowledge",
                    "createReservationDraft"),
            "TEACHER", List.of(
                    "queryAvailableRooms",
                    "queryAvailableDevices",
                    "queryReservationReview",
                    "queryPendingReservations",
                    "queryStudentQualification",
                    "searchSafetyKnowledge"),
            "ADMIN", List.of(
                    "queryAvailableRooms",
                    "queryAvailableDevices",
                    "queryReservationReview",
                    "queryPendingReservations",
                    "queryStudentQualification",
                    "searchSafetyKnowledge"),
            "USER", List.of(
                    "queryAvailableRooms",
                    "queryAvailableDevices",
                    "searchSafetyKnowledge"));

    public List<String> allowedTools(String role)
    {
        return TOOLS_BY_ROLE.getOrDefault(role, TOOLS_BY_ROLE.get("USER"));
    }

    public void requireAllowed(String role, String toolName)
    {
        if (!allowedTools(role).contains(toolName))
        {
            throw new ServiceException("当前角色无权调用该工具", HttpStatus.FORBIDDEN);
        }
    }
}
