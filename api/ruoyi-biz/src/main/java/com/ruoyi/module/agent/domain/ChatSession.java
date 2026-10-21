package com.ruoyi.module.agent.domain;

import java.util.Date;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import com.fasterxml.jackson.annotation.JsonFormat;

import lombok.Data;

/**
 * 智能助手会话实体。
 *
 * <p>会话只保存归属用户、标题和更新时间；具体消息放在 chat_message，
 * 工具调用放在 tool_call_log，便于分别审计和清理。</p>
 *
 * @author itjialu
 */
@Data
@TableName("chat_session")
public class ChatSession
{
    /** 会话主键。 */
    @TableId(type = IdType.AUTO)
    private Long id;

    /** 会话所属的若依用户 ID，不允许跨用户读取。 */
    private Long userId;

    /** 左侧会话列表标题，新会话取第一条用户消息。 */
    private String title;

    /** 创建时间。 */
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private Date createTime;

    /** 最后更新时间，用于会话列表排序。 */
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private Date updateTime;
}
