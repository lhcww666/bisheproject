package com.ruoyi.module.classrep.domain;

import java.util.Date;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import com.fasterxml.jackson.annotation.JsonFormat;
import com.ruoyi.common.annotation.Excel;
import com.ruoyi.common.core.domain.BaseEntity;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

/**
 * 班级负责人对象 class_representative
 *
 * @author itjialu
 */
@Data
@TableName("class_representative")
public class ClassRepresentative extends BaseEntity
{
    private static final long serialVersionUID = 1L;

    @TableId(type = IdType.AUTO)
    private Long id;

    /** 班级ID（sys_dept.dept_id） */
    @Excel(name = "班级ID")
    @TableField("dept_id")
    @NotNull(message = "班级不能为空")
    private Long deptId;

    /** 学生ID（sys_user.user_id） */
    @Excel(name = "学生ID")
    @TableField("student_id")
    @NotNull(message = "学生不能为空")
    private Long studentId;

    /** 状态：0正常，1停用 */
    @Excel(name = "状态", readConverterExp = "0=正常,1=停用")
    @TableField("status")
    @NotBlank(message = "状态不能为空")
    private String status;

    /** 生效时间 */
    @Excel(name = "生效时间", width = 30, dateFormat = "yyyy-MM-dd HH:mm:ss")
    @TableField("valid_from")
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private Date validFrom;

    /** 失效时间 */
    @Excel(name = "失效时间", width = 30, dateFormat = "yyyy-MM-dd HH:mm:ss")
    @TableField("valid_until")
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private Date validUntil;

    /** 班级名称，仅用于列表展示 */
    @TableField(exist = false)
    private String deptName;

    /** 学生姓名，仅用于列表展示 */
    @TableField(exist = false)
    private String studentName;

    /** 学生账号，仅用于列表展示 */
    @TableField(exist = false)
    private String studentUserName;
}
