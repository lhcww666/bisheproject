package com.ruoyi.module.safety.domain;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import com.ruoyi.common.core.domain.BaseEntity;

import lombok.Data;
import lombok.EqualsAndHashCode;

/**
 * 安全考试答题明细。
 *
 * @author itjialu
 */
@Data
@EqualsAndHashCode(callSuper = true)
@TableName("safety_exam_answer")
public class SafetyExamAnswer extends BaseEntity
{
    private static final long serialVersionUID = 1L;

    @TableId(type = IdType.AUTO)
    private Long id;

    private Long recordId;

    private Long questionId;

    /** 学生答案 JSON 数组 */
    private String answerJson;

    /** 1正确，0错误，NULL未作答 */
    private String isCorrect;

    private Integer score;
}
