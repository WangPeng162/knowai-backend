package com.knowai.knowaibackend.eval;

import com.fasterxml.jackson.databind.ObjectMapper;
import dev.langchain4j.data.embedding.Embedding;
import dev.langchain4j.data.segment.TextSegment;
import dev.langchain4j.model.embedding.EmbeddingModel;
import dev.langchain4j.model.scoring.ScoringModel;
import dev.langchain4j.store.embedding.EmbeddingMatch;
import dev.langchain4j.store.embedding.EmbeddingSearchRequest;
import dev.langchain4j.store.embedding.EmbeddingSearchResult;
import dev.langchain4j.store.embedding.EmbeddingStore;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import static dev.langchain4j.store.embedding.filter.MetadataFilterBuilder.metadataKey;

/**
 * RAG 检索质量对比评估：embedding 直排 vs embedding 粗召回 + rerank 精排
 *
 * 对比口径（同一批题、同一次粗召回池，保证公平）：
 *  - embedding 直排 = 粗召回池的原始顺序（池内前 1/2/5 就是原基线的 Top-1/2/5）
 *  - rerank 精排    = 把整个召回池交给 ScoringModel 打分后重排的顺序
 *  两条流水线各自算 HitRate@1/@2/@5 + MRR，直接看 rerank 把指标拉高了多少
 *
 * 判分口径：
 *  - chunk 命中 = 该 chunk 文本含任一 answerKeyword（OR，忽略大小写）
 *  - loose=true（宽松题）：Top-K 拼接文本含任一关键词即命中
 *  - loose=false（严格题，多关键词答案）：Top-K 拼接文本须覆盖全部关键词
 *  - MRR 的首个命中 = 顺序中第一个"含任一关键词"的 chunk（对所有题统一用 OR 找线索，避免严格题歧义）
 *
 * 运行前提：MySQL + Qdrant(localhost:6334) 已启动，环境变量 DASHSCOPE_API_KEY 已配置
 */
@SpringBootTest
public class EvalRunner {

    /** 粗召回窗口（与线上 KnowledgeSearchTools.RECALL_K 保持一致） */
    private static final int RECALL_K = 20;
    /** 指标判定到 @K（与线上 FINAL_K=2 不同，这里看完整排序质量） */
    private static final int JUDGE_K = 5;

    @Autowired
    private EmbeddingModel embeddingModel;

    @Autowired
    private EmbeddingStore<TextSegment> embeddingStore;

    @Autowired
    private ScoringModel scoringModel;

    /** 单条排序流水线的统计结果 */
    static class Stats {
        final int[] hits = new int[JUDGE_K + 1]; // hits[k] = 该题在 Top-K 是否命中(0/1)
        double rr = 0.0;                          // 第一个线索 chunk 排名的倒数
        int firstHitRank = -1;
    }

    @Test
    public void runCompare() throws Exception {
        List<Map<String, Object>> dataset = loadDataset();
        int total = dataset.size();
        System.out.println("=============================================================");
        System.out.println("RAG 检索对比评估：" + total + " 题 · 粗召回 Top-" + RECALL_K + " · 判定到 @"
                + JUDGE_K + " · rerank 模型 gte-rerank-v2");
        System.out.println("=============================================================");

        // 汇总
        Stats embedTotal = new Stats();
        Stats rerankTotal = new Stats();
        List<String> savedByRerank = new ArrayList<>();   // rerank 救回：embed@5 没中、rerank@5 中了
        List<String> lostByRerank = new ArrayList<>();    // rerank 搞丢：embed@5 中了、rerank@5 没中
        List<String> movedUp = new ArrayList<>();         // 排名提前：都中，但 rerank 首个命中更靠前

        for (int qIndex = 0; qIndex < dataset.size(); qIndex++) {
            Map<String, Object> item = dataset.get(qIndex);
            String question = (String) item.get("question");
            Long knowledgeId = ((Number) item.get("knowledgeId")).longValue();
            List<String> keywords = (List<String>) item.get("answerKeywords");
            boolean loose = Boolean.TRUE.equals(item.get("loose"));

            // 1. 向量化 query
            Embedding queryEmbedding = embeddingModel.embed(question).content();

            // 2. 粗召回 Top-RECALL_K（knowledgeId 过滤）——两条流水线共用同一个池
            EmbeddingSearchRequest.EmbeddingSearchRequestBuilder builder =
                    EmbeddingSearchRequest.builder().queryEmbedding(queryEmbedding).maxResults(RECALL_K);
            if (knowledgeId != null && knowledgeId > 0) {
                builder.filter(metadataKey("knowledgeId").isEqualTo(knowledgeId));
            }
            List<EmbeddingMatch<TextSegment>> recallMatches =
                    embeddingStore.search(builder.build()).matches();

            if (recallMatches.isEmpty()) {
                System.out.printf("Q%-2d 召回池为空，跳过%n", qIndex + 1);
                continue;
            }

            // 3. 计算 rerank 重排后的顺序
            List<EmbeddingMatch<TextSegment>> rerankMatches = rerank(question, recallMatches);

            // 4. 两条流水线各自判分
            Stats embedStats = judge(recallMatches, keywords, loose);
            Stats rerankStats = judge(rerankMatches, keywords, loose);

            // 5. 汇总
            addStats(embedTotal, embedStats);
            addStats(rerankTotal, rerankStats);

            // 6. 逐题明细 + 变化归类
            System.out.printf("Q%-2d %s embed:  @1%s @2%s @5%s RR=%s | rerank: @1%s @2%s @5%s RR=%s | 首中 rank: %d→%d%n",
                    qIndex + 1, loose ? "[宽松]" : "[严格]",
                    mark(embedStats.hits[1]), mark(embedStats.hits[2]), mark(embedStats.hits[5]), fmt(embedStats.rr),
                    mark(rerankStats.hits[1]), mark(rerankStats.hits[2]), mark(rerankStats.hits[5]), fmt(rerankStats.rr),
                    embedStats.firstHitRank, rerankStats.firstHitRank);

            boolean embedHit5 = embedStats.hits[5] == 1;
            boolean rerankHit5 = rerankStats.hits[5] == 1;
            if (!embedHit5 && rerankHit5) {
                savedByRerank.add("Q" + (qIndex + 1));
            } else if (embedHit5 && !rerankHit5) {
                lostByRerank.add("Q" + (qIndex + 1));
            } else if (embedHit5 && rerankHit5 && rerankStats.rr > embedStats.rr + 0.0001) {
                movedUp.add("Q" + (qIndex + 1) + "(" + embedStats.firstHitRank + "→" + rerankStats.firstHitRank + ")");
            }
        }

        // 7. 汇总输出
        System.out.println();
        System.out.println("================ 汇总对比 ================");
        printRow("embedding 直排", embedTotal, total);
        printRow("embedding+rerank", rerankTotal, total);
        System.out.println();
        System.out.printf("HitRate@1 提升: %+.1f%%    HitRate@2 提升: %+.1f%%    HitRate@5 提升: %+.1f%%    MRR 提升: %+.4f%n",
                (rerankTotal.hits[1] - embedTotal.hits[1]) * 100.0 / total,
                (rerankTotal.hits[2] - embedTotal.hits[2]) * 100.0 / total,
                (rerankTotal.hits[5] - embedTotal.hits[5]) * 100.0 / total,
                rerankTotal.rr / total - embedTotal.rr / total);
        if (!savedByRerank.isEmpty()) System.out.println("rerank 救回（embed@5 没中 → rerank@5 中）: " + savedByRerank);
        if (!lostByRerank.isEmpty()) System.out.println("rerank 搞丢（embed@5 中了 → rerank@5 没中）: " + lostByRerank);
        if (!movedUp.isEmpty()) System.out.println("排名提前（都中但 rerank 首中更靠前）: " + movedUp);
        if (savedByRerank.isEmpty() && lostByRerank.isEmpty() && movedUp.isEmpty()) {
            System.out.println("（两道流水线在 @5 口径下无差异）");
        }
    }

    /** 对给定顺序的候选列表判分：算 Top-1..JUDGE_K 命中 + 首个线索 rank（MRR 用） */
    private Stats judge(List<EmbeddingMatch<TextSegment>> ordered, List<String> keywords, boolean loose) {
        Stats s = new Stats();
        StringBuilder combined = new StringBuilder();
        for (int r = 1; r <= Math.min(JUDGE_K, ordered.size()); r++) {
            String text = ordered.get(r - 1).embedded().text();
            combined.append(text);
            if (s.firstHitRank < 0 && containsAny(text, keywords)) {
                s.firstHitRank = r;
            }
            s.hits[r] = loose
                    ? (containsAny(combined.toString(), keywords) ? 1 : 0)
                    : (containsAll(combined.toString(), keywords) ? 1 : 0);
        }
        // 首个线索可能在 Top-5 之外（比如 rank 8）——MRR 仍按真实 rank 算
        if (s.firstHitRank < 0) {
            for (int r = JUDGE_K + 1; r <= ordered.size(); r++) {
                if (containsAny(ordered.get(r - 1).embedded().text(), keywords)) {
                    s.firstHitRank = r;
                    break;
                }
            }
        }
        s.rr = s.firstHitRank > 0 ? 1.0 / s.firstHitRank : 0.0;
        return s;
    }

    /** 用 ScoringModel 对整个召回池打分，返回按分数降序重排的候选（数量不符时退回原序） */
    private List<EmbeddingMatch<TextSegment>> rerank(String query,
                                                     List<EmbeddingMatch<TextSegment>> recallMatches) {
        List<TextSegment> segments = new ArrayList<>();
        for (EmbeddingMatch<TextSegment> m : recallMatches) {
            segments.add(m.embedded());
        }
        List<Double> scores = scoringModel.scoreAll(segments, query).content();
        if (scores.size() != recallMatches.size()) {
            return new ArrayList<>(recallMatches);
        }
        List<Integer> order = new ArrayList<>();
        for (int i = 0; i < recallMatches.size(); i++) {
            order.add(i);
        }
        order.sort(Comparator.comparingDouble((Integer i) -> scores.get(i)).reversed());
        List<EmbeddingMatch<TextSegment>> reranked = new ArrayList<>();
        for (Integer i : order) {
            reranked.add(recallMatches.get(i));
        }
        return reranked;
    }

    private void addStats(Stats total, Stats row) {
        for (int k = 1; k <= JUDGE_K; k++) {
            total.hits[k] += row.hits[k];
        }
        total.rr += row.rr;
    }

    private void printRow(String name, Stats total, int n) {
        System.out.printf("%-18s  HitRate@1=%2d/%-2d (%4.1f%%)  @2=%2d/%-2d (%4.1f%%)  @5=%2d/%-2d (%4.1f%%)  MRR=%.4f%n",
                name,
                total.hits[1], n, total.hits[1] * 100.0 / n,
                total.hits[2], n, total.hits[2] * 100.0 / n,
                total.hits[5], n, total.hits[5] * 100.0 / n,
                total.rr / n);
    }

    private String mark(int v) {
        return v == 1 ? "✓" : "✗";
    }

    private String fmt(double v) {
        return String.format("%.2f", v);
    }

    private boolean containsAny(String text, List<String> keywords) {
        String lower = text.toLowerCase();
        for (String kw : keywords) {
            if (lower.contains(kw.toLowerCase())) {
                return true;
            }
        }
        return false;
    }

    private boolean containsAll(String text, List<String> keywords) {
        String lower = text.toLowerCase();
        for (String kw : keywords) {
            if (!lower.contains(kw.toLowerCase())) {
                return false;
            }
        }
        return true;
    }

    private List<Map<String, Object>> loadDataset() throws Exception {
        Path path = Path.of("eval", "dataset.json");
        if (!Files.exists(path)) {
            path = Path.of("..", "eval", "dataset.json");
        }
        if (!Files.exists(path)) {
            throw new RuntimeException("❌ 找不到 eval/dataset.json，请确认文件位置（项目根/eval/ 下）");
        }
        ObjectMapper mapper = new ObjectMapper();
        return mapper.readValue(path.toFile(),
                mapper.getTypeFactory().constructCollectionType(List.class, LinkedHashMap.class));
    }
}
