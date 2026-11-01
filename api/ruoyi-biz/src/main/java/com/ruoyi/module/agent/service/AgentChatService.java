package com.ruoyi.module.agent.service;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.ruoyi.common.exception.ServiceException;
import com.ruoyi.module.agent.config.AgentProperties;
import com.ruoyi.module.agent.domain.AgentChatRequest;
import com.ruoyi.module.agent.domain.AgentChatView;
import com.ruoyi.module.agent.domain.AgentHistoryItem;
import com.ruoyi.module.agent.domain.AgentKnowledgeCitation;
import com.ruoyi.module.agent.domain.ChatMessage;
import com.ruoyi.module.agent.domain.ChatSession;
import com.ruoyi.module.agent.domain.AgentContextClaims;
import com.ruoyi.module.agent.domain.AgentToolCallSummary;
import com.ruoyi.module.agent.domain.DifyWorkflowResult;
import com.ruoyi.module.agent.domain.PythonChatRequest;
import com.ruoyi.module.agent.domain.PythonChatResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Locale;
import java.util.Map;
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

    private final AgentDifyClient difyClient;

    private final ObjectMapper objectMapper;

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
        if (!"python".equalsIgnoreCase(engine) && !"dify".equalsIgnoreCase(engine))
        {
            throw new ServiceException("不支持的 Agent 引擎", HttpStatus.SERVICE_UNAVAILABLE.value());
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
        List<String> allowedTools = toolPolicyService.allowedTools(role);
        String contextToken = contextService.issue(
                userId,
                role,
                request.getSessionId(),
                traceId,
                allowedTools);

        chatSessionService.appendMessage(
                request.getSessionId(),
                userId,
                "USER",
                request.getMessage(),
                traceId);
        AgentChatView result;
        if ("dify".equalsIgnoreCase(engine))
        {
            result = chatWithDify(
                    userId,
                    role,
                    request,
                    traceId,
                    contextToken,
                    history,
                    allowedTools);
        }
        else
        {
            PythonChatResponse pythonResponse = pythonClient.chat(
                    contextToken,
                    traceId,
                    new PythonChatRequest(request.getSessionId(), request.getMessage(), history));
            if (!request.getSessionId().equals(pythonResponse.getSessionId())
                    || !traceId.equals(pythonResponse.getTraceId()))
            {
                throw new ServiceException("Python Agent 返回的会话或追踪信息不一致");
            }
            List<AgentKnowledgeCitation> citations = pythonResponse.getCitations() == null
                    ? List.of()
                    : pythonResponse.getCitations();
            ChatMessage assistantMessage = chatSessionService.appendAssistantMessage(
                    request.getSessionId(),
                    userId,
                    pythonResponse.getAnswer(),
                    traceId,
                    serializeReferences(citations));
            result = new AgentChatView(
                    assistantMessage.getId(),
                    request.getSessionId(),
                    pythonResponse.getAnswer(),
                    pythonResponse.getIntent(),
                    pythonResponse.getToolCalls() == null
                            ? List.of()
                            : pythonResponse.getToolCalls(),
                    citations,
                    pythonResponse.getDraft(),
                    traceId,
                    pythonResponse.getModel());
        }
        return result;
    }

    private AgentChatView chatWithDify(
            Long userId,
            String role,
            AgentChatRequest request,
            String traceId,
            String contextToken,
            List<AgentHistoryItem> history,
            List<String> allowedTools)
    {
        AgentContextClaims context = new AgentContextClaims(
                1,
                userId,
                role,
                request.getSessionId(),
                traceId,
                allowedTools,
                0,
                0);
        String workflowKey = isSafetyQuestion(request.getMessage()) ? "safety" : "reservation";
        DifyWorkflowResult difyResult = difyClient.runWorkflow(
                workflowKey,
                context,
                contextToken,
                request.getMessage(),
                history);
        Map<String, Object> outputs = difyResult.getOutputs();
        String answer = String.valueOf(outputs.getOrDefault("answer", ""));
        if (answer.isBlank())
        {
            throw new ServiceException("Dify 工作流没有返回有效回答");
        }
        String intent = String.valueOf(outputs.getOrDefault("intent", "GENERAL"));
        Object rawToolCalls = outputs.containsKey("toolCalls")
                ? outputs.get("toolCalls")
                : outputs.get("tool_calls");
        List<AgentToolCallSummary> toolCalls = rawToolCalls == null
                ? List.of()
                : objectMapper.convertValue(
                        rawToolCalls,
                        new TypeReference<List<AgentToolCallSummary>>() { });
        Object rawCitations = outputs.get("citations");
        List<AgentKnowledgeCitation> citations = rawCitations == null
                ? List.of()
                : objectMapper.convertValue(
                        rawCitations,
                        new TypeReference<List<AgentKnowledgeCitation>>() { });
        Object rawDraft = outputs.get("draft");
        Map<String, Object> draft = rawDraft instanceof Map<?, ?>
                ? objectMapper.convertValue(
                        rawDraft,
                        new TypeReference<Map<String, Object>>() { })
                : null;
        ChatMessage assistantMessage = chatSessionService.appendAssistantMessage(
                request.getSessionId(),
                userId,
                answer,
                traceId,
                serializeReferences(citations));
        return new AgentChatView(
                assistantMessage.getId(),
                request.getSessionId(),
                answer,
                intent,
                toolCalls,
                citations,
                draft,
                traceId,
                "Dify");
    }

    private boolean isSafetyQuestion(String message)
    {
        String text = message == null ? "" : message.toLowerCase(Locale.ROOT);
        return text.contains("安全")
                || text.contains("着火")
                || text.contains("泄漏")
                || text.contains("防护")
                || text.contains("应急")
                || text.contains("气体")
                || text.contains("化学品")
                || text.contains("灭火")
                || text.contains("洗眼器")
                || text.contains("危险废物");
    }

    private String serializeReferences(List<AgentKnowledgeCitation> citations)
    {
        return objectMapper.valueToTree(citations).toString();
    }
}
