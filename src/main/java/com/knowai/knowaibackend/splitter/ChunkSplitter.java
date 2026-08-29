package com.knowai.knowaibackend.splitter;

import com.knowai.knowaibackend.domain.document.ChunkData;
import com.knowai.knowaibackend.domain.document.DocumentPage;

import java.util.List;

public interface ChunkSplitter {
    List<ChunkData>split(List<DocumentPage> pages);
}
