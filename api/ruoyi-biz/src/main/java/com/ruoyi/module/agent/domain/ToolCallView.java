package com.ruoyi.module.agent.domain;

import lombok.Data;

/**
 * 展示给前端的工具调用摘要。
 *
 * @author itjialu
 */
@Data
public class ToolCallView
{
    /** 工具名称。 */
    private String toolName;

    /** 工具参数摘要。 */
    private String arguments;

    /** 工具返回摘要。 */
    private String result;

    /** 是否成功。 */
    private Boolean success;

    /** 耗时毫秒。 */
    private Integer costMs;
}
