package com.knowai.knowaibackend.dto.knowledge;

import lombok.Data;

@Data
public class KnowledgeQueryDTO {
    /**
     * 当前页
     */
    private Integer pageNum;

    /**
     * 每页大小
     */
    private Integer pageSize;

    /**
     * 搜索关键字（知识库名称）
     */
    private String keyword;
}
