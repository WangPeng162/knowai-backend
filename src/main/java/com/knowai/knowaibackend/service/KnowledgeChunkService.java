package com.knowai.knowaibackend.service;

import com.baomidou.mybatisplus.spring.service.IService;
import com.knowai.knowaibackend.domain.document.ChunkData;
import com.knowai.knowaibackend.entity.KnowledgeChunk;

import java.util.List;

public interface KnowledgeChunkService extends IService<KnowledgeChunk> {
    void saveChunks(Long documentId, List<ChunkData> chunks);
    void replaceChunks(Long documentId, List<ChunkData> chunks);
    List<KnowledgeChunk> listByDocumentId(Long documentId);
    void updateStatus(List<Long> chunkIds,Integer status);

}
