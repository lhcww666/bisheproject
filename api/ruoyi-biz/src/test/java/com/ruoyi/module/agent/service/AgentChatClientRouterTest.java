package com.ruoyi.module.agent.service;

import static org.junit.jupiter.api.Assertions.assertSame;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import org.junit.jupiter.api.Test;
import org.springframework.ai.chat.client.ChatClient;

import com.ruoyi.module.agent.role.AgentRole;
import com.ruoyi.module.agent.role.AgentRoleService;

/**
 * 角色 ChatClient 路由测试。
 *
 * @author itjialu
 */
class AgentChatClientRouterTest
{
    @Test
    void selectsClientByTrustedRole()
    {
        AgentRoleService roleService = mock(AgentRoleService.class);
        ChatClient student = mock(ChatClient.class);
        ChatClient teacher = mock(ChatClient.class);
        ChatClient admin = mock(ChatClient.class);
        AgentChatClientRouter router = new AgentChatClientRouter(roleService, student, teacher, admin);

        when(roleService.currentRole()).thenReturn(AgentRole.STUDENT);
        assertSame(student, router.current());
        when(roleService.currentRole()).thenReturn(AgentRole.TEACHER);
        assertSame(teacher, router.current());
        when(roleService.currentRole()).thenReturn(AgentRole.ADMIN);
        assertSame(admin, router.current());
    }
}
