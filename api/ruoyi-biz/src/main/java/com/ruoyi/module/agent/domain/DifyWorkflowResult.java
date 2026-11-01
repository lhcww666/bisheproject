package com.ruoyi.module.agent.domain;

import lombok.AllArgsConstructor;
import lombok.Data;

import java.util.Map;

/**
 * Dify 工作流 blocking 调用结果。
 */
@Data
@AllArgsConstructor
public class DifyWorkflowResult
{
    private String workflowRunId;

    private String status;

    private Map<String, Object> outputs;

    private String error;

    private Long elapsedTime;

    private Long totalTokens;
}
