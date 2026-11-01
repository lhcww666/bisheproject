package com.ruoyi.module.agent.domain;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import lombok.Data;

/**
 * 浏览器同步对话请求。
 */
@Data
public class AgentChatRequest
{
    @NotNull(message = "会话ID不能为空")
    private Long sessionId;

    @NotBlank(message = "消息内容不能为空")
    @Size(max = 8000, message = "消息内容不能超过8000个字符")
    private String message;

    @Pattern(regexp = "python|dify", message = "Agent 引擎只能是 python 或 dify")
    private String engine;
}
