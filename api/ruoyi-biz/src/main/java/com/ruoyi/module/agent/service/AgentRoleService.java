package com.ruoyi.module.agent.service;

import com.ruoyi.common.core.domain.entity.SysRole;
import com.ruoyi.common.utils.SecurityUtils;
import org.springframework.stereotype.Service;

import java.util.List;

/**
 * 从当前登录用户解析 Agent 角色。
 */
@Service
public class AgentRoleService
{
    public String currentRole()
    {
        Long userId = SecurityUtils.getUserId();
        return resolveRole(userId, SecurityUtils.getLoginUser().getUser().getRoles());
    }

    String resolveRole(Long userId, List<SysRole> roles)
    {
        if (SecurityUtils.isAdmin(userId))
        {
            return "ADMIN";
        }
        if (roles == null)
        {
            return "USER";
        }
        List<String> roleKeys = roles.stream()
                .map(SysRole::getRoleKey)
                .filter(key -> key != null)
                .toList();
        if (roleKeys.contains("admin"))
        {
            return "ADMIN";
        }
        if (roleKeys.contains("teacher"))
        {
            return "TEACHER";
        }
        if (roleKeys.contains("student"))
        {
            return "STUDENT";
        }
        return "USER";
    }
}
