package com.ruoyi.module.agent.service;

import com.ruoyi.common.utils.StringUtils;
import com.ruoyi.module.agent.config.AgentProperties;
import com.ruoyi.module.agent.domain.AgentStatusView;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;

/**
 * Agent 服务状态检查。
 */
@Service
@RequiredArgsConstructor
public class AgentStatusService
{
    private final AgentProperties properties;

    private final AgentRoleService agentRoleService;

    public AgentStatusView getStatus()
    {
        String engine = properties.normalizedEngine();
        return new AgentStatusView(
                properties.isEnabled(),
                engine,
                StringUtils.isNotEmpty(properties.getPythonBaseUrl()),
                isPythonAvailable(),
                StringUtils.isNotEmpty(properties.getDifyBaseUrl()),
                agentRoleService.currentRole());
    }

    private boolean isPythonAvailable()
    {
        if (!properties.isEnabled() || StringUtils.isEmpty(properties.getPythonBaseUrl()))
        {
            return false;
        }
        try
        {
            String baseUrl = properties.getPythonBaseUrl().replaceAll("/+$", "");
            HttpRequest request = HttpRequest.newBuilder(URI.create(baseUrl + "/api/ai/health"))
                    .timeout(Duration.ofSeconds(1))
                    .GET()
                    .build();
            HttpResponse<Void> response = HttpClient.newBuilder()
                    .connectTimeout(Duration.ofSeconds(1))
                    .build()
                    .send(request, HttpResponse.BodyHandlers.discarding());
            return response.statusCode() == 200;
        }
        catch (Exception ignored)
        {
            return false;
        }
    }

}
