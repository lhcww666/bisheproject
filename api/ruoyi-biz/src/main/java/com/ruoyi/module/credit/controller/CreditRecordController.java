package com.ruoyi.module.credit.controller;

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
import com.ruoyi.module.credit.domain.CreditRecord;
import com.ruoyi.module.credit.service.ICreditRecordService;
import com.ruoyi.module.credit.mapper.CreditRecordMapper;
import com.ruoyi.module.common.service.ModuleDataScopeService;
import com.ruoyi.common.utils.poi.ExcelUtil;
import com.ruoyi.common.core.page.TableDataInfo;

/**
 * 信用分流水Controller
 *
 * @author itjialu
 * @date 2026-10-16
 */
@RestController
@RequestMapping("/module/creditRecord")
@RequiredArgsConstructor
public class CreditRecordController extends BaseController {
    private final ICreditRecordService creditRecordService;
    private final ModuleDataScopeService moduleDataScopeService;
    // private final CreditRecordMapper creditRecordMapper;

/**
 * 查询信用分流水列表
 */
@PreAuthorize("@ss.hasPermi('module:creditRecord:list')")
@GetMapping("/list")
    public TableDataInfo list(CreditRecord creditRecord) {
        QueryWrapper<CreditRecord> queryWrapper = getQueryWrapper(CreditRecord.class);
        moduleDataScopeService.applyStudentRecordScope(queryWrapper, "user_id");
        IPage<CreditRecord> page = creditRecordService.pageCreditRecord(getPage(), queryWrapper);
        return getDataTableByPage(page);
    }

    /**
     * 导出信用分流水列表
     */
    @PreAuthorize("@ss.hasPermi('module:creditRecord:export')")
    @Log(title = "信用分流水", businessType = BusinessType.EXPORT)
    @PostMapping("/export")
    public void export(HttpServletResponse response, CreditRecord creditRecord) {
        QueryWrapper<CreditRecord> queryWrapper = getQueryWrapper(CreditRecord.class);
        moduleDataScopeService.applyStudentRecordScope(queryWrapper, "user_id");
        IPage<CreditRecord> page = creditRecordService.pageCreditRecord(getPage(), queryWrapper);
        List<CreditRecord> list = page.getRecords();
        ExcelUtil<CreditRecord> util = new ExcelUtil<CreditRecord>(CreditRecord. class);
        util.exportExcel(response, list, "信用分流水");
    }

    /**
     * 导入信用分流水数据
     */
    @PreAuthorize("@ss.hasPermi('module:creditRecord:import')")
    @Log(title = "信用分流水", businessType = BusinessType.IMPORT)
    @PostMapping("/importData")
    public AjaxResult importData(MultipartFile file, boolean updateSupport) throws Exception {
        int titleNum = 0;
        ExcelUtil<CreditRecord> util = new ExcelUtil<CreditRecord>(CreditRecord. class);
        List<CreditRecord> list = util.importExcel(file.getInputStream(), titleNum);
        String operName = getUsername();
        String message = creditRecordService.importCreditRecord(list, titleNum, updateSupport, operName);
        return success(message);
    }

    @PostMapping("/importTemplate")
    public void importTemplate(HttpServletResponse response, CreditRecord creditRecord) {
        List<CreditRecord> list = new ArrayList<>();
        ExcelUtil<CreditRecord> util = new ExcelUtil<CreditRecord>(CreditRecord. class);
        util.exportExcel(response, list, "信用分流水");
    }

    /**
     * 获取信用分流水详细信息
     */
    @PreAuthorize("@ss.hasPermi('module:creditRecord:query')")
    @GetMapping(value = "/{id}")
    public AjaxResult getInfo(@PathVariable("id") Long id) {
        CreditRecord creditRecord = creditRecordService.selectCreditRecordById(id);
        if (creditRecord != null) {
            moduleDataScopeService.checkStudentRecordAccess(creditRecord.getUserId());
        }
        return success(creditRecord);
    }

    /**
     * 新增信用分流水
     */
    @PreAuthorize("@ss.hasPermi('module:creditRecord:add')")
    @Log(title = "信用分流水", businessType = BusinessType.INSERT)
    @PostMapping
    @RepeatSubmit
    public AjaxResult add(@Validated @RequestBody CreditRecord creditRecord) {
            creditRecordService.insertCreditRecord(creditRecord);
        JSONObject jsonObject = new JSONObject();
        jsonObject.putOpt("id", creditRecord.getId());
        return success(jsonObject);
    }

    /**
     * 修改信用分流水
     */
    @PreAuthorize("@ss.hasPermi('module:creditRecord:edit')")
    @Log(title = "信用分流水", businessType = BusinessType.UPDATE)
    @PutMapping
    public AjaxResult edit(@Validated @RequestBody CreditRecord creditRecord) {
        return toAjax(creditRecordService.updateCreditRecord(creditRecord));
    }

    /**
     * 删除信用分流水
     */
    @PreAuthorize("@ss.hasPermi('module:creditRecord:remove')")
    @Log(title = "信用分流水", businessType = BusinessType.DELETE)
    @DeleteMapping("/{ids}")
    public AjaxResult remove(@PathVariable Long[] ids) {
        return toAjax(creditRecordService.deleteCreditRecordByIds(ids));
    }
}
