package com.ruoyi.module.agent.domain;

import java.util.List;

import com.ruoyi.module.agent.draft.domain.ReservationDraftView;

import lombok.Data;

/**
 * 聊天回复结果。
 *
 * @author itjialu
 */
@Data
public class ChatReply
{
    /** 会话 ID，前端据此刷新会话列表和后续消息。 */
    private Long sessionId;

    /** 助手回复正文。 */
    private String content;

    /** 本次回复期间的工具调用摘要。 */
    private List<ToolCallView> toolCalls;

    /** 本次回复生成的预约草稿；没有草稿时为空。 */
    private ReservationDraftView draft;
}
