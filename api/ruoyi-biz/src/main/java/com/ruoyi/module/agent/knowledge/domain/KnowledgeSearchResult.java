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

    /** 混合检索综合分数。 */
    private Double score;

    /** 词汇检索分数，归一化到 0-1。 */
    private Double lexicalScore;

    /** 本地向量余弦分数，归一化到 0-1。 */
    private Double vectorScore;
}
