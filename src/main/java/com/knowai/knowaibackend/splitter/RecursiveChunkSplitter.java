package com.knowai.knowaibackend.splitter;

import com.knowai.knowaibackend.domain.document.ChunkData;
import com.knowai.knowaibackend.domain.document.DocumentPage;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;


@Component
public class RecursiveChunkSplitter implements ChunkSplitter {
    private static final int CHUNK_SIZE = 800;
    private static final int OVERLAP = 100;

    @Override
    public List<ChunkData> split(List<DocumentPage> pages) {

        List<ChunkData> chunks = new ArrayList<>();
        int index = 0;

        for (DocumentPage page : pages) {
            List<String> texts = splitText(page.getContent());

            for (String text : texts) {
                ChunkData chunk = new ChunkData();
                chunk.setChunkIndex(index++);
                chunk.setPageNumber(page.getPageNumber());
                chunk.setContent(text);
                chunks.add(chunk);
            }
        }

        return chunks;
    }

    private List<String> splitText(String text){

        List<String> result = new ArrayList<>();
        int start = 0;
        while (start < text.length()){
            int end = Math.min(start + CHUNK_SIZE,text.length());
            String chunk = text.substring(start,end);
            result.add(chunk);
            start = end - OVERLAP;

            if (start < 0){
                start = 0;
            }

            if (end == text.length()){
                break;
            }
        }
        return result;

    }
}
