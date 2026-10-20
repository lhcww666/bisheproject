package com.ruoyi.module.safety.domain;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import com.ruoyi.common.core.domain.BaseEntity;

import lombok.Data;
import lombok.EqualsAndHashCode;

/**
 * 安全考试配置。
 *
 * @author itjialu
 */
@Data
@EqualsAndHashCode(callSuper = true)
@TableName("safety_exam_config")
public class SafetyExamConfig extends BaseEntity
{
    private static final long serialVersionUID = 1L;

    @TableId(type = IdType.AUTO)
    private Long id;

    private String configName;

    private Integer questionCount;

    private Integer passScore;

    private Integer durationMinutes;

    private Integer validDays;

    /** 0启用，1停用 */
    private String status;
}
