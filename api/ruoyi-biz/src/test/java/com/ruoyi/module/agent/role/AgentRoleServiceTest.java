package com.ruoyi.module.agent.role;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import org.junit.jupiter.api.Test;

import com.ruoyi.module.common.service.ModuleDataScopeService;

/**
 * 智能助手角色解析测试。
 *
 * @author itjialu
 */
class AgentRoleServiceTest
{
    @Test
    void resolvesAdminBeforeTeacherAndStudent()
    {
        ModuleDataScopeService scope = mock(ModuleDataScopeService.class);
        when(scope.isAdmin()).thenReturn(true);
        when(scope.isTeacher()).thenReturn(true);
        when(scope.isStudent()).thenReturn(true);

        assertEquals(AgentRole.ADMIN, new AgentRoleService(scope).currentRole());
    }

    @Test
    void resolvesTeacherAndStudentRoles()
    {
        ModuleDataScopeService teacherScope = mock(ModuleDataScopeService.class);
        when(teacherScope.isTeacher()).thenReturn(true);
        when(teacherScope.isStudent()).thenReturn(true);
        assertEquals(AgentRole.TEACHER, new AgentRoleService(teacherScope).currentRole());

        ModuleDataScopeService studentScope = mock(ModuleDataScopeService.class);
        when(studentScope.isStudent()).thenReturn(true);
        assertEquals(AgentRole.STUDENT, new AgentRoleService(studentScope).currentRole());
    }
}
