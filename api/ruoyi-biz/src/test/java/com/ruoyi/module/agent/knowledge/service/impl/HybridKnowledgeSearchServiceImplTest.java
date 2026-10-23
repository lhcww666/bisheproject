package com.ruoyi.module.agent.knowledge.service.impl;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.util.List;
import java.util.Set;

import org.junit.jupiter.api.Test;

import com.ruoyi.module.agent.knowledge.domain.KnowledgeChunk;
import com.ruoyi.module.agent.knowledge.domain.KnowledgeDocument;
import com.ruoyi.module.agent.knowledge.domain.KnowledgeSearchResult;
import com.ruoyi.module.agent.knowledge.mapper.KnowledgeChunkMapper;
import com.ruoyi.module.agent.knowledge.mapper.KnowledgeDocumentMapper;

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

    @Test
    void titleCoverageWinsOverLongAdjacentContent()
    {
        KnowledgeDocumentMapper documentMapper = mock(KnowledgeDocumentMapper.class);
        KnowledgeChunkMapper chunkMapper = mock(KnowledgeChunkMapper.class);
        KnowledgeDocument centrifuge = document(1006L, "离心机安全操作");
        KnowledgeDocument autoclave = document(1009L, "高压灭菌锅安全操作");
        KnowledgeDocument bioWaste = document(1050L, "生物废物与实验动物管理");
        when(documentMapper.selectList(any())).thenReturn(List.of(centrifuge, autoclave, bioWaste));
        when(chunkMapper.selectList(any())).thenReturn(List.of(
                chunk(1L, centrifuge.getId(), "离心机 离心管 配平 对称 盖子",
                        "使用离心机前应检查离心管是否完好，按对称位置配平，盖好并锁紧转子盖。"),
                chunk(2L, autoclave.getId(), "高压灭菌锅 灭菌 水位 密封 冷空气 压力",
                        "使用高压灭菌锅前应检查水位、密封圈和排气阀，确认锅内有足够水并排尽冷空气后再升压。"),
                chunk(3L, bioWaste.getId(), "生物废物 灭活 高压灭菌 双层包装 标识 转运",
                        "生物废物应使用防渗漏容器和双层包装，按规定进行高压灭菌。离心管应使用安全杯，"
                                + "实验区域还应落实生物安全、消毒、动物实验和污染区管理要求。")));

        HybridKnowledgeSearchServiceImpl service =
                new HybridKnowledgeSearchServiceImpl(documentMapper, chunkMapper);

        List<KnowledgeSearchResult> centrifugeResults = service.search("离心机操作安全注意事项", 1);
        List<KnowledgeSearchResult> autoclaveResults = service.search("高压灭菌锅怎么用？", 1);

        assertEquals("离心机安全操作", centrifugeResults.get(0).getDocumentTitle());
        assertEquals("高压灭菌锅安全操作", autoclaveResults.get(0).getDocumentTitle());
    }

    private KnowledgeDocument document(Long id, String title)
    {
        KnowledgeDocument document = new KnowledgeDocument();
        document.setId(id);
        document.setTitle(title);
        document.setSource("测试");
        document.setStatus("0");
        return document;
    }

    private KnowledgeChunk chunk(Long id, Long documentId, String keywords, String content)
    {
        KnowledgeChunk chunk = new KnowledgeChunk();
        chunk.setId(id);
        chunk.setDocumentId(documentId);
        chunk.setKeywords(keywords);
        chunk.setContent(content);
        chunk.setStatus("0");
        return chunk;
    }
}
