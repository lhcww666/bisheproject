package com.ruoyi.module.agent.domain;

import lombok.Data;

/**
 * FastAPI 同步对话响应。
 */
@Data
public class PythonChatResponse
{
    private String answer;

    private String intent;

    private Long sessionId;

    private String traceId;

    private String model;
}
