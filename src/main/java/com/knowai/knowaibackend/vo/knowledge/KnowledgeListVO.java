package com.knowai.knowaibackend.vo.knowledge;

import lombok.Data;

import java.time.LocalDateTime;

@Data
public class KnowledgeListVO {

    private Long id;

    private String name;

    private String description;

    private LocalDateTime updateTime;
}
