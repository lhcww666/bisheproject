package com.ruoyi.module.agent.knowledge.service;

import java.util.List;

import com.ruoyi.module.agent.knowledge.domain.KnowledgeChunk;
import com.ruoyi.module.agent.knowledge.domain.KnowledgeDocument;

/**
 * 安全知识库维护服务。
 *
 * @author itjialu
 */
public interface IKnowledgeAdminService
{
    List<KnowledgeDocument> listDocuments(String title, String category, String knowledgeType,
            String audience, String status);

    KnowledgeDocument getDocument(Long id);

    int createDocument(KnowledgeDocument document);

    int updateDocument(KnowledgeDocument document);

    int deleteDocument(Long id);

    List<KnowledgeChunk> listChunks(Long documentId, String keyword, String status);

    int createChunk(KnowledgeChunk chunk);

    int updateChunk(KnowledgeChunk chunk);

    int deleteChunk(Long id);
}
