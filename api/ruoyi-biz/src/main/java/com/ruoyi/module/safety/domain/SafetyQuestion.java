package com.ruoyi.module.safety.domain;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import com.ruoyi.common.core.domain.BaseEntity;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Data;
import lombok.EqualsAndHashCode;

/**
 * 安全考试题库对象 safety_question。
 *
 * @author itjialu
 */
@Data
@EqualsAndHashCode(callSuper = true)
@TableName("safety_question")
public class SafetyQuestion extends BaseEntity
{
    private static final long serialVersionUID = 1L;

    @TableId(type = IdType.AUTO)
    private Long id;

    @NotBlank(message = "题目编号不能为空")
    @Size(max = 30, message = "题目编号长度不能超过30个字符")
    private String questionCode;

    /** SINGLE/MULTIPLE/JUDGE */
    @NotBlank(message = "题型不能为空")
    private String questionType;

    @NotBlank(message = "题干不能为空")
    @Size(max = 1000, message = "题干长度不能超过1000个字符")
    private String content;

    /** 选项 JSON 数组 */
    @NotBlank(message = "选项不能为空")
    private String optionsJson;

    /** 正确答案 JSON 数组，值为 A/B/C/D */
    @NotBlank(message = "答案不能为空")
    private String answerJson;

    @NotNull(message = "分值不能为空")
    private Integer score;

    /** 安全等级 1/2/3 */
    @NotNull(message = "安全等级不能为空")
    private Integer safetyLevel;

    /** 0启用，1停用 */
    private String status;
}
