package com.ruoyi.module.agent.domain;

import java.util.Date;

import com.fasterxml.jackson.annotation.JsonFormat;

import lombok.Data;

/**
 * 会话列表视图，只暴露前端需要的最小字段。
 *
 * @author itjialu
 */
@Data
public class ChatSessionView
{
    /** 会话 ID。 */
    private Long id;

    /** 会话标题。 */
    private String title;

    /** 最后更新时间。 */
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private Date updateTime;
}
