package com.ruoyi.module.agent.eval.controller;

import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.ruoyi.common.annotation.Log;
import com.ruoyi.common.core.controller.BaseController;
import com.ruoyi.common.core.domain.AjaxResult;
import com.ruoyi.common.enums.BusinessType;
import com.ruoyi.module.agent.eval.service.IAgentEvalService;

import lombok.RequiredArgsConstructor;

/**
 * Agent 离线评测接口，仅管理员可见。
 *
 * @author itjialu
 */
@RestController
@RequestMapping("/module/agent/eval")
@RequiredArgsConstructor
public class AgentEvalController extends BaseController
{
    private final IAgentEvalService evalService;

    @PreAuthorize("@ss.hasPermi('module:agent:eval')")
    @GetMapping("/cases")
    public AjaxResult cases()
    {
        return success(evalService.listCases());
    }

    @PreAuthorize("@ss.hasPermi('module:agent:eval')")
    @GetMapping("/runs")
    public AjaxResult runs()
    {
        return success(evalService.listRuns());
    }

    @PreAuthorize("@ss.hasPermi('module:agent:eval')")
    @GetMapping("/runs/{id}")
    public AjaxResult run(@PathVariable Long id)
    {
        return success(evalService.getRun(id));
    }

    @PreAuthorize("@ss.hasPermi('module:agent:eval')")
    @PostMapping("/run")
    @Log(title = "Agent评测", businessType = BusinessType.INSERT)
    public AjaxResult execute()
    {
        return success(evalService.runOffline());
    }

    @PreAuthorize("@ss.hasPermi('module:agent:eval')")
    @PostMapping("/run-llm")
    @Log(title = "Agent真实模型评测", businessType = BusinessType.INSERT)
    public AjaxResult executeLlm()
    {
        return success(evalService.runLlm());
    }
}
