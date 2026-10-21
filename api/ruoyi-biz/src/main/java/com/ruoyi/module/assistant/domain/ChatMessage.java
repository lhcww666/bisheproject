package com.ruoyi.module.assistant.domain;

import java.util.Date;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import com.fasterxml.jackson.annotation.JsonFormat;

import lombok.Data;

/**
 * 智能助手消息。
 *
 * @author itjialu
 */
@Data
@TableName("chat_message")
public class ChatMessage
{
    public static final String ROLE_USER = "USER";
    public static final String ROLE_ASSISTANT = "ASSISTANT";
    public static final String ROLE_SYSTEM = "SYSTEM";

    @TableId(type = IdType.AUTO)
    private Long id;

    private Long sessionId;

    private String role;

    private String content;

    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private Date createTime;
}
