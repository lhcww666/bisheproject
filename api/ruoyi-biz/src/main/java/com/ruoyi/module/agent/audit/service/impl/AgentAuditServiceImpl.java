package com.ruoyi.module.agent.audit.service.impl;

import java.util.ArrayList;
import java.util.List;

import org.springframework.stereotype.Service;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.ruoyi.common.core.domain.entity.SysUser;
import com.ruoyi.common.utils.StringUtils;
import com.ruoyi.module.agent.audit.domain.AgentAuditSessionView;
import com.ruoyi.module.agent.audit.service.IAgentAuditService;
import com.ruoyi.module.agent.domain.ChatMessage;
import com.ruoyi.module.agent.domain.ChatSession;
import com.ruoyi.module.agent.domain.ToolCallLog;
import com.ruoyi.module.agent.mapper.ChatMessageMapper;
import com.ruoyi.module.agent.mapper.ChatSessionMapper;
import com.ruoyi.module.agent.mapper.ToolCallLogMapper;
import com.ruoyi.system.service.ISysUserService;

import lombok.RequiredArgsConstructor;

/**
 * Agent 审计服务实现。
 *
 * @author itjialu
 */
@Service
@RequiredArgsConstructor
public class AgentAuditServiceImpl implements IAgentAuditService
{
    private final ChatSessionMapper chatSessionMapper;

    private final ChatMessageMapper chatMessageMapper;

    private final ToolCallLogMapper toolCallLogMapper;

    private final ISysUserService sysUserService;

    @Override
    public IPage<AgentAuditSessionView> pageSessions(Page<ChatSession> page,
            QueryWrapper<ChatSession> queryWrapper)
    {
        IPage<ChatSession> pageData = chatSessionMapper.selectPage(page, queryWrapper);
        Page<AgentAuditSessionView> views = new Page<>(pageData.getCurrent(), pageData.getSize(), pageData.getTotal());
        List<AgentAuditSessionView> records = new ArrayList<>();
        for (ChatSession session : pageData.getRecords())
        {
            AgentAuditSessionView view = new AgentAuditSessionView();
            view.setId(session.getId());
            view.setUserId(session.getUserId());
            view.setTitle(session.getTitle());
            view.setCreateTime(session.getCreateTime());
            view.setUpdateTime(session.getUpdateTime());
            SysUser user = sysUserService.selectUserById(session.getUserId());
            view.setUserName(user == null ? null : StringUtils.isNotEmpty(user.getNickName())
                    ? user.getNickName() : user.getUserName());
            view.setMessageCount(chatMessageMapper.selectCount(
                    new QueryWrapper<ChatMessage>().eq("session_id", session.getId())));
            view.setToolCallCount(toolCallLogMapper.selectCount(
                    new QueryWrapper<ToolCallLog>().eq("session_id", session.getId())));
            records.add(view);
        }
        views.setRecords(records);
        return views;
    }

    @Override
    public List<ChatMessage> listMessages(Long sessionId)
    {
        if (chatSessionMapper.selectById(sessionId) == null)
        {
            throw new com.ruoyi.common.exception.ServiceException("会话不存在");
        }
        return chatMessageMapper.selectList(new QueryWrapper<ChatMessage>()
                .eq("session_id", sessionId).orderByAsc("id"));
    }

    @Override
    public IPage<ToolCallLog> pageToolCalls(Page<ToolCallLog> page, QueryWrapper<ToolCallLog> queryWrapper)
    {
        IPage<ToolCallLog> pageData = toolCallLogMapper.selectPage(page, queryWrapper);
        for (ToolCallLog log : pageData.getRecords())
        {
            SysUser user = log.getUserId() == null ? null : sysUserService.selectUserById(log.getUserId());
            log.setUserName(user == null ? null : StringUtils.isNotEmpty(user.getNickName())
                    ? user.getNickName() : user.getUserName());
        }
        return pageData;
    }
}
