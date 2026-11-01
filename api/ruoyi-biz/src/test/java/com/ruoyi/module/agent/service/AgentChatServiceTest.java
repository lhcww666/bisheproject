package com.ruoyi.module.agent.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.ruoyi.module.agent.config.AgentProperties;
import com.ruoyi.module.agent.domain.AgentChatRequest;
import com.ruoyi.module.agent.domain.AgentChatView;
import com.ruoyi.module.agent.domain.ChatMessage;
import com.ruoyi.module.agent.domain.ChatSession;
import com.ruoyi.module.agent.domain.PythonChatRequest;
import com.ruoyi.module.agent.domain.PythonChatResponse;
import com.ruoyi.module.agent.domain.AgentToolCallSummary;
import com.ruoyi.module.agent.domain.DifyWorkflowResult;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class AgentChatServiceTest
{
    @Test
    void forwardsChatAndPersistsBothMessages()
    {
        AgentProperties properties = new AgentProperties();
        IChatSessionService sessionService = mock(IChatSessionService.class);
        AgentRoleService roleService = mock(AgentRoleService.class);
        AgentToolPolicyService policyService = mock(AgentToolPolicyService.class);
        AgentContextService contextService = mock(AgentContextService.class);
        AgentPythonClient pythonClient = mock(AgentPythonClient.class);
        AgentDifyClient difyClient = mock(AgentDifyClient.class);
        AgentChatService service = new AgentChatService(
                properties,
                sessionService,
                roleService,
                policyService,
                contextService,
                pythonClient,
                difyClient,
                new ObjectMapper());

        ChatSession session = new ChatSession();
        session.setId(101L);
        session.setEngine("python");
        when(sessionService.requireOwnedSession(101L, 7L)).thenReturn(session);
        when(sessionService.listMessages(101L, 7L)).thenReturn(List.of());
        when(roleService.currentRole()).thenReturn("STUDENT");
        when(policyService.allowedTools("STUDENT")).thenReturn(List.of("queryMyReservations"));
        when(contextService.issue(eq(7L), eq("STUDENT"), eq(101L), any(), any()))
                .thenReturn("signed-context");
        ChatMessage assistant = new ChatMessage();
        assistant.setId(205L);
        when(sessionService.appendMessage(eq(101L), eq(7L), eq("ASSISTANT"), eq("模型回答"), any()))
                .thenAnswer(invocation -> {
                    assistant.setSessionId(101L);
                    return assistant;
                });
        when(pythonClient.chat(eq("signed-context"), any(), any(PythonChatRequest.class)))
                .thenAnswer(invocation -> {
                    PythonChatRequest request = invocation.getArgument(2);
                    PythonChatResponse response = new PythonChatResponse();
                    response.setAnswer("模型回答");
                    response.setIntent("GENERAL");
                    AgentToolCallSummary toolCall = new AgentToolCallSummary();
                    toolCall.setToolName("queryMyReservations");
                    toolCall.setStatus("succeeded");
                    response.setToolCalls(List.of(toolCall));
                    response.setSessionId(request.getSessionId());
                    response.setTraceId(((String) invocation.getArgument(1)));
                    response.setModel("test-model");
                    return response;
                });

        AgentChatRequest request = new AgentChatRequest();
        request.setSessionId(101L);
        request.setMessage("你好");
        AgentChatView result = service.chat(7L, request);

        assertEquals(205L, result.getMessageId());
        assertEquals("模型回答", result.getAnswer());
        assertEquals("GENERAL", result.getIntent());
        assertEquals("queryMyReservations", result.getToolCalls().get(0).getToolName());
        assertEquals(null, result.getDraft());
        verify(sessionService).appendMessage(eq(101L), eq(7L), eq("USER"), eq("你好"), any());
        verify(sessionService).appendMessage(eq(101L), eq(7L), eq("ASSISTANT"), eq("模型回答"), any());
    }

    @Test
    void routesSafetyMessageThroughDify()
    {
        AgentProperties properties = new AgentProperties();
        IChatSessionService sessionService = mock(IChatSessionService.class);
        AgentRoleService roleService = mock(AgentRoleService.class);
        AgentToolPolicyService policyService = mock(AgentToolPolicyService.class);
        AgentContextService contextService = mock(AgentContextService.class);
        AgentPythonClient pythonClient = mock(AgentPythonClient.class);
        AgentDifyClient difyClient = mock(AgentDifyClient.class);
        AgentChatService service = new AgentChatService(
                properties,
                sessionService,
                roleService,
                policyService,
                contextService,
                pythonClient,
                difyClient,
                new ObjectMapper());

        ChatSession session = new ChatSession();
        session.setId(102L);
        session.setEngine("dify");
        when(sessionService.requireOwnedSession(102L, 7L)).thenReturn(session);
        when(sessionService.listMessages(102L, 7L)).thenReturn(List.of());
        when(roleService.currentRole()).thenReturn("STUDENT");
        when(policyService.allowedTools("STUDENT")).thenReturn(List.of("searchSafetyKnowledge"));
        when(contextService.issue(eq(7L), eq("STUDENT"), eq(102L), any(), any()))
                .thenReturn("signed-context");
        when(difyClient.runWorkflow(eq("safety"), any(), eq("signed-context"), eq("实验室安全规程是什么"), any()))
                .thenReturn(new DifyWorkflowResult(
                        "run-1",
                        "succeeded",
                        Map.of(
                                "answer", "Dify安全回答",
                                "intent", "SAFETY_KNOWLEDGE",
                                "tool_calls", List.of()),
                        null,
                        12L,
                        20L));
        ChatMessage assistant = new ChatMessage();
        assistant.setId(206L);
        when(sessionService.appendMessage(eq(102L), eq(7L), eq("ASSISTANT"), eq("Dify安全回答"), any()))
                .thenReturn(assistant);

        AgentChatRequest request = new AgentChatRequest();
        request.setSessionId(102L);
        request.setMessage("实验室安全规程是什么");
        request.setEngine("dify");
        AgentChatView result = service.chat(7L, request);

        assertEquals("Dify安全回答", result.getAnswer());
        assertEquals("Dify", result.getModel());
        verify(difyClient).runWorkflow(eq("safety"), any(), eq("signed-context"), any(), any());
        verify(pythonClient, org.mockito.Mockito.never())
                .chat(any(), any(), any(PythonChatRequest.class));
    }
}
