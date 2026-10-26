package com.ruoyi.module.material.domain;

import java.math.BigDecimal;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Digits;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Data;

/**
 * 耗材库存手工调整请求。
 */
@Data
public class MaterialStockAdjustRequest
{
    @NotBlank(message = "调整方向不能为空")
    private String changeType;

    @NotNull(message = "调整数量不能为空")
    @DecimalMin(value = "0.01", message = "调整数量必须大于0")
    @Digits(integer = 10, fraction = 2, message = "调整数量最多保留两位小数")
    private BigDecimal quantity;

    @NotBlank(message = "调整原因不能为空")
    @Size(max = 500, message = "调整原因长度不能超过500个字符")
    private String reason;
}
