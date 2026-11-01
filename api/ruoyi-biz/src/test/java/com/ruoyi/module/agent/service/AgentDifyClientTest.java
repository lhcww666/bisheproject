package com.ruoyi.module.agent.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.ruoyi.module.agent.config.AgentProperties;
import com.ruoyi.module.agent.domain.AgentContextClaims;
import com.ruoyi.module.agent.domain.AgentHistoryItem;
import com.ruoyi.module.agent.domain.DifyWorkflowResult;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class AgentDifyClientTest
{
    @Test
    @SuppressWarnings("unchecked")
    void callsWorkflowWithServerSideContext() throws Exception
    {
        HttpClient httpClient = mock(HttpClient.class);
        HttpResponse<String> response = mock(HttpResponse.class);
        when(response.statusCode()).thenReturn(200);
        when(response.body()).thenReturn(
                "{\"workflow_run_id\":\"run-1\",\"data\":{\"status\":\"succeeded\","
                        + "\"outputs\":{\"answer\":\"Dify回答\",\"intent\":\"GENERAL\"},"
                        + "\"error\":null,\"elapsed_time\":1.2,\"total_tokens\":20}}");
        when(httpClient.send(any(HttpRequest.class), any(HttpResponse.BodyHandler.class)))
                .thenReturn(response);
        AgentProperties properties = new AgentProperties();
        properties.setDifyBaseUrl("http://dify.local/");
        properties.setDifyApiKey("dify-key");
        ObjectMapper objectMapper = new ObjectMapper();
        AgentDifyClient client = new AgentDifyClient(properties, objectMapper, httpClient);
        AgentContextClaims context = new AgentContextClaims();
        context.setUserId(7L);
        context.setSessionId(101L);
        context.setRole("STUDENT");
        context.setTraceId("trace-1");

        DifyWorkflowResult result = client.runWorkflow(
                "safety",
                context,
                "signed-context",
                "你好",
                List.of(new AgentHistoryItem("user", "上一轮")));

        assertEquals("Dify回答", result.getOutputs().get("answer"));
        ArgumentCaptor<HttpRequest> captor = ArgumentCaptor.forClass(HttpRequest.class);
        verify(httpClient).send(captor.capture(), any(HttpResponse.BodyHandler.class));
        HttpRequest request = captor.getValue();
        assertEquals("Bearer dify-key", request.headers().firstValue("Authorization").orElse(null));
        assertTrue(request.uri().toString().endsWith("/v1/workflows/run"));
    }
}
