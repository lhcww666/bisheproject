package com.ruoyi.module.agent.service;

import com.ruoyi.module.agent.domain.AgentSessionCreateRequest;
import com.ruoyi.module.agent.domain.ChatMessage;
import com.ruoyi.module.agent.domain.ChatSession;

import java.util.List;

/**
 * 智能助手会话服务。
 */
public interface IChatSessionService
{
    ChatSession createSession(Long userId, AgentSessionCreateRequest request, String defaultEngine);

    List<ChatSession> listByUser(Long userId);

    ChatSession requireOwnedSession(Long sessionId, Long userId);

    List<ChatMessage> listMessages(Long sessionId, Long userId);

    ChatMessage appendMessage(Long sessionId, Long userId, String role, String content, String traceId);

    ChatMessage appendMessage(
            Long sessionId,
            Long userId,
            String role,
            String content,
            String traceId,
            String operator);

    ChatMessage appendAssistantMessage(
            Long sessionId,
            Long userId,
            String content,
            String traceId,
            String referenceJson);

    ChatMessage appendAssistantMessage(
            Long sessionId,
            Long userId,
            String content,
            String traceId,
            String referenceJson,
            String operator);

    void deleteOwnedSession(Long sessionId, Long userId);
}
