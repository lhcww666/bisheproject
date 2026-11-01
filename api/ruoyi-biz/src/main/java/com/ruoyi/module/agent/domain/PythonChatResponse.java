package com.ruoyi.module.agent.domain;

import lombok.Data;

import java.util.List;
import java.util.Map;

/**
 * FastAPI 同步对话响应。
 */
@Data
public class PythonChatResponse
{
    private String answer;

    private String intent;

    private List<AgentToolCallSummary> toolCalls;

    private List<AgentKnowledgeCitation> citations;

    private Map<String, Object> draft;

    private Long sessionId;

    private String traceId;

    private String model;
}
