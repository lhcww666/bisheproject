package com.ruoyi.module.violation.controller;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.ruoyi.common.annotation.Log;
import com.ruoyi.common.core.controller.BaseController;
import com.ruoyi.common.core.domain.AjaxResult;
import com.ruoyi.common.core.page.TableDataInfo;
import com.ruoyi.common.enums.BusinessType;
import com.ruoyi.module.common.service.ModuleDataScopeService;
import com.ruoyi.module.violation.domain.ViolationRecord;
import com.ruoyi.module.violation.domain.ViolationRevokeRequest;
import com.ruoyi.module.violation.service.IViolationRecordService;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * 违规记录Controller。
 *
 * @author itjialu
 */
@RestController
@RequestMapping("/module/violationRecord")
@RequiredArgsConstructor
public class ViolationRecordController extends BaseController
{
    private final IViolationRecordService violationRecordService;

    private final ModuleDataScopeService moduleDataScopeService;

    @PreAuthorize("@ss.hasPermi('module:violationRecord:list')")
    @GetMapping("/list")
    public TableDataInfo list(ViolationRecord violationRecord)
    {
        QueryWrapper<ViolationRecord> queryWrapper = getQueryWrapper(ViolationRecord.class);
        moduleDataScopeService.applyStudentRecordScope(queryWrapper, "user_id");
        IPage<ViolationRecord> page = violationRecordService.pageViolation(getPage(), queryWrapper);
        return getDataTableByPage(page);
    }

    @PreAuthorize("@ss.hasPermi('module:violationRecord:query')")
    @GetMapping("/{id}")
    public AjaxResult getInfo(@PathVariable Long id)
    {
        ViolationRecord record = violationRecordService.selectViolationById(id);
        if (record != null)
        {
            moduleDataScopeService.checkStudentRecordAccess(record.getUserId());
        }
        return success(record);
    }

    @PreAuthorize("@ss.hasPermi('module:violationRecord:add')")
    @Log(title = "违规记录", businessType = BusinessType.INSERT)
    @PostMapping
    public AjaxResult add(@Validated @RequestBody ViolationRecord record)
    {
        return success(violationRecordService.createViolation(record));
    }

    @PreAuthorize("@ss.hasPermi('module:violationRecord:edit')")
    @Log(title = "违规记录", businessType = BusinessType.UPDATE)
    @PostMapping("/{id}/revoke")
    public AjaxResult revoke(@PathVariable Long id, @Validated @RequestBody ViolationRevokeRequest request)
    {
        violationRecordService.revokeViolation(id, request.getReason());
        return success();
    }
}
