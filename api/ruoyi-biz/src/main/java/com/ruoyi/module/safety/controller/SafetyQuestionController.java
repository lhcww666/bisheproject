package com.ruoyi.module.safety.controller;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.ruoyi.common.annotation.Log;
import com.ruoyi.common.annotation.RepeatSubmit;
import com.ruoyi.common.core.controller.BaseController;
import com.ruoyi.common.core.domain.AjaxResult;
import com.ruoyi.common.core.page.TableDataInfo;
import com.ruoyi.common.enums.BusinessType;
import com.ruoyi.module.safety.domain.SafetyQuestion;
import com.ruoyi.module.safety.domain.SafetyQuestionRequest;
import com.ruoyi.module.safety.domain.SafetyQuestionView;
import com.ruoyi.module.safety.service.ISafetyQuestionService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * 安全题库Controller。
 *
 * @author itjialu
 */
@RestController
@RequestMapping("/module/safetyQuestion")
@RequiredArgsConstructor
public class SafetyQuestionController extends BaseController
{
    private final ISafetyQuestionService safetyQuestionService;

    @PreAuthorize("@ss.hasPermi('module:safetyQuestion:list')")
    @GetMapping("/list")
    public TableDataInfo list(SafetyQuestion safetyQuestion)
    {
        QueryWrapper<SafetyQuestion> queryWrapper = getQueryWrapper(SafetyQuestion.class);
        IPage<SafetyQuestionView> page = safetyQuestionService.pageQuestion(getPage(), queryWrapper);
        return getDataTableByPage(page);
    }

    @PreAuthorize("@ss.hasPermi('module:safetyQuestion:query')")
    @GetMapping("/{id}")
    public AjaxResult getInfo(@PathVariable Long id)
    {
        return success(safetyQuestionService.selectQuestionView(id));
    }

    @PreAuthorize("@ss.hasPermi('module:safetyQuestion:add')")
    @Log(title = "安全题库", businessType = BusinessType.INSERT)
    @PostMapping
    @RepeatSubmit
    public AjaxResult add(@Valid @RequestBody SafetyQuestionRequest request)
    {
        return toAjax(safetyQuestionService.insertQuestion(request));
    }

    @PreAuthorize("@ss.hasPermi('module:safetyQuestion:edit')")
    @Log(title = "安全题库", businessType = BusinessType.UPDATE)
    @PutMapping
    public AjaxResult edit(@Valid @RequestBody SafetyQuestionRequest request)
    {
        return toAjax(safetyQuestionService.updateQuestion(request));
    }

    @PreAuthorize("@ss.hasPermi('module:safetyQuestion:remove')")
    @Log(title = "安全题库", businessType = BusinessType.DELETE)
    @DeleteMapping("/{ids}")
    public AjaxResult remove(@PathVariable Long[] ids)
    {
        return toAjax(safetyQuestionService.deleteQuestionByIds(ids));
    }
}
