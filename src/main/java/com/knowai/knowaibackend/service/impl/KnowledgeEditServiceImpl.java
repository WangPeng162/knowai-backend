package com.knowai.knowaibackend.service.impl;

import com.knowai.knowaibackend.entity.KnowledgeDocument;
import com.knowai.knowaibackend.service.DocumentParseService;
import com.knowai.knowaibackend.service.KnowledgeDocumentService;
import com.knowai.knowaibackend.service.KnowledgeEditService;
import com.knowai.knowaibackend.service.KnowledgeService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;

@RequiredArgsConstructor
@Service
public class KnowledgeEditServiceImpl implements KnowledgeEditService {

    private final KnowledgeService knowledgeService;
    private final KnowledgeDocumentService knowledgeDocumentService;
    private final DocumentParseService documentParseService;

    @Override
    public void deleteKnowledge(Long knowledgeId) {

        //1.判断知识库是否该用户所建
        knowledgeDocumentService.checkKnowledgeOwnership(knowledgeId);

        //2.查询该库所有文档
        List<KnowledgeDocument> documentList = knowledgeDocumentService.lambdaQuery()
                .eq(KnowledgeDocument::getKnowledgeId, knowledgeId).list();

        //3.循环删除文档
        for (KnowledgeDocument doc : documentList) {
            documentParseService.deleteDocument(doc.getId());
        }

        //4.删除知识库本身
        knowledgeService.removeById(knowledgeId);


    }
}
