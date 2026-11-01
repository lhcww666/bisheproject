package com.ruoyi.module.agent.domain;

import lombok.AllArgsConstructor;
import lombok.Data;

import java.util.List;

/**
 * FastAPI 同步对话请求。
 */
@Data
@AllArgsConstructor
public class PythonChatRequest
{
    private Long sessionId;

    private String message;

    private List<AgentHistoryItem> history;
}
