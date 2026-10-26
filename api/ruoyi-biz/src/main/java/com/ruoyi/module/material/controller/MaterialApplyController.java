package com.ruoyi.module.material.controller;

import java.util.List;

import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.ruoyi.common.annotation.Log;
import com.ruoyi.common.annotation.RepeatSubmit;
import com.ruoyi.common.core.controller.BaseController;
import com.ruoyi.common.core.domain.AjaxResult;
import com.ruoyi.common.core.page.TableDataInfo;
import com.ruoyi.common.enums.BusinessType;
import com.ruoyi.common.exception.ServiceException;
import com.ruoyi.common.utils.poi.ExcelUtil;
import com.ruoyi.module.common.service.ModuleDataScopeService;
import com.ruoyi.module.material.domain.MaterialApply;
import com.ruoyi.module.material.domain.MaterialReviewRequest;
import com.ruoyi.module.material.service.IMaterialApplyService;

import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;

/**
 * 实验室耗材领用 Controller。
 */
@RestController
@RequestMapping("/module/material/apply")
@RequiredArgsConstructor
public class MaterialApplyController extends BaseController
{
    private final IMaterialApplyService materialApplyService;

    private final ModuleDataScopeService moduleDataScopeService;

    @PreAuthorize("@ss.hasPermi('module:material:apply:list')")
    @GetMapping("/list")
    public TableDataInfo list(MaterialApply materialApply)
    {
        QueryWrapper<MaterialApply> queryWrapper = getQueryWrapper(MaterialApply.class);
        moduleDataScopeService.applyStudentRecordScope(queryWrapper, "applicant_id");
        IPage<MaterialApply> page = materialApplyService.pageApply(getPage(), queryWrapper);
        return getDataTableByPage(page);
    }

    @PreAuthorize("@ss.hasPermi('module:material:apply:export')")
    @Log(title = "耗材领用", businessType = BusinessType.EXPORT)
    @PostMapping("/export")
    public void export(HttpServletResponse response, MaterialApply materialApply)
    {
        QueryWrapper<MaterialApply> queryWrapper = getQueryWrapper(MaterialApply.class);
        moduleDataScopeService.applyStudentRecordScope(queryWrapper, "applicant_id");
        List<MaterialApply> list = materialApplyService.pageApply(new Page<>(1, 10000, false), queryWrapper)
                .getRecords();
        ExcelUtil<MaterialApply> util = new ExcelUtil<>(MaterialApply.class);
        util.exportExcel(response, list, "耗材领用");
    }

    @PreAuthorize("@ss.hasPermi('module:material:apply')")
    @GetMapping("/reservationOptions")
    public AjaxResult reservationOptions()
    {
        return success(materialApplyService.selectReservationOptions());
    }

    @PreAuthorize("@ss.hasPermi('module:material:apply')")
    @GetMapping("/materialOptions")
    public AjaxResult materialOptions(@RequestParam Long reservationId)
    {
        return success(materialApplyService.selectMaterialOptions(reservationId));
    }

    @PreAuthorize("@ss.hasPermi('module:material:apply:query')")
    @GetMapping("/{id}")
    public AjaxResult getInfo(@PathVariable Long id)
    {
        MaterialApply apply = materialApplyService.selectApplyById(id);
        if (apply == null)
        {
            throw new ServiceException("领用申请不存在");
        }
        moduleDataScopeService.checkStudentRecordAccess(apply.getApplicantId());
        return success(apply);
    }

    @PreAuthorize("@ss.hasPermi('module:material:apply')")
    @Log(title = "耗材领用", businessType = BusinessType.INSERT)
    @PostMapping
    @RepeatSubmit
    public AjaxResult apply(@Validated @RequestBody MaterialApply materialApply)
    {
        return success(materialApplyService.applyMaterial(materialApply));
    }

    @PreAuthorize("@ss.hasPermi('module:material:review')")
    @Log(title = "耗材领用", businessType = BusinessType.UPDATE)
    @PostMapping("/{id}/review")
    public AjaxResult review(@PathVariable Long id,
            @Validated @RequestBody MaterialReviewRequest request)
    {
        return success(materialApplyService.review(id, request));
    }
}
