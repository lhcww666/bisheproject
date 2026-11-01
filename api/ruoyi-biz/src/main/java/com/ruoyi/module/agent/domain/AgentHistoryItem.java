package com.ruoyi.module.agent.domain;

import lombok.AllArgsConstructor;
import lombok.Data;

/**
 * 发送给 Python Agent 的历史消息。
 */
@Data
@AllArgsConstructor
public class AgentHistoryItem
{
    private String role;

    private String content;
}
