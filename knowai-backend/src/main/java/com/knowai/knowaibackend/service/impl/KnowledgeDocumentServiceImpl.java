package com.knowai.knowaibackend.service.impl;

import com.baomidou.mybatisplus.spring.service.impl.ServiceImpl;
import com.knowai.knowaibackend.dto.knowledge_document.UploadDocumentDTO;
import com.knowai.knowaibackend.entity.KnowledgeBase;
import com.knowai.knowaibackend.entity.KnowledgeDocument;
import com.knowai.knowaibackend.exception.BusinessException;
import com.knowai.knowaibackend.mapper.KnowledgeDocumentMapper;
import com.knowai.knowaibackend.mapper.KnowledgeMapper;
import com.knowai.knowaibackend.service.KnowledgeDocumentService;
import com.knowai.knowaibackend.utils.OssUtil;
import com.knowai.knowaibackend.vo.knowlegeDocument.DocumentDetailVO;
import com.knowai.knowaibackend.vo.knowlegeDocument.DocumentListVO;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import java.io.IOException;
import java.util.List;
import java.util.stream.Collectors;

@Service
@SuppressWarnings("all")
@RequiredArgsConstructor
public class KnowledgeDocumentServiceImpl extends ServiceImpl<KnowledgeDocumentMapper, KnowledgeDocument> implements KnowledgeDocumentService {

    private final OssUtil ossUtil;
    @Autowired
    private KnowledgeMapper knowledgeMapper;

    @Autowired
    private KnowledgeDocumentMapper knowledgeDocumentMapper;

    @Override
    public boolean uploadDocument(UploadDocumentDTO dto, MultipartFile file) {

        //1.获取knowledgeId
        Long knowledgeId = dto.getKnowledgeId();

        //2.校验knowledgeId
        if (knowledgeId == null){
            throw new BusinessException("没有该知识库");
        }
        //查询知识库是否存在
        Long count = knowledgeMapper.selectCount(
                Wrappers.<KnowledgeBase>lambdaQuery()
                        .eq(KnowledgeBase::getId, knowledgeId)
        );
        if (count == 0) {
            throw new BusinessException("指定知识库不存在");
        }

        //3.获取文件信息（名称、大小、类型）
        String name = file.getOriginalFilename();
        long size = file.getSize();
        String type = file.getContentType();

        //4.上传文件到OSS，获取filePath
        String path;
        try {
            path = ossUtil.upload(file, "knowledge");
        } catch (IOException e) {
            throw new RuntimeException("文件上传oss失败",e);
        }

        //5.创建 KnowledgeDocument 对象
        KnowledgeDocument knowledgeDocument = new KnowledgeDocument();

        //6.设置各项属性
        knowledgeDocument.setKnowledgeId(knowledgeId);
        knowledgeDocument.setFileName(name);
        knowledgeDocument.setFileSize(size);
        knowledgeDocument.setFilePath(path);
        knowledgeDocument.setMimeType(type);
        //setfileType
        String fileType = null;
        if (name != null && name.contains(".")){
            fileType = name.substring(name.lastIndexOf(".")+1);
        }
        knowledgeDocument.setFileSuffix(fileType);

        //7.保存数据库
        boolean save = this.save(knowledgeDocument);
        if (!save){
            throw new BusinessException("数据库保存失败");
        }

        //8.返回上传成功
        return save;
    }

    @Override
    public List<DocumentListVO> listDocumentsByKnowledgeId(Long knowledgeId) {
        //1.判断knowledgeId是否存在
        Long count = knowledgeMapper.selectCount(
                Wrappers.<KnowledgeBase>lambdaQuery()
                        .eq(KnowledgeBase::getId, knowledgeId)
        );
        if (count == 0) {
            throw new BusinessException("该知识库不存在");
        }

        //2.构建查询条件（根据知识库id查询，并按创建时间筛选）
        LambdaQueryWrapper<KnowledgeDocument> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(KnowledgeDocument::getKnowledgeId,knowledgeId);
        wrapper.orderByDesc(KnowledgeDocument::getCreateTime);

        //3.查询文档
        List<KnowledgeDocument> documentList = knowledgeDocumentMapper.selectList(wrapper);

        //4.entity转换vo
        List<DocumentListVO> voList = documentList.stream()
                .map(entity -> {
                    DocumentListVO vo = new DocumentListVO();
                    BeanUtils.copyProperties(entity, vo);
                    return vo;
                })
                .collect(Collectors.toList());


        return voList;
    }

    @Override
    public DocumentDetailVO documentDetail(Long id) {
        //1.查询文件
        KnowledgeDocument document = knowledgeDocumentMapper.selectById(id);

        //2.判断文件是否为空
        if (document == null){
            throw new BusinessException("该文件不存在");
        }

        //3.entity转换vo
        DocumentDetailVO vo = new DocumentDetailVO();
        BeanUtils.copyProperties(document,vo);

        return vo;
    }

    @Override
    public void updateStatus(Long documentId, Integer status) {
        KnowledgeDocument document = new KnowledgeDocument();

        document.setId(documentId);

        document.setStatus(status);

        updateById(document);
    }
}
