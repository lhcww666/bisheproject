package com.ruoyi.module.agent.role;

import org.springframework.stereotype.Service;

import com.ruoyi.module.common.service.ModuleDataScopeService;

import lombok.RequiredArgsConstructor;

/**
 * 从当前 JWT 用户解析智能助手角色。
 *
 * <p>教师同时拥有学生角色时优先使用教师助手；管理员优先使用管理员助手。</p>
 *
 * @author itjialu
 */
@Service
@RequiredArgsConstructor
public class AgentRoleService
{
    private final ModuleDataScopeService moduleDataScopeService;

    public AgentRole currentRole()
    {
        if (moduleDataScopeService.isAdmin())
        {
            return AgentRole.ADMIN;
        }
        if (moduleDataScopeService.isTeacher())
        {
            return AgentRole.TEACHER;
        }
        if (moduleDataScopeService.isStudent())
        {
            return AgentRole.STUDENT;
        }
        return AgentRole.UNKNOWN;
    }
}
