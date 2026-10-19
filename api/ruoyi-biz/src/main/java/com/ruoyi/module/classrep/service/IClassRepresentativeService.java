package com.ruoyi.module.classrep.service;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.ruoyi.module.classrep.domain.ClassRepresentative;
import com.ruoyi.module.classrep.domain.ClassReservationInfo;

/**
 * 班级负责人Service接口
 *
 * @author itjialu
 */
public interface IClassRepresentativeService
{
    IPage<ClassRepresentative> pageClassRepresentative(Page<ClassRepresentative> page,
            QueryWrapper<ClassRepresentative> queryWrapper);

    ClassRepresentative selectClassRepresentativeById(Long id);

    int insertClassRepresentative(ClassRepresentative representative);

    int updateClassRepresentative(ClassRepresentative representative);

    int deleteClassRepresentativeByIds(Long[] ids);

    ClassRepresentative selectActiveByStudentId(Long studentId);

    boolean isRepresentative(Long studentId, Long deptId);

    long countClassStudents(Long deptId);

    ClassReservationInfo getMyClassReservationInfo(Long studentId);
}
