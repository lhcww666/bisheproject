package com.ruoyi.module.classrep.mapper;

import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.ruoyi.module.classrep.domain.ClassRepresentative;

/**
 * 班级负责人Mapper接口
 *
 * @author itjialu
 */
public interface ClassRepresentativeMapper extends BaseMapper<ClassRepresentative>
{
    @Select("SELECT COUNT(DISTINCT u.user_id) FROM sys_user u "
            + "JOIN sys_user_role ur ON ur.user_id = u.user_id "
            + "JOIN sys_role r ON r.role_id = ur.role_id "
            + "WHERE u.dept_id = #{deptId} AND u.del_flag = '0' AND u.status = '0' "
            + "AND r.role_key = 'student' AND r.status = '0' AND r.del_flag = '0'")
    long countClassStudents(@Param("deptId") Long deptId);
}
