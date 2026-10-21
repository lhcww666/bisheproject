package com.ruoyi.module.agent.knowledge.service.impl;

import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.Set;

import org.junit.jupiter.api.Test;

/**
 * 轻量 RAG 检索分词测试。
 *
 * @author itjialu
 */
class LexicalKnowledgeSearchServiceImplTest
{
    @Test
    void chineseQuestionIsSplitIntoBigrams()
    {
        Set<String> terms = LexicalKnowledgeSearchServiceImpl.tokenize("酒精着火怎么办");
        assertTrue(terms.contains("酒精"));
        assertTrue(terms.contains("着火"));
        assertTrue(terms.size() >= 3);
    }

    @Test
    void englishWordsArePreserved()
    {
        Set<String> terms = LexicalKnowledgeSearchServiceImpl.tokenize("离心机 safety check");
        assertTrue(terms.contains("safety"));
        assertTrue(terms.contains("check"));
    }
}
