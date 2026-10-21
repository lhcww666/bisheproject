package com.ruoyi.module.assistant.domain;

import lombok.Data;

/**
 * 展示给前端的工具调用摘要。
 *
 * @author itjialu
 */
@Data
public class ToolCallView
{
    private String toolName;

    private String arguments;

    private String result;

    private Boolean success;

    private Integer costMs;
}
