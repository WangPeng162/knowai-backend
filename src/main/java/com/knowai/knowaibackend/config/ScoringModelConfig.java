package com.knowai.knowaibackend.config;

import dev.langchain4j.community.model.dashscope.QwenScoringModel;
import dev.langchain4j.model.scoring.ScoringModel;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * Rerank 精排模型配置（DashScope Text Rerank 官方封装）
 *
 * 模型选型说明：
 *  - 老 gte-rerank 已于 2026-05-30 下线，官方推荐 qwen3-rerank
 *  - 但 qwen3-rerank 走 OpenAI 兼容端点(/compatible-api/v1/reranks)，
 *    而 QwenScoringModel 封装的是老端点(/services/rerank/text-rerank/text-rerank)，
 *    老端点当前确认可用的是 gte-rerank-v2（50+ 语种、中文支持好、按量计费便宜）
 *  - 所以这里默认 gte-rerank-v2；若后续验证 qwen3-rerank 也被 QwenScoringModel 支持，换 modelName 即可
 */
@Configuration
public class ScoringModelConfig {

    @Bean
    public ScoringModel scoringModel() {
        String API_KEY = System.getenv("DASHSCOPE_API_KEY");

        if (API_KEY == null || API_KEY.isBlank()) {
            throw new RuntimeException("❌ 没有读到环境变量 DASHSCOPE_API_KEY！检查名字+重启IDEA");
        }

        return QwenScoringModel.builder()
                .apiKey(API_KEY)
                .modelName("gte-rerank-v2")
                .build();
    }
}
