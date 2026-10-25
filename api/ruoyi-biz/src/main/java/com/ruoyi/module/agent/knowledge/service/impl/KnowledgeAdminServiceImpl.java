package com.ruoyi.module.agent.knowledge.service.impl;

import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.ruoyi.common.exception.ServiceException;
import com.ruoyi.common.utils.SecurityUtils;
import com.ruoyi.common.utils.StringUtils;
import com.ruoyi.module.agent.knowledge.domain.KnowledgeChunk;
import com.ruoyi.module.agent.knowledge.domain.KnowledgeDocument;
import com.ruoyi.module.agent.knowledge.mapper.KnowledgeChunkMapper;
import com.ruoyi.module.agent.knowledge.mapper.KnowledgeDocumentMapper;
import com.ruoyi.module.agent.knowledge.service.IKnowledgeAdminService;

import lombok.RequiredArgsConstructor;

/**
 * 安全知识库维护实现。页面修改后，混合检索会立即读取最新数据。
 *
 * @author itjialu
 */
@Service
@RequiredArgsConstructor
public class KnowledgeAdminServiceImpl implements IKnowledgeAdminService
{
    private static final String STATUS_ENABLED = "0";

    private final KnowledgeDocumentMapper documentMapper;

    private final KnowledgeChunkMapper chunkMapper;

    @Override
    public List<KnowledgeDocument> listDocuments(String title, String category, String knowledgeType,
            String audience, String status)
    {
        QueryWrapper<KnowledgeDocument> query = new QueryWrapper<>();
        if (StringUtils.isNotEmpty(title))
        {
            query.like("title", title.trim());
        }
        if (StringUtils.isNotEmpty(category))
        {
            query.eq("category", category.trim());
        }
        if (StringUtils.isNotEmpty(knowledgeType))
        {
            query.eq("knowledge_type", knowledgeType);
        }
        if (StringUtils.isNotEmpty(audience))
        {
            query.eq("audience", audience);
        }
        if (StringUtils.isNotEmpty(status))
        {
            query.eq("status", status);
        }
        query.orderByDesc("update_time").orderByDesc("id");
        List<KnowledgeDocument> documents = documentMapper.selectList(query);
        fillChunkCounts(documents);
        return documents;
    }

    @Override
    public KnowledgeDocument getDocument(Long id)
    {
        KnowledgeDocument document = requireDocument(id);
        Long count = chunkMapper.selectCount(new QueryWrapper<KnowledgeChunk>().eq("document_id", id));
        document.setChunkCount(count);
        return document;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public int createDocument(KnowledgeDocument document)
    {
        validateDocument(document, null);
        Date now = new Date();
        document.setKnowledgeType(normalizeKnowledgeType(document.getKnowledgeType()));
        document.setAudience(normalizeAudience(document.getAudience()));
        document.setStatus(normalizeStatus(document.getStatus()));
        document.setCreateBy(SecurityUtils.getUsername());
        document.setCreateTime(now);
        document.setUpdateBy(SecurityUtils.getUsername());
        document.setUpdateTime(now);
        return documentMapper.insert(document);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public int updateDocument(KnowledgeDocument document)
    {
        requireDocument(document == null ? null : document.getId());
        validateDocument(document, document.getId());
        document.setKnowledgeType(normalizeKnowledgeType(document.getKnowledgeType()));
        document.setAudience(normalizeAudience(document.getAudience()));
        document.setStatus(normalizeStatus(document.getStatus()));
        document.setUpdateBy(SecurityUtils.getUsername());
        document.setUpdateTime(new Date());
        return documentMapper.updateById(document);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public int deleteDocument(Long id)
    {
        requireDocument(id);
        return documentMapper.deleteById(id);
    }

    @Override
    public List<KnowledgeChunk> listChunks(Long documentId, String keyword, String status)
    {
        QueryWrapper<KnowledgeChunk> query = new QueryWrapper<>();
        if (documentId != null)
        {
            query.eq("document_id", documentId);
        }
        if (StringUtils.isNotEmpty(keyword))
        {
            query.and(wrapper -> wrapper.like("keywords", keyword.trim()).or().like("content", keyword.trim()));
        }
        if (StringUtils.isNotEmpty(status))
        {
            query.eq("status", status);
        }
        query.orderByAsc("document_id").orderByAsc("chunk_no").orderByAsc("id");
        List<KnowledgeChunk> chunks = chunkMapper.selectList(query);
        Map<Long, String> titles = new HashMap<>();
        for (KnowledgeChunk chunk : chunks)
        {
            if (!titles.containsKey(chunk.getDocumentId()))
            {
                KnowledgeDocument document = documentMapper.selectById(chunk.getDocumentId());
                titles.put(chunk.getDocumentId(), document == null ? "未知文档" : document.getTitle());
            }
            chunk.setDocumentTitle(titles.get(chunk.getDocumentId()));
        }
        return chunks;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public int createChunk(KnowledgeChunk chunk)
    {
        validateChunk(chunk, null);
        if (chunk.getChunkNo() == null)
        {
            chunk.setChunkNo(nextChunkNo(chunk.getDocumentId()));
        }
        chunk.setStatus(normalizeStatus(chunk.getStatus()));
        chunk.setCreateTime(new Date());
        chunk.setUpdateTime(new Date());
        return chunkMapper.insert(chunk);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public int updateChunk(KnowledgeChunk chunk)
    {
        if (chunk == null || chunk.getId() == null || chunkMapper.selectById(chunk.getId()) == null)
        {
            throw new ServiceException("知识片段不存在");
        }
        validateChunk(chunk, chunk.getId());
        chunk.setStatus(normalizeStatus(chunk.getStatus()));
        chunk.setUpdateTime(new Date());
        return chunkMapper.updateById(chunk);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public int deleteChunk(Long id)
    {
        if (id == null || chunkMapper.selectById(id) == null)
        {
            throw new ServiceException("知识片段不存在");
        }
        return chunkMapper.deleteById(id);
    }

    private void validateDocument(KnowledgeDocument document, Long excludeId)
    {
        if (document == null || StringUtils.isEmpty(document.getTitle()))
        {
            throw new ServiceException("文档标题不能为空");
        }
        if (document.getTitle().trim().length() > 200)
        {
            throw new ServiceException("文档标题不能超过 200 个字符");
        }
        QueryWrapper<KnowledgeDocument> duplicate = new QueryWrapper<>();
        duplicate.eq("title", document.getTitle().trim());
        if (excludeId != null)
        {
            duplicate.ne("id", excludeId);
        }
        if (documentMapper.selectCount(duplicate) > 0)
        {
            throw new ServiceException("文档标题已存在");
        }
        document.setTitle(document.getTitle().trim());
        document.setCategory(trimToNull(document.getCategory()));
        document.setKnowledgeType(normalizeKnowledgeType(document.getKnowledgeType()));
        document.setAudience(normalizeAudience(document.getAudience()));
        document.setSource(trimToNull(document.getSource()));
        document.setRemark(trimToNull(document.getRemark()));
    }

    private void validateChunk(KnowledgeChunk chunk, Long excludeId)
    {
        if (chunk == null || chunk.getDocumentId() == null)
        {
            throw new ServiceException("请选择所属文档");
        }
        requireDocument(chunk.getDocumentId());
        if (StringUtils.isEmpty(chunk.getContent()))
        {
            throw new ServiceException("知识片段正文不能为空");
        }
        if (chunk.getChunkNo() != null && chunk.getChunkNo() < 1)
        {
            throw new ServiceException("片段序号必须大于 0");
        }
        if (chunk.getChunkNo() != null)
        {
            QueryWrapper<KnowledgeChunk> duplicate = new QueryWrapper<>();
            duplicate.eq("document_id", chunk.getDocumentId()).eq("chunk_no", chunk.getChunkNo());
            if (excludeId != null)
            {
                duplicate.ne("id", excludeId);
            }
            if (chunkMapper.selectCount(duplicate) > 0)
            {
                throw new ServiceException("同一文档中的片段序号不能重复");
            }
        }
        chunk.setKeywords(trimToNull(chunk.getKeywords()));
        chunk.setContent(chunk.getContent().trim());
    }

    private KnowledgeDocument requireDocument(Long id)
    {
        KnowledgeDocument document = id == null ? null : documentMapper.selectById(id);
        if (document == null)
        {
            throw new ServiceException("知识文档不存在");
        }
        return document;
    }

    private void fillChunkCounts(List<KnowledgeDocument> documents)
    {
        if (documents.isEmpty())
        {
            return;
        }
        List<Long> documentIds = new ArrayList<>(documents.size());
        for (KnowledgeDocument document : documents)
        {
            document.setChunkCount(0L);
            documentIds.add(document.getId());
        }
        QueryWrapper<KnowledgeChunk> query = new QueryWrapper<>();
        query.in("document_id", documentIds);
        Map<Long, Long> counts = new HashMap<>();
        for (KnowledgeChunk chunk : chunkMapper.selectList(query))
        {
            counts.merge(chunk.getDocumentId(), 1L, Long::sum);
        }
        for (KnowledgeDocument document : documents)
        {
            document.setChunkCount(counts.getOrDefault(document.getId(), 0L));
        }
    }

    private int nextChunkNo(Long documentId)
    {
        QueryWrapper<KnowledgeChunk> query = new QueryWrapper<>();
        query.eq("document_id", documentId).orderByDesc("chunk_no").last("limit 1");
        List<KnowledgeChunk> chunks = chunkMapper.selectList(query);
        return chunks.isEmpty() || chunks.get(0).getChunkNo() == null ? 1 : chunks.get(0).getChunkNo() + 1;
    }

    private String normalizeStatus(String status)
    {
        return "1".equals(status) ? "1" : STATUS_ENABLED;
    }

    private String normalizeKnowledgeType(String knowledgeType)
    {
        return "PLATFORM".equalsIgnoreCase(knowledgeType) ? "PLATFORM" : "SAFETY";
    }

    private String normalizeAudience(String audience)
    {
        if (StringUtils.isEmpty(audience))
        {
            return "ALL";
        }
        String normalized = audience.trim().toUpperCase();
        return switch (normalized)
        {
            case "STUDENT", "TEACHER", "ADMIN" -> normalized;
            default -> "ALL";
        };
    }

    private String trimToNull(String value)
    {
        return StringUtils.isEmpty(value) ? null : value.trim();
    }
}
