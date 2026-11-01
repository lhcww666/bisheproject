package com.ruoyi.module.agent.service;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.ruoyi.common.exception.ServiceException;
import com.ruoyi.common.utils.StringUtils;
import com.ruoyi.module.agent.config.AgentProperties;
import com.ruoyi.module.agent.domain.AgentContextClaims;
import com.ruoyi.module.agent.domain.AgentHistoryItem;
import com.ruoyi.module.agent.domain.DifyWorkflowResult;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Java 侧 Dify Workflow API 客户端。
 */
@Component
public class AgentDifyClient
{
    private final AgentProperties properties;

    private final ObjectMapper objectMapper;

    private final HttpClient httpClient;

    @Autowired
    public AgentDifyClient(AgentProperties properties, ObjectMapper objectMapper)
    {
        this(properties, objectMapper, HttpClient.newBuilder()
                .connectTimeout(Duration.ofSeconds(3))
                .build());
    }

    AgentDifyClient(AgentProperties properties, ObjectMapper objectMapper, HttpClient httpClient)
    {
        this.properties = properties;
        this.objectMapper = objectMapper;
        this.httpClient = httpClient;
    }

    public DifyWorkflowResult runWorkflow(
            String workflowKey,
            AgentContextClaims context,
            String contextToken,
            String query,
            List<AgentHistoryItem> history)
    {
        String apiKey = resolveApiKey(workflowKey);
        if (StringUtils.isEmpty(properties.getDifyBaseUrl())
                || StringUtils.isEmpty(apiKey))
        {
            throw unavailable("Dify 服务地址或 API Key 未配置");
        }
        try
        {
            Map<String, Object> inputs = new LinkedHashMap<>();
            inputs.put("query", query);
            inputs.put("session_id", String.valueOf(context.getSessionId()));
            inputs.put("trace_id", context.getTraceId());
            inputs.put("role", context.getRole());
            inputs.put("history", objectMapper.writeValueAsString(history));
            inputs.put("agent_context", contextToken);

            Map<String, Object> body = new LinkedHashMap<>();
            body.put("inputs", inputs);
            body.put("response_mode", "blocking");
            body.put("user", "bishe-" + context.getUserId());

            String baseUrl = properties.getDifyBaseUrl().replaceAll("/+$", "");
            HttpRequest request = HttpRequest.newBuilder(URI.create(baseUrl + "/v1/workflows/run"))
                    .timeout(Duration.ofSeconds(Math.max(properties.getDifyTimeoutSeconds(), 1)))
                    .header("Authorization", "Bearer " + apiKey)
                    .header("Content-Type", "application/json")
                    .POST(HttpRequest.BodyPublishers.ofString(objectMapper.writeValueAsString(body)))
                    .build();
            HttpResponse<String> response = httpClient.send(
                    request,
                    HttpResponse.BodyHandlers.ofString());
            if (response.statusCode() != HttpStatus.OK.value())
            {
                throw unavailable(readErrorMessage(response.body(), response.statusCode()));
            }
            JsonNode root = objectMapper.readTree(response.body());
            JsonNode data = root.path("data");
            String workflowStatus = data.path("status").asText();
            if (!"succeeded".equals(workflowStatus))
            {
                throw unavailable(data.path("error").asText("Dify 工作流执行失败"));
            }
            Map<String, Object> outputs = objectMapper.convertValue(
                    data.path("outputs"),
                    new TypeReference<Map<String, Object>>() { });
            return new DifyWorkflowResult(
                    data.path("workflow_run_id").asText(root.path("workflow_run_id").asText()),
                    workflowStatus,
                    outputs,
                    data.path("error").isNull() ? null : data.path("error").asText(),
                    data.path("elapsed_time").isNumber() ? data.path("elapsed_time").asLong() : null,
                    data.path("total_tokens").isNumber() ? data.path("total_tokens").asLong() : null);
        }
        catch (ServiceException exception)
        {
            throw exception;
        }
        catch (InterruptedException exception)
        {
            Thread.currentThread().interrupt();
            throw unavailable("Dify 工作流调用被中断");
        }
        catch (Exception exception)
        {
            throw unavailable("Dify 工作流调用失败");
        }
    }

    private String resolveApiKey(String workflowKey)
    {
        if ("safety".equalsIgnoreCase(workflowKey)
                && StringUtils.isNotEmpty(properties.getDifySafetyApiKey()))
        {
            return properties.getDifySafetyApiKey();
        }
        if ("reservation".equalsIgnoreCase(workflowKey)
                && StringUtils.isNotEmpty(properties.getDifyReservationApiKey()))
        {
            return properties.getDifyReservationApiKey();
        }
        return properties.getDifyApiKey();
    }

    private String readErrorMessage(String body, int statusCode)
    {
        try
        {
            JsonNode root = objectMapper.readTree(body);
            String message = root.path("message").asText();
            if (StringUtils.isNotEmpty(message))
            {
                return message;
            }
        }
        catch (Exception ignored)
        {
        }
        return "Dify 工作流返回错误，HTTP " + statusCode;
    }

    private ServiceException unavailable(String message)
    {
        return new ServiceException(message, HttpStatus.SERVICE_UNAVAILABLE.value());
    }
}
