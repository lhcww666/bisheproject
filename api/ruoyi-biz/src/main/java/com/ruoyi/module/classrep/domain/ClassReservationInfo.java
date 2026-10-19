package com.ruoyi.module.classrep.domain;

import lombok.Data;

/**
 * 当前学生可用的班级预约信息。
 */
@Data
public class ClassReservationInfo
{
    /** 当前学生是否为本班负责人 */
    private boolean representative;

    /** 班级ID */
    private Long deptId;

    /** 班级名称 */
    private String deptName;

    /** 班级学生数，班级预约时作为参与人数 */
    private Long studentCount;
}
