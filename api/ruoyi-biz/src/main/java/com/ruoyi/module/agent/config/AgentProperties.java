package com.ruoyi.module.agent.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

/**
 * Python Agent 服务配置。
 */
@Data
@Component
@ConfigurationProperties(prefix = "bishe.ai")
public class AgentProperties
{
    private boolean enabled = true;

    private String engine = "python";

    private String pythonBaseUrl = "http://127.0.0.1:8091";

    private String difyBaseUrl;

    private String difyApiKey;

    private String contextSecret;

    private long contextTtlSeconds = 300;

    private long pythonTimeoutSeconds = 60;

    private long difyTimeoutSeconds = 60;

    private long draftTtlMinutes = 30;

    public String normalizedEngine()
    {
        return engine == null || engine.isBlank() ? "python" : engine.trim().toLowerCase();
    }
}
