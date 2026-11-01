package com.ruoyi.module.agent.domain;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import com.ruoyi.common.core.domain.BaseEntity;
import lombok.Data;

/**
 * Agent 工具调用审计。
 */
@Data
@TableName("agent_tool_call_log")
public class ToolCallLog extends BaseEntity
{
    private static final long serialVersionUID = 1L;

    @TableId(type = IdType.AUTO)
    private Long id;

    private Long sessionId;

    private Long messageId;

    private Long userId;

    private String toolName;

    private String toolCallId;

    private String arguments;

    private String result;

    private String success;

    private Long costMs;

    private String errorCode;
}
