package com.ruoyi.module.agent.knowledge.controller;

import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.ruoyi.common.core.controller.BaseController;
import com.ruoyi.common.core.domain.AjaxResult;
import com.ruoyi.module.agent.knowledge.service.IKnowledgeSearchService;

import lombok.RequiredArgsConstructor;

/**
 * 安全知识库检索入口，供 Agent 工具和前端知识库弹窗使用。
 *
 * @author itjialu
 */
@RestController
@RequestMapping("/module/agent/knowledge")
@RequiredArgsConstructor
public class KnowledgeController extends BaseController
{
    private final IKnowledgeSearchService knowledgeSearchService;

    /**
     * 按自然语言问题检索安全知识片段。
     */
    @PreAuthorize("@ss.hasPermi('module:agent:use')")
    @GetMapping("/search")
    public AjaxResult search(@RequestParam String q,
            @RequestParam(defaultValue = "3") int topK)
    {
        return success(knowledgeSearchService.search(q, topK));
    }
}
