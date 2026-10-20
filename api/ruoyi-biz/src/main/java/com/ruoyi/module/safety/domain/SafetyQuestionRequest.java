package com.ruoyi.module.safety.domain;

import java.util.List;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Data;

/**
 * 题库新增/修改请求。选项按 A/B/C/D 顺序传字符串，答案传选项键。
 *
 * @author itjialu
 */
@Data
public class SafetyQuestionRequest
{
    private Long id;

    @Size(max = 30, message = "题目编号长度不能超过30个字符")
    private String questionCode;

    @NotBlank(message = "题型不能为空")
    private String questionType;

    @NotBlank(message = "题干不能为空")
    @Size(max = 1000, message = "题干长度不能超过1000个字符")
    private String content;

    @NotNull(message = "选项不能为空")
    private List<String> options;

    @NotNull(message = "答案不能为空")
    private List<String> answer;

    @NotNull(message = "分值不能为空")
    private Integer score;

    @NotNull(message = "安全等级不能为空")
    private Integer safetyLevel;

    private String status;

    @Size(max = 500, message = "备注长度不能超过500个字符")
    private String remark;
}
