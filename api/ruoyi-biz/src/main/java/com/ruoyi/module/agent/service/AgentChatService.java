package com.ruoyi.module.agent.service;

import com.ruoyi.common.exception.ServiceException;
import com.ruoyi.module.agent.config.AgentProperties;
import com.ruoyi.module.agent.domain.AgentChatRequest;
import com.ruoyi.module.agent.domain.AgentChatView;
import com.ruoyi.module.agent.domain.AgentHistoryItem;
import com.ruoyi.module.agent.domain.ChatMessage;
import com.ruoyi.module.agent.domain.ChatSession;
import com.ruoyi.module.agent.domain.PythonChatRequest;
import com.ruoyi.module.agent.domain.PythonChatResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Locale;
import java.util.UUID;

/**
 * Spring Boot 同步对话 Gateway。
 */
@Service
@RequiredArgsConstructor
public class AgentChatService
{
    private static final int MAX_HISTORY_MESSAGES = 20;

    private final AgentProperties properties;

    private final IChatSessionService chatSessionService;

    private final AgentRoleService agentRoleService;

    private final AgentToolPolicyService toolPolicyService;

    private final AgentContextService contextService;

    private final AgentPythonClient pythonClient;

    public AgentChatView chat(Long userId, AgentChatRequest request)
    {
        if (!properties.isEnabled())
        {
            throw new ServiceException("智能助手当前未启用", HttpStatus.SERVICE_UNAVAILABLE.value());
        }
        ChatSession session = chatSessionService.requireOwnedSession(request.getSessionId(), userId);
        String engine = request.getEngine() == null || request.getEngine().isBlank()
                ? session.getEngine()
                : request.getEngine();
        if (!"python".equalsIgnoreCase(engine))
        {
            throw new ServiceException("当前对话接口仅支持 python 引擎", HttpStatus.SERVICE_UNAVAILABLE.value());
        }

        List<ChatMessage> storedMessages = chatSessionService.listMessages(request.getSessionId(), userId)
                .stream()
                .filter(message -> "USER".equals(message.getRole()) || "ASSISTANT".equals(message.getRole()))
                .toList();
        List<AgentHistoryItem> history = storedMessages.stream()
                .skip(Math.max(0, storedMessages.size() - MAX_HISTORY_MESSAGES))
                .map(message -> new AgentHistoryItem(
                        message.getRole().toLowerCase(Locale.ROOT),
                        message.getContent()))
                .toList();
        String role = agentRoleService.currentRole();
        String traceId = UUID.randomUUID().toString();
        String contextToken = contextService.issue(
                userId,
                role,
                request.getSessionId(),
                traceId,
                toolPolicyService.allowedTools(role));

        chatSessionService.appendMessage(
                request.getSessionId(),
                userId,
                "USER",
                request.getMessage(),
                traceId);
        PythonChatResponse pythonResponse = pythonClient.chat(
                contextToken,
                traceId,
                new PythonChatRequest(request.getSessionId(), request.getMessage(), history));
        if (!request.getSessionId().equals(pythonResponse.getSessionId())
                || !traceId.equals(pythonResponse.getTraceId()))
        {
            throw new ServiceException("Python Agent 返回的会话或追踪信息不一致");
        }
        ChatMessage assistantMessage = chatSessionService.appendMessage(
                request.getSessionId(),
                userId,
                "ASSISTANT",
                pythonResponse.getAnswer(),
                traceId);
        return new AgentChatView(
                assistantMessage.getId(),
                request.getSessionId(),
                pythonResponse.getAnswer(),
                pythonResponse.getIntent(),
                traceId,
                pythonResponse.getModel());
    }
}
