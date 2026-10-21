package com.ruoyi.module.assistant.service;

import java.util.List;

import com.ruoyi.module.assistant.domain.ChatMessage;
import com.ruoyi.module.assistant.domain.ChatReply;
import com.ruoyi.module.assistant.domain.ChatRequest;
import com.ruoyi.module.assistant.domain.ChatSessionView;

/**
 * 智能助手Service。
 *
 * @author itjialu
 */
public interface IAssistantService
{
    ChatSessionView createSession(String title);

    List<ChatSessionView> listSessions();

    List<ChatMessage> listMessages(Long sessionId);

    ChatReply chat(ChatRequest request);

    void deleteSession(Long sessionId);
}
