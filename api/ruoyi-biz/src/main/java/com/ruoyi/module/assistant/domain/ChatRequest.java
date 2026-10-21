package com.ruoyi.module.assistant.domain;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Data;

/**
 * 聊天请求。
 *
 * @author itjialu
 */
@Data
public class ChatRequest
{
    /** 为空时自动创建会话 */
    private Long sessionId;

    @NotBlank(message = "消息不能为空")
    @Size(max = 2000, message = "单条消息不能超过2000个字符")
    private String message;
}
