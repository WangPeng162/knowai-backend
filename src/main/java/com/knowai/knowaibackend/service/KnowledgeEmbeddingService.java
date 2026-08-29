package com.knowai.knowaibackend.service;

import com.baomidou.mybatisplus.spring.service.IService;
import com.knowai.knowaibackend.entity.KnowledgeEmbedding;

import java.util.List;

public interface KnowledgeEmbeddingService extends IService<KnowledgeEmbedding> {
    void batchInsert(List<KnowledgeEmbedding> knowledgeEmbedding);
}
