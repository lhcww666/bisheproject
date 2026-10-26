package com.ruoyi.module.material.domain;

import java.math.BigDecimal;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import com.ruoyi.common.annotation.Excel;
import com.ruoyi.common.core.domain.BaseEntity;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Digits;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Data;
import lombok.EqualsAndHashCode;

/**
 * 实验室耗材档案。
 *
 * @author itjialu
 */
@Data
@EqualsAndHashCode(callSuper = true)
@TableName("material")
public class Material extends BaseEntity
{
    private static final long serialVersionUID = 1L;

    @TableId(type = IdType.AUTO)
    private Long id;

    @NotNull(message = "所属实验室不能为空")
    private Long roomId;

    @Excel(name = "耗材编号")
    @NotBlank(message = "耗材编号不能为空")
    @Size(max = 30, message = "耗材编号长度不能超过30个字符")
    private String materialCode;

    @Excel(name = "耗材名称")
    @NotBlank(message = "耗材名称不能为空")
    @Size(max = 100, message = "耗材名称长度不能超过100个字符")
    private String materialName;

    @Excel(name = "规格型号")
    @Size(max = 100, message = "规格型号长度不能超过100个字符")
    private String specification;

    @Excel(name = "计量单位")
    @NotBlank(message = "计量单位不能为空")
    @Size(max = 20, message = "计量单位长度不能超过20个字符")
    private String unit;

    @Excel(name = "当前库存")
    @NotNull(message = "当前库存不能为空")
    @DecimalMin(value = "0.00", message = "当前库存不能小于0")
    @Digits(integer = 10, fraction = 2, message = "当前库存最多保留两位小数")
    private BigDecimal stockQuantity;

    @Excel(name = "最低库存")
    @NotNull(message = "最低库存不能为空")
    @DecimalMin(value = "0.00", message = "最低库存不能小于0")
    @Digits(integer = 10, fraction = 2, message = "最低库存最多保留两位小数")
    private BigDecimal minimumStock;

    @Excel(name = "状态", dictType = "module_material_status")
    @NotBlank(message = "耗材状态不能为空")
    private String status;

    @TableField(exist = false)
    @Excel(name = "实验室编号")
    private String roomCode;

    @TableField(exist = false)
    @Excel(name = "实验室名称")
    private String roomName;
}
