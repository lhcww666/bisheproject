package com.ruoyi.module.agent.controller;

import com.ruoyi.common.annotation.Log;
import com.ruoyi.common.core.controller.BaseController;
import com.ruoyi.common.core.domain.AjaxResult;
import com.ruoyi.common.enums.BusinessType;
import com.ruoyi.common.utils.SecurityUtils;
import com.ruoyi.module.agent.config.AgentProperties;
import com.ruoyi.module.agent.domain.AgentSessionCreateRequest;
import com.ruoyi.module.agent.domain.ChatSession;
import com.ruoyi.module.agent.service.AgentStatusService;
import com.ruoyi.module.agent.service.IChatSessionService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

/**
 * 智能助手 Gateway。
 */
@RestController
@RequestMapping("/module/agent")
@RequiredArgsConstructor
public class AgentController extends BaseController
{
    private final IChatSessionService chatSessionService;

    private final AgentStatusService agentStatusService;

    private final AgentProperties agentProperties;

    @PreAuthorize("@ss.hasPermi('module:agent:use')")
    @GetMapping("/status")
    public AjaxResult status()
    {
        return success(agentStatusService.getStatus());
    }

    @PreAuthorize("@ss.hasPermi('module:agent:use')")
    @GetMapping("/sessions")
    public AjaxResult sessions()
    {
        return success(chatSessionService.listByUser(SecurityUtils.getUserId()));
    }

    @PreAuthorize("@ss.hasPermi('module:agent:use')")
    @Log(title = "智能助手会话", businessType = BusinessType.INSERT)
    @PostMapping("/sessions")
    public AjaxResult createSession(@Valid @RequestBody AgentSessionCreateRequest request)
    {
        ChatSession session = chatSessionService.createSession(
                SecurityUtils.getUserId(),
                request,
                agentProperties.normalizedEngine());
        return success(Map.of("sessionId", session.getId()));
    }

    @PreAuthorize("@ss.hasPermi('module:agent:use')")
    @GetMapping("/sessions/{sessionId}/messages")
    public AjaxResult messages(@PathVariable Long sessionId)
    {
        return success(chatSessionService.listMessages(sessionId, SecurityUtils.getUserId()));
    }

    @PreAuthorize("@ss.hasPermi('module:agent:use')")
    @Log(title = "智能助手会话", businessType = BusinessType.DELETE)
    @DeleteMapping("/sessions/{sessionId}")
    public AjaxResult deleteSession(@PathVariable Long sessionId)
    {
        chatSessionService.deleteOwnedSession(sessionId, SecurityUtils.getUserId());
        return success();
    }
}
