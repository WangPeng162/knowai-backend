package com.knowai.knowaibackend.controller.system;

import com.knowai.knowaibackend.common.Result;
import com.knowai.knowaibackend.dto.chat.ChatDTO;
import com.knowai.knowaibackend.service.ChatService;
import com.knowai.knowaibackend.vo.chat.ChatResultVO;
import io.swagger.v3.oas.annotations.Operation;
import jakarta.validation.Valid;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/chat")
public class ChatController {
    @Autowired
    private ChatService chatService;

    @Operation(summary = "对话接口")
    @PostMapping
    public Result<ChatResultVO> ask(@Valid @RequestBody ChatDTO dto) {
        return Result.success(chatService.ask(dto.getQuestion(),dto.getKnowledgeId(),dto.getSessionId()));
    }
}
