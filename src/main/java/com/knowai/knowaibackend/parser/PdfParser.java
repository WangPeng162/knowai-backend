package com.knowai.knowaibackend.parser;

import com.knowai.knowaibackend.domain.document.DocumentPage;

import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.text.PDFTextStripper;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;

@Component
public class PdfParser implements DocumentParser{

    @Override
    public List<DocumentPage> parse(InputStream inputStream) throws IOException {

        List<DocumentPage> pages = new ArrayList<>();

        try(PDDocument document = PDDocument.load(inputStream)){
            PDFTextStripper stripper = new PDFTextStripper();
            int pageCount = document.getNumberOfPages();

            for (int i = 1; i <= pageCount ; i++) {
                stripper.setStartPage(i);
                stripper.setEndPage(i);
                String content = stripper.getText(document);
                DocumentPage page = new DocumentPage();
                page.setPageNumber(i);
                page.setContent(content);
                pages.add(page);
            }
        }

        return pages;
    }

    @Override
    public String supportType() {
        return "pdf";
    }
}
