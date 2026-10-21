package com.ruoyi.module.agent.knowledge.domain;

import lombok.Data;

/**
 * 知识库检索结果。
 *
 * @author itjialu
 */
@Data
public class KnowledgeSearchResult
{
    private Long documentId;

    private String documentTitle;

    private Long chunkId;

    private String content;

    private String source;

    /** 检索相关度，当前为关键词/二元词 TF-IDF 分数。 */
    private Double score;
}
