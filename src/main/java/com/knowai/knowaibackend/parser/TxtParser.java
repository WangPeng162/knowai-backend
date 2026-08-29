package com.knowai.knowaibackend.parser;

import com.knowai.knowaibackend.domain.document.DocumentPage;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.List;

@Component
public class TxtParser implements DocumentParser{
    @Override
    public List<DocumentPage> parse(InputStream inputStream) throws IOException {

        //1.读取文件流
        String text = new String(inputStream.readAllBytes(), StandardCharsets.UTF_8);

        //2.封装DocumentPage
        DocumentPage page = new DocumentPage();
        page.setPageNumber(null);
        page.setContent(text);

        //3.返回
        return List.of(page);
    }

    @Override
    public String supportType() {
        return "txt";
    }
}
