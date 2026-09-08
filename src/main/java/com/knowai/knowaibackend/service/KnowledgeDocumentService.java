package com.knowai.knowaibackend.service;

import com.baomidou.mybatisplus.spring.service.IService;
import com.knowai.knowaibackend.dto.knowledge_document.UploadDocumentDTO;
import com.knowai.knowaibackend.entity.KnowledgeDocument;
import com.knowai.knowaibackend.vo.knowlegeDocument.DocumentDetailVO;
import com.knowai.knowaibackend.vo.knowlegeDocument.DocumentListVO;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;

public interface KnowledgeDocumentService extends IService<KnowledgeDocument> {

    boolean uploadDocument(UploadDocumentDTO dto, MultipartFile file);

    List<DocumentListVO> listDocumentsByKnowledgeId(Long knowledgeId);

    DocumentDetailVO documentDetail(Long id);

    void updateStatus(Long documentId,Integer status);

    void checkKnowledgeOwnership(Long knowledgeId);

}
