package com.ruoyi.module.safety.controller;

import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.ruoyi.common.annotation.Log;
import com.ruoyi.common.core.controller.BaseController;
import com.ruoyi.common.core.domain.AjaxResult;
import com.ruoyi.common.enums.BusinessType;
import com.ruoyi.common.utils.SecurityUtils;
import com.ruoyi.module.safety.domain.SafetyExamSubmitRequest;
import com.ruoyi.module.safety.service.ISafetyExamService;

import lombok.RequiredArgsConstructor;

/**
 * 学生安全考试Controller。
 *
 * @author itjialu
 */
@RestController
@RequestMapping("/module/safetyExam")
@RequiredArgsConstructor
public class SafetyExamController extends BaseController
{
    private final ISafetyExamService safetyExamService;

    @PreAuthorize("@ss.hasPermi('module:safetyExam:status')")
    @GetMapping("/status")
    public AjaxResult status()
    {
        return success(safetyExamService.getStatus(SecurityUtils.getUserId()));
    }

    @PreAuthorize("@ss.hasPermi('module:safetyExam:start')")
    @Log(title = "安全考试", businessType = BusinessType.INSERT)
    @PostMapping("/start")
    public AjaxResult start()
    {
        return success(safetyExamService.startExam(SecurityUtils.getUserId()));
    }

    @PreAuthorize("@ss.hasPermi('module:safetyExam:submit')")
    @Log(title = "安全考试", businessType = BusinessType.UPDATE)
    @PostMapping("/submit")
    public AjaxResult submit(@Validated @RequestBody SafetyExamSubmitRequest request)
    {
        return success(safetyExamService.submitExam(SecurityUtils.getUserId(), request));
    }
}
