package com.ruoyi.module.agent.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.ruoyi.common.exception.ServiceException;
import com.ruoyi.common.utils.SecurityUtils;
import com.ruoyi.module.agent.config.AgentProperties;
import com.ruoyi.module.agent.domain.AgentChatRequest;
import com.ruoyi.module.agent.domain.AgentHistoryItem;
import com.ruoyi.module.agent.domain.ChatMessage;
import com.ruoyi.module.agent.domain.ChatSession;
import com.ruoyi.module.agent.domain.PythonChatRequest;
import com.ruoyi.module.agent.domain.PythonChatResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import java.io.BufferedReader;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ExecutorService;

/**
 * Spring Boot SSE 对话 Gateway。
 */
@Service
@RequiredArgsConstructor
public class AgentStreamService
{
    private static final int MAX_HISTORY_MESSAGES = 20;

    private final AgentProperties properties;

    private final IChatSessionService chatSessionService;

    private final AgentRoleService agentRoleService;

    private final AgentToolPolicyService toolPolicyService;

    private final AgentContextService contextService;

    private final AgentPythonClient pythonClient;

    private final ObjectMapper objectMapper;

    private final ExecutorService agentStreamExecutor;

    public SseEmitter stream(Long userId, AgentChatRequest request)
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
            throw new ServiceException("当前流式接口仅支持 python 引擎", HttpStatus.SERVICE_UNAVAILABLE.value());
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
        String operator = SecurityUtils.getUsername();
        chatSessionService.appendMessage(
                request.getSessionId(),
                userId,
                "USER",
                request.getMessage(),
                traceId,
                operator);

        SseEmitter emitter = new SseEmitter(120_000L);
        agentStreamExecutor.submit(() -> streamBlocking(
                emitter,
                request,
                userId,
                operator,
                traceId,
                contextToken,
                history));
        return emitter;
    }

    private void streamBlocking(
            SseEmitter emitter,
            AgentChatRequest request,
            Long userId,
            String operator,
            String traceId,
            String contextToken,
            List<AgentHistoryItem> history)
    {
        boolean terminal = false;
        try
        {
            InputStream input = pythonClient.streamChat(
                    contextToken,
                    traceId,
                    new PythonChatRequest(request.getSessionId(), request.getMessage(), history));
            try (BufferedReader reader = new BufferedReader(
                    new InputStreamReader(input, StandardCharsets.UTF_8)))
            {
                String eventName = "message";
                String data = null;
                String line;
                while ((line = reader.readLine()) != null)
                {
                    if (line.startsWith("event:"))
                    {
                        eventName = line.substring(6).trim();
                    }
                    else if (line.startsWith("data:"))
                    {
                        data = line.substring(5).trim();
                    }
                    else if (line.isBlank() && data != null)
                    {
                        terminal = handleEvent(
                                emitter,
                                eventName,
                                data,
                                request,
                                userId,
                                operator,
                                traceId);
                        eventName = "message";
                        data = null;
                        if (terminal)
                        {
                            break;
                        }
                    }
                }
                if (!terminal && data != null)
                {
                    terminal = handleEvent(
                            emitter,
                            eventName,
                            data,
                            request,
                            userId,
                            operator,
                            traceId);
                }
            }
            if (!terminal)
            {
                sendError(emitter, "AGENT_STREAM_INCOMPLETE", "Agent 流式响应意外结束");
            }
        }
        catch (Exception exception)
        {
            try
            {
                sendError(
                        emitter,
                        "AGENT_STREAM_FAILED",
                        exception instanceof ServiceException
                                ? exception.getMessage()
                                : "Agent 流式服务调用失败");
            }
            catch (Exception ignored)
            {
            }
        }
        finally
        {
            emitter.complete();
        }
    }

    private boolean handleEvent(
            SseEmitter emitter,
            String eventName,
            String data,
            AgentChatRequest request,
            Long userId,
            String operator,
            String traceId) throws Exception
    {
        JsonNode payload = objectMapper.readTree(data);
        if ("done".equals(eventName))
        {
            PythonChatResponse response = objectMapper.treeToValue(payload, PythonChatResponse.class);
            if (!request.getSessionId().equals(response.getSessionId())
                    || !traceId.equals(response.getTraceId()))
            {
                throw new ServiceException("Python Agent 返回的会话或追踪信息不一致");
            }
            ChatMessage assistant = chatSessionService.appendMessage(
                    request.getSessionId(),
                    userId,
                    "ASSISTANT",
                    response.getAnswer(),
                    traceId,
                    operator);
            Map<String, Object> done = new LinkedHashMap<>();
            done.put("type", "done");
            done.put("messageId", assistant.getId());
            done.put("sessionId", request.getSessionId());
            done.put("traceId", traceId);
            done.put("intent", response.getIntent());
            done.put("toolCalls", response.getToolCalls() == null ? List.of() : response.getToolCalls());
            done.put("draft", response.getDraft());
            done.put("model", response.getModel());
            emitter.send(SseEmitter.event()
                    .name("done")
                    .data(objectMapper.writeValueAsString(done), MediaType.TEXT_PLAIN));
            return true;
        }
        emitter.send(SseEmitter.event()
                .name(eventName)
                .data(data, MediaType.TEXT_PLAIN));
        return "error".equals(eventName);
    }

    private void sendError(SseEmitter emitter, String errorCode, String message) throws Exception
    {
        Map<String, Object> error = new LinkedHashMap<>();
        error.put("type", "error");
        error.put("errorCode", errorCode);
        error.put("message", message);
        error.put("retryable", true);
        emitter.send(SseEmitter.event()
                .name("error")
                .data(objectMapper.writeValueAsString(error), MediaType.TEXT_PLAIN));
    }
}
