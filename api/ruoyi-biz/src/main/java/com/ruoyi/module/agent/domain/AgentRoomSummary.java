package com.ruoyi.module.agent.domain;

import lombok.AllArgsConstructor;
import lombok.Data;

/**
 * 提供给 Agent 的实验室摘要。
 */
@Data
@AllArgsConstructor
public class AgentRoomSummary
{
    private Long id;

    private String roomCode;

    private String roomName;

    private String roomType;

    private String location;

    private Long capacity;

    private Long safetyLevel;
}
