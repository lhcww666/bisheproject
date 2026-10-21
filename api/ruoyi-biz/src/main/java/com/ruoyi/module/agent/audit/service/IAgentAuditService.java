package com.ruoyi.module.agent.audit.service;

import java.util.List;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.ruoyi.module.agent.audit.domain.AgentAuditSessionView;
import com.ruoyi.module.agent.domain.ChatMessage;
import com.ruoyi.module.agent.domain.ChatSession;
import com.ruoyi.module.agent.domain.ToolCallLog;

/**
 * Agent 审计服务，供管理员查看会话、消息和工具调用。
 *
 * @author itjialu
 */
public interface IAgentAuditService
{
    IPage<AgentAuditSessionView> pageSessions(Page<ChatSession> page, QueryWrapper<ChatSession> queryWrapper);

    List<ChatMessage> listMessages(Long sessionId);

    IPage<ToolCallLog> pageToolCalls(Page<ToolCallLog> page, QueryWrapper<ToolCallLog> queryWrapper);
}
