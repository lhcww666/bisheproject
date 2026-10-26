package com.ruoyi.module.material.domain;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Data;

/**
 * 耗材领用审核请求。
 */
@Data
public class MaterialReviewRequest
{
    @NotBlank(message = "审核结果不能为空")
    private String status;

    @Size(max = 500, message = "审核意见长度不能超过500个字符")
    private String reviewComment;
}
