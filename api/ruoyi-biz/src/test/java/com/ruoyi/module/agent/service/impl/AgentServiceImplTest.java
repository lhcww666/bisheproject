package com.ruoyi.module.agent.service.impl;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.RETURNS_DEEP_STUBS;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.util.Collections;
import java.util.List;
import java.util.Map;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.test.util.ReflectionTestUtils;

import com.baomidou.mybatisplus.core.conditions.update.UpdateWrapper;
import com.ruoyi.common.core.domain.model.LoginUser;
import com.ruoyi.module.agent.domain.ChatMessage;
import com.ruoyi.module.agent.domain.ChatRequest;
import com.ruoyi.module.agent.domain.ChatSession;
import com.ruoyi.module.agent.domain.ToolCallLog;
import com.ruoyi.module.agent.mapper.ChatMessageMapper;
import com.ruoyi.module.agent.mapper.ChatSessionMapper;
import com.ruoyi.module.agent.mapper.ToolCallLogMapper;
import com.ruoyi.module.agent.service.AgentChatClientRouter;
import com.ruoyi.module.agent.service.AgentContext;

/**
 * 智能助手模型连接测试。
 *
 * @author itjialu
 */
class AgentServiceImplTest
{
    @AfterEach
    void clearSecurityContext()
    {
        SecurityContextHolder.clearContext();
        AgentContext.close();
    }

    @Test
    void missingKeyReturnsActionableStatus()
    {
        AgentServiceImpl service = new AgentServiceImpl(null, null, null, null);
        ReflectionTestUtils.setField(service, "apiKey", "not-configured");
        ReflectionTestUtils.setField(service, "modelName", "deepseek-chat");

        Map<String, Object> result = service.checkModelConnection();

        assertFalse((Boolean) result.get("ok"));
        assertFalse((Boolean) result.get("configured"));
        assertTrue(String.valueOf(result.get("message")).contains("DEEPSEEK_API_KEY"));
    }

    @Test
    void configuredModelReturnsConnectionSuccess()
    {
        ChatClient chatClient = mock(ChatClient.class, RETURNS_DEEP_STUBS);
        when(chatClient.prompt().system(anyString()).user(anyString()).call().content()).thenReturn("OK");
        AgentChatClientRouter router = mock(AgentChatClientRouter.class);
        when(router.current()).thenReturn(chatClient);
        AgentServiceImpl service = new AgentServiceImpl(null, null, null, router);
        ReflectionTestUtils.setField(service, "apiKey", "sk-test");
        ReflectionTestUtils.setField(service, "modelName", "deepseek-chat");

        Map<String, Object> result = service.checkModelConnection();

        assertTrue((Boolean) result.get("ok"));
        assertTrue((Boolean) result.get("configured"));
        assertEquals("deepseek-chat", result.get("model"));
    }

    @Test
    void listMessagesRestoresToolCallsForAssistantMessage()
    {
        ChatSessionMapper sessionMapper = mock(ChatSessionMapper.class);
        ChatMessageMapper messageMapper = mock(ChatMessageMapper.class);
        ToolCallLogMapper toolCallLogMapper = mock(ToolCallLogMapper.class);
        ChatSession session = new ChatSession();
        session.setId(7L);
        session.setUserId(42L);
        when(sessionMapper.selectById(7L)).thenReturn(session);

        ChatMessage userMessage = message(11L, ChatMessage.ROLE_USER, "查询我的信用分");
        ChatMessage assistantMessage = message(12L, ChatMessage.ROLE_ASSISTANT, "你的信用分为 95 分");
        when(messageMapper.selectList(any())).thenReturn(List.of(userMessage, assistantMessage));

        ToolCallLog log = new ToolCallLog();
        log.setId(21L);
        log.setMessageId(12L);
        log.setToolName("getMyCreditSummary");
        log.setArguments("{}");
        log.setResult("信用分 95");
        log.setSuccess("1");
        log.setCostMs(18);
        when(toolCallLogMapper.selectList(any())).thenReturn(List.of(log));

        authenticate(42L);
        AgentServiceImpl service = new AgentServiceImpl(sessionMapper, messageMapper, toolCallLogMapper, null);
        List<ChatMessage> messages = service.listMessages(7L);

        assertTrue(messages.get(0).getToolCalls().isEmpty());
        assertEquals(1, messages.get(1).getToolCalls().size());
        assertEquals("getMyCreditSummary", messages.get(1).getToolCalls().get(0).getToolName());
        assertTrue(messages.get(1).getToolCalls().get(0).getSuccess());
    }

    @Test
    void chatBindsToolAuditRecordsToAssistantMessage()
    {
        ChatSessionMapper sessionMapper = mock(ChatSessionMapper.class);
        ChatMessageMapper messageMapper = mock(ChatMessageMapper.class);
        ToolCallLogMapper toolCallLogMapper = mock(ToolCallLogMapper.class);
        ChatSession session = new ChatSession();
        session.setId(7L);
        session.setUserId(42L);
        session.setTitle("已有会话");
        when(sessionMapper.selectById(7L)).thenReturn(session);
        when(messageMapper.selectList(any())).thenReturn(Collections.emptyList());
        when(messageMapper.insert(any(ChatMessage.class))).thenAnswer(invocation -> {
            ChatMessage message = invocation.getArgument(0);
            message.setId(ChatMessage.ROLE_ASSISTANT.equals(message.getRole()) ? 900L : 800L);
            return 1;
        });

        ChatClient chatClient = mock(ChatClient.class, RETURNS_DEEP_STUBS);
        when(chatClient.prompt().messages(anyList()).call().content()).thenAnswer(invocation -> {
            AgentContext.Context context = AgentContext.current();
            context.getToolCallLogIds().add(201L);
            context.getToolCallLogIds().add(202L);
            return "已完成查询。";
        });
        AgentChatClientRouter router = mock(AgentChatClientRouter.class);
        when(router.current()).thenReturn(chatClient);

        authenticate(42L);
        AgentServiceImpl service = new AgentServiceImpl(sessionMapper, messageMapper, toolCallLogMapper, router);
        ReflectionTestUtils.setField(service, "apiKey", "sk-test");
        ChatRequest request = new ChatRequest();
        request.setSessionId(7L);
        request.setMessage("帮我查一下预约");

        service.chat(request);

        ArgumentCaptor<UpdateWrapper<ToolCallLog>> updateCaptor =
                ArgumentCaptor.forClass(UpdateWrapper.class);
        verify(toolCallLogMapper).update(any(), updateCaptor.capture());
        assertTrue(updateCaptor.getValue().getSqlSet().contains("message_id"));
        assertTrue(updateCaptor.getValue().getParamNameValuePairs().containsValue(900L));
    }

    private ChatMessage message(Long id, String role, String content)
    {
        ChatMessage message = new ChatMessage();
        message.setId(id);
        message.setRole(role);
        message.setContent(content);
        return message;
    }

    private void authenticate(Long userId)
    {
        LoginUser loginUser = new LoginUser();
        loginUser.setUserId(userId);
        SecurityContextHolder.getContext().setAuthentication(
                new UsernamePasswordAuthenticationToken(loginUser, null, Collections.emptyList()));
    }
}
