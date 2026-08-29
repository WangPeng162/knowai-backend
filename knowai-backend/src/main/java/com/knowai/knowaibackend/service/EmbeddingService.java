package com.knowai.knowaibackend.service;

public interface EmbeddingService {

    /**
     * 为指定文档的所有Chunk生成向量
     */
    void generateEmbedding(Long documentId);

    /**
     * 按文档 ID 删除向量
     * @param documentId
     */
    void deleteEmbeddingByDocumentId(Long documentId);
}
