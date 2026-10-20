package com.ruoyi.module.violation.domain;

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
import jakarta.validation.constraints.Size;
import lombok.Data;
import lombok.EqualsAndHashCode;

/**
 * 实验室违规记录对象 violation_record。
 *
 * @author itjialu
 */
@Data
@EqualsAndHashCode(callSuper = true)
@TableName("violation_record")
public class ViolationRecord extends BaseEntity
{
    private static final long serialVersionUID = 1L;

    @TableId(type = IdType.AUTO)
    private Long id;

    @Excel(name = "学生ID")
    @NotNull(message = "学生ID不能为空")
    private Long userId;

    @Excel(name = "预约ID")
    private Long reservationId;

    @Excel(name = "违规类型", dictType = "module_violation_type")
    @NotBlank(message = "违规类型不能为空")
    private String violationType;

    @Excel(name = "违规说明")
    @Size(min = 0, max = 500, message = "违规说明长度不能超过500个字符")
    private String description;

    @Excel(name = "信用变动")
    @NotNull(message = "信用变动不能为空")
    private Integer creditChange;

    /** 0待处理，1已处理，2已撤销 */
    @Excel(name = "状态", dictType = "module_violation_status")
    private String status;

    private Long handlerId;

    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private Date handleTime;

    @TableField(exist = false)
    private String nickName;

    @TableField(exist = false)
    private String userName;

    @TableField(exist = false)
    private String deptName;

    @TableField(exist = false)
    private String reservationNo;
}
