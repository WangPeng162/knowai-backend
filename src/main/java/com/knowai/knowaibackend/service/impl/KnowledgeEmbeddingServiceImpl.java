package com.knowai.knowaibackend.service.impl;

import com.baomidou.mybatisplus.spring.service.impl.ServiceImpl;
import com.knowai.knowaibackend.entity.KnowledgeEmbedding;
import com.knowai.knowaibackend.mapper.KnowledgeEmbeddingMapper;
import com.knowai.knowaibackend.service.KnowledgeEmbeddingService;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class KnowledgeEmbeddingServiceImpl extends ServiceImpl<KnowledgeEmbeddingMapper, KnowledgeEmbedding>
        implements KnowledgeEmbeddingService {


    @Override
    public void batchInsert(List<KnowledgeEmbedding> list) {
        this.saveBatch(list);
    }
}
