package com.ruoyi.module.agent.domain;

import java.util.Date;
import java.util.List;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import com.fasterxml.jackson.annotation.JsonFormat;

import lombok.Data;

/**
 * 智能助手消息实体。
 *
 * <p>只保存 USER 和 ASSISTANT 两类对话内容；工具调用明细单独存审计表，
 * 避免一条消息里混入大量工具参数。</p>
 *
 * @author itjialu
 */
@Data
@TableName("chat_message")
public class ChatMessage
{
    /** 用户消息角色。 */
    public static final String ROLE_USER = "USER";

    /** 助手消息角色。 */
    public static final String ROLE_ASSISTANT = "ASSISTANT";

    /** 预留的系统消息角色。 */
    public static final String ROLE_SYSTEM = "SYSTEM";

    /** 消息主键。 */
    @TableId(type = IdType.AUTO)
    private Long id;

    /** 所属会话。 */
    private Long sessionId;

    /** 消息角色：USER/ASSISTANT/SYSTEM。 */
    private String role;

    /** 消息正文。 */
    private String content;

    /** 消息时间。 */
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private Date createTime;

    /** 该条助手消息关联的工具调用，查询展示用。 */
    @TableField(exist = false)
    private List<ToolCallView> toolCalls;
}
