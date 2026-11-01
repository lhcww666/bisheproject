package com.ruoyi.module.agent.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.ruoyi.common.exception.ServiceException;
import com.ruoyi.common.utils.DateUtils;
import com.ruoyi.common.utils.SecurityUtils;
import com.ruoyi.common.utils.StringUtils;
import com.ruoyi.module.agent.domain.AgentSessionCreateRequest;
import com.ruoyi.module.agent.domain.ChatMessage;
import com.ruoyi.module.agent.domain.ChatSession;
import com.ruoyi.module.agent.mapper.ChatMessageMapper;
import com.ruoyi.module.agent.mapper.ChatSessionMapper;
import com.ruoyi.module.agent.service.IChatSessionService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Locale;
import java.util.Set;

/**
 * 智能助手会话服务实现。
 */
@Service
@RequiredArgsConstructor
public class ChatSessionServiceImpl implements IChatSessionService
{
    private static final String ACTIVE = "ACTIVE";

    private static final Set<String> MESSAGE_ROLES = Set.of("USER", "ASSISTANT", "SYSTEM");

    private final ChatSessionMapper chatSessionMapper;

    private final ChatMessageMapper chatMessageMapper;

    @Override
    public ChatSession createSession(Long userId, AgentSessionCreateRequest request, String defaultEngine)
    {
        if (userId == null)
        {
            throw new ServiceException("用户身份不能为空");
        }
        String engine = StringUtils.isEmpty(request.getEngine()) ? defaultEngine : request.getEngine();
        if (!"python".equals(engine) && !"dify".equals(engine))
        {
            throw new ServiceException("Agent 引擎只能是 python 或 dify");
        }

        String title = request.getTitle() == null ? "" : request.getTitle().trim();
        ChatSession session = new ChatSession();
        session.setUserId(userId);
        session.setTitle(title.isEmpty() ? "新会话" : title);
        session.setEngine(engine);
        session.setStatus(ACTIVE);
        session.setCreateBy(SecurityUtils.getUsername());
        session.setCreateTime(DateUtils.getNowDate());
        session.setUpdateBy(SecurityUtils.getUsername());
        session.setUpdateTime(DateUtils.getNowDate());
        chatSessionMapper.insert(session);
        return session;
    }

    @Override
    public List<ChatSession> listByUser(Long userId)
    {
        LambdaQueryWrapper<ChatSession> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(ChatSession::getUserId, userId)
                .orderByDesc(ChatSession::getUpdateTime)
                .orderByDesc(ChatSession::getId);
        return chatSessionMapper.selectList(wrapper);
    }

    @Override
    public ChatSession requireOwnedSession(Long sessionId, Long userId)
    {
        ChatSession session = chatSessionMapper.selectById(sessionId);
        if (session == null || !userId.equals(session.getUserId()))
        {
            throw new ServiceException("会话不存在或无权访问");
        }
        return session;
    }

    @Override
    public List<ChatMessage> listMessages(Long sessionId, Long userId)
    {
        requireOwnedSession(sessionId, userId);
        LambdaQueryWrapper<ChatMessage> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(ChatMessage::getSessionId, sessionId)
                .orderByAsc(ChatMessage::getId);
        return chatMessageMapper.selectList(wrapper);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public ChatMessage appendMessage(Long sessionId, Long userId, String role, String content, String traceId)
    {
        return appendMessage(
                sessionId,
                userId,
                role,
                content,
                traceId,
                SecurityUtils.getUsername());
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public ChatMessage appendMessage(
            Long sessionId,
            Long userId,
            String role,
            String content,
            String traceId,
            String operator)
    {
        requireOwnedSession(sessionId, userId);
        String normalizedRole = role == null ? "" : role.trim().toUpperCase(Locale.ROOT);
        if (!MESSAGE_ROLES.contains(normalizedRole) || StringUtils.isEmpty(content) || content.isBlank())
        {
            throw new ServiceException("消息角色或内容不合法");
        }

        ChatMessage message = new ChatMessage();
        message.setSessionId(sessionId);
        message.setRole(normalizedRole);
        message.setContent(content);
        message.setTraceId(traceId);
        message.setCreateBy(operator);
        message.setCreateTime(DateUtils.getNowDate());
        chatMessageMapper.insert(message);

        ChatSession session = new ChatSession();
        session.setId(sessionId);
        session.setUpdateBy(operator);
        session.setUpdateTime(DateUtils.getNowDate());
        chatSessionMapper.updateById(session);
        return message;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void deleteOwnedSession(Long sessionId, Long userId)
    {
        requireOwnedSession(sessionId, userId);
        chatSessionMapper.deleteById(sessionId);
    }
}
