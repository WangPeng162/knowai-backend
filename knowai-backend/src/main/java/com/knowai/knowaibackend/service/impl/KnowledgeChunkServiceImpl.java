package com.knowai.knowaibackend.service.impl;

import com.baomidou.mybatisplus.core.conditions.Wrapper;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.baomidou.mybatisplus.spring.service.impl.ServiceImpl;
import com.knowai.knowaibackend.domain.document.ChunkData;
import com.knowai.knowaibackend.entity.KnowledgeChunk;
import com.knowai.knowaibackend.exception.BusinessException;
import com.knowai.knowaibackend.mapper.KnowledgeChunkMapper;
import com.knowai.knowaibackend.service.KnowledgeChunkService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.List;

@Service
public class KnowledgeChunkServiceImpl extends ServiceImpl<KnowledgeChunkMapper, KnowledgeChunk> implements KnowledgeChunkService {

    @Autowired
    private KnowledgeChunkMapper knowledgeChunkMapper;

    @Override
    public void saveChunks(Long documentId, List<ChunkData> chunks) {

        if(chunks == null || chunks.isEmpty()){
            return;
        }

        List<KnowledgeChunk> entities = getChunks(documentId, chunks);

        saveBatch(entities);
    }


    @Override
    @Transactional(rollbackFor = Exception.class)
    public void replaceChunks(Long documentId, List<ChunkData> chunks) {

        //重新解析文档
        List<KnowledgeChunk> entities = getChunks(documentId, chunks);

        //删除旧Chunk
        LambdaQueryWrapper<KnowledgeChunk> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(KnowledgeChunk::getDocumentId,documentId);
        remove(wrapper);

        //保存新Chunk
        saveBatch(entities);

    }

    @Override
    public List<KnowledgeChunk> listByDocumentId(Long documentId) {

        //1.构建查询条件（根据知文件id查询状态为0的Chunk，并按索引排序）
        LambdaQueryWrapper<KnowledgeChunk> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(KnowledgeChunk::getDocumentId,documentId);
        wrapper.eq(KnowledgeChunk::getStatus, KnowledgeChunk.STATUS_WAIT_EMBEDDING);
        wrapper.orderByAsc(KnowledgeChunk::getChunkIndex);


        //2.查询
        List<KnowledgeChunk> knowledgeChunks = knowledgeChunkMapper.selectList(wrapper);
        return knowledgeChunks;
    }

    @Override
    public void updateStatus(List<Long> chunkIds, Integer status) {
        if (chunkIds == null || chunkIds.isEmpty()) {
            return;
        }
        LambdaUpdateWrapper<KnowledgeChunk> wrapper = new LambdaUpdateWrapper<>();
        wrapper.in(KnowledgeChunk::getId,chunkIds);
        wrapper.set(KnowledgeChunk::getStatus,status);
        update(wrapper);
    }

    @Override
    public List<Long> getChunkIdsByDocumentId(Long documentId) {
        //构造查询条件
        LambdaQueryWrapper<KnowledgeChunk> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(KnowledgeChunk::getDocumentId,documentId);
        wrapper.select(KnowledgeChunk::getId);
        //查询
        List<KnowledgeChunk> knowledgeChunks = knowledgeChunkMapper.selectList(wrapper);
        //提取id
        List<Long> chunkIds = knowledgeChunks.stream().map(KnowledgeChunk::getId).toList();
        return chunkIds;
    }


    private List<KnowledgeChunk> getChunks(Long documentId, List<ChunkData> chunks) {
        List<KnowledgeChunk> entities = new ArrayList<>();

        for (ChunkData chunkData : chunks) {

            KnowledgeChunk chunk = new KnowledgeChunk();

            chunk.setDocumentId(documentId);

            chunk.setContent(chunkData.getContent());

            chunk.setChunkIndex(chunkData.getChunkIndex());

            chunk.setPageNumber(chunkData.getPageNumber());

            entities.add(chunk);

        }
        return entities;
    }
}
