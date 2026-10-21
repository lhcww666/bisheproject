package com.ruoyi.module.agent.domain;

import java.util.Date;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import com.fasterxml.jackson.annotation.JsonFormat;
import com.baomidou.mybatisplus.annotation.TableField;

import lombok.Data;

/**
 * Agent 工具调用审计记录。
 *
 * <p>用于回答“模型调用了什么工具、带了什么参数、返回了什么”。
 * 这对安全类毕业设计比单纯保存聊天记录更有说服力。</p>
 *
 * @author itjialu
 */
@Data
@TableName("tool_call_log")
public class ToolCallLog
{
    /** 审计主键。 */
    @TableId(type = IdType.AUTO)
    private Long id;

    /** 所属会话，可为空（便于未来独立调用工具时审计）。 */
    private Long sessionId;

    /** 调用用户。 */
    private Long userId;

    /** 工具名称。 */
    private String toolName;

    /** 工具参数 JSON/文本摘要。 */
    private String arguments;

    /** 工具返回内容摘要。 */
    private String result;

    /** 1成功，0失败。 */
    private String success;

    /** 执行耗时，单位毫秒。 */
    private Integer costMs;

    /** 调用时间。 */
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private Date createTime;

    /** 查询展示用的用户名，不映射数据库字段。 */
    @TableField(exist = false)
    private String userName;
}
