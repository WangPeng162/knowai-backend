package com.knowai.knowaibackend.eval;

import com.fasterxml.jackson.databind.ObjectMapper;
import dev.langchain4j.data.embedding.Embedding;
import dev.langchain4j.data.segment.TextSegment;
import dev.langchain4j.model.embedding.EmbeddingModel;
import dev.langchain4j.model.scoring.ScoringModel;
import dev.langchain4j.store.embedding.EmbeddingMatch;
import dev.langchain4j.store.embedding.EmbeddingSearchRequest;
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
 * rerank 相关性阈值校准（为 KnowledgeSearchTools.SCORE_THRESHOLD 提供取值依据）
 *
 * 为什么需要它：
 *   线上用 "rerank 分数 < 阈值 → 判定为无相关资料" 来挡域外问题（防答非所问）。
 *   阈值拍脑袋会有两个风险：
 *     - 定太高 → 把正确答案也过滤掉（正常问题开始答"查不到"）
 *     - 定太低 → 无关问题照样混进上下文（答非所问）
 *
 * 校准方法（用数据找分界线）：
 *   正样本 = 评估集里的问题（知识库内确实有答案）→ 期望分数高
 *   负样本 = 与知识库内容完全无关的问题            → 期望分数低
 *   两个分布之间若存在"缝"，缝里取阈值最安全。
 *
 * 运行前提：MySQL + Qdrant + DASHSCOPE_API_KEY 就绪（与 EvalRunner 相同）
 */
@SpringBootTest
public class ThresholdCalibrator {

    /** 与线上 KnowledgeSearchTools.RECALL_K 保持一致 */
    private static final int RECALL_K = 20;

    /**
     * 负样本：与知识库主题（广西农业职业技术大学）无关的问题。
     * 注意：负样本必须真的与库内容无关，否则分数会偏高、污染校准结论。
     */
    private static final List<String> NEGATIVE_QUERIES = List.of(
            "今天天气怎么样",
            "怎么才能快速赚钱",
            "推荐几部好看的电影",
            "Python 应该怎么入门",
            "北京有哪些好玩的地方"
    );

    @Autowired
    private EmbeddingModel embeddingModel;

    @Autowired
    private EmbeddingStore<TextSegment> embeddingStore;

    @Autowired
    private ScoringModel scoringModel;

    @Test
    public void calibrate() throws Exception {
        List<Map<String, Object>> dataset = loadDataset();

        System.out.println("=============================================================");
        System.out.println("rerank 阈值校准：正样本 " + dataset.size() + " 题（库内有答案） vs 负样本 "
                + NEGATIVE_QUERIES.size() + " 题（与库无关）");
        System.out.println("=============================================================");
        System.out.println();

        // ---------- 正样本 ----------
        System.out.println("---------- 正样本（评估集问题）----------");
        List<Double> positiveHitTop1 = new ArrayList<>();    // 命中题的 top1 分数（阈值必须低于这些）
        List<Double> positiveMissTop1 = new ArrayList<>();   // 未命中题的 top1 分数
        for (int i = 0; i < dataset.size(); i++) {
            Map<String, Object> item = dataset.get(i);
            String question = (String) item.get("question");
            Long knowledgeId = ((Number) item.get("knowledgeId")).longValue();
            List<String> keywords = (List<String>) item.get("answerKeywords");

            RerankOutcome ro = retrieveAndRerank(question, knowledgeId);
            if (ro == null) {
                System.out.printf("Q%-2d %-40s 召回池为空%n", i + 1, question);
                continue;
            }
            boolean hit = containsAny(topN(ro, 5), keywords);
            (hit ? positiveHitTop1 : positiveMissTop1).add(ro.top1Score());
            System.out.printf("Q%-2d %s top1=%.4f top2=%.4f  %s%n",
                    i + 1, hit ? "[命中]" : "[未中]", ro.top1Score(), ro.top2Score(), question);
        }

        // ---------- 负样本 ----------
        System.out.println();
        System.out.println("---------- 负样本（与知识库无关）----------");
        List<Double> negativeTop1 = new ArrayList<>();
        Long anyKbId = ((Number) dataset.get(0).get("knowledgeId")).longValue();  // 用同一个知识库范围检索
        for (String q : NEGATIVE_QUERIES) {
            RerankOutcome ro = retrieveAndRerank(q, anyKbId);
            if (ro == null) {
                System.out.printf("%-24s 召回池为空%n", q);
                continue;
            }
            negativeTop1.add(ro.top1Score());
            System.out.printf("%-24s top1=%.4f top2=%.4f%n", q, ro.top1Score(), ro.top2Score());
        }

        // ---------- 校准结论 ----------
        System.out.println();
        System.out.println("================ 分数分布与校准建议 ================");
        printDistribution("正样本·命中题 top1", positiveHitTop1);
        printDistribution("正样本·未中题 top1", positiveMissTop1);
        printDistribution("负样本 top1", negativeTop1);

        double negMax = negativeTop1.stream().mapToDouble(Double::doubleValue).max().orElse(0);
        double posHitMin = positiveHitTop1.stream().mapToDouble(Double::doubleValue).min().orElse(1);
        System.out.println();
        System.out.printf("负样本最高分 = %.4f（阈值必须 高于 它，否则无关问题会溜进来）%n", negMax);
        System.out.printf("命中题最低分 = %.4f（阈值必须 低于 它，否则正确答案会被误杀）%n", posHitMin);
        if (posHitMin > negMax) {
            System.out.printf("→ 安全区间 (%.4f, %.4f)，建议取中点 ≈ %.2f%n",
                    negMax, posHitMin, (negMax + posHitMin) / 2);
        } else {
            System.out.println("→ ⚠️ 两个分布有重叠（无安全缝隙）：单靠 rerank 分数无法完全分开，");
            System.out.println("   建议取靠上的值（宁可放过一点无关内容，也别误杀正确答案）。");
        }
    }

    // ==================== 工具方法 ====================

    /** 检索（粗召回 + rerank），返回带分数的重排结果；召回池为空返回 null */
    private RerankOutcome retrieveAndRerank(String query, Long knowledgeId) {
        Embedding qe = embeddingModel.embed(query).content();
        EmbeddingSearchRequest.EmbeddingSearchRequestBuilder b =
                EmbeddingSearchRequest.builder().queryEmbedding(qe).maxResults(RECALL_K);
        if (knowledgeId != null && knowledgeId > 0) {
            b.filter(metadataKey("knowledgeId").isEqualTo(knowledgeId));
        }
        List<EmbeddingMatch<TextSegment>> recall = embeddingStore.search(b.build()).matches();
        if (recall.isEmpty()) {
            return null;
        }
        List<TextSegment> segments = new ArrayList<>();
        for (EmbeddingMatch<TextSegment> m : recall) {
            segments.add(m.embedded());
        }
        List<Double> scores = scoringModel.scoreAll(segments, query).content();
        if (scores.size() != recall.size()) {
            return new RerankOutcome(new ArrayList<>(recall), new ArrayList<>());
        }
        List<Integer> order = new ArrayList<>();
        for (int i = 0; i < recall.size(); i++) {
            order.add(i);
        }
        order.sort(Comparator.comparingDouble((Integer i) -> scores.get(i)).reversed());
        List<EmbeddingMatch<TextSegment>> ordered = new ArrayList<>();
        List<Double> orderedScores = new ArrayList<>();
        for (Integer i : order) {
            ordered.add(recall.get(i));
            orderedScores.add(scores.get(i));
        }
        return new RerankOutcome(ordered, orderedScores);
    }

    private String topN(RerankOutcome ro, int n) {
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < Math.min(n, ro.ordered.size()); i++) {
            sb.append(ro.ordered.get(i).embedded().text());
        }
        return sb.toString();
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

    private void printDistribution(String name, List<Double> values) {
        if (values.isEmpty()) {
            System.out.printf("%-20s （无数据）%n", name);
            return;
        }
        double min = values.stream().mapToDouble(Double::doubleValue).min().orElse(0);
        double max = values.stream().mapToDouble(Double::doubleValue).max().orElse(0);
        double avg = values.stream().mapToDouble(Double::doubleValue).average().orElse(0);
        System.out.printf("%-20s n=%-3d min=%.4f  avg=%.4f  max=%.4f%n", name, values.size(), min, avg, max);
    }

    private List<Map<String, Object>> loadDataset() throws Exception {
        Path path = Path.of("eval", "dataset.json");
        if (!Files.exists(path)) {
            path = Path.of("..", "eval", "dataset.json");
        }
        if (!Files.exists(path)) {
            throw new RuntimeException("❌ 找不到 eval/dataset.json");
        }
        ObjectMapper mapper = new ObjectMapper();
        return mapper.readValue(path.toFile(),
                mapper.getTypeFactory().constructCollectionType(List.class, LinkedHashMap.class));
    }

    /** rerank 输出：重排后的候选 + 对应分数 */
    static class RerankOutcome {
        final List<EmbeddingMatch<TextSegment>> ordered;
        final List<Double> scores;

        RerankOutcome(List<EmbeddingMatch<TextSegment>> ordered, List<Double> scores) {
            this.ordered = ordered;
            this.scores = scores;
        }

        double top1Score() {
            return scores.isEmpty() ? 0.0 : scores.get(0);
        }

        double top2Score() {
            return scores.size() < 2 ? 0.0 : scores.get(1);
        }
    }
}
