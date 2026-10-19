package com.ruoyi.module.classrep.service.impl;

import java.util.Arrays;
import java.util.Date;
import java.util.List;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.ruoyi.common.core.domain.entity.SysDept;
import com.ruoyi.common.core.domain.entity.SysUser;
import com.ruoyi.common.exception.ServiceException;
import com.ruoyi.common.utils.DateUtils;
import com.ruoyi.common.utils.SecurityUtils;
import com.ruoyi.common.utils.StringUtils;
import com.ruoyi.module.classrep.domain.ClassRepresentative;
import com.ruoyi.module.classrep.domain.ClassReservationInfo;
import com.ruoyi.module.classrep.mapper.ClassRepresentativeMapper;
import com.ruoyi.module.classrep.service.IClassRepresentativeService;
import com.ruoyi.system.service.ISysDeptService;
import com.ruoyi.system.service.ISysUserService;

import lombok.RequiredArgsConstructor;

/**
 * 班级负责人Service业务层处理
 *
 * @author itjialu
 */
@Service
@RequiredArgsConstructor
public class ClassRepresentativeServiceImpl extends ServiceImpl<ClassRepresentativeMapper, ClassRepresentative>
        implements IClassRepresentativeService
{
    private final ClassRepresentativeMapper classRepresentativeMapper;

    private final ISysUserService sysUserService;

    private final ISysDeptService sysDeptService;

    @Override
    public IPage<ClassRepresentative> pageClassRepresentative(Page<ClassRepresentative> page,
            QueryWrapper<ClassRepresentative> queryWrapper)
    {
        IPage<ClassRepresentative> pageData = classRepresentativeMapper.selectPage(page, queryWrapper);
        fillDisplayNames(pageData.getRecords());
        return pageData;
    }

    @Override
    public ClassRepresentative selectClassRepresentativeById(Long id)
    {
        ClassRepresentative representative = classRepresentativeMapper.selectById(id);
        if (representative != null)
        {
            fillDisplayNames(List.of(representative));
        }
        return representative;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public int insertClassRepresentative(ClassRepresentative representative)
    {
        validateRepresentative(representative, false);
        representative.setStatus(StringUtils.isEmpty(representative.getStatus()) ? "0" : representative.getStatus());
        representative.setCreateBy(SecurityUtils.getUsername());
        representative.setCreateTime(DateUtils.getNowDate());
        return classRepresentativeMapper.insert(representative);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public int updateClassRepresentative(ClassRepresentative representative)
    {
        if (representative.getId() == null)
        {
            throw new ServiceException("班级负责人ID不能为空");
        }
        validateRepresentative(representative, true);
        representative.setStatus(StringUtils.isEmpty(representative.getStatus()) ? "0" : representative.getStatus());
        representative.setUpdateBy(SecurityUtils.getUsername());
        representative.setUpdateTime(DateUtils.getNowDate());
        return classRepresentativeMapper.updateById(representative);
    }

    @Override
    public int deleteClassRepresentativeByIds(Long[] ids)
    {
        return classRepresentativeMapper.deleteByIds(Arrays.asList(ids));
    }

    @Override
    public ClassRepresentative selectActiveByStudentId(Long studentId)
    {
        if (studentId == null)
        {
            return null;
        }
        QueryWrapper<ClassRepresentative> queryWrapper = new QueryWrapper<>();
        queryWrapper.eq("student_id", studentId);
        queryWrapper.eq("status", "0");
        queryWrapper.and(wrapper -> wrapper.isNull("valid_from").or().le("valid_from", new Date()));
        queryWrapper.and(wrapper -> wrapper.isNull("valid_until").or().ge("valid_until", new Date()));
        queryWrapper.orderByDesc("id");
        queryWrapper.last("limit 1");
        return classRepresentativeMapper.selectOne(queryWrapper);
    }

    @Override
    public boolean isRepresentative(Long studentId, Long deptId)
    {
        if (studentId == null || deptId == null)
        {
            return false;
        }
        ClassRepresentative representative = selectActiveByStudentId(studentId);
        return representative != null && deptId.equals(representative.getDeptId());
    }

    @Override
    public long countClassStudents(Long deptId)
    {
        if (deptId == null)
        {
            return 0L;
        }
        return classRepresentativeMapper.countClassStudents(deptId);
    }

    @Override
    public ClassReservationInfo getMyClassReservationInfo(Long studentId)
    {
        ClassReservationInfo info = new ClassReservationInfo();
        SysUser student = sysUserService.selectUserById(studentId);
        if (student == null || student.getDeptId() == null)
        {
            return info;
        }
        ClassRepresentative representative = selectActiveByStudentId(studentId);
        if (representative == null || !student.getDeptId().equals(representative.getDeptId()))
        {
            return info;
        }
        info.setRepresentative(true);
        info.setDeptId(student.getDeptId());
        SysDept dept = sysDeptService.selectDeptById(student.getDeptId());
        info.setDeptName(dept == null ? null : dept.getDeptName());
        info.setStudentCount(countClassStudents(student.getDeptId()));
        return info;
    }

    private void validateRepresentative(ClassRepresentative representative, boolean update)
    {
        if (representative == null)
        {
            throw new ServiceException("班级负责人不能为空");
        }
        if (representative.getDeptId() == null)
        {
            throw new ServiceException("请选择班级");
        }
        if (sysDeptService.selectDeptById(representative.getDeptId()) == null)
        {
            throw new ServiceException("班级不存在");
        }
        if (representative.getStudentId() == null)
        {
            throw new ServiceException("请选择学生");
        }
        SysUser student = sysUserService.selectUserById(representative.getStudentId());
        if (student == null)
        {
            throw new ServiceException("学生不存在");
        }
        if (student.getRoles() == null || student.getRoles().stream()
                .noneMatch(role -> "student".equals(role.getRoleKey())))
        {
            throw new ServiceException("班级负责人只能选择学生账号");
        }
        if (!representative.getDeptId().equals(student.getDeptId()))
        {
            throw new ServiceException("所选学生不属于该班级");
        }
        String status = representative.getStatus();
        if (StringUtils.isNotEmpty(status) && !"0".equals(status) && !"1".equals(status))
        {
            throw new ServiceException("状态只能是正常或停用");
        }
        if (representative.getValidFrom() != null && representative.getValidUntil() != null
                && representative.getValidFrom().after(representative.getValidUntil()))
        {
            throw new ServiceException("生效时间不能晚于失效时间");
        }

        QueryWrapper<ClassRepresentative> deptQuery = new QueryWrapper<>();
        deptQuery.eq("dept_id", representative.getDeptId());
        if (update)
        {
            deptQuery.ne("id", representative.getId());
        }
        if (classRepresentativeMapper.selectCount(deptQuery) > 0)
        {
            throw new ServiceException("该班级已有负责人，请直接修改原负责人");
        }

        QueryWrapper<ClassRepresentative> studentQuery = new QueryWrapper<>();
        studentQuery.eq("student_id", representative.getStudentId());
        if (update)
        {
            studentQuery.ne("id", representative.getId());
        }
        if (classRepresentativeMapper.selectCount(studentQuery) > 0)
        {
            throw new ServiceException("该学生已经是其他班级的负责人");
        }
    }

    private void fillDisplayNames(List<ClassRepresentative> records)
    {
        if (records == null || records.isEmpty())
        {
            return;
        }
        for (ClassRepresentative record : records)
        {
            SysDept dept = sysDeptService.selectDeptById(record.getDeptId());
            record.setDeptName(dept == null ? null : dept.getDeptName());
            SysUser student = sysUserService.selectUserById(record.getStudentId());
            if (student != null)
            {
                record.setStudentName(StringUtils.isNotEmpty(student.getNickName())
                        ? student.getNickName() : student.getUserName());
                record.setStudentUserName(student.getUserName());
            }
        }
    }
}
