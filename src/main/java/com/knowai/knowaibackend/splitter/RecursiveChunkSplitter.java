package com.knowai.knowaibackend.splitter;

import com.knowai.knowaibackend.domain.document.ChunkData;
import com.knowai.knowaibackend.domain.document.DocumentPage;
import dev.langchain4j.data.document.Document;
import dev.langchain4j.data.document.DocumentSplitter;
import dev.langchain4j.data.document.splitter.DocumentSplitters;
import dev.langchain4j.data.segment.TextSegment;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;

/**
 * 递归切分器（v2）：手写 800 字符硬切 → LangChain4j 官方递归切分
 *
 * 为什么换：
 *  - v1 是"固定 800 字符一刀"，完全不看文本语义 → 句子被腰斩成"师578人"这种残块
 *    （chunk 里"教职工总数1506"和"高级职称578"被劈到两块，embedding 语义残缺）
 *  - v2 用 DocumentSplitters.recursive：按 段落 → 句子 → 词 → 字符 的层级找语义边界再下刀，
 *    只在"整个单元放不下"时才继续细分，chunk 边界尽量落在句号/换行等完整处
 *  - overlap 也由框架处理（按语义边界重叠，替代 v1 的固定 100 字符硬叠）
 *
 * 为什么每页独立切：
 *  - DocumentPage 带 pageNumber，每页独立切保证 chunk 的 pageNumber 归属正确
 *  - PDF 的分页符本来就是物理断点，跨页句子本就不该拼回一个 chunk
 */
@Component
public class RecursiveChunkSplitter implements ChunkSplitter {

    /** 目标 chunk 大小（字符），与原配置保持一致，方便横向对比 */
    private static final int CHUNK_SIZE = 800;
    /** 相邻 chunk 重叠量（字符），保证被边界切开的语义不会丢上下文 */
    private static final int OVERLAP = 100;

    /** LangChain4j 官方递归切分器（线程安全，可复用单例） */
    private final DocumentSplitter delegate = DocumentSplitters.recursive(CHUNK_SIZE, OVERLAP);

    @Override
    public List<ChunkData> split(List<DocumentPage> pages) {
        List<ChunkData> chunks = new ArrayList<>();
        int index = 0;

        for (DocumentPage page : pages) {
            // 1. 把纯文本包成 LangChain4j 的 Document
            Document document = Document.from(page.getContent());

            // 2. 交给官方递归切分器 → 得到语义边界完整的 TextSegment 列表
            List<TextSegment> segments = delegate.split(document);

            // 3. TextSegment → 项目自己的 ChunkData（保留 chunk_index 全局自增 + pageNumber 归属）
            for (TextSegment segment : segments) {
                ChunkData chunk = new ChunkData();
                chunk.setChunkIndex(index++);
                chunk.setPageNumber(page.getPageNumber());
                chunk.setContent(segment.text());
                chunks.add(chunk);
            }
        }

        return chunks;
    }
}
