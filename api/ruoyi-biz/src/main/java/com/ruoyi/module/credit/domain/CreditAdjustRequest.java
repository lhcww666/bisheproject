package com.ruoyi.module.credit.domain;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Data;

/**
 * 管理员手工调整信用分请求。
 *
 * @author itjialu
 */
@Data
public class CreditAdjustRequest
{
    @NotNull(message = "用户ID不能为空")
    private Long userId;

    @NotNull(message = "变动分值不能为空")
    private Integer changeScore;

    @NotBlank(message = "调整原因不能为空")
    @Size(max = 200, message = "调整原因长度不能超过200个字符")
    private String reason;
}
