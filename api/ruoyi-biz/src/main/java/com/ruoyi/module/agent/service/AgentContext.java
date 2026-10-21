package com.ruoyi.module.assistant.service;

import java.util.ArrayList;
import java.util.List;

import com.ruoyi.module.assistant.domain.ToolCallView;

/**
 * 一次聊天请求的工具调用上下文。同步调用下 Tool 与 ChatClient 在同一线程执行。
 *
 * @author itjialu
 */
public final class AssistantContext
{
    private static final ThreadLocal<Context> HOLDER = new ThreadLocal<>();

    private AssistantContext()
    {
    }

    public static void open(Long sessionId, Long userId)
    {
        Context context = new Context();
        context.sessionId = sessionId;
        context.userId = userId;
        HOLDER.set(context);
    }

    public static Context current()
    {
        return HOLDER.get();
    }

    public static Context require()
    {
        Context context = HOLDER.get();
        if (context == null)
        {
            throw new IllegalStateException("智能助手上下文不存在");
        }
        return context;
    }

    public static void close()
    {
        HOLDER.remove();
    }

    public static final class Context
    {
        private Long sessionId;
        private Long userId;
        private final List<ToolCallView> toolCalls = new ArrayList<>();

        public Long getSessionId()
        {
            return sessionId;
        }

        public Long getUserId()
        {
            return userId;
        }

        public List<ToolCallView> getToolCalls()
        {
            return toolCalls;
        }
    }
}
