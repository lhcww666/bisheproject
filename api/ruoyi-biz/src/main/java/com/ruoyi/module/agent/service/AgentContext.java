package com.ruoyi.module.agent.service;

import java.util.ArrayList;
import java.util.List;

import com.ruoyi.module.agent.domain.ToolCallView;

/**
 * 一次聊天请求的 Agent 上下文。
 *
 * <p>Spring AI 在同步 call() 模式下与工具方法运行在同一线程，因此这里用
 * ThreadLocal 传递当前会话和用户身份。这样模型只能调用工具，无法通过参数
 * 伪造 userId 或 sessionId。</p>
 *
 * <p>请求结束后必须在 finally 中调用 {@link #close()}，否则 Web 线程池复用
 * 时可能把上一个用户的身份泄漏给下一个请求。</p>
 *
 * @author itjialu
 */
public final class AgentContext
{
    /**
     * 当前线程对应的 Agent 上下文。
     */
    private static final ThreadLocal<Context> HOLDER = new ThreadLocal<>();

    private AgentContext()
    {
    }

    /**
     * 在当前线程开启一次 Agent 调用上下文。
     */
    public static void open(Long sessionId, Long userId)
    {
        Context context = new Context();
        context.sessionId = sessionId;
        context.userId = userId;
        HOLDER.set(context);
    }

    /**
     * 获取当前上下文，没有上下文时返回 null。
     */
    public static Context current()
    {
        return HOLDER.get();
    }

    /**
     * 获取当前上下文，工具方法必须使用这个版本，避免静默丢身份。
     */
    public static Context require()
    {
        Context context = HOLDER.get();
        if (context == null)
        {
            throw new IllegalStateException("智能助手上下文不存在");
        }
        return context;
    }

    /**
     * 清理上下文，必须在请求结束时调用。
     */
    public static void close()
    {
        HOLDER.remove();
    }

    public static final class Context
    {
        /** 当前聊天会话。 */
        private Long sessionId;

        /** 当前登录用户，只从 JWT 上下文写入。 */
        private Long userId;

        /** 本次请求期间的工具调用摘要，用于返回前端展示。 */
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
