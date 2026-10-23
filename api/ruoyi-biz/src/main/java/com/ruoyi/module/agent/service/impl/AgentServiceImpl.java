package com.ruoyi.module.agent.service.impl;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Date;
import java.util.List;
import java.util.Map;

import org.springframework.ai.chat.messages.AssistantMessage;
import org.springframework.ai.chat.messages.Message;
import org.springframework.ai.chat.messages.SystemMessage;
import org.springframework.ai.chat.messages.UserMessage;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.ruoyi.common.exception.ServiceException;
import com.ruoyi.common.utils.SecurityUtils;
import com.ruoyi.common.utils.StringUtils;
import com.ruoyi.module.agent.domain.ChatMessage;
import com.ruoyi.module.agent.domain.ChatReply;
import com.ruoyi.module.agent.domain.ChatRequest;
import com.ruoyi.module.agent.domain.ChatSession;
import com.ruoyi.module.agent.domain.ChatSessionView;
import com.ruoyi.module.agent.domain.ToolCallView;
import com.ruoyi.module.agent.draft.domain.ReservationDraftView;
import com.ruoyi.module.agent.mapper.ChatMessageMapper;
import com.ruoyi.module.agent.mapper.ChatSessionMapper;
import com.ruoyi.module.agent.mapper.ToolCallLogMapper;
import com.ruoyi.module.agent.service.AgentContext;
import com.ruoyi.module.agent.service.AgentChatClientRouter;
import com.ruoyi.module.agent.service.AgentPromptContext;
import com.ruoyi.module.agent.service.IAgentService;

import lombok.RequiredArgsConstructor;

/**
 * 智能助手核心服务。
 *
 * <p>一次聊天大致经过以下步骤：</p>
 * <ol>
 *   <li>校验会话是否属于当前登录用户；</li>
 *   <li>保存用户消息；</li>
 *   <li>把 sessionId、userId 放入 AgentContext，供工具调用和审计使用；</li>
 *   <li>读取最近 20 条历史消息，交给 Spring AI ChatClient；</li>
 *   <li>模型选择并执行只读工具，工具内部再调用现有业务 Service；</li>
 *   <li>保存助手回复和工具调用摘要，返回前端。</li>
 * </ol>
 *
 * <p>AI 不直接访问数据库，也不负责最终的权限、冲突和业务规则判断。</p>
 *
 * @author itjialu
 */
@Service
@RequiredArgsConstructor
public class AgentServiceImpl implements IAgentService
{
    private final ChatSessionMapper chatSessionMapper;

    private final ChatMessageMapper chatMessageMapper;

    private final ToolCallLogMapper toolCallLogMapper;

    private final AgentChatClientRouter chatClientRouter;

    /**
     * 真实 Key 由环境变量注入；not-configured 只用于本地无 Key 启动。
     */
    @Value("${spring.ai.openai.api-key:}")
    private String apiKey;

    @Value("${spring.ai.openai.chat.options.model:deepseek-chat}")
    private String modelName;

    /**
     * 新建空会话，标题只用于左侧会话列表展示。
     */
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

    /**
     * 查询当前用户自己的会话，其他用户的数据不会出现在结果中。
     */
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

    /**
     * 查询会话消息，先校验会话归属，再按时间正序返回。
     */
    @Override
    public List<ChatMessage> listMessages(Long sessionId)
    {
        requireSession(sessionId);
        QueryWrapper<ChatMessage> query = new QueryWrapper<>();
        query.eq("session_id", sessionId).orderByAsc("id");
        return chatMessageMapper.selectList(query);
    }

    /**
     * 发送消息并获取 Agent 回复。
     *
     * <p>事务覆盖“保存用户消息、调用模型、保存助手消息”三个步骤；
     * 模型失败时也会写入可读错误，避免页面没有反馈。</p>
     */
    @Override
    @Transactional(rollbackFor = Exception.class)
    public ChatReply chat(ChatRequest request)
    {
        if (request == null || StringUtils.isEmpty(request.getMessage()))
        {
            throw new ServiceException("消息不能为空");
        }
        // 用户身份只从登录上下文获取，不能使用请求体中的任何用户字段。
        Long userId = SecurityUtils.getUserId();
        // sessionId 为空表示新会话；否则必须验证当前用户拥有该会话。
        ChatSession session = request.getSessionId() == null ? createSessionEntity(userId, request.getMessage())
                : requireSession(request.getSessionId());

        // 先保存用户消息，保证模型超时或失败时仍能看到用户输入。
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

        // 工具通过 ThreadLocal 读取会话和用户身份，模型无法伪造 userId。
        AgentContext.open(session.getId(), userId);
        String answer;
        List<ToolCallView> toolCalls;
        ReservationDraftView draft;
        try
        {
            answer = isConfigured() ? askModel(session.getId()) : missingKeyMessage();
            // 工具执行过程中会把调用摘要放进 Context，这里复制给前端展示。
            AgentContext.Context context = AgentContext.current();
            toolCalls = context == null ? Collections.emptyList() : new ArrayList<>(context.getToolCalls());
            draft = context == null ? null : context.getDraft();
        }
        catch (Exception e)
        {
            answer = "智能助手暂时不可用：" + (StringUtils.isEmpty(e.getMessage()) ? e.getClass().getSimpleName()
                    : e.getMessage());
            AgentContext.Context context = AgentContext.current();
            toolCalls = context == null ? Collections.emptyList() : new ArrayList<>(context.getToolCalls());
            draft = context == null ? null : context.getDraft();
        }
        finally
        {
            // 及时清理 ThreadLocal，避免线程池复用造成用户身份串线。
            AgentContext.close();
        }

        // 模型成功或失败都保存助手消息，保证会话记录完整。
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
        reply.setDraft(draft);

        session.setUpdateTime(new Date());
        chatSessionMapper.updateById(session);
        return reply;
    }

    /**
     * 用一次最小提示词检查真实模型连接。
     *
     * <p>这里不会执行业务工具，也不会写入会话数据；仅用于部署后确认
     * Key、base-url、模型名和网络是否可用。</p>
     */
    @Override
    public Map<String, Object> checkModelConnection()
    {
        if (!isConfigured())
        {
            return Map.of("ok", false, "configured", false, "model", modelName,
                    "message", missingKeyMessage());
        }
        long started = System.currentTimeMillis();
        try
        {
            String content = chatClientRouter.current().prompt()
                    .system("这是模型连通性检测。不得调用工具，只回复一个单词：OK")
                    .user("请回复 OK")
                    .call()
                    .content();
            long costMs = System.currentTimeMillis() - started;
            return Map.of("ok", true, "configured", true, "model", modelName, "costMs", costMs,
                    "message", StringUtils.isEmpty(content) ? "模型已响应，但没有返回文本" : truncate(content.trim(), 100));
        }
        catch (Exception e)
        {
            long costMs = System.currentTimeMillis() - started;
            String message = StringUtils.isEmpty(e.getMessage()) ? e.getClass().getSimpleName() : e.getMessage();
            // 极端情况下第三方错误可能回显请求内容，避免把 Key 暴露到前端。
            message = message.replace(apiKey, "***");
            return Map.of("ok", false, "configured", true, "model", modelName, "costMs", costMs,
                    "message", truncate(message, 300));
        }
    }

    /**
     * 删除会话及其消息、工具调用审计。
     */
    @Override
    @Transactional(rollbackFor = Exception.class)
    public void deleteSession(Long sessionId)
    {
        ChatSession session = requireSession(sessionId);
        chatMessageMapper.delete(new QueryWrapper<ChatMessage>().eq("session_id", session.getId()));
        toolCallLogMapper.delete(new QueryWrapper<com.ruoyi.module.agent.domain.ToolCallLog>()
                .eq("session_id", session.getId()));
        chatSessionMapper.deleteById(session.getId());
    }

    /**
     * 把数据库消息转换为 Spring AI 的消息格式，最多带入最近 20 条。
     */
    private String askModel(Long sessionId)
    {
        List<ChatMessage> messages = history(sessionId);
        List<Message> history = new ArrayList<>();
        history.add(new SystemMessage(AgentPromptContext.currentTimeInstruction()));
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
        // ChatClient 已配置系统提示词和只读工具，模型会在需要时自动选择工具。
        String content = chatClientRouter.current().prompt().messages(history).call().content();
        return StringUtils.isEmpty(content) ? "模型没有返回内容，请稍后重试。" : content;
    }

    /**
     * 读取最近消息：先倒序取 20 条，再反转成正序，避免把倒序上下文交给模型。
     */
    private List<ChatMessage> history(Long sessionId)
    {
        QueryWrapper<ChatMessage> query = new QueryWrapper<>();
        query.eq("session_id", sessionId).orderByDesc("id").last("limit 20");
        List<ChatMessage> messages = chatMessageMapper.selectList(query);
        Collections.reverse(messages);
        return messages;
    }

    /**
     * 校验会话存在且属于当前登录用户。
     */
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

    /**
     * 创建首次对话使用的会话实体。
     */
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

    /**
     * 本地没有真实 Key 时不让应用启动失败，而是返回可操作的提示。
     */
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
