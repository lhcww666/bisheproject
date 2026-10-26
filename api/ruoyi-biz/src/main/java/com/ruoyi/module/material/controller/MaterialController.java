package com.ruoyi.module.material.controller;

import java.util.List;

import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
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
import com.ruoyi.common.utils.poi.ExcelUtil;
import com.ruoyi.module.material.domain.Material;
import com.ruoyi.module.material.domain.MaterialStockAdjustRequest;
import com.ruoyi.module.material.service.IMaterialService;

import cn.hutool.json.JSONObject;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;

/**
 * 实验室耗材档案 Controller。
 */
@RestController
@RequestMapping("/module/material")
@RequiredArgsConstructor
public class MaterialController extends BaseController
{
    private final IMaterialService materialService;

    @PreAuthorize("@ss.hasPermi('module:material:list')")
    @GetMapping("/list")
    public TableDataInfo list(Material material)
    {
        QueryWrapper<Material> queryWrapper = getQueryWrapper(Material.class);
        IPage<Material> page = materialService.pageMaterial(getPage(), queryWrapper);
        return getDataTableByPage(page);
    }

    @PreAuthorize("@ss.hasPermi('module:material:export')")
    @Log(title = "耗材库存", businessType = BusinessType.EXPORT)
    @PostMapping("/export")
    public void export(HttpServletResponse response, Material material)
    {
        QueryWrapper<Material> queryWrapper = getQueryWrapper(Material.class);
        List<Material> list = materialService.pageMaterial(new Page<>(1, 10000, false), queryWrapper)
                .getRecords();
        ExcelUtil<Material> util = new ExcelUtil<>(Material.class);
        util.exportExcel(response, list, "耗材库存");
    }

    @PreAuthorize("@ss.hasAnyPermi('module:material:list,module:material:add,module:material:edit')")
    @GetMapping("/roomOptions")
    public AjaxResult roomOptions()
    {
        return success(materialService.selectRoomOptions());
    }

    @PreAuthorize("@ss.hasPermi('module:material:query')")
    @GetMapping("/{id}")
    public AjaxResult getInfo(@PathVariable Long id)
    {
        return success(materialService.selectMaterialById(id));
    }

    @PreAuthorize("@ss.hasPermi('module:material:add')")
    @Log(title = "耗材库存", businessType = BusinessType.INSERT)
    @PostMapping
    @RepeatSubmit
    public AjaxResult add(@Validated @RequestBody Material material)
    {
        materialService.insertMaterial(material);
        JSONObject result = new JSONObject();
        result.putOpt("id", material.getId());
        return success(result);
    }

    @PreAuthorize("@ss.hasPermi('module:material:edit')")
    @Log(title = "耗材库存", businessType = BusinessType.UPDATE)
    @PutMapping
    public AjaxResult edit(@Validated @RequestBody Material material)
    {
        return toAjax(materialService.updateMaterial(material));
    }

    @PreAuthorize("@ss.hasPermi('module:material:adjust')")
    @Log(title = "耗材库存", businessType = BusinessType.UPDATE)
    @PostMapping("/{id}/stock-adjust")
    public AjaxResult adjustStock(@PathVariable Long id,
            @Validated @RequestBody MaterialStockAdjustRequest request)
    {
        materialService.adjustStock(id, request);
        return success();
    }

    @PreAuthorize("@ss.hasPermi('module:material:remove')")
    @Log(title = "耗材库存", businessType = BusinessType.DELETE)
    @DeleteMapping("/{ids}")
    public AjaxResult remove(@PathVariable Long[] ids)
    {
        return toAjax(materialService.deleteMaterialByIds(ids));
    }
}
