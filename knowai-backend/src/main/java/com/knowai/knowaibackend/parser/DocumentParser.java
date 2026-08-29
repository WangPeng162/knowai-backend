package com.knowai.knowaibackend.parser;

import com.knowai.knowaibackend.domain.document.DocumentPage;

import java.io.IOException;
import java.io.InputStream;
import java.util.List;

public interface DocumentParser {


    /**
     *
     * @param inputStream 文件输入流
     * @return 文档页面内容
     * @throws IOException
     */
    List<DocumentPage> parse(InputStream inputStream) throws IOException;

    String supportType();


}
