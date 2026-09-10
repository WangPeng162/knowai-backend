package com.knowai.knowaibackend.config;

import dev.langchain4j.data.segment.TextSegment;
import dev.langchain4j.store.embedding.EmbeddingStore;
import dev.langchain4j.store.embedding.qdrant.QdrantEmbeddingStore;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * Qdrant 向量库连接配置
 *
 * 配置外置：地址/端口/集合名都可通过配置或环境变量覆盖，默认值保持本地开发行为不变。
 *  - 本地开发：不配置任何东西 → localhost:6334
 *  - Docker 部署：环境变量 QDRANT_HOST=qdrant（容器名）即可指向 Qdrant 容器
 *    （容器内的 localhost 指容器自身，绝不能写死 localhost）
 */
@Configuration
public class EmbeddingStoreConfig {

    @Value("${qdrant.host:localhost}")
    private String host;

    @Value("${qdrant.port:6334}")
    private int port;

    @Value("${qdrant.collection:knowai_knowledge}")
    private String collectionName;

    @Bean
    public EmbeddingStore<TextSegment> embeddingStore() {
        return QdrantEmbeddingStore.builder()
                .host(host)
                .port(port)
                .collectionName(collectionName)
                .useTls(false)
                .build();
    }
}
