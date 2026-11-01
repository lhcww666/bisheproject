package com.ruoyi.module.agent.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.ruoyi.common.exception.ServiceException;
import com.ruoyi.common.utils.StringUtils;
import com.ruoyi.module.agent.config.AgentProperties;
import com.ruoyi.module.agent.domain.PythonChatRequest;
import com.ruoyi.module.agent.domain.PythonChatResponse;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;

/**
 * 调用 FastAPI Agent 服务。
 */
@Component
public class AgentPythonClient
{
    private final AgentProperties properties;

    private final ObjectMapper objectMapper;

    private final HttpClient httpClient;

    public AgentPythonClient(AgentProperties properties, ObjectMapper objectMapper)
    {
        this(properties, objectMapper, HttpClient.newBuilder()
                .connectTimeout(Duration.ofSeconds(3))
                .build());
    }

    AgentPythonClient(AgentProperties properties, ObjectMapper objectMapper, HttpClient httpClient)
    {
        this.properties = properties;
        this.objectMapper = objectMapper;
        this.httpClient = httpClient;
    }

    public PythonChatResponse chat(String contextToken, String requestId, PythonChatRequest request)
    {
        if (StringUtils.isEmpty(properties.getPythonBaseUrl()))
        {
            throw unavailable("Python Agent 服务地址未配置");
        }
        try
        {
            String baseUrl = properties.getPythonBaseUrl().replaceAll("/+$", "");
            HttpRequest httpRequest = HttpRequest.newBuilder(URI.create(baseUrl + "/api/ai/chat"))
                    .timeout(Duration.ofSeconds(Math.max(properties.getPythonTimeoutSeconds(), 1)))
                    .header("Content-Type", "application/json")
                    .header("X-Agent-Context", contextToken)
                    .header("X-Request-Id", requestId)
                    .POST(HttpRequest.BodyPublishers.ofString(objectMapper.writeValueAsString(request)))
                    .build();
            HttpResponse<String> response = httpClient.send(
                    httpRequest,
                    HttpResponse.BodyHandlers.ofString());
            if (response.statusCode() != HttpStatus.OK.value())
            {
                throw unavailable(readErrorMessage(response.body(), response.statusCode()));
            }
            return objectMapper.readValue(response.body(), PythonChatResponse.class);
        }
        catch (ServiceException exception)
        {
            throw exception;
        }
        catch (InterruptedException exception)
        {
            Thread.currentThread().interrupt();
            throw unavailable("Python Agent 调用被中断");
        }
        catch (Exception exception)
        {
            throw unavailable("Python Agent 服务调用失败");
        }
    }

    private String readErrorMessage(String body, int statusCode)
    {
        try
        {
            String message = objectMapper.readTree(body).path("msg").asText();
            if (StringUtils.isNotEmpty(message))
            {
                return message;
            }
        }
        catch (Exception ignored)
        {
        }
        return "Python Agent 服务返回错误，HTTP " + statusCode;
    }

    private ServiceException unavailable(String message)
    {
        return new ServiceException(message, HttpStatus.SERVICE_UNAVAILABLE.value());
    }
}
