package com.ruoyi.module.agent.knowledge.service.impl;

import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.Set;

import org.junit.jupiter.api.Test;

/**
 * 轻量 RAG 混合检索测试。
 *
 * @author itjialu
 */
class HybridKnowledgeSearchServiceImplTest
{
    @Test
    void chineseQuestionIsSplitIntoBigrams()
    {
        Set<String> terms = HybridKnowledgeSearchServiceImpl.tokenize("酒精着火怎么办");
        assertTrue(terms.contains("酒精"));
        assertTrue(terms.contains("着火"));
        assertTrue(terms.size() >= 3);
    }

    @Test
    void englishWordsArePreserved()
    {
        Set<String> terms = HybridKnowledgeSearchServiceImpl.tokenize("离心机 safety check");
        assertTrue(terms.contains("safety"));
        assertTrue(terms.contains("check"));
    }

    @Test
    void hashedVectorRewardsRelatedSafetyText()
    {
        double[] query = HybridKnowledgeSearchServiceImpl.encode("酒精着火怎么处理");
        double[] related = HybridKnowledgeSearchServiceImpl.encode("酒精起火时使用湿抹布覆盖灭火");
        double[] unrelated = HybridKnowledgeSearchServiceImpl.encode("图书借阅和奖学金申请流程");

        double relatedScore = HybridKnowledgeSearchServiceImpl.cosine(query, related);
        double unrelatedScore = HybridKnowledgeSearchServiceImpl.cosine(query, unrelated);
        assertTrue(relatedScore > unrelatedScore);
        assertTrue(relatedScore > 0.05D);
    }
}
