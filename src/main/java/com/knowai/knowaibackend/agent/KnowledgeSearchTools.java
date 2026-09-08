package com.knowai.knowaibackend.agent;

import dev.langchain4j.agent.tool.P;
import dev.langchain4j.agent.tool.Tool;
import dev.langchain4j.data.embedding.Embedding;
import dev.langchain4j.data.segment.TextSegment;
import dev.langchain4j.model.embedding.EmbeddingModel;
import dev.langchain4j.model.output.Response;
import dev.langchain4j.model.scoring.ScoringModel;
import dev.langchain4j.store.embedding.EmbeddingMatch;
import dev.langchain4j.store.embedding.EmbeddingSearchRequest;
import dev.langchain4j.store.embedding.EmbeddingSearchResult;
import dev.langchain4j.store.embedding.EmbeddingStore;
import lombok.RequiredArgsConstructor;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

import static dev.langchain4j.store.embedding.filter.MetadataFilterBuilder.metadataKey;

/**
 * 两级检索工具（v2）：粗召回(embedding Top-20) → rerank 精排 → 只取 Top-2 喂 LLM
 *
 * 为什么这么设计（RAG 检索的 recall/precision 解耦）：
 *  - 粗召回大窗口(RECALL_K=20)：保证"答案别漏出候选池"（召回率）
 *  - embedding 排序只按"长得像不像"排，精度有限（双塔无法让 query 和 doc 交互）
 *  - rerank(ScoringModel) 把 query 和每个候选 chunk 拼接后整体打分，能判断"是否答了问题"，
 *    把最相关的顶到最前（精度），再把噪声挡在 LLM 上下文之外
 */
@SuppressWarnings("all")
@RequiredArgsConstructor
public class KnowledgeSearchTools {

    /** 粗召回条数：先把候选池放大，保证答案在池子里 */
    private static final int RECALL_K = 20;
    /** 精排后真正进 LLM 的条数（保持线上 Top-2 不变，token 成本不涨） */
    private static final int FINAL_K = 2;

    private final EmbeddingModel embeddingModel;
    private final EmbeddingStore<TextSegment> embeddingStore;
    private final ScoringModel scoringModel;
    private final Long knowledgeId;
    // 存本次检索最终喂给 LLM 的结果，供外部组装 references 用
    private final List<EmbeddingMatch<TextSegment>> matches = new ArrayList<>();

    @Tool("在知识库中检索与问题相关的文档片段，返回片段内容")
    public String searchKnowledge(@P("要检索的问题")String query){
        // 1. 向量化 query
        Response<Embedding> embed = embeddingModel.embed(query);
        Embedding content = embed.content();

        // 2.【粗召回】Qdrant Top-20，带 knowledgeId 过滤（保证答案在候选池里）
        EmbeddingSearchRequest.EmbeddingSearchRequestBuilder builder =
                EmbeddingSearchRequest.builder()
                        .queryEmbedding(content)
                        .maxResults(RECALL_K);
        if (knowledgeId != null && knowledgeId > 0) {
            builder.filter(metadataKey("knowledgeId").isEqualTo(knowledgeId));
        }
        EmbeddingSearchResult<TextSegment> searchResult = embeddingStore.search(builder.build());
        List<EmbeddingMatch<TextSegment>> recallMatches = searchResult.matches();

        // 候选池为空，直接告诉模型没资料
        if (recallMatches.isEmpty()) {
            return "资料中没有相关信息";
        }

        // 3.【精排】rerank 打分重排，取 Top-2
        List<EmbeddingMatch<TextSegment>> finalMatches = rerank(query, recallMatches);

        // 4. 保存最终结果，供外部组装 references（保持多轮累积语义）
        matches.addAll(finalMatches);

        // 5. 拼 context 返回（只拼真正进 LLM 的 Top-2，噪声被挡在外面）
        StringBuilder context = new StringBuilder();
        for (int i = 0; i < finalMatches.size(); i++) {
            context.append("[").append(i + 1).append("]")
                    .append(finalMatches.get(i).embedded().text())
                    .append("\n\n");
        }

        return context.toString();
    }

    /** 粗召回结果 → rerank 打分 → 按分降序取 Top-2 */
    private List<EmbeddingMatch<TextSegment>> rerank(String query,
                                                     List<EmbeddingMatch<TextSegment>> recallMatches) {
        // （1）把候选 chunk 提出来，一次性交给 ScoringModel 打分（分数与 segments 一一对应）
        List<TextSegment> segments = new ArrayList<>();
        for (EmbeddingMatch<TextSegment> m : recallMatches) {
            segments.add(m.embedded());
        }
        List<Double> scores;
        try {
            scores = scoringModel.scoreAll(segments, query).content();
        } catch (Exception e) {
            // rerank 是外部 API，超时/限流/额度不足都可能抛异常 —— 降级回 embedding 排序，绝不让问答挂掉
            return recallMatches.subList(0, Math.min(FINAL_K, recallMatches.size()));
        }

        // （2）防御：rerank 返回的分数数量对不上时（异常/降级），退回 embedding 原始顺序取前 FINAL_K
        if (scores.size() != recallMatches.size()) {
            return recallMatches.subList(0, Math.min(FINAL_K, recallMatches.size()));
        }

        // （3）按 rerank 分数降序排列候选的下标（分数并列时保持 embedding 原序，保证稳定）
        List<Integer> order = new ArrayList<>();
        for (int i = 0; i < recallMatches.size(); i++) {
            order.add(i);
        }
        order.sort(Comparator.comparingDouble((Integer i) -> scores.get(i)).reversed());

        // （4）取分数最高的前 FINAL_K 条
        List<EmbeddingMatch<TextSegment>> finalMatches = new ArrayList<>();
        int n = Math.min(FINAL_K, order.size());
        for (int i = 0; i < n; i++) {
            finalMatches.add(recallMatches.get(order.get(i)));
        }
        return finalMatches;
    }

    // 供 ask() 取出检索结果组装 references
    public List<EmbeddingMatch<TextSegment>> getMatches() {
        return matches;
    }
}
