package com.ruoyi.module.stats.controller;

import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.ruoyi.common.core.controller.BaseController;
import com.ruoyi.common.core.domain.AjaxResult;
import com.ruoyi.module.stats.service.IStatsService;

import lombok.RequiredArgsConstructor;

/**
 * 数据看板Controller。
 *
 * @author itjialu
 */
@RestController
@RequestMapping("/module/stats")
@RequiredArgsConstructor
public class StatsController extends BaseController
{
    private final IStatsService statsService;

    @PreAuthorize("@ss.hasPermi('module:stats:view')")
    @GetMapping("/dashboard")
    public AjaxResult dashboard(@RequestParam(defaultValue = "30") int days,
            @RequestParam(required = false) String startDate,
            @RequestParam(required = false) String endDate)
    {
        return success(statsService.getDashboard(days, startDate, endDate));
    }
}
