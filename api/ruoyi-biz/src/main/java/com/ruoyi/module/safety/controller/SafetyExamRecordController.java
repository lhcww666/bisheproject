package com.ruoyi.module.safety.controller;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import lombok.RequiredArgsConstructor;
import java.util.List;
import java.util.ArrayList;

import org.springframework.validation.annotation.Validated;
import cn.hutool.json.JSONObject;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.ruoyi.common.annotation.RepeatSubmit;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;
import com.ruoyi.common.annotation.Log;
import com.ruoyi.common.core.controller.BaseController;
import com.ruoyi.common.core.domain.AjaxResult;
import com.ruoyi.common.enums.BusinessType;
import com.ruoyi.module.safety.domain.SafetyExamRecord;
import com.ruoyi.module.safety.service.ISafetyExamRecordService;
import com.ruoyi.module.safety.mapper.SafetyExamRecordMapper;
import com.ruoyi.module.common.service.ModuleDataScopeService;
import com.ruoyi.common.utils.poi.ExcelUtil;
import com.ruoyi.common.core.page.TableDataInfo;

/**
 * 安全考试记录Controller
 *
 * @author itjialu
 * @date 2026-10-16
 */
@RestController
@RequestMapping("/module/safetyExamRecord")
@RequiredArgsConstructor
public class SafetyExamRecordController extends BaseController {
    private final ISafetyExamRecordService safetyExamRecordService;
    private final ModuleDataScopeService moduleDataScopeService;
    // private final SafetyExamRecordMapper safetyExamRecordMapper;

/**
 * 查询安全考试记录列表
 */
@PreAuthorize("@ss.hasPermi('module:safetyExamRecord:list')")
@GetMapping("/list")
    public TableDataInfo list(SafetyExamRecord safetyExamRecord) {
        QueryWrapper<SafetyExamRecord> queryWrapper = getQueryWrapper(SafetyExamRecord.class);
        moduleDataScopeService.applyStudentRecordScope(queryWrapper, "student_id");
        IPage<SafetyExamRecord> page = safetyExamRecordService.pageSafetyExamRecord(getPage(), queryWrapper);
        return getDataTableByPage(page);
    }

    /**
     * 导出安全考试记录列表
     */
    @PreAuthorize("@ss.hasPermi('module:safetyExamRecord:export')")
    @Log(title = "安全考试记录", businessType = BusinessType.EXPORT)
    @PostMapping("/export")
    public void export(HttpServletResponse response, SafetyExamRecord safetyExamRecord) {
        QueryWrapper<SafetyExamRecord> queryWrapper = getQueryWrapper(SafetyExamRecord.class);
        moduleDataScopeService.applyStudentRecordScope(queryWrapper, "student_id");
        IPage<SafetyExamRecord> page = safetyExamRecordService.pageSafetyExamRecord(getPage(), queryWrapper);
        List<SafetyExamRecord> list = page.getRecords();
        ExcelUtil<SafetyExamRecord> util = new ExcelUtil<SafetyExamRecord>(SafetyExamRecord. class);
        util.exportExcel(response, list, "安全考试记录");
    }

    /**
     * 导入安全考试记录数据
     */
    @PreAuthorize("@ss.hasPermi('module:safetyExamRecord:import')")
    @Log(title = "安全考试记录", businessType = BusinessType.IMPORT)
    @PostMapping("/importData")
    public AjaxResult importData(MultipartFile file, boolean updateSupport) throws Exception {
        int titleNum = 0;
        ExcelUtil<SafetyExamRecord> util = new ExcelUtil<SafetyExamRecord>(SafetyExamRecord. class);
        List<SafetyExamRecord> list = util.importExcel(file.getInputStream(), titleNum);
        String operName = getUsername();
        String message = safetyExamRecordService.importSafetyExamRecord(list, titleNum, updateSupport, operName);
        return success(message);
    }

    @PostMapping("/importTemplate")
    public void importTemplate(HttpServletResponse response, SafetyExamRecord safetyExamRecord) {
        List<SafetyExamRecord> list = new ArrayList<>();
        ExcelUtil<SafetyExamRecord> util = new ExcelUtil<SafetyExamRecord>(SafetyExamRecord. class);
        util.exportExcel(response, list, "安全考试记录");
    }

    /**
     * 获取安全考试记录详细信息
     */
    @PreAuthorize("@ss.hasPermi('module:safetyExamRecord:query')")
    @GetMapping(value = "/{id}")
    public AjaxResult getInfo(@PathVariable("id") Long id) {
        SafetyExamRecord safetyExamRecord = safetyExamRecordService.selectSafetyExamRecordById(id);
        if (safetyExamRecord != null) {
            moduleDataScopeService.checkStudentRecordAccess(safetyExamRecord.getStudentId());
        }
        return success(safetyExamRecord);
    }

    /**
     * 新增安全考试记录
     */
    @PreAuthorize("@ss.hasPermi('module:safetyExamRecord:add')")
    @Log(title = "安全考试记录", businessType = BusinessType.INSERT)
    @PostMapping
    @RepeatSubmit
    public AjaxResult add(@Validated @RequestBody SafetyExamRecord safetyExamRecord) {
            safetyExamRecordService.insertSafetyExamRecord(safetyExamRecord);
        JSONObject jsonObject = new JSONObject();
        jsonObject.putOpt("id", safetyExamRecord.getId());
        return success(jsonObject);
    }

    /**
     * 修改安全考试记录
     */
    @PreAuthorize("@ss.hasPermi('module:safetyExamRecord:edit')")
    @Log(title = "安全考试记录", businessType = BusinessType.UPDATE)
    @PutMapping
    public AjaxResult edit(@Validated @RequestBody SafetyExamRecord safetyExamRecord) {
        return toAjax(safetyExamRecordService.updateSafetyExamRecord(safetyExamRecord));
    }

    /**
     * 删除安全考试记录
     */
    @PreAuthorize("@ss.hasPermi('module:safetyExamRecord:remove')")
    @Log(title = "安全考试记录", businessType = BusinessType.DELETE)
    @DeleteMapping("/{ids}")
    public AjaxResult remove(@PathVariable Long[] ids) {
        return toAjax(safetyExamRecordService.deleteSafetyExamRecordByIds(ids));
    }
}
