package com.ruoyi.module.agent.domain;

import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import lombok.Data;

/**
 * 新建 Agent 会话请求。
 */
@Data
public class AgentSessionCreateRequest
{
    @Size(max = 100, message = "会话标题长度不能超过100个字符")
    private String title;

    @Pattern(regexp = "python|dify", message = "Agent 引擎只能是 python 或 dify")
    private String engine;
}
