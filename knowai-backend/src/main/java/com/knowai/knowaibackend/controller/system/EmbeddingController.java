package com.knowai.knowaibackend.controller.system;

import com.knowai.knowaibackend.service.EmbeddingService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/embedding")
@RequiredArgsConstructor
public class EmbeddingController {
    private final EmbeddingService embeddingService;

    @PostMapping("/{documentId}")
    public void generateEmbedding(@PathVariable Long documentId){
        embeddingService.generateEmbedding(documentId);
    }
}
