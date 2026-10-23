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
 * 轻量 RAG 混合检索：中文二元词 TF-IDF + 本地哈希向量余弦。
 *
 * <p>向量由稳定的特征哈希生成，不依赖外部 embedding 服务，适合离线演示和回归；
 * 后续可把 {@link #encode(String)} 替换为 Spring AI EmbeddingModel，检索接口保持不变。</p>
 *
 * @author itjialu
 */
@Service
@RequiredArgsConstructor
public class HybridKnowledgeSearchServiceImpl implements IKnowledgeSearchService
{
    private static final Pattern ENGLISH_WORD = Pattern.compile("[a-zA-Z0-9_]+");

    private static final int VECTOR_DIMENSIONS = 384;

    private static final Map<String, List<String>> SYNONYMS = Map.ofEntries(
            Map.entry("着火", List.of("起火", "火灾", "燃烧")),
            Map.entry("起火", List.of("着火", "火灾", "燃烧")),
            Map.entry("皮肤", List.of("沾到", "溅到", "灼伤")),
            Map.entry("沾到", List.of("溅到", "皮肤接触")),
            Map.entry("泄漏", List.of("洒出", "溢出", "打翻")),
            Map.entry("割伤", List.of("割破", "伤口", "出血")),
            Map.entry("手套", List.of("防护", "丁腈", "橡胶")),
            Map.entry("停电", List.of("断电", "停运")),
            Map.entry("刺伤", List.of("扎伤", "针头", "锐器")));

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
        double[] queryVector = encodeTerms(expandTerms(queryTerms));

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
            // 标题和关键词命中给额外权重，避免正文过长稀释短问题的相关性。
            String titleAndKeywords = document.getTitle() + " " + (chunk.getKeywords() == null ? "" : chunk.getKeywords());
            for (String term : queryTerms)
            {
                if (titleAndKeywords.contains(term))
                {
                    int df = documentFrequency.getOrDefault(term, 1);
                    double idf = Math.log((totalChunks + 1D) / (df + 1D)) + 1D;
                    rawScore += idf * 2D;
                }
            }
            double lexicalScore = rawScore <= 0D ? 0D
                    : 1D - Math.exp(-(rawScore / Math.sqrt(Math.max(20D, chunk.getContent().length()))));
            double vectorScore = Math.max(0D, cosine(queryVector, encode(chunkText(chunk))));
            if (lexicalScore <= 0D && vectorScore <= 0D)
            {
                continue;
            }
            // 标题和关键词是人工维护的高置信结构信号，单独加权可避免正文中的相邻主题抢占结果。
            double titleCoverage = coverage(queryTerms, tokenize(document.getTitle()));
            double keywordCoverage = coverage(queryTerms, tokenize(chunk.getKeywords()));
            double structuralScore = titleCoverage * 0.75D + keywordCoverage * 0.25D;
            double hybridScore = lexicalScore * 0.55D + vectorScore * 0.25D + structuralScore * 0.20D;
            KnowledgeSearchResult result = new KnowledgeSearchResult();
            result.setDocumentId(document.getId());
            result.setDocumentTitle(document.getTitle());
            result.setChunkId(chunk.getId());
            result.setContent(chunk.getContent());
            result.setSource(document.getSource());
            result.setScore(round(hybridScore));
            result.setLexicalScore(round(lexicalScore));
            result.setVectorScore(round(vectorScore));
            results.add(result);
        }

        int limit = Math.max(1, Math.min(5, topK));
        results.sort(Comparator.comparing(KnowledgeSearchResult::getScore).reversed());
        if (results.isEmpty())
        {
            return results;
        }
        // 过滤掉明显弱相关的片段，避免模型把“着火”这类泛词命中的无关资料当成答案依据。
        double threshold = Math.max(0.05D, results.get(0).getScore() * 0.35D);
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

    /**
     * 固定维度的本地特征哈希向量。相同文本得到相同向量，不依赖外部模型。
     */
    static double[] encode(String text)
    {
        return encodeTerms(expandTerms(tokenize(text)));
    }

    static double cosine(double[] left, double[] right)
    {
        if (left == null || right == null || left.length != right.length)
        {
            return 0D;
        }
        double dot = 0D;
        for (int i = 0; i < left.length; i++)
        {
            dot += left[i] * right[i];
        }
        return dot;
    }

    static Set<String> expandTerms(Set<String> terms)
    {
        Set<String> expanded = new LinkedHashSet<>(terms);
        for (String term : terms)
        {
            expanded.addAll(SYNONYMS.getOrDefault(term, List.of()));
        }
        return expanded;
    }

    static double[] encodeTerms(Set<String> terms)
    {
        double[] vector = new double[VECTOR_DIMENSIONS];
        for (String term : terms)
        {
            int hash = stableHash(term);
            int index = (hash & 0x7fffffff) % VECTOR_DIMENSIONS;
            double sign = (hash & 1) == 0 ? 1D : -1D;
            double weight = term.length() > 1 ? 1D : 0.6D;
            vector[index] += sign * weight;
        }
        double norm = 0D;
        for (double value : vector)
        {
            norm += value * value;
        }
        norm = Math.sqrt(norm);
        if (norm <= 0D)
        {
            return vector;
        }
        for (int i = 0; i < vector.length; i++)
        {
            vector[i] /= norm;
        }
        return vector;
    }

    private static int stableHash(String value)
    {
        int hash = 0x811c9dc5;
        for (int i = 0; i < value.length(); i++)
        {
            hash ^= value.charAt(i);
            hash *= 0x01000193;
        }
        return hash;
    }

    private static double round(double value)
    {
        return Math.round(value * 10000D) / 10000D;
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

    private double coverage(Set<String> queryTerms, Set<String> targetTerms)
    {
        if (queryTerms.isEmpty() || targetTerms.isEmpty())
        {
            return 0D;
        }
        long hits = targetTerms.stream().filter(queryTerms::contains).count();
        return hits * 1D / targetTerms.size();
    }
}
