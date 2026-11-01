package com.ruoyi.module.agent.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.ruoyi.module.agent.domain.AgentContextClaims;
import com.ruoyi.module.agent.domain.ToolCallLog;
import com.ruoyi.module.agent.mapper.ToolCallLogMapper;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;

class AgentToolAuditServiceTest
{
    @Test
    void recordsRedactedToolSummary()
    {
        ToolCallLogMapper mapper = mock(ToolCallLogMapper.class);
        AgentToolAuditService service = new AgentToolAuditService(mapper, new ObjectMapper());
        AgentContextClaims context = new AgentContextClaims();
        context.setUserId(7L);
        context.setSessionId(101L);

        service.success(
                context,
                "trace-1",
                "queryMyReservations",
                Map.of("pageNum", 1),
                Map.of("returned", 1),
                12L);

        ArgumentCaptor<ToolCallLog> captor = ArgumentCaptor.forClass(ToolCallLog.class);
        verify(mapper).insert(captor.capture());
        assertEquals(7L, captor.getValue().getUserId());
        assertEquals(101L, captor.getValue().getSessionId());
        assertEquals("queryMyReservations", captor.getValue().getToolName());
        assertEquals("1", captor.getValue().getSuccess());
    }
}
