package com.knowai.knowaibackend.controller.system;


import com.knowai.knowaibackend.service.DocumentParseService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/document/parse")
@RequiredArgsConstructor
public class DocumentParseController {

    private final DocumentParseService documentParseService;


    @PostMapping("/{documentId}")
    public void parse(@PathVariable Long documentId){

        documentParseService.parseDocument(documentId);

    }
}
