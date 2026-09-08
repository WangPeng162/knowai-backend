package com.knowai.knowaibackend.controller.system;


import com.knowai.knowaibackend.common.Result;
import com.knowai.knowaibackend.dto.knowledge_document.UploadDocumentDTO;
import com.knowai.knowaibackend.service.DocumentParseService;
import com.knowai.knowaibackend.service.KnowledgeDocumentService;
import com.knowai.knowaibackend.vo.knowlegeDocument.DocumentDetailVO;
import com.knowai.knowaibackend.vo.knowlegeDocument.DocumentListVO;
import io.swagger.v3.oas.annotations.Operation;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;

@RestController
@RequestMapping("/knowledge/document")
public class KnowledgeDocumentController {
    @Autowired
    private KnowledgeDocumentService knowledgeDocumentService;

    @Autowired
    private DocumentParseService documentParseService;


    @Operation(summary = "上传文件")
    @PostMapping(consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public Result<String> uploadDocument(@RequestParam("knowledgeId") Long knowledgeId,
                                         @RequestParam("file") MultipartFile file){
        UploadDocumentDTO dto = new UploadDocumentDTO();
        dto.setKnowledgeId(knowledgeId);
        boolean success = knowledgeDocumentService.uploadDocument(dto, file);
        return success ? Result.success("上传成功"): Result.fail("上传失败");
    }

    @Operation(summary = "文件列表")
    @GetMapping
    public Result<List<DocumentListVO>> listDocumentsByKnowledgeId(@RequestParam Long knowledgeId){
        List<DocumentListVO> voList = knowledgeDocumentService.listDocumentsByKnowledgeId(knowledgeId);
        return Result.success(voList);

    }

    @Operation(summary = "文件详情")
    @GetMapping("/detail")
    public Result<DocumentDetailVO> documentDetail(@RequestParam Long id){
        DocumentDetailVO documentDetailVO = knowledgeDocumentService.documentDetail(id);
        return Result.success(documentDetailVO);
    }

    @Operation(summary = "删除文件")
    @DeleteMapping("/{id}")
    public Result<String> deleteDocument(@PathVariable Long id){
        return documentParseService.deleteDocument(id) ?
                Result.success("删除成功"): Result.fail("删除失败");
    }
}
