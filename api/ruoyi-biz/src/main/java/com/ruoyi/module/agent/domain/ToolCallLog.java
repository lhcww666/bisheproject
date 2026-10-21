package com.ruoyi.module.assistant.domain;

import java.util.Date;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import com.fasterxml.jackson.annotation.JsonFormat;

import lombok.Data;

/**
 * 工具调用审计记录。
 *
 * @author itjialu
 */
@Data
@TableName("tool_call_log")
public class ToolCallLog
{
    @TableId(type = IdType.AUTO)
    private Long id;

    private Long sessionId;

    private Long userId;

    private String toolName;

    private String arguments;

    private String result;

    private String success;

    private Integer costMs;

    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private Date createTime;
}
