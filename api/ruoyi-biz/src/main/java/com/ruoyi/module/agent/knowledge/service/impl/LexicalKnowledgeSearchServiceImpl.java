package com.ruoyi.module.agent.knowledge.service.impl;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.springframework.stereotype.Service;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.ruoyi.common.utils.StringUtils;
import com.ruoyi.module.agent.knowledge.domain.KnowledgeChunk;
import com.ruoyi.module.agent.knowledge.domain.KnowledgeDocument;
import com.ruoyi.module.agent.knowledge.domain.KnowledgeSearchResult;
import com.ruoyi.module.agent.knowledge.mapper.KnowledgeChunkMapper;
import com.ruoyi.module.agent.knowledge.mapper.KnowledgeDocumentMapper;
import com.ruoyi.module.agent.knowledge.service.IKnowledgeSearchService;

import lombok.RequiredArgsConstructor;

/**
 * 轻量 RAG 检索基线：中文二元词 + TF-IDF/余弦打分。
 *
 * <p>当前不依赖外部 embedding 服务，便于在没有模型 Key 的环境下测试检索。
 * 后续把 search 的实现替换为 Spring AI VectorStore 即可，Agent 工具接口不变。</p>
 *
 * @author itjialu
 */
@Service
@RequiredArgsConstructor
public class LexicalKnowledgeSearchServiceImpl implements IKnowledgeSearchService
{
    private static final Pattern ENGLISH_WORD = Pattern.compile("[a-zA-Z0-9_]+");

    private final KnowledgeDocumentMapper documentMapper;

    private final KnowledgeChunkMapper chunkMapper;

    @Override
    public List<KnowledgeSearchResult> search(String question, int topK)
    {
        if (StringUtils.isEmpty(question))
        {
            return List.of();
        }
        Set<String> queryTerms = tokenize(question);
        if (queryTerms.isEmpty())
        {
            return List.of();
        }

        QueryWrapper<KnowledgeDocument> documentQuery = new QueryWrapper<>();
        documentQuery.eq("status", "0");
        Map<Long, KnowledgeDocument> documents = new HashMap<>();
        for (KnowledgeDocument document : documentMapper.selectList(documentQuery))
        {
            documents.put(document.getId(), document);
        }

        QueryWrapper<KnowledgeChunk> chunkQuery = new QueryWrapper<>();
        chunkQuery.eq("status", "0");
        List<KnowledgeChunk> chunks = chunkMapper.selectList(chunkQuery);
        if (chunks.isEmpty())
        {
            return List.of();
        }

        // 统计每个查询词出现在多少个片段中，作为 IDF 权重。
        Map<String, Integer> documentFrequency = new HashMap<>();
        Map<Long, Map<String, Integer>> termFrequency = new HashMap<>();
        for (KnowledgeChunk chunk : chunks)
        {
            Map<String, Integer> frequencies = countTerms(chunkText(chunk), queryTerms);
            termFrequency.put(chunk.getId(), frequencies);
            for (String term : queryTerms)
            {
                if (frequencies.getOrDefault(term, 0) > 0)
                {
                    documentFrequency.merge(term, 1, Integer::sum);
                }
            }
        }

        int totalChunks = chunks.size();
        List<KnowledgeSearchResult> results = new ArrayList<>();
        for (KnowledgeChunk chunk : chunks)
        {
            KnowledgeDocument document = documents.get(chunk.getDocumentId());
            if (document == null)
            {
                continue;
            }
            Map<String, Integer> frequencies = termFrequency.getOrDefault(chunk.getId(), Map.of());
            double rawScore = 0D;
            for (String term : queryTerms)
            {
                int tf = frequencies.getOrDefault(term, 0);
                if (tf == 0)
                {
                    continue;
                }
                int df = documentFrequency.getOrDefault(term, 1);
                double idf = Math.log((totalChunks + 1D) / (df + 1D)) + 1D;
                rawScore += tf * idf;
            }
            if (rawScore <= 0)
            {
                continue;
            }
            // 标题和关键词命中给额外权重，避免正文过长稀释短问题的相关性。
            String titleAndKeywords = document.getTitle() + " " + (chunk.getKeywords() == null ? "" : chunk.getKeywords());
            for (String term : queryTerms)
            {
                if (titleAndKeywords.contains(term))
                {
                    rawScore += 2.5D;
                }
            }
            double normalized = rawScore / Math.sqrt(Math.max(20D, chunk.getContent().length()));
            KnowledgeSearchResult result = new KnowledgeSearchResult();
            result.setDocumentId(document.getId());
            result.setDocumentTitle(document.getTitle());
            result.setChunkId(chunk.getId());
            result.setContent(chunk.getContent());
            result.setSource(document.getSource());
            result.setScore(Math.round(normalized * 100D) / 100D);
            results.add(result);
        }

        int limit = Math.max(1, Math.min(5, topK));
        results.sort(Comparator.comparing(KnowledgeSearchResult::getScore).reversed());
        if (results.isEmpty())
        {
            return results;
        }
        // 过滤掉明显弱相关的片段，避免模型把“着火”这类泛词命中的无关资料当成答案依据。
        double threshold = Math.max(0.5D, results.get(0).getScore() * 0.35D);
        results.removeIf(item -> item.getScore() < threshold);
        return results.size() <= limit ? results : new ArrayList<>(results.subList(0, limit));
    }

    /**
     * 中文用二元词，英文/数字用单词；加入二元词可以处理“酒精着火”这类短查询。
     */
    static Set<String> tokenize(String text)
    {
        if (StringUtils.isEmpty(text))
        {
            return Set.of();
        }
        String normalized = text.toLowerCase()
                .replaceAll("[\\p{Punct}\\p{Z}\\s]+", "");
        Set<String> terms = new LinkedHashSet<>();
        Matcher matcher = ENGLISH_WORD.matcher(text.toLowerCase());
        while (matcher.find())
        {
            terms.add(matcher.group());
        }
        for (int i = 0; i < normalized.length() - 1; i++)
        {
            terms.add(normalized.substring(i, i + 2));
        }
        if (terms.isEmpty() && normalized.length() == 1)
        {
            terms.add(normalized);
        }
        return terms;
    }

    private String chunkText(KnowledgeChunk chunk)
    {
        return (chunk.getKeywords() == null ? "" : chunk.getKeywords() + " ") + chunk.getContent();
    }

    private Map<String, Integer> countTerms(String text, Set<String> terms)
    {
        String normalized = text == null ? "" : text.toLowerCase().replaceAll("[\\p{Punct}\\p{Z}\\s]+", "");
        Map<String, Integer> result = new HashMap<>();
        for (String term : terms)
        {
            int count = 0;
            int index = 0;
            while ((index = normalized.indexOf(term, index)) >= 0)
            {
                count++;
                index += term.length();
            }
            result.put(term, count);
        }
        return result;
    }
}
