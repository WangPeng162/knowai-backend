package com.knowai.knowaibackend.config;

import dev.langchain4j.community.model.dashscope.QwenEmbeddingModel;
import dev.langchain4j.model.embedding.EmbeddingModel;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;


@Configuration
public class EmbeddingConfig {

    @Bean
    public EmbeddingModel embeddingModel(){

        String API_KEY = System.getenv("DASHSCOPE_API_KEY");

        if (API_KEY == null || API_KEY.isBlank()) {
            throw new RuntimeException("❌ 没有读到环境变量 DASHSCOPE_API_KEY！检查名字+重启IDEA");
        }

        QwenEmbeddingModel embeddingModel = QwenEmbeddingModel.builder()
                .apiKey(API_KEY)
                .modelName("text-embedding-v4")
                .build();

        return embeddingModel;
    }
}
