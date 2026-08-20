package com.knowai.knowaibackend.service.impl;

import com.knowai.knowaibackend.entity.KnowledgeChunk;
import com.knowai.knowaibackend.entity.KnowledgeEmbedding;
import com.knowai.knowaibackend.service.EmbeddingService;
import com.knowai.knowaibackend.service.KnowledgeChunkService;
import com.knowai.knowaibackend.service.KnowledgeEmbeddingService;
import dev.langchain4j.data.document.Metadata;
import dev.langchain4j.data.embedding.Embedding;
import dev.langchain4j.data.segment.TextSegment;
import dev.langchain4j.model.embedding.EmbeddingModel;
import dev.langchain4j.model.output.Response;
import dev.langchain4j.store.embedding.EmbeddingStore;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;


@RequiredArgsConstructor
@Service
public class EmbeddingServiceImpl implements EmbeddingService {

    private final KnowledgeEmbeddingService knowledgeEmbeddingService;

    private final EmbeddingStore<TextSegment> embeddingStore;

    private final KnowledgeChunkService knowledgeChunkService;

    private final EmbeddingModel embeddingModel;

    @Override
    public void generateEmbedding(Long documentId) {

        //1. 查询chunk
        List<KnowledgeChunk> knowledgeChunks = knowledgeChunkService.listByDocumentId(documentId);

        //2. chunk转换TextSegment
        List<TextSegment> textSegments = knowledgeChunks.stream().map(knowledgeChunk -> {
                    Metadata metadata = new Metadata();
                    metadata.put("chunkId", knowledgeChunk.getId());
                    metadata.put("documentId", knowledgeChunk.getDocumentId());
                    metadata.put("pageNumber", knowledgeChunk.getPageNumber());

                    return TextSegment.from(knowledgeChunk.getContent(), metadata);
                }).toList();

        //3. batch embedding
        int batchSize = 100;
        int size = textSegments.size();
        for (int start = 0; start < size; start += batchSize) {
            int end = Math.min(start + batchSize,size);
            List<TextSegment> batch = new ArrayList<>(textSegments.subList(start,end));
            //收集本批次 chunkId
            List<Long> batchChunkIds = batch.stream().map(segment -> segment.metadata()
                    .getLong("chunkId")).toList();


            try {
                //批量向量化
                Response<List<Embedding>> listResponse = embeddingModel.embedAll(batch);
                //获取content
                List<Embedding> embeddings = listResponse.content();

                //4. 保存 Embedding + TextSegment
                List<String> vectorIds  = embeddingStore.addAll(embeddings, batch);

                //5. 创建knowledgeEmbedding
                List<KnowledgeEmbedding> embeddingList = new ArrayList<>();
                for (int i = 0; i < batch.size(); i++) {
                    TextSegment segment = batch.get(i);
                    Embedding embedding = embeddings.get(i);
                    String vectorId = vectorIds.get(i);

                    KnowledgeEmbedding knowledgeEmbedding = new KnowledgeEmbedding();
                    Long chunkId = segment.metadata().getLong("chunkId");
                    knowledgeEmbedding.setChunkId(chunkId);
                    knowledgeEmbedding.setVectorId(vectorId);
                    knowledgeEmbedding.setDimension(embedding.vector().length);
                    knowledgeEmbedding.setStatus(2);
                    embeddingList.add(knowledgeEmbedding);
                }

                //6. 保存 knowledge_embedding
                knowledgeEmbeddingService.batchInsert(embeddingList);

                //7.更新chunk status = 2(成功)
                knowledgeChunkService.updateStatus(batchChunkIds,KnowledgeChunk.STATUS_SUCCESS);
            } catch (Exception e) {
                //更新 chunk status = 3(失败)
                knowledgeChunkService.updateStatus(batchChunkIds,KnowledgeChunk.STATUS_FAIL);
                throw new RuntimeException("文档向量化失败",e);
            }
        }
    }
}
