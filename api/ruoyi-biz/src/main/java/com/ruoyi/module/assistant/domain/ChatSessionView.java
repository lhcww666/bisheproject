package com.ruoyi.module.assistant.domain;

import java.util.Date;

import com.fasterxml.jackson.annotation.JsonFormat;

import lombok.Data;

/**
 * 会话列表视图。
 *
 * @author itjialu
 */
@Data
public class ChatSessionView
{
    private Long id;

    private String title;

    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private Date updateTime;
}
