package com.ruoyi.module.agent.knowledge.controller;

import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.ruoyi.common.annotation.Log;
import com.ruoyi.common.annotation.RepeatSubmit;
import com.ruoyi.common.core.controller.BaseController;
import com.ruoyi.common.core.domain.AjaxResult;
import com.ruoyi.common.enums.BusinessType;
import com.ruoyi.module.agent.knowledge.domain.KnowledgeChunk;
import com.ruoyi.module.agent.knowledge.domain.KnowledgeDocument;
import com.ruoyi.module.agent.knowledge.service.IKnowledgeAdminService;

import lombok.RequiredArgsConstructor;

/**
 * 安全知识库管理接口，仅管理员可维护。
 *
 * @author itjialu
 */
@RestController
@RequestMapping("/module/agent/knowledge/admin")
@RequiredArgsConstructor
public class KnowledgeAdminController extends BaseController
{
    private final IKnowledgeAdminService knowledgeAdminService;

    @PreAuthorize("@ss.hasPermi('module:agent:knowledge:list')")
    @GetMapping("/documents")
    public AjaxResult listDocuments(@RequestParam(required = false) String title,
            @RequestParam(required = false) String category,
            @RequestParam(required = false) String status)
    {
        return success(knowledgeAdminService.listDocuments(title, category, status));
    }

    @PreAuthorize("@ss.hasPermi('module:agent:knowledge:query')")
    @GetMapping("/documents/{id}")
    public AjaxResult getDocument(@PathVariable Long id)
    {
        return success(knowledgeAdminService.getDocument(id));
    }

    @PreAuthorize("@ss.hasPermi('module:agent:knowledge:add')")
    @PostMapping("/documents")
    @RepeatSubmit
    @Log(title = "安全知识文档", businessType = BusinessType.INSERT)
    public AjaxResult addDocument(@RequestBody KnowledgeDocument document)
    {
        return toAjax(knowledgeAdminService.createDocument(document));
    }

    @PreAuthorize("@ss.hasPermi('module:agent:knowledge:edit')")
    @PutMapping("/documents")
    @Log(title = "安全知识文档", businessType = BusinessType.UPDATE)
    public AjaxResult editDocument(@RequestBody KnowledgeDocument document)
    {
        return toAjax(knowledgeAdminService.updateDocument(document));
    }

    @PreAuthorize("@ss.hasPermi('module:agent:knowledge:remove')")
    @DeleteMapping("/documents/{id}")
    @Log(title = "安全知识文档", businessType = BusinessType.DELETE)
    public AjaxResult removeDocument(@PathVariable Long id)
    {
        return toAjax(knowledgeAdminService.deleteDocument(id));
    }

    @PreAuthorize("@ss.hasPermi('module:agent:knowledge:list')")
    @GetMapping("/chunks")
    public AjaxResult listChunks(@RequestParam(required = false) Long documentId,
            @RequestParam(required = false) String keyword,
            @RequestParam(required = false) String status)
    {
        return success(knowledgeAdminService.listChunks(documentId, keyword, status));
    }

    @PreAuthorize("@ss.hasPermi('module:agent:knowledge:add')")
    @PostMapping("/chunks")
    @RepeatSubmit
    @Log(title = "安全知识片段", businessType = BusinessType.INSERT)
    public AjaxResult addChunk(@RequestBody KnowledgeChunk chunk)
    {
        return toAjax(knowledgeAdminService.createChunk(chunk));
    }

    @PreAuthorize("@ss.hasPermi('module:agent:knowledge:edit')")
    @PutMapping("/chunks")
    @Log(title = "安全知识片段", businessType = BusinessType.UPDATE)
    public AjaxResult editChunk(@RequestBody KnowledgeChunk chunk)
    {
        return toAjax(knowledgeAdminService.updateChunk(chunk));
    }

    @PreAuthorize("@ss.hasPermi('module:agent:knowledge:remove')")
    @DeleteMapping("/chunks/{id}")
    @Log(title = "安全知识片段", businessType = BusinessType.DELETE)
    public AjaxResult removeChunk(@PathVariable Long id)
    {
        return toAjax(knowledgeAdminService.deleteChunk(id));
    }
}
