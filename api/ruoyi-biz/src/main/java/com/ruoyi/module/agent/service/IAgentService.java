package com.ruoyi.module.agent.service;

import java.util.List;

import com.ruoyi.module.agent.domain.ChatMessage;
import com.ruoyi.module.agent.domain.ChatReply;
import com.ruoyi.module.agent.domain.ChatRequest;
import com.ruoyi.module.agent.domain.ChatSessionView;

/**
 * 智能助手服务接口。
 *
 * <p>对 Controller 暴露会话和聊天能力；对工具层不暴露数据库细节。
 * 后续如果要切换 LangChain4j 或增加流式输出，可以保持这个接口不变。</p>
 *
 * @author itjialu
 */
public interface IAgentService
{
    /**
     * 新建空会话。
     */
    ChatSessionView createSession(String title);

    /**
     * 查询当前用户的会话列表。
     */
    List<ChatSessionView> listSessions();

    /**
     * 查询当前用户某个会话的消息。
     */
    List<ChatMessage> listMessages(Long sessionId);

    /**
     * 发送一条消息并获取 Agent 回复。
     */
    ChatReply chat(ChatRequest request);

    /**
     * 删除当前用户自己的会话。
     */
    void deleteSession(Long sessionId);
}
