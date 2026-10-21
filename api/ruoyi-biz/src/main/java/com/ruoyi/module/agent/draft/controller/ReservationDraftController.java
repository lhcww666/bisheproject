package com.ruoyi.module.agent.draft.controller;

import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.ruoyi.common.annotation.Log;
import com.ruoyi.common.core.controller.BaseController;
import com.ruoyi.common.core.domain.AjaxResult;
import com.ruoyi.common.enums.BusinessType;
import com.ruoyi.common.utils.SecurityUtils;
import com.ruoyi.module.agent.draft.domain.ReservationDraftRequest;
import com.ruoyi.module.agent.draft.service.IReservationDraftService;

import lombok.RequiredArgsConstructor;

/**
 * Agent 预约草稿接口。
 *
 * <p>模型只能创建草稿；真正的提交必须由用户点击前端“确认提交”按钮，
 * 或者由用户明确要求后调用确认接口。确认接口会再次执行完整业务规则。</p>
 *
 * @author itjialu
 */
@RestController
@RequestMapping("/module/agent/drafts")
@RequiredArgsConstructor
public class ReservationDraftController extends BaseController
{
    private final IReservationDraftService draftService;

    /**
     * 创建草稿，供 Agent 工具和前端调试接口共同使用。
     */
    @PreAuthorize("@ss.hasPermi('module:agent:use')")
    @Log(title = "Agent预约草稿", businessType = BusinessType.INSERT)
    @PostMapping
    public AjaxResult create(@Validated @RequestBody ReservationDraftRequest request,
            @RequestParam(required = false) Long sessionId)
    {
        return success(draftService.createDraft(SecurityUtils.getUserId(), sessionId, request));
    }

    /**
     * 查询自己的草稿。
     */
    @PreAuthorize("@ss.hasPermi('module:agent:use')")
    @GetMapping("/{id}")
    public AjaxResult get(@PathVariable Long id)
    {
        return success(draftService.getDraft(id, SecurityUtils.getUserId()));
    }

    /**
     * 查询当前会话中待确认的草稿，用于页面刷新后恢复草稿卡片。
     */
    @PreAuthorize("@ss.hasPermi('module:agent:use')")
    @GetMapping("/pending")
    public AjaxResult pending(@RequestParam(required = false) Long sessionId)
    {
        return success(draftService.listPendingDrafts(SecurityUtils.getUserId(), sessionId));
    }

    /**
     * 用户确认草稿，后端重新校验后创建正式预约。
     */
    @PreAuthorize("@ss.hasPermi('module:agent:use')")
    @Log(title = "Agent预约草稿", businessType = BusinessType.UPDATE)
    @PostMapping("/{id}/confirm")
    public AjaxResult confirm(@PathVariable Long id)
    {
        return success(draftService.confirmDraft(id, SecurityUtils.getUserId()));
    }

    /**
     * 取消尚未确认的草稿。
     */
    @PreAuthorize("@ss.hasPermi('module:agent:use')")
    @Log(title = "Agent预约草稿", businessType = BusinessType.UPDATE)
    @PostMapping("/{id}/cancel")
    public AjaxResult cancel(@PathVariable Long id)
    {
        return success(draftService.cancelDraft(id, SecurityUtils.getUserId()));
    }
}
