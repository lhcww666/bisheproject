package com.ruoyi.module.agent.knowledge.service.impl;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.Collections;
import java.util.List;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;

import com.ruoyi.common.core.domain.entity.SysUser;
import com.ruoyi.common.core.domain.model.LoginUser;
import com.ruoyi.common.exception.ServiceException;
import com.ruoyi.module.agent.knowledge.domain.KnowledgeChunk;
import com.ruoyi.module.agent.knowledge.domain.KnowledgeDocument;
import com.ruoyi.module.agent.knowledge.mapper.KnowledgeChunkMapper;
import com.ruoyi.module.agent.knowledge.mapper.KnowledgeDocumentMapper;

/**
 * 安全知识库维护规则测试。
 *
 * @author itjialu
 */
class KnowledgeAdminServiceImplTest
{
    @AfterEach
    void clearSecurityContext()
    {
        SecurityContextHolder.clearContext();
    }

    @Test
    void duplicateDocumentTitleIsRejected()
    {
        KnowledgeDocumentMapper documentMapper = mock(KnowledgeDocumentMapper.class);
        KnowledgeAdminServiceImpl service = new KnowledgeAdminServiceImpl(documentMapper, mock(KnowledgeChunkMapper.class));
        when(documentMapper.selectCount(any())).thenReturn(1L);
        KnowledgeDocument document = document("离心机安全操作");

        ServiceException exception = assertThrows(ServiceException.class,
                () -> service.createDocument(document));

        assertTrue(exception.getMessage().contains("已存在"));
    }

    @Test
    void createDocumentNormalizesStatusAndAuditFields()
    {
        KnowledgeDocumentMapper documentMapper = mock(KnowledgeDocumentMapper.class);
        when(documentMapper.selectCount(any())).thenReturn(0L);
        when(documentMapper.insert(any(KnowledgeDocument.class))).thenReturn(1);
        authenticate();
        KnowledgeAdminServiceImpl service = new KnowledgeAdminServiceImpl(documentMapper, mock(KnowledgeChunkMapper.class));
        KnowledgeDocument document = document("  化学品泄漏处置  ");
        document.setStatus("invalid");

        service.createDocument(document);

        assertEquals("化学品泄漏处置", document.getTitle());
        assertEquals("0", document.getStatus());
        assertEquals("admin", document.getCreateBy());
        verify(documentMapper).insert(document);
    }

    @Test
    void newChunkGetsNextSequenceNumber()
    {
        KnowledgeDocumentMapper documentMapper = mock(KnowledgeDocumentMapper.class);
        KnowledgeChunkMapper chunkMapper = mock(KnowledgeChunkMapper.class);
        KnowledgeDocument document = document("离心机安全操作");
        document.setId(10L);
        when(documentMapper.selectById(10L)).thenReturn(document);
        KnowledgeChunk existing = new KnowledgeChunk();
        existing.setChunkNo(2);
        when(chunkMapper.selectList(any())).thenReturn(List.of(existing));
        when(chunkMapper.insert(any(KnowledgeChunk.class))).thenReturn(1);
        KnowledgeAdminServiceImpl service = new KnowledgeAdminServiceImpl(documentMapper, chunkMapper);
        KnowledgeChunk chunk = new KnowledgeChunk();
        chunk.setDocumentId(10L);
        chunk.setContent("运行前检查配平和盖子。");

        service.createChunk(chunk);

        assertEquals(3, chunk.getChunkNo());
        assertEquals("0", chunk.getStatus());
    }

    private KnowledgeDocument document(String title)
    {
        KnowledgeDocument document = new KnowledgeDocument();
        document.setTitle(title);
        return document;
    }

    private void authenticate()
    {
        SysUser user = new SysUser();
        user.setUserId(1L);
        user.setUserName("admin");
        LoginUser loginUser = new LoginUser(1L, 100L, user, Collections.emptySet());
        SecurityContextHolder.getContext().setAuthentication(
                new UsernamePasswordAuthenticationToken(loginUser, null, Collections.emptyList()));
    }
}
