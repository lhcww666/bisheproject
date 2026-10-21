package com.ruoyi.module.agent.eval.domain;

import java.math.BigDecimal;
import java.util.Date;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import com.fasterxml.jackson.annotation.JsonFormat;

import lombok.Data;

/**
 * Agent 评测批次。
 *
 * @author itjialu
 */
@Data
@TableName("agent_eval_run")
public class AgentEvalRun
{
    @TableId(type = IdType.AUTO)
    private Long id;

    private String runNo;

    private String mode;

    private Integer total;

    private Integer passed;

    private BigDecimal intentAccuracy;

    private BigDecimal toolAccuracy;

    private BigDecimal ragHitRate;

    private BigDecimal passRate;

    private String createBy;

    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private Date createTime;
}
