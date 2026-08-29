package com.knowai.knowaibackend.domain.document;

import lombok.Data;

@Data
public class ChunkData {
    /**
     * 文档内分片序号
     */
    private Integer chunkIndex;


    /**
     * 来源页码
     */
    private Integer pageNumber;


    /**
     * 分片内容
     */
    private String content;
}
