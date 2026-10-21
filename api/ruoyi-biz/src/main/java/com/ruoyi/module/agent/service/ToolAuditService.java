package com.ruoyi.module.assistant.service;

import java.util.Date;

import org.springframework.stereotype.Service;

import com.ruoyi.module.assistant.domain.ToolCallLog;
import com.ruoyi.module.assistant.domain.ToolCallView;
import com.ruoyi.module.assistant.mapper.ToolCallLogMapper;

import lombok.RequiredArgsConstructor;

/**
 * 工具调用审计。
 *
 * @author itjialu
 */
@Service
@RequiredArgsConstructor
public class ToolAuditService
{
    private final ToolCallLogMapper toolCallLogMapper;

    public void record(String toolName, String arguments, String result, boolean success, long costMs)
    {
        AssistantContext.Context context = AssistantContext.current();
        ToolCallLog log = new ToolCallLog();
        log.setSessionId(context == null ? null : context.getSessionId());
        log.setUserId(context == null ? null : context.getUserId());
        log.setToolName(toolName);
        log.setArguments(truncate(arguments, 4000));
        log.setResult(truncate(result, 8000));
        log.setSuccess(success ? "1" : "0");
        log.setCostMs((int) Math.min(Integer.MAX_VALUE, costMs));
        log.setCreateTime(new Date());
        toolCallLogMapper.insert(log);

        ToolCallView view = new ToolCallView();
        view.setToolName(toolName);
        view.setArguments(log.getArguments());
        view.setResult(log.getResult());
        view.setSuccess(success);
        view.setCostMs(log.getCostMs());
        if (context != null)
        {
            context.getToolCalls().add(view);
        }
    }

    private String truncate(String value, int max)
    {
        if (value == null || value.length() <= max)
        {
            return value;
        }
        return value.substring(0, max) + "...";
    }
}
