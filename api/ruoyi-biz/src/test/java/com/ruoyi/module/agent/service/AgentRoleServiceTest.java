package com.ruoyi.module.agent.service;

import com.ruoyi.common.core.domain.entity.SysRole;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;

class AgentRoleServiceTest
{
    private final AgentRoleService service = new AgentRoleService();

    @Test
    void resolvesSupportedLaboratoryRoles()
    {
        assertEquals("STUDENT", service.resolveRole(7L, List.of(role("student"))));
        assertEquals("TEACHER", service.resolveRole(8L, List.of(role("teacher"))));
        assertEquals("ADMIN", service.resolveRole(9L, List.of(role("admin"))));
        assertEquals("USER", service.resolveRole(10L, List.of()));
        assertEquals("ADMIN", service.resolveRole(1L, List.of()));
    }

    private SysRole role(String key)
    {
        SysRole role = new SysRole();
        role.setRoleKey(key);
        return role;
    }
}
