package com.ruoyi.module.agent.tool;

import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.util.List;

import org.junit.jupiter.api.Test;

import com.ruoyi.module.agent.service.AgentContext;
import com.ruoyi.module.agent.service.ToolAuditService;
import com.ruoyi.module.credit.domain.CreditSummary;
import com.ruoyi.module.credit.service.ICreditService;
import com.ruoyi.module.reservation.service.IReservationRuleService;
import com.ruoyi.module.room.domain.Room;
import com.ruoyi.module.room.mapper.RoomMapper;
import com.ruoyi.module.roomaccess.service.IRoomAccessService;
import com.ruoyi.module.safety.domain.SafetyExamStatusView;
import com.ruoyi.module.safety.service.ISafetyExamService;

/**
 * Agent 综合预约资格工具测试。
 *
 * @author itjialu
 */
class LabAgentToolsTest
{
    @Test
    void eligibilityExplainsLowRiskAndExamRequiredRooms()
    {
        RoomMapper roomMapper = mock(RoomMapper.class);
        ISafetyExamService safetyExamService = mock(ISafetyExamService.class);
        ICreditService creditService = mock(ICreditService.class);
        IReservationRuleService ruleService = mock(IReservationRuleService.class);
        IRoomAccessService roomAccessService = mock(IRoomAccessService.class);
        ToolAuditService auditService = mock(ToolAuditService.class);

        CreditSummary credit = new CreditSummary();
        credit.setScore(90);
        credit.setMinScore(60);
        credit.setCanBook(true);
        when(creditService.getSummary(1L)).thenReturn(credit);

        SafetyExamStatusView exam = new SafetyExamStatusView();
        exam.setHasValidPass(false);
        exam.setLatestScore(58);
        exam.setLatestPassed(false);
        when(safetyExamService.getStatus(1L)).thenReturn(exam);

        Room lowRisk = room(10L, "R101", "基础实验室");
        Room highRisk = room(11L, "R201", "有机实验室");
        when(roomMapper.selectList(any())).thenReturn(List.of(lowRisk, highRisk));
        when(roomAccessService.filterBookableRooms(eq(1L), anyList())).thenReturn(List.of(lowRisk, highRisk));
        when(ruleService.requiresSafetyExam(lowRisk)).thenReturn(false);
        when(ruleService.requiresSafetyExam(highRisk)).thenReturn(true);

        LabAgentTools tools = new LabAgentTools(null, null, roomMapper, safetyExamService, creditService,
                ruleService, roomAccessService, auditService, null, null);
        AgentContext.open(100L, 1L);
        try
        {
            String result = tools.checkMyReservationEligibility(null);
            assertTrue(result.contains("R101 基础实验室"));
            assertTrue(result.contains("R201 有机实验室"));
            assertTrue(result.contains("低风险实验室可继续选择时段预约"));
        }
        finally
        {
            AgentContext.close();
        }
    }

    private Room room(Long id, String code, String name)
    {
        Room room = new Room();
        room.setId(id);
        room.setRoomCode(code);
        room.setRoomName(name);
        room.setSafetyLevel(1L);
        room.setStatus("0");
        room.setReservationMode("OPEN");
        return room;
    }
}
