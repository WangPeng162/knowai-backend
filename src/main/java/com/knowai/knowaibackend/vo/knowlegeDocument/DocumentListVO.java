package com.knowai.knowaibackend.vo.knowlegeDocument;

import lombok.Data;

import java.time.LocalDateTime;

@Data
public class DocumentListVO {
    private Long id;
    private String fileName;
    private String fileSuffix;
    private Long mimeType;
    private Integer status;
    private LocalDateTime createTime;
}
