package com.ruoyi.module.agent.service;

import org.springframework.ai.chat.client.ChatClient;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Component;

import com.ruoyi.common.exception.ServiceException;
import com.ruoyi.module.agent.role.AgentRole;
import com.ruoyi.module.agent.role.AgentRoleService;

/**
 * 根据当前登录角色选择对应 ChatClient。
 *
 * <p>角色和工具白名单都在服务端确定，前端不能通过参数切换。</p>
 *
 * @author itjialu
 */
@Component
public class AgentChatClientRouter
{
    private final AgentRoleService roleService;

    private final ChatClient studentChatClient;

    private final ChatClient teacherChatClient;

    private final ChatClient adminChatClient;

    public AgentChatClientRouter(AgentRoleService roleService,
            @Qualifier("studentAgentChatClient") ChatClient studentChatClient,
            @Qualifier("teacherAgentChatClient") ChatClient teacherChatClient,
            @Qualifier("adminAgentChatClient") ChatClient adminChatClient)
    {
        this.roleService = roleService;
        this.studentChatClient = studentChatClient;
        this.teacherChatClient = teacherChatClient;
        this.adminChatClient = adminChatClient;
    }

    public ChatClient current()
    {
        AgentRole role = roleService.currentRole();
        if (role == AgentRole.STUDENT)
        {
            return studentChatClient;
        }
        if (role == AgentRole.TEACHER)
        {
            return teacherChatClient;
        }
        if (role == AgentRole.ADMIN)
        {
            return adminChatClient;
        }
        throw new ServiceException("当前账号没有可用的智能助手角色");
    }
}
