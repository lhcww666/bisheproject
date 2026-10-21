package com.ruoyi.module.assistant.service.impl;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Date;
import java.util.List;

import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.chat.messages.AssistantMessage;
import org.springframework.ai.chat.messages.Message;
import org.springframework.ai.chat.messages.UserMessage;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.ruoyi.common.exception.ServiceException;
import com.ruoyi.common.utils.SecurityUtils;
import com.ruoyi.common.utils.StringUtils;
import com.ruoyi.module.assistant.domain.ChatMessage;
import com.ruoyi.module.assistant.domain.ChatReply;
import com.ruoyi.module.assistant.domain.ChatRequest;
import com.ruoyi.module.assistant.domain.ChatSession;
import com.ruoyi.module.assistant.domain.ChatSessionView;
import com.ruoyi.module.assistant.domain.ToolCallView;
import com.ruoyi.module.assistant.mapper.ChatMessageMapper;
import com.ruoyi.module.assistant.mapper.ChatSessionMapper;
import com.ruoyi.module.assistant.mapper.ToolCallLogMapper;
import com.ruoyi.module.assistant.service.AssistantContext;
import com.ruoyi.module.assistant.service.IAssistantService;

import lombok.RequiredArgsConstructor;

/**
 * 智能助手Service实现。
 *
 * @author itjialu
 */
@Service
@RequiredArgsConstructor
public class AssistantServiceImpl implements IAssistantService
{
    private final ChatSessionMapper chatSessionMapper;

    private final ChatMessageMapper chatMessageMapper;

    private final ToolCallLogMapper toolCallLogMapper;

    private final ChatClient chatClient;

    @Value("${spring.ai.openai.api-key:}")
    private String apiKey;

    @Override
    public ChatSessionView createSession(String title)
    {
        Long userId = SecurityUtils.getUserId();
        ChatSession session = new ChatSession();
        session.setUserId(userId);
        session.setTitle(StringUtils.isEmpty(title) ? "新对话" : truncate(title.trim(), 100));
        Date now = new Date();
        session.setCreateTime(now);
        session.setUpdateTime(now);
        chatSessionMapper.insert(session);
        return toView(session);
    }

    @Override
    public List<ChatSessionView> listSessions()
    {
        QueryWrapper<ChatSession> query = new QueryWrapper<>();
        query.eq("user_id", SecurityUtils.getUserId()).orderByDesc("update_time");
        List<ChatSessionView> result = new ArrayList<>();
        for (ChatSession session : chatSessionMapper.selectList(query))
        {
            result.add(toView(session));
        }
        return result;
    }

    @Override
    public List<ChatMessage> listMessages(Long sessionId)
    {
        requireSession(sessionId);
        QueryWrapper<ChatMessage> query = new QueryWrapper<>();
        query.eq("session_id", sessionId).orderByAsc("id");
        return chatMessageMapper.selectList(query);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public ChatReply chat(ChatRequest request)
    {
        if (request == null || StringUtils.isEmpty(request.getMessage()))
        {
            throw new ServiceException("消息不能为空");
        }
        Long userId = SecurityUtils.getUserId();
        ChatSession session = request.getSessionId() == null ? createSessionEntity(userId, request.getMessage())
                : requireSession(request.getSessionId());

        ChatMessage userMessage = new ChatMessage();
        userMessage.setSessionId(session.getId());
        userMessage.setRole(ChatMessage.ROLE_USER);
        userMessage.setContent(request.getMessage());
        userMessage.setCreateTime(new Date());
        chatMessageMapper.insert(userMessage);

        if ("新对话".equals(session.getTitle()))
        {
            session.setTitle(truncate(request.getMessage().trim(), 30));
        }

        AssistantContext.open(session.getId(), userId);
        String answer;
        List<ToolCallView> toolCalls;
        try
        {
            answer = isConfigured() ? askModel(session.getId()) : missingKeyMessage();
            AssistantContext.Context context = AssistantContext.current();
            toolCalls = context == null ? Collections.emptyList() : new ArrayList<>(context.getToolCalls());
        }
        catch (Exception e)
        {
            answer = "智能助手暂时不可用：" + (StringUtils.isEmpty(e.getMessage()) ? e.getClass().getSimpleName()
                    : e.getMessage());
            AssistantContext.Context context = AssistantContext.current();
            toolCalls = context == null ? Collections.emptyList() : new ArrayList<>(context.getToolCalls());
        }
        finally
        {
            AssistantContext.close();
        }

        ChatMessage assistantMessage = new ChatMessage();
        assistantMessage.setSessionId(session.getId());
        assistantMessage.setRole(ChatMessage.ROLE_ASSISTANT);
        assistantMessage.setContent(answer);
        assistantMessage.setCreateTime(new Date());
        chatMessageMapper.insert(assistantMessage);

        ChatReply reply = new ChatReply();
        reply.setSessionId(session.getId());
        reply.setContent(answer);
        reply.setToolCalls(toolCalls);

        session.setUpdateTime(new Date());
        chatSessionMapper.updateById(session);
        return reply;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void deleteSession(Long sessionId)
    {
        ChatSession session = requireSession(sessionId);
        chatMessageMapper.delete(new QueryWrapper<ChatMessage>().eq("session_id", session.getId()));
        toolCallLogMapper.delete(new QueryWrapper<com.ruoyi.module.assistant.domain.ToolCallLog>()
                .eq("session_id", session.getId()));
        chatSessionMapper.deleteById(session.getId());
    }

    private String askModel(Long sessionId)
    {
        List<ChatMessage> messages = history(sessionId);
        List<Message> history = new ArrayList<>();
        for (ChatMessage message : messages)
        {
            if (ChatMessage.ROLE_USER.equals(message.getRole()))
            {
                history.add(new UserMessage(message.getContent()));
            }
            else if (ChatMessage.ROLE_ASSISTANT.equals(message.getRole()))
            {
                history.add(new AssistantMessage(message.getContent()));
            }
        }
        String content = chatClient.prompt().messages(history).call().content();
        return StringUtils.isEmpty(content) ? "模型没有返回内容，请稍后重试。" : content;
    }

    private List<ChatMessage> history(Long sessionId)
    {
        QueryWrapper<ChatMessage> query = new QueryWrapper<>();
        query.eq("session_id", sessionId).orderByDesc("id").last("limit 20");
        List<ChatMessage> messages = chatMessageMapper.selectList(query);
        Collections.reverse(messages);
        return messages;
    }

    private ChatSession requireSession(Long sessionId)
    {
        if (sessionId == null)
        {
            throw new ServiceException("会话ID不能为空");
        }
        ChatSession session = chatSessionMapper.selectById(sessionId);
        if (session == null || !SecurityUtils.getUserId().equals(session.getUserId()))
        {
            throw new ServiceException("会话不存在或无权访问");
        }
        return session;
    }

    private ChatSession createSessionEntity(Long userId, String message)
    {
        ChatSession session = new ChatSession();
        session.setUserId(userId);
        session.setTitle(truncate(message.trim(), 30));
        Date now = new Date();
        session.setCreateTime(now);
        session.setUpdateTime(now);
        chatSessionMapper.insert(session);
        return session;
    }

    private boolean isConfigured()
    {
        return StringUtils.isNotEmpty(apiKey) && !"not-configured".equals(apiKey.trim());
    }

    private String missingKeyMessage()
    {
        return "智能助手尚未配置模型密钥。请在启动后端前设置环境变量 DEEPSEEK_API_KEY，"
                + "然后重启后端；当前的会话和工具审计功能已经可以正常使用。";
    }

    private ChatSessionView toView(ChatSession session)
    {
        ChatSessionView view = new ChatSessionView();
        view.setId(session.getId());
        view.setTitle(session.getTitle());
        view.setUpdateTime(session.getUpdateTime());
        return view;
    }

    private String truncate(String text, int max)
    {
        if (text == null || text.length() <= max)
        {
            return text;
        }
        return text.substring(0, max) + "...";
    }
}
