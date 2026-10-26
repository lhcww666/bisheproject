package com.ruoyi.module.material.domain;

import java.math.BigDecimal;
import java.util.Date;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import com.fasterxml.jackson.annotation.JsonFormat;
import com.ruoyi.common.annotation.Excel;

import lombok.Data;

/**
 * 耗材库存变动流水。
 *
 * @author itjialu
 */
@Data
@TableName("material_stock_record")
public class MaterialStockRecord
{
    @TableId(type = IdType.AUTO)
    private Long id;

    private Long materialId;

    @Excel(name = "变动类型", dictType = "module_material_stock_type")
    private String changeType;

    @Excel(name = "变动数量")
    private BigDecimal changeQuantity;

    @Excel(name = "变动前库存")
    private BigDecimal beforeQuantity;

    @Excel(name = "变动后库存")
    private BigDecimal afterQuantity;

    private Long applyId;

    private Long reservationId;

    private Long operatorId;

    @Excel(name = "操作人")
    private String operatorName;

    @Excel(name = "变动原因")
    private String reason;

    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    @Excel(name = "变动时间", width = 30, dateFormat = "yyyy-MM-dd HH:mm:ss")
    private Date createTime;

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
    @Excel(name = "实验室编号")
    private String roomCode;

    @TableField(exist = false)
    @Excel(name = "实验室名称")
    private String roomName;
}
