package com.ruoyi.module.roomaccess.domain;

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

/**
 * 实验室准入规则对象 room_access
 *
 * @author itjialu
 */
@Data
@TableName("room_access")
public class RoomAccess extends BaseEntity
{
    private static final long serialVersionUID = 1L;

    /** 主键 */
    @TableId(type = IdType.AUTO)
    private Long id;

    /** 实验室ID */
    @Excel(name = "实验室ID")
    @TableField("room_id")
    @NotNull(message = "实验室不能为空")
    private Long roomId;

    /** 授权对象：DEPT/CLASS/USER */
    @Excel(name = "授权对象", dictType = "module_room_access_subject")
    @TableField("subject_type")
    @NotBlank(message = "授权对象不能为空")
    @Size(max = 20, message = "授权对象长度不能超过20个字符")
    private String subjectType;

    /** 部门ID、班级ID或学生用户ID */
    @Excel(name = "授权对象ID")
    @TableField("subject_id")
    @NotNull(message = "授权对象ID不能为空")
    private Long subjectId;

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

    /** 实验室名称，仅用于列表展示 */
    @TableField(exist = false)
    private String roomName;

    /** 授权对象名称，仅用于列表展示 */
    @TableField(exist = false)
    private String subjectName;
}
