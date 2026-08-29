package com.knowai.knowaibackend.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;
import java.time.LocalDateTime;

/**
 * 知识库文件表
 */
@Data
@TableName("knowledge_document")
public class KnowledgeDocument {

    /**
     * 主键ID
     */
    @TableId(type = IdType.AUTO)
    private Long id;

    /**
     * 关联知识库id
     */
    private Long knowledgeId;

    /**
     * 文件原始名称
     */
    private String fileName;

    /**
     * 文件存储路径/OSS地址
     */
    private String filePath;

    /**
     * 文件后缀
     */
    private String fileSuffix;

    /**
     * 文件类型（pdf/txt/docx等）
     */
    private String mimeType;

    /**
     * 文件大小，单位字节
     */
    private Long fileSize;

    /**
     * 状态：0待解析，1解析中，2解析成功，3解析失败
     */
    private Integer status;

    /**
     * 逻辑删除：0正常，1已删除
     */
    @TableLogic(value = "0", delval = "1")
    private Integer isDeleted;

    /**
     * 创建时间
     */
    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createTime;

    /**
     * 更新时间
     */
    @TableField(fill = FieldFill.INSERT_UPDATE)
    private LocalDateTime updateTime;

    public static final int STATUS_WAIT = 0;

    public static final int STATUS_PARSING = 1;

    public static final int STATUS_SUCCESS = 2;

    public static final int STATUS_FAIL = 3;
}
