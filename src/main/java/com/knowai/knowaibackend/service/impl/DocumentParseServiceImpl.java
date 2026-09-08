package com.knowai.knowaibackend.service.impl;

import com.knowai.knowaibackend.domain.document.ChunkData;
import com.knowai.knowaibackend.domain.document.DocumentPage;
import com.knowai.knowaibackend.entity.KnowledgeDocument;
import com.knowai.knowaibackend.exception.BusinessException;
import com.knowai.knowaibackend.parser.DocumentParser;
import com.knowai.knowaibackend.service.*;
import com.knowai.knowaibackend.splitter.ChunkSplitter;
import com.knowai.knowaibackend.utils.OssUtil;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.io.InputStream;
import java.util.List;


@RequiredArgsConstructor
@Service
public class DocumentParseServiceImpl implements DocumentParseService {

    private final KnowledgeService knowledgeService;

    private final EmbeddingService embeddingService;

    private final KnowledgeDocumentService documentService;

    private final List<DocumentParser> parsers;

    private final ChunkSplitter chunkSplitter;

    private final KnowledgeChunkService chunkService;

    private final OssUtil ossUtil;

    //Parser选择器
    private  DocumentParser getParser(String suffix){
        for (DocumentParser parser : parsers) {
            if (parser.supportType().equals(suffix)){
                return parser;
            }
        }

        throw new RuntimeException("不支持的文件类型:" + suffix);
    }

    @Override
    public void parseDocument(Long documentId) {

        //1.根据documentId查询文档
        KnowledgeDocument document = documentService.getById(documentId);

        if (document == null){
            throw new BusinessException("文档不存在");
        }

        //2.修改状态（解析中）
        documentService.updateStatus(documentId,KnowledgeDocument.STATUS_PARSING);

        //3.获取文件后缀
        String suffix = document.getFileSuffix();

        //4.找到对应解析器
        DocumentParser parser = getParser(suffix);

        //5.获取文件路径
        try (InputStream inputStream = ossUtil.download(document.getFilePath())){

            //6.调用parser解析文档
            List<DocumentPage> pages = parser.parse(inputStream);

            //7.切分chunk
            List<ChunkData> chunks = chunkSplitter.split(pages);

            //8.清理旧 embedding（必须在 replaceChunks 删旧 chunk 之前）
            embeddingService.deleteEmbeddingByDocumentId(documentId);

            //9.保存
            //chunkService.saveChunks(documentId,chunks);
            chunkService.replaceChunks(documentId,chunks);

            //10.向量化
            embeddingService.generateEmbedding(documentId);

            //11.修改状态（解析成功）
            documentService.updateStatus(documentId,KnowledgeDocument.STATUS_SUCCESS);

        } catch (Exception e) {
            //修改状态（解析失败）
            documentService.updateStatus(documentId,KnowledgeDocument.STATUS_FAIL);

            throw new RuntimeException("文档解析失败",e);
        }




    }

    @Override
    public boolean deleteDocument(Long documentId) {

        //1.根据documentId查询文档
        KnowledgeDocument document = documentService.getById(documentId);

        //1.1判断
        if (document == null){
            throw new BusinessException("文档不存在");
        }

        //1.2拿到关联knowledge
        Long knowledgeId = document.getKnowledgeId();

        documentService.checkKnowledgeOwnership(knowledgeId);

        //2.删除向量
        embeddingService.deleteEmbeddingByDocumentId(documentId);

        //3.删除chunk
        chunkService.deleteByDocumentId(documentId);

        //4.删除document
        return documentService.removeById(documentId);

    }
}
