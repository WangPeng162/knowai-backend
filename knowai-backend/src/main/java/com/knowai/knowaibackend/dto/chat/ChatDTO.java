package com.knowai.knowaibackend.dto.chat;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

@Data
public class ChatDTO {

    @NotBlank(message = "问题不能为空")
    private String question;

    private Long knowledgeId;

    private String sessionId;
}
