package com.ruoyi.module.agent.service.impl;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.RETURNS_DEEP_STUBS;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.util.Map;

import org.junit.jupiter.api.Test;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.test.util.ReflectionTestUtils;

import com.ruoyi.module.agent.service.AgentChatClientRouter;

/**
 * 智能助手模型连接测试。
 *
 * @author itjialu
 */
class AgentServiceImplTest
{
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
}
