package com.knowai.knowaibackend.vo.chat;

import lombok.Data;

@Data
public class ReferenceVO {
    private Long documentId;      // 文档 id
    private String documentName;  // 文档名（fileName）
    private Integer pageNumber;   // 页码
    private Double score;         // 相似度分数
}
