package com.knowai.knowaibackend.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;
import java.time.LocalDateTime;

/**
 * 知识库向量嵌入表 knowledge_embedding
 */
@Data
@TableName("knowledge_embedding")
public class KnowledgeEmbedding {

    /**
     * 主键id
     */
    @TableId(value = "id", type = IdType.AUTO)
    private Long id;

    /**
     * 分片块id，关联knowledge_chunk表
     */
    @TableField("chunk_id")
    private Long chunkId;

    /**
     * 向量库id（Qdrant等向量数据库返回的id）
     */
    @TableField("vector_id")
    private String vectorId;

    /**
     * 向量维度
     */
    @TableField("dimension")
    private Integer dimension;

    /**
     * 状态：0待生成，1生成中，2生成成功，3生成失败
     */
    @TableField("status")
    private Integer status;

    /**
     * 创建时间
     */
    @TableField(value = "create_time", fill = FieldFill.INSERT)
    private LocalDateTime createTime;

    /**
     * 更新时间
     */
    @TableField(value = "update_time", fill = FieldFill.INSERT_UPDATE)
    private LocalDateTime updateTime;
}