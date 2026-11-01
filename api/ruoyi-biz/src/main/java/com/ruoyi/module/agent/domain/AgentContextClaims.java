package com.ruoyi.module.agent.domain;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

/**
 * Python 与 Dify 内部调用使用的短期可信上下文。
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class AgentContextClaims
{
    private int version;

    private Long userId;

    private String role;

    private Long sessionId;

    private String traceId;

    private List<String> allowedTools;

    private long issuedAt;

    private long expiresAt;
}
