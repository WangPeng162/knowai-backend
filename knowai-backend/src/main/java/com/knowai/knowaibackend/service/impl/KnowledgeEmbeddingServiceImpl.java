package com.knowai.knowaibackend.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.spring.service.impl.ServiceImpl;
import com.knowai.knowaibackend.entity.KnowledgeEmbedding;
import com.knowai.knowaibackend.mapper.KnowledgeEmbeddingMapper;
import com.knowai.knowaibackend.service.KnowledgeEmbeddingService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class KnowledgeEmbeddingServiceImpl extends ServiceImpl<KnowledgeEmbeddingMapper, KnowledgeEmbedding>
        implements KnowledgeEmbeddingService {

    @Autowired
    private KnowledgeEmbeddingMapper knowledgeEmbeddingMapper;


    @Override
    public void batchInsert(List<KnowledgeEmbedding> list) {
        this.saveBatch(list);
    }

    @Override
    public void deleteByChunkIds(List<Long> chunkIds) {
        //判断
        if (chunkIds == null || chunkIds.isEmpty()) {
            return;
        }
        //构造条件
        LambdaQueryWrapper<KnowledgeEmbedding> wrapper = new LambdaQueryWrapper<>();
        wrapper.in(KnowledgeEmbedding::getChunkId,chunkIds);
        //删除
        this.remove(wrapper);
    }
}
