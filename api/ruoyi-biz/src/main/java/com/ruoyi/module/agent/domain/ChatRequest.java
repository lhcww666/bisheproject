package com.ruoyi.module.agent.domain;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Data;

/**
 * 聊天请求参数。
 *
 * @author itjialu
 */
@Data
public class ChatRequest
{
    /** 为空时自动创建会话；非空时由 Service 校验归属。 */
    private Long sessionId;

    /** 用户输入，限制长度避免把超大文本直接传给模型。 */
    @NotBlank(message = "消息不能为空")
    @Size(max = 2000, message = "单条消息不能超过2000个字符")
    private String message;
}
