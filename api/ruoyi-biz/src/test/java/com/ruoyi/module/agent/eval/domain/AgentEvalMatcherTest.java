package com.ruoyi.module.agent.eval.domain;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.List;

import org.junit.jupiter.api.Test;

/**
 * Agent 等价工具匹配测试。
 *
 * @author itjialu
 */
class AgentEvalMatcherTest
{
    @Test
    void currentAvailabilityToolMatchesGeneralRoomAvailabilityCase()
    {
        List<String> calls = List.of("queryAvailableRoomsNow");

        assertTrue(AgentEvalMatcher.intentMatches(LocalIntentRouter.QUERY_ROOMS, calls));
        assertTrue(AgentEvalMatcher.toolMatches("queryAvailableRooms", calls));
        assertEquals(LocalIntentRouter.QUERY_ROOMS,
                AgentEvalMatcher.resolveActualIntent(
                        LocalIntentRouter.QUERY_ROOMS, "queryAvailableRooms", calls));
    }

    @Test
    void expectedToolCanBeLaterInMultiToolAnswer()
    {
        List<String> calls = List.of("queryAvailableRoomsNow", "queryAvailableDevices");

        assertTrue(AgentEvalMatcher.toolMatches("queryAvailableDevices", calls));
        assertEquals(LocalIntentRouter.DEVICES,
                AgentEvalMatcher.resolveActualIntent(
                        LocalIntentRouter.DEVICES, "queryAvailableDevices", calls));
    }

    @Test
    void unknownCaseRequiresNoSuccessfulTool()
    {
        assertTrue(AgentEvalMatcher.toolMatches("unknown", List.of()));
        assertTrue(AgentEvalMatcher.intentMatches(LocalIntentRouter.UNKNOWN, List.of()));
        assertFalse(AgentEvalMatcher.toolMatches("unknown", List.of("queryAvailableRooms")));
        assertFalse(AgentEvalMatcher.intentMatches(LocalIntentRouter.UNKNOWN, List.of("queryAvailableRooms")));
    }

    @Test
    void unrelatedToolsDoNotMatch()
    {
        assertFalse(AgentEvalMatcher.intentMatches(
                LocalIntentRouter.QUERY_ROOMS, List.of("searchSafetyKnowledge")));
        assertFalse(AgentEvalMatcher.toolMatches(
                "queryAvailableRooms", List.of("searchSafetyKnowledge")));
    }
}
