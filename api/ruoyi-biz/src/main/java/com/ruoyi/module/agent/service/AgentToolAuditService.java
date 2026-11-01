package com.ruoyi.module.agent.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.ruoyi.common.utils.DateUtils;
import com.ruoyi.module.agent.domain.AgentContextClaims;
import com.ruoyi.module.agent.domain.ToolCallLog;
import com.ruoyi.module.agent.mapper.ToolCallLogMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.Map;

/**
 * Agent 工具调用审计。
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class AgentToolAuditService
{
    private static final int MAX_FIELD_LENGTH = 4000;

    private final ToolCallLogMapper toolCallLogMapper;

    private final ObjectMapper objectMapper;

    public void success(
            AgentContextClaims context,
            String toolCallId,
            String toolName,
            Map<String, Object> arguments,
            Map<String, Object> resultSummary,
            long costMs)
    {
        record(context, toolCallId, toolName, arguments, resultSummary, "1", null, costMs);
    }

    public void failure(
            AgentContextClaims context,
            String toolCallId,
            String toolName,
            Map<String, Object> arguments,
            String errorCode,
            String errorMessage,
            long costMs)
    {
        record(
                context,
                toolCallId,
                toolName,
                arguments,
                Map.of("error", errorMessage == null ? "" : errorMessage),
                "0",
                errorCode,
                costMs);
    }

    private void record(
            AgentContextClaims context,
            String toolCallId,
            String toolName,
            Map<String, Object> arguments,
            Map<String, Object> result,
            String success,
            String errorCode,
            long costMs)
    {
        try
        {
            ToolCallLog logRecord = new ToolCallLog();
            logRecord.setSessionId(context == null ? null : context.getSessionId());
            logRecord.setUserId(context == null ? null : context.getUserId());
            logRecord.setToolName(toolName);
            logRecord.setToolCallId(toolCallId);
            logRecord.setArguments(truncate(objectMapper.writeValueAsString(arguments)));
            logRecord.setResult(truncate(objectMapper.writeValueAsString(result)));
            logRecord.setSuccess(success);
            logRecord.setErrorCode(errorCode);
            logRecord.setCostMs(costMs);
            logRecord.setCreateTime(DateUtils.getNowDate());
            toolCallLogMapper.insert(logRecord);
        }
        catch (Exception exception)
        {
            log.warn("记录 Agent 工具审计失败: {}", toolName, exception);
        }
    }

    private String truncate(String value)
    {
        return value.length() <= MAX_FIELD_LENGTH
                ? value
                : value.substring(0, MAX_FIELD_LENGTH);
    }
}
