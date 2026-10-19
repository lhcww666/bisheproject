package com.ruoyi.module.classrep.controller;

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
import com.ruoyi.common.annotation.Log;
import com.ruoyi.common.core.controller.BaseController;
import com.ruoyi.common.core.domain.AjaxResult;
import com.ruoyi.common.core.page.TableDataInfo;
import com.ruoyi.common.enums.BusinessType;
import com.ruoyi.common.utils.StringUtils;
import com.ruoyi.module.classrep.domain.ClassRepresentative;
import com.ruoyi.module.classrep.service.IClassRepresentativeService;

import lombok.RequiredArgsConstructor;

/**
 * 班级负责人Controller
 *
 * @author itjialu
 */
@RestController
@RequestMapping("/module/classRepresentative")
@RequiredArgsConstructor
public class ClassRepresentativeController extends BaseController
{
    private final IClassRepresentativeService classRepresentativeService;

    @PreAuthorize("@ss.hasPermi('module:classRepresentative:list')")
    @GetMapping("/list")
    public TableDataInfo list(ClassRepresentative representative)
    {
        QueryWrapper<ClassRepresentative> queryWrapper = new QueryWrapper<>();
        if (representative.getDeptId() != null)
        {
            queryWrapper.eq("dept_id", representative.getDeptId());
        }
        if (representative.getStudentId() != null)
        {
            queryWrapper.eq("student_id", representative.getStudentId());
        }
        if (StringUtils.isNotEmpty(representative.getStatus()))
        {
            queryWrapper.eq("status", representative.getStatus());
        }
        queryWrapper.orderByDesc("id");
        IPage<ClassRepresentative> page = classRepresentativeService.pageClassRepresentative(getPage(),
                queryWrapper);
        return getDataTableByPage(page);
    }

    @PreAuthorize("@ss.hasPermi('module:classRepresentative:query')")
    @GetMapping("/{id}")
    public AjaxResult getInfo(@PathVariable Long id)
    {
        return success(classRepresentativeService.selectClassRepresentativeById(id));
    }

    @PreAuthorize("@ss.hasPermi('module:classRepresentative:add')")
    @Log(title = "班级负责人", businessType = BusinessType.INSERT)
    @PostMapping
    public AjaxResult add(@Validated @RequestBody ClassRepresentative representative)
    {
        classRepresentativeService.insertClassRepresentative(representative);
        return success(representative.getId());
    }

    @PreAuthorize("@ss.hasPermi('module:classRepresentative:edit')")
    @Log(title = "班级负责人", businessType = BusinessType.UPDATE)
    @PutMapping
    public AjaxResult edit(@Validated @RequestBody ClassRepresentative representative)
    {
        return toAjax(classRepresentativeService.updateClassRepresentative(representative));
    }

    @PreAuthorize("@ss.hasPermi('module:classRepresentative:remove')")
    @Log(title = "班级负责人", businessType = BusinessType.DELETE)
    @DeleteMapping("/{ids}")
    public AjaxResult remove(@PathVariable Long[] ids)
    {
        return toAjax(classRepresentativeService.deleteClassRepresentativeByIds(ids));
    }
}
