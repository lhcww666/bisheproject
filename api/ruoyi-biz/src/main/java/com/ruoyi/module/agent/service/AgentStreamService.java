package com.ruoyi.module.agent.service;

import java.time.Duration;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import org.springframework.http.codec.ServerSentEvent;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;

import com.ruoyi.common.utils.StringUtils;
import com.ruoyi.module.agent.domain.ChatReply;
import com.ruoyi.module.agent.domain.ChatRequest;

import lombok.RequiredArgsConstructor;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import reactor.core.scheduler.Schedulers;

/**
 * Agent SSE 输出服务。
 *
 * <p>模型回答仍通过现有的同步受控工具流程生成，再把最终答案按安全边界分块输出。
 * 这样做可以让页面逐步显示内容，同时保留工具审计、权限校验和业务事务的现有行为。</p>
 *
 * @author itjialu
 */
@Service
@RequiredArgsConstructor
public class AgentStreamService
{
    private static final int CHUNK_SIZE = 12;

    private static final Duration CHUNK_DELAY = Duration.ofMillis(24);

    private final IAgentService agentService;

    /**
     * 立即发送处理中事件，随后在工作线程执行同步 Agent，最后分块发送答案。
     */
    public Flux<ServerSentEvent<Object>> stream(ChatRequest request)
    {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        ServerSentEvent<Object> thinking = event("thinking",
                Map.of("message", "正在查询系统数据..."));
        Flux<ServerSentEvent<Object>> result = Mono.fromCallable(() -> withAuthentication(authentication,
                        () -> agentService.chat(request)))
                .subscribeOn(Schedulers.boundedElastic())
                .flatMapMany(this::streamReply)
                .onErrorResume(error -> Flux.just(event("error",
                        Map.of("message", safeError(error)))));
        return Flux.concat(Flux.just(thinking), result);
    }

    static List<String> splitChunks(String content)
    {
        List<String> chunks = new ArrayList<>();
        if (StringUtils.isEmpty(content))
        {
            return chunks;
        }
        int start = 0;
        while (start < content.length())
        {
            int remainingCodePoints = content.codePointCount(start, content.length());
            int nextCodePoints = Math.min(CHUNK_SIZE, remainingCodePoints);
            int end = content.offsetByCodePoints(start, nextCodePoints);
            chunks.add(content.substring(start, end));
            start = end;
        }
        return chunks;
    }

    private Flux<ServerSentEvent<Object>> streamReply(ChatReply reply)
    {
        Map<String, Object> done = new LinkedHashMap<>();
        done.put("sessionId", reply.getSessionId());
        done.put("toolCalls", reply.getToolCalls());
        done.put("draft", reply.getDraft());
        Flux<ServerSentEvent<Object>> chunks = Flux.fromIterable(splitChunks(reply.getContent()))
                .delayElements(CHUNK_DELAY)
                .map(text -> event("chunk", Map.of("text", text)));
        return Flux.concat(chunks, Mono.just(event("done", done)));
    }

    private ServerSentEvent<Object> event(String event, Object data)
    {
        return ServerSentEvent.builder(data).event(event).build();
    }

    private String safeError(Throwable error)
    {
        String message = error == null ? null : error.getMessage();
        return StringUtils.isEmpty(message) ? "智能助手暂时不可用，请稍后重试" : message;
    }

    private <T> T withAuthentication(Authentication authentication, ThrowingSupplier<T> action) throws Exception
    {
        SecurityContext previous = SecurityContextHolder.getContext();
        SecurityContext context = SecurityContextHolder.createEmptyContext();
        context.setAuthentication(authentication);
        SecurityContextHolder.setContext(context);
        try
        {
            return action.get();
        }
        finally
        {
            SecurityContextHolder.setContext(previous);
        }
    }

    @FunctionalInterface
    private interface ThrowingSupplier<T>
    {
        T get() throws Exception;
    }
}
