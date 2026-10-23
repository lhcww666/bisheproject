package com.ruoyi.module.common.service;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.util.Collections;
import java.util.List;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.ruoyi.common.core.domain.entity.SysRole;
import com.ruoyi.common.core.domain.entity.SysUser;
import com.ruoyi.common.core.domain.model.LoginUser;
import com.ruoyi.module.reservation.domain.Reservation;
import com.ruoyi.system.service.ISysRoleService;
import com.ruoyi.system.service.ISysUserService;

/**
 * 教师、学生业务数据范围测试。
 *
 * @author itjialu
 */
class ModuleDataScopeServiceTest
{
    @AfterEach
    void clearSecurityContext()
    {
        SecurityContextHolder.clearContext();
    }

    @Test
    void teacherCanOnlyAccessStudentsInOwnDepartment()
    {
        ISysUserService userService = mock(ISysUserService.class);
        ModuleDataScopeService service = new ModuleDataScopeService(userService, mock(ISysRoleService.class));
        authenticate(200L, 10L, role("teacher"));
        when(userService.selectUserById(300L)).thenReturn(student(300L, 10L));
        when(userService.selectUserById(301L)).thenReturn(student(301L, 11L));

        assertTrue(service.canAccessStudentRecord(300L));
        assertFalse(service.canAccessStudentRecord(301L));
    }

    @Test
    void studentScopeIsRestrictedToCurrentUser()
    {
        ISysUserService userService = mock(ISysUserService.class);
        ModuleDataScopeService service = new ModuleDataScopeService(userService, mock(ISysRoleService.class));
        authenticate(300L, 10L, role("student"));

        assertTrue(service.canAccessStudentRecord(300L));
        assertFalse(service.canAccessStudentRecord(301L));
        QueryWrapper<Reservation> wrapper = new QueryWrapper<>();
        service.applyStudentRecordScope(wrapper, "applicant_id");
        assertTrue(wrapper.getSqlSegment().contains("applicant_id"));
        assertTrue(wrapper.getParamNameValuePairs().containsValue(300L));
    }

    private SysUser student(Long userId, Long deptId)
    {
        SysUser user = new SysUser();
        user.setUserId(userId);
        user.setDeptId(deptId);
        user.setStatus("0");
        user.setDelFlag("0");
        user.setRoles(List.of(role("student")));
        return user;
    }

    private SysRole role(String roleKey)
    {
        SysRole role = new SysRole();
        role.setRoleKey(roleKey);
        role.setStatus("0");
        role.setDelFlag("0");
        return role;
    }

    private void authenticate(Long userId, Long deptId, SysRole role)
    {
        SysUser user = new SysUser();
        user.setUserId(userId);
        user.setDeptId(deptId);
        user.setRoles(List.of(role));
        LoginUser loginUser = new LoginUser(userId, deptId, user, Collections.emptySet());
        SecurityContextHolder.getContext().setAuthentication(
                new UsernamePasswordAuthenticationToken(loginUser, null, Collections.emptyList()));
    }
}
