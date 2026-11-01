package com.ruoyi.module.agent.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.ruoyi.module.agent.config.AgentProperties;
import com.ruoyi.module.agent.domain.AgentHistoryItem;
import com.ruoyi.module.agent.domain.PythonChatRequest;
import com.ruoyi.module.agent.domain.PythonChatResponse;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class AgentPythonClientTest
{
    @Test
    @SuppressWarnings("unchecked")
    void sendsSignedContextAndParsesResponse() throws Exception
    {
        HttpClient httpClient = mock(HttpClient.class);
        HttpResponse<String> response = mock(HttpResponse.class);
        when(response.statusCode()).thenReturn(200);
        when(response.body()).thenReturn(
                "{\"answer\":\"模型回答\",\"intent\":\"GENERAL\","
                        + "\"toolCalls\":[{\"toolName\":\"queryMyReservations\","
                        + "\"status\":\"succeeded\",\"error\":null}],"
                        + "\"citations\":[{\"documentId\":\"safety-1\","
                        + "\"title\":\"实验室火灾处置\",\"source\":\"lab-safety.md\","
                        + "\"snippet\":\"先切断电源。\",\"score\":0.9}],\"sessionId\":101,"
                        + "\"traceId\":\"trace-1\",\"model\":\"test-model\"}");
        when(httpClient.send(any(HttpRequest.class), any(HttpResponse.BodyHandler.class)))
                .thenReturn(response);
        AgentProperties properties = new AgentProperties();
        properties.setPythonBaseUrl("http://127.0.0.1:8091/");
        AgentPythonClient client = new AgentPythonClient(properties, new ObjectMapper(), httpClient);

        PythonChatResponse result = client.chat(
                "signed-context",
                "trace-1",
                new PythonChatRequest(101L, "你好", List.<AgentHistoryItem>of()));

        assertEquals("模型回答", result.getAnswer());
        assertEquals("GENERAL", result.getIntent());
        assertEquals("queryMyReservations", result.getToolCalls().get(0).getToolName());
        assertEquals("safety-1", result.getCitations().get(0).getDocumentId());
        assertEquals(null, result.getDraft());
        ArgumentCaptor<HttpRequest> requestCaptor = ArgumentCaptor.forClass(HttpRequest.class);
        verify(httpClient).send(requestCaptor.capture(), any(HttpResponse.BodyHandler.class));
        assertEquals("signed-context", requestCaptor.getValue().headers()
                .firstValue("X-Agent-Context").orElse(null));
        assertEquals("/api/ai/chat", requestCaptor.getValue().uri().getPath());
    }
}
