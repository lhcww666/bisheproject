package com.ruoyi.module.agent.domain;

import java.util.List;
import java.util.Map;

import lombok.AllArgsConstructor;
import lombok.Data;

/**
 * 浏览器同步对话结果。
 */
@Data
@AllArgsConstructor
public class AgentChatView
{
    private Long messageId;

    private Long sessionId;

    private String answer;

    private String intent;

    private List<AgentToolCallSummary> toolCalls;

    private List<AgentKnowledgeCitation> citations;

    private Map<String, Object> draft;

    private String traceId;

    private String model;
}
