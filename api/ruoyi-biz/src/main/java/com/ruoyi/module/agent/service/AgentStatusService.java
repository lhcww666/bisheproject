package com.ruoyi.module.agent.service;

import com.ruoyi.common.utils.SecurityUtils;
import com.ruoyi.common.utils.StringUtils;
import com.ruoyi.common.core.domain.entity.SysRole;
import com.ruoyi.module.agent.config.AgentProperties;
import com.ruoyi.module.agent.domain.AgentStatusView;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.util.List;

/**
 * Agent 服务状态检查。
 */
@Service
@RequiredArgsConstructor
public class AgentStatusService
{
    private final AgentProperties properties;

    public AgentStatusView getStatus()
    {
        String engine = properties.normalizedEngine();
        return new AgentStatusView(
                properties.isEnabled(),
                engine,
                StringUtils.isNotEmpty(properties.getPythonBaseUrl()),
                isPythonAvailable(),
                StringUtils.isNotEmpty(properties.getDifyBaseUrl()),
                resolveRole());
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

    private String resolveRole()
    {
        Long userId = SecurityUtils.getUserId();
        if (SecurityUtils.isAdmin(userId))
        {
            return "ADMIN";
        }
        List<SysRole> roles = SecurityUtils.getLoginUser().getUser().getRoles();
        if (roles == null)
        {
            return "USER";
        }
        List<String> roleKeys = roles.stream()
                .map(role -> role.getRoleKey())
                .toList();
        if (roleKeys.contains("admin"))
        {
            return "ADMIN";
        }
        if (roleKeys.contains("teacher"))
        {
            return "TEACHER";
        }
        if (roleKeys.contains("student"))
        {
            return "STUDENT";
        }
        return "USER";
    }
}
