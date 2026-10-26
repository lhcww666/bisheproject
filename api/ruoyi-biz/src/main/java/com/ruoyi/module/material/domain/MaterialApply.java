package com.ruoyi.module.material.domain;

import java.math.BigDecimal;
import java.util.Date;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import com.fasterxml.jackson.annotation.JsonFormat;
import com.ruoyi.common.annotation.Excel;
import com.ruoyi.common.core.domain.BaseEntity;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Digits;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Data;
import lombok.EqualsAndHashCode;

/**
 * 实验室耗材领用申请。
 *
 * @author itjialu
 */
@Data
@EqualsAndHashCode(callSuper = true)
@TableName("material_apply")
public class MaterialApply extends BaseEntity
{
    private static final long serialVersionUID = 1L;

    @TableId(type = IdType.AUTO)
    private Long id;

    @Excel(name = "领用编号")
    private String applyNo;

    @NotNull(message = "关联预约不能为空")
    private Long reservationId;

    private Long applicantId;

    @NotNull(message = "耗材不能为空")
    private Long materialId;

    @Excel(name = "申请数量")
    @NotNull(message = "申请数量不能为空")
    @DecimalMin(value = "0.01", message = "申请数量必须大于0")
    @Digits(integer = 10, fraction = 2, message = "申请数量最多保留两位小数")
    private BigDecimal applyQuantity;

    @Excel(name = "审核状态", dictType = "module_material_apply_status")
    private String status;

    private Long reviewerId;

    @Excel(name = "审核意见")
    @Size(max = 500, message = "审核意见长度不能超过500个字符")
    private String reviewComment;

    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private Date reviewTime;

    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private Date issueTime;

    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private Date returnTime;

    @TableField(exist = false)
    @Excel(name = "预约编号")
    private String reservationNo;

    @TableField(exist = false)
    @Excel(name = "申请人")
    private String applicantName;

    @TableField(exist = false)
    @Excel(name = "耗材编号")
    private String materialCode;

    @TableField(exist = false)
    @Excel(name = "耗材名称")
    private String materialName;

    @TableField(exist = false)
    @Excel(name = "规格型号")
    private String specification;

    @TableField(exist = false)
    @Excel(name = "计量单位")
    private String unit;

    @TableField(exist = false)
    private Long roomId;

    @TableField(exist = false)
    @Excel(name = "实验室编号")
    private String roomCode;

    @TableField(exist = false)
    @Excel(name = "实验室名称")
    private String roomName;

    @TableField(exist = false)
    @Excel(name = "当前库存")
    private BigDecimal currentStock;

    @TableField(exist = false)
    @Excel(name = "审核人")
    private String reviewerName;
}
