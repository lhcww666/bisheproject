package com.ruoyi.module.common.service;

import java.util.Collections;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

import org.springframework.stereotype.Service;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.ruoyi.common.core.domain.entity.SysRole;
import com.ruoyi.common.core.domain.entity.SysUser;
import com.ruoyi.common.core.domain.model.LoginUser;
import com.ruoyi.common.exception.ServiceException;
import com.ruoyi.common.utils.SecurityUtils;
import com.ruoyi.system.service.ISysRoleService;
import com.ruoyi.system.service.ISysUserService;

import lombok.RequiredArgsConstructor;

/**
 * 实验室业务数据范围。
 *
 * <p>角色口径：管理员看全部；教师看本人部门（班级）中的学生；学生只看本人。
 * 项目以 sys_user.dept_id 作为学生班级归属，教师需要分配到对应班级部门。</p>
 *
 * @author itjialu
 */
@Service
@RequiredArgsConstructor
public class ModuleDataScopeService
{
    private static final String ROLE_ADMIN = "admin";

    private static final String ROLE_STUDENT = "student";

    private static final String ROLE_TEACHER = "teacher";

    private static final String STATUS_NORMAL = "0";

    private final ISysUserService sysUserService;

    private final ISysRoleService sysRoleService;

    /**
     * 是否为管理员。管理员不受业务数据范围限制。
     */
    public boolean isAdmin()
    {
        if (SecurityUtils.isAdmin(SecurityUtils.getUserId()))
        {
            return true;
        }
        List<SysRole> roles = currentRoles();
        if (roles.stream().anyMatch(role -> ROLE_ADMIN.equals(role.getRoleKey())))
        {
            return true;
        }
        boolean hasStudentOrTeacher = roles.stream()
                .anyMatch(role -> ROLE_STUDENT.equals(role.getRoleKey()) || ROLE_TEACHER.equals(role.getRoleKey()));
        return !hasStudentOrTeacher && roles.stream().anyMatch(role -> "1".equals(role.getDataScope()));
    }

    /**
     * 是否为教师视角。同时拥有教师和学生角色时按教师视角处理。
     */
    public boolean isTeacher()
    {
        return !isAdmin() && currentRoleKeys().contains(ROLE_TEACHER);
    }

    /**
     * 是否为学生视角。
     */
    public boolean isStudent()
    {
        return !isAdmin() && !isTeacher() && currentRoleKeys().contains(ROLE_STUDENT);
    }

    /**
     * 给列表查询追加数据范围条件。
     *
     * @param wrapper     查询条件
     * @param ownerColumn 记录所属学生的字段，如 user_id、student_id、applicant_id
     */
    public <T> void applyStudentRecordScope(QueryWrapper<T> wrapper, String ownerColumn)
    {
        if (isAdmin())
        {
            return;
        }
        if (isStudent())
        {
            wrapper.eq(ownerColumn, SecurityUtils.getUserId());
            return;
        }
        if (isTeacher())
        {
            String studentIdsSql = teacherStudentIdsSql();
            if (studentIdsSql == null)
            {
                wrapper.apply("1 = 0");
            }
            else
            {
                wrapper.inSql(ownerColumn, studentIdsSql);
            }
            return;
        }
        // 没有业务角色的账号不应通过菜单之外的方式拿到业务数据
        wrapper.apply("1 = 0");
    }

    /**
     * 判断当前登录用户能否查看某位学生的记录。
     *
     * @param ownerId 学生用户主键
     * @return 是否有权查看
     */
    public boolean canAccessStudentRecord(Long ownerId)
    {
        if (isAdmin())
        {
            return true;
        }
        if (ownerId == null)
        {
            return false;
        }
        if (isStudent())
        {
            return ownerId.equals(SecurityUtils.getUserId());
        }
        if (!isTeacher())
        {
            return false;
        }
        Long teacherDeptId = SecurityUtils.getDeptId();
        if (teacherDeptId == null)
        {
            return false;
        }
        SysUser student = sysUserService.selectUserById(ownerId);
        if (student == null || !STATUS_NORMAL.equals(student.getStatus()) || !STATUS_NORMAL.equals(student.getDelFlag()))
        {
            return false;
        }
        if (!isStudentUser(student) || student.getDeptId() == null)
        {
            return false;
        }
        return teacherDeptId.equals(student.getDeptId());
    }

    /**
     * 校验当前登录用户是否有权查看某位学生的记录，无权时抛出业务异常。
     */
    public void checkStudentRecordAccess(Long ownerId)
    {
        if (!canAccessStudentRecord(ownerId))
        {
            throw new ServiceException("没有权限查看其他学生的记录");
        }
    }

    /**
     * 查询当前账号有权查看的学生。
     *
     * <p>管理员返回全部正常学生，教师返回本人部门中的正常学生，学生只返回本人。
     * 没有实验室业务角色时返回空集合。</p>
     *
     * @return 当前数据范围内的正常学生
     */
    public List<SysUser> selectScopedStudents()
    {
        if (isStudent())
        {
            SysUser current = sysUserService.selectUserById(SecurityUtils.getUserId());
            return current == null ? Collections.emptyList() : List.of(current);
        }
        if (!isAdmin() && !isTeacher())
        {
            return Collections.emptyList();
        }

        SysRole roleQuery = new SysRole();
        roleQuery.setRoleKey(ROLE_STUDENT);
        SysRole studentRole = sysRoleService.selectRoleList(roleQuery).stream()
                .filter(role -> ROLE_STUDENT.equals(role.getRoleKey()))
                .findFirst()
                .orElse(null);
        if (studentRole == null)
        {
            return Collections.emptyList();
        }

        SysUser userQuery = new SysUser();
        userQuery.setRoleId(studentRole.getRoleId());
        return sysUserService.selectAllocatedList(userQuery).stream()
                .filter(user -> STATUS_NORMAL.equals(user.getStatus()))
                .filter(user -> STATUS_NORMAL.equals(user.getDelFlag()))
                .collect(Collectors.toList());
    }

    private boolean isStudentUser(SysUser user)
    {
        List<SysRole> roles = user.getRoles();
        return roles != null && roles.stream().anyMatch(role -> ROLE_STUDENT.equals(role.getRoleKey()));
    }

    private List<SysRole> currentRoles()
    {
        LoginUser loginUser = SecurityUtils.getLoginUser();
        if (loginUser.getUser() == null || loginUser.getUser().getRoles() == null)
        {
            return Collections.emptyList();
        }
        return loginUser.getUser().getRoles();
    }

    private Set<String> currentRoleKeys()
    {
        return currentRoles().stream()
                .map(SysRole::getRoleKey)
                .filter(roleKey -> roleKey != null && !roleKey.isEmpty())
                .collect(Collectors.toSet());
    }

    /**
     * 教师负责范围内的学生 ID 子查询。部门 ID 来自登录上下文，不拼接用户输入。
     */
    private String teacherStudentIdsSql()
    {
        Long deptId = SecurityUtils.getDeptId();
        if (deptId == null)
        {
            return null;
        }
        return "SELECT u.user_id FROM sys_user u "
                + "JOIN sys_user_role ur ON ur.user_id = u.user_id "
                + "JOIN sys_role r ON r.role_id = ur.role_id "
                + "WHERE u.del_flag = '0' AND u.status = '0' "
                + "AND r.role_key = 'student' AND r.status = '0' AND r.del_flag = '0' "
                + "AND u.dept_id = " + deptId;
    }
}
