package com.ruoyi.module.assistant.domain;

import java.util.List;

import lombok.Data;

/**
 * 聊天回复。
 *
 * @author itjialu
 */
@Data
public class ChatReply
{
    private Long sessionId;

    private String content;

    private List<ToolCallView> toolCalls;
}
