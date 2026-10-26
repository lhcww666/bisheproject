package com.ruoyi.module.material.controller;

import java.util.List;

import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.ruoyi.common.annotation.Log;
import com.ruoyi.common.core.controller.BaseController;
import com.ruoyi.common.core.domain.AjaxResult;
import com.ruoyi.common.core.page.TableDataInfo;
import com.ruoyi.common.enums.BusinessType;
import com.ruoyi.common.utils.poi.ExcelUtil;
import com.ruoyi.module.material.domain.MaterialStockRecord;
import com.ruoyi.module.material.service.IMaterialStockRecordService;

import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;

/**
 * 实验室耗材库存流水 Controller。
 */
@RestController
@RequestMapping("/module/material/stock-record")
@RequiredArgsConstructor
public class MaterialStockRecordController extends BaseController
{
    private final IMaterialStockRecordService stockRecordService;

    @PreAuthorize("@ss.hasPermi('module:material:stock:list')")
    @GetMapping("/list")
    public TableDataInfo list(MaterialStockRecord stockRecord)
    {
        QueryWrapper<MaterialStockRecord> queryWrapper = getQueryWrapper(MaterialStockRecord.class);
        IPage<MaterialStockRecord> page = stockRecordService.pageRecord(getPage(), queryWrapper);
        return getDataTableByPage(page);
    }

    @PreAuthorize("@ss.hasPermi('module:material:stock:export')")
    @Log(title = "耗材库存流水", businessType = BusinessType.EXPORT)
    @PostMapping("/export")
    public void export(HttpServletResponse response, MaterialStockRecord stockRecord)
    {
        QueryWrapper<MaterialStockRecord> queryWrapper = getQueryWrapper(MaterialStockRecord.class);
        List<MaterialStockRecord> list = stockRecordService
                .pageRecord(new Page<>(1, 10000, false), queryWrapper).getRecords();
        ExcelUtil<MaterialStockRecord> util = new ExcelUtil<>(MaterialStockRecord.class);
        util.exportExcel(response, list, "耗材库存流水");
    }

    @PreAuthorize("@ss.hasPermi('module:material:stock:query')")
    @GetMapping("/{id}")
    public AjaxResult getInfo(@PathVariable Long id)
    {
        return success(stockRecordService.selectRecordById(id));
    }
}
