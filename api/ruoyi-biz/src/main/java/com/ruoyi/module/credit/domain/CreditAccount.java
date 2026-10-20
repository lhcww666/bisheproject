package com.ruoyi.module.credit.domain;

import java.util.Date;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import com.fasterxml.jackson.annotation.JsonFormat;

import lombok.Data;

/**
 * 学生信用分账户。流水表只追加，账户表保存当前余额，避免每次倒序查流水。
 *
 * @author itjialu
 */
@Data
@TableName("credit_account")
public class CreditAccount
{
    @TableId(value = "user_id", type = IdType.INPUT)
    private Long userId;

    private Integer score;

    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private Date updateTime;
}
