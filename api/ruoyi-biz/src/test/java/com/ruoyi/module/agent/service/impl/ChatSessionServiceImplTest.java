package com.ruoyi.module.agent.service.impl;

import com.ruoyi.common.exception.ServiceException;
import com.ruoyi.common.core.domain.entity.SysUser;
import com.ruoyi.common.core.domain.model.LoginUser;
import com.ruoyi.module.agent.domain.AgentSessionCreateRequest;
import com.ruoyi.module.agent.domain.ChatSession;
import com.ruoyi.module.agent.mapper.ChatMessageMapper;
import com.ruoyi.module.agent.mapper.ChatSessionMapper;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;

import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ChatSessionServiceImplTest
{
    @Mock
    private ChatSessionMapper chatSessionMapper;

    @Mock
    private ChatMessageMapper chatMessageMapper;

    private ChatSessionServiceImpl service;

    @BeforeEach
    void setUp()
    {
        SysUser user = new SysUser(7L);
        user.setUserName("student");
        LoginUser loginUser = new LoginUser(7L, 1L, user, Set.of());
        SecurityContextHolder.getContext().setAuthentication(
                new UsernamePasswordAuthenticationToken(loginUser, null, loginUser.getAuthorities()));
        service = new ChatSessionServiceImpl(chatSessionMapper, chatMessageMapper);
    }

    @AfterEach
    void tearDown()
    {
        SecurityContextHolder.clearContext();
    }

    @Test
    void createSessionUsesDefaultEngineAndOwner()
    {
        doAnswer(invocation -> {
            ChatSession session = invocation.getArgument(0);
            session.setId(11L);
            return 1;
        }).when(chatSessionMapper).insert(any(ChatSession.class));

        AgentSessionCreateRequest request = new AgentSessionCreateRequest();
        ChatSession created = service.createSession(7L, request, "python");

        assertEquals(11L, created.getId());
        assertEquals(7L, created.getUserId());
        assertEquals("python", created.getEngine());
        assertEquals("新会话", created.getTitle());
        assertEquals("ACTIVE", created.getStatus());
    }

    @Test
    void createSessionUsesFallbackTitleForBlankInput()
    {
        doAnswer(invocation -> {
            ChatSession session = invocation.getArgument(0);
            session.setId(12L);
            return 1;
        }).when(chatSessionMapper).insert(any(ChatSession.class));

        AgentSessionCreateRequest request = new AgentSessionCreateRequest();
        request.setTitle("   ");
        ChatSession created = service.createSession(7L, request, "python");

        assertEquals("新会话", created.getTitle());
    }

    @Test
    void requireOwnedSessionRejectsAnotherUser()
    {
        ChatSession session = new ChatSession();
        session.setId(3L);
        session.setUserId(9L);
        when(chatSessionMapper.selectById(3L)).thenReturn(session);

        ServiceException exception = assertThrows(
                ServiceException.class,
                () -> service.requireOwnedSession(3L, 8L));

        assertEquals("会话不存在或无权访问", exception.getMessage());
    }

    @Test
    void deleteOwnedSessionRemovesOnlyAfterOwnershipCheck()
    {
        ChatSession session = new ChatSession();
        session.setId(3L);
        session.setUserId(8L);
        when(chatSessionMapper.selectById(3L)).thenReturn(session);

        service.deleteOwnedSession(3L, 8L);

        verify(chatSessionMapper).deleteById(3L);
    }
}
