package com.knowai.knowaibackend.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.FieldFill;
import com.baomidou.mybatisplus.annotation.TableLogic;
import lombok.Data;
import java.time.LocalDateTime;

/**
 * 文档分片表
 */
@Data
@TableName("knowledge_chunk")
public class KnowledgeChunk {

    /**
     * 主键ID
     */
    @TableId(type = IdType.AUTO)
    private Long id;

    /**
     * 关联文档主表id
     */
    @TableField("document_id")
    private Long documentId;

    /**
     * 分片内容，支持Markdown格式
     */
    @TableField("content")
    private String content;

    /**
     * 文档内分片序号，从0开始
     */
    @TableField("chunk_index")
    private Integer chunkIndex;

    /**
     * PDF文档页码
     */
    @TableField("page_number")
    private Integer pageNumber;

    /**
     * 状态：0待向量化，1向量生成中，2向量生成成功，3向量生成失败
     */
    @TableField("status")
    private Integer status;

    /**
     * 扩展元数据JSON
     */
    @TableField("metadata")
    private String metadata;

    /**
     * 创建时间（自动填充）
     */
    @TableField(value = "create_time", fill = FieldFill.INSERT)
    private LocalDateTime createTime;

    /**
     * 更新时间（新增+更新自动填充）
     */
    @TableField(value = "update_time", fill = FieldFill.INSERT_UPDATE)
    private LocalDateTime updateTime;

    /**
     * 状态常量
     */
    public static final int STATUS_WAIT_EMBEDDING = 0;

    public static final int STATUS_EMBEDDING = 1;

    public static final int STATUS_SUCCESS = 2;

    public static final int STATUS_FAIL = 3;
}
