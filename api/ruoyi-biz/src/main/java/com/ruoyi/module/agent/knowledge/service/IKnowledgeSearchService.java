package com.ruoyi.module.agent.knowledge.service;

import java.util.List;

import com.ruoyi.module.agent.knowledge.domain.KnowledgeSearchResult;

/**
 * 安全知识检索服务。
 *
 * @author itjialu
 */
public interface IKnowledgeSearchService
{
    /**
     * 根据自然语言问题检索最相关的知识片段。
     *
     * @param question 用户问题
     * @param topK     返回条数
     */
    List<KnowledgeSearchResult> search(String question, int topK);
}
