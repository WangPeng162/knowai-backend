package com.knowai.knowaibackend.vo.knowlegeDocument;

import lombok.Data;

import java.time.LocalDateTime;

@Data
public class DocumentDetailVO {
    private Long id;
    private String fileName;
    private String fileType;
    private Long fileSize;
    private Integer status;
    private LocalDateTime createTime;
    private LocalDateTime updateTime;
}
