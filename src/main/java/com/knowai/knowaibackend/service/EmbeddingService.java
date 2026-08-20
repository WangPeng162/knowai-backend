package com.knowai.knowaibackend.service;

public interface EmbeddingService {

    /**
     * 为指定文档的所有Chunk生成向量
     */
    void generateEmbedding(Long documentId);
}
