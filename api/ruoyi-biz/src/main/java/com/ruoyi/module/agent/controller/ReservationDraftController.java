package com.ruoyi.module.agent.controller;

import com.ruoyi.common.annotation.Log;
import com.ruoyi.common.core.controller.BaseController;
import com.ruoyi.common.core.domain.AjaxResult;
import com.ruoyi.common.enums.BusinessType;
import com.ruoyi.common.utils.SecurityUtils;
import com.ruoyi.module.agent.service.IReservationDraftService;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * 浏览器预约草稿接口。
 */
@RestController
@RequestMapping("/module/agent/drafts")
@RequiredArgsConstructor
public class ReservationDraftController extends BaseController
{
    private final IReservationDraftService draftService;

    @PreAuthorize("@ss.hasPermi('module:agent:use')")
    @GetMapping("/{draftId}")
    public AjaxResult get(@PathVariable Long draftId)
    {
        return success(draftService.get(draftId, SecurityUtils.getUserId()));
    }

    @PreAuthorize("@ss.hasPermi('module:agent:use')")
    @Log(title = "智能助手预约草稿", businessType = BusinessType.UPDATE)
    @PostMapping("/{draftId}/cancel")
    public AjaxResult cancel(@PathVariable Long draftId)
    {
        return success(draftService.cancel(draftId, SecurityUtils.getUserId()));
    }
}
