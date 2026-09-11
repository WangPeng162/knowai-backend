package com.knowai.knowaibackend.agent;

import com.knowai.knowaibackend.service.QueryRewriter;
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
import lombok.extern.slf4j.Slf4j;

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
@Slf4j
@SuppressWarnings("all")
@RequiredArgsConstructor
public class KnowledgeSearchTools {

    /** 粗召回条数：先把候选池放大，保证答案在池子里 */
    private static final int RECALL_K = 20;
    /** 精排后真正进 LLM 的条数（保持线上 Top-2 不变，token 成本不涨） */
    private static final int FINAL_K = 2;
    /**
     * rerank 相关性阈值：低于此分视为"与用户问题无关"，直接丢弃，不给 LLM。
     *
     * 为什么需要它：向量检索"永远有结果"——无关问题（如"如何赚钱"）也会返回 Top-K，
     * 模型拿到这些"看起来像资料"的内容就容易答非所问。这道硬门槛不依赖模型自觉。
     *
     * 取值依据：gte-rerank-v2 输出 0~1 相关性分。需结合评估集实测（看日志中的分数分布）：
     * 正常命中通常 0.7+，无关问题一般 < 0.5，两者之间取阈值。
     */
    private static final double SCORE_THRESHOLD = 0.3;

    private final String context;
    private final QueryRewriter queryRewriter;
    private final EmbeddingModel embeddingModel;
    private final EmbeddingStore<TextSegment> embeddingStore;
    private final ScoringModel scoringModel;
    private final List<Long> knowledgeIds;
    // 存本次检索最终喂给 LLM 的结果，供外部组装 references 用
    private final List<EmbeddingMatch<TextSegment>> matches = new ArrayList<>();

    @Tool("在知识库中检索与问题相关的文档片段，返回片段内容")
    public String searchKnowledge(@P("要检索的问题")String query){
        //1.有历史对话改写：含代词→补全实体；
        String searchQuery = (context == null || context.isBlank()
                ? query : queryRewriter.rewrite(context, query));

        //2.检索范围为空（用户一个知识库都没有）→ 直接返回，绝不做无过滤的全库检索
        if (knowledgeIds == null || knowledgeIds.isEmpty()) {
            return "资料中没有相关信息";
        }

        // 3. 向量化 query
        Response<Embedding> embed = embeddingModel.embed(searchQuery );
        Embedding content = embed.content();

        // 4.【粗召回】Qdrant Top-20，带 knowledgeId 过滤（保证答案在候选池里）
        EmbeddingSearchRequest.EmbeddingSearchRequestBuilder builder =
                EmbeddingSearchRequest.builder()
                        .queryEmbedding(content)
                        .maxResults(RECALL_K);
        // 范围内过滤：单个库/多个库统一用 isIn（此处 knowledgeIds 必非空——上面已早返回）
        builder.filter(metadataKey("knowledgeId").isIn(knowledgeIds));

        EmbeddingSearchResult<TextSegment> searchResult = embeddingStore.search(builder.build());
        List<EmbeddingMatch<TextSegment>> recallMatches = searchResult.matches();

        //5. 候选池为空，直接告诉模型没资料
        if (recallMatches.isEmpty()) {
            return "资料中没有相关信息";
        }

        //6.【精排】rerank 打分重排，取 Top-2（并过滤掉低于相关性阈值的片段）
        List<EmbeddingMatch<TextSegment>> finalMatches = rerank(searchQuery, recallMatches);

        //6.5 精排后没有一条达到相关性阈值 → 说明知识库里没有与这个问题相关的资料
        if (finalMatches.isEmpty()) {
            log.info("[检索] 全部候选低于阈值，判定为无相关资料: query='{}'", searchQuery);
            return "资料中没有相关信息";
        }

        //7. 保存最终结果，供外部组装 references（保持多轮累积语义）
        matches.addAll(finalMatches);

        //8. 拼 context 返回（只拼真正进 LLM 的 Top-2，噪声被挡在外面）
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < finalMatches.size(); i++) {
            sb.append("[").append(i + 1).append("]")
                    .append(finalMatches.get(i).embedded().text())
                    .append("\n\n");
        }

        return sb.toString();
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

        // （4）取分数最高的前 FINAL_K 条；低于相关性阈值的直接丢弃（与问题无关，不进 LLM）
        List<EmbeddingMatch<TextSegment>> finalMatches = new ArrayList<>();
        int n = Math.min(FINAL_K, order.size());
        for (int i = 0; i < n; i++) {
            int idx = order.get(i);
            double score = scores.get(idx);
            if (score < SCORE_THRESHOLD) {
                continue;
            }
            finalMatches.add(recallMatches.get(idx));
        }

        // （5）打印分数分布，便于线上观察阈值是否合理（可用日志调参）
        log.info("[检索] query='{}' 候选{}条 分数Top{}={} 通过阈值{}条(阈值={})",
                query, recallMatches.size(),
                Math.min(3, order.size()),
                order.stream().limit(3).map(i -> String.format("%.4f", scores.get(i))).toList(),
                finalMatches.size(), SCORE_THRESHOLD);

        return finalMatches;
    }

    // 供 ask() 取出检索结果组装 references
    public List<EmbeddingMatch<TextSegment>> getMatches() {
        return matches;
    }
}
