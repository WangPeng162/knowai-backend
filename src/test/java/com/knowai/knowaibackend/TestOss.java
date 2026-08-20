package com.knowai.knowaibackend;

import com.knowai.knowaibackend.common.properties.OssProperties;
import com.knowai.knowaibackend.domain.document.ChunkData;
import com.knowai.knowaibackend.domain.document.DocumentPage;
import com.knowai.knowaibackend.parser.PdfParser;
import com.knowai.knowaibackend.parser.TxtParser;
import com.knowai.knowaibackend.splitter.RecursiveChunkSplitter;
import lombok.extern.slf4j.Slf4j;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import java.io.FileInputStream;
import java.util.List;

@Slf4j
@SpringBootTest
public class TestOss {

    @Autowired
    private OssProperties ossProperties;


    @Test
    public void printOssConfig() {
        log.info("读取到的 bucketName：{}", ossProperties.getBucketName());
    }


    @Test
    void testTxtParser() throws Exception {


        FileInputStream inputStream =
                new FileInputStream("D:/test.txt");

        TxtParser txtParser = new TxtParser();
        List<DocumentPage> pages = txtParser.parse(inputStream);


        System.out.println(pages.get(0).getContent());

    }

    @Test
    void testPdfParser() throws Exception {

        FileInputStream inputStream = new FileInputStream("D:/Redis.pdf");

        PdfParser pdfParser = new PdfParser();

        List<DocumentPage> pages = pdfParser.parse(inputStream);

        System.out.println("PDF页数:" + pages.size());

        for(DocumentPage page : pages){

            System.out.println("----------------");

            System.out.println("当前页:" + page.getPageNumber());

            System.out.println(page.getContent());
        }

        System.out.println("------------------------------------");

        RecursiveChunkSplitter splitter = new RecursiveChunkSplitter();

        List<ChunkData> chunks = splitter.split(pages);

        for(ChunkData chunk:chunks){

            System.out.println("index:" +chunk.getChunkIndex());
            System.out.println("==================");
            System.out.println("page:" +chunk.getPageNumber());
            System.out.println(chunk.getContent());

        }
    }
}
