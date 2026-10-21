package com.ruoyi.module.agent.audit.controller;

import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.ruoyi.common.core.controller.BaseController;
import com.ruoyi.common.core.domain.AjaxResult;
import com.ruoyi.common.core.page.TableDataInfo;
import com.ruoyi.module.agent.audit.domain.AgentAuditSessionView;
import com.ruoyi.module.agent.audit.service.IAgentAuditService;
import com.ruoyi.module.agent.domain.ChatSession;
import com.ruoyi.module.agent.domain.ToolCallLog;

import lombok.RequiredArgsConstructor;

/**
 * Agent 审计接口，仅管理员可见。
 *
 * @author itjialu
 */
@RestController
@RequestMapping("/module/agent/audit")
@RequiredArgsConstructor
public class AgentAuditController extends BaseController
{
    private final IAgentAuditService agentAuditService;

    @PreAuthorize("@ss.hasPermi('module:agent:audit')")
    @GetMapping("/sessions")
    public TableDataInfo sessions(ChatSession session)
    {
        QueryWrapper<ChatSession> query = getQueryWrapper(ChatSession.class);
        IPage<AgentAuditSessionView> page = agentAuditService.pageSessions(getPage(), query);
        return getDataTableByPage(page);
    }

    @PreAuthorize("@ss.hasPermi('module:agent:audit')")
    @GetMapping("/sessions/{id}/messages")
    public AjaxResult messages(@PathVariable Long id)
    {
        return success(agentAuditService.listMessages(id));
    }

    @PreAuthorize("@ss.hasPermi('module:agent:audit')")
    @GetMapping("/tool-calls")
    public TableDataInfo toolCalls(ToolCallLog log)
    {
        QueryWrapper<ToolCallLog> query = getQueryWrapper(ToolCallLog.class);
        IPage<ToolCallLog> page = agentAuditService.pageToolCalls(getPage(), query);
        return getDataTableByPage(page);
    }
}
