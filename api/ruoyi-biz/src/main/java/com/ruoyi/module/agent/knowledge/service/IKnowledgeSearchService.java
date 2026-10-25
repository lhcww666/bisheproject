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

    /**
     * 按知识类型检索，并自动应用当前登录角色的可见范围。
     *
     * @param question      用户问题
     * @param topK          返回条数
     * @param knowledgeType SAFETY 或 PLATFORM；为空时检索全部类型
     */
    List<KnowledgeSearchResult> search(String question, int topK, String knowledgeType);
}
