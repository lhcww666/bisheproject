package com.ruoyi.module.agent.knowledge.domain;

import java.util.Date;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import com.fasterxml.jackson.annotation.JsonFormat;

import lombok.Data;

/**
 * RAG 安全知识文档。
 *
 * @author itjialu
 */
@Data
@TableName("knowledge_document")
public class KnowledgeDocument
{
    @TableId(type = IdType.AUTO)
    private Long id;

    private String title;

    private String category;

    private String source;

    private String status;

    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private Date createTime;
}
