package com.ruoyi.module.agent.knowledge.domain;

import java.util.Date;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import com.fasterxml.jackson.annotation.JsonFormat;

import lombok.Data;

/**
 * RAG 知识片段。一个文档可以拆成多个片段，检索时以片段为单位。
 *
 * @author itjialu
 */
@Data
@TableName("knowledge_chunk")
public class KnowledgeChunk
{
    @TableId(type = IdType.AUTO)
    private Long id;

    private Long documentId;

    private Integer chunkNo;

    /** 人工维护的关键词，用于补充正文检索。 */
    private String keywords;

    private String content;

    private String status;

    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private Date createTime;
}
