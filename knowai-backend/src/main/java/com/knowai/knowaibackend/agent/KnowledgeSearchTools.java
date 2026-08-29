package com.knowai.knowaibackend.agent;

import dev.langchain4j.agent.tool.P;
import dev.langchain4j.agent.tool.Tool;
import dev.langchain4j.data.embedding.Embedding;
import dev.langchain4j.data.segment.TextSegment;
import dev.langchain4j.model.embedding.EmbeddingModel;
import dev.langchain4j.model.output.Response;
import dev.langchain4j.store.embedding.EmbeddingMatch;
import dev.langchain4j.store.embedding.EmbeddingSearchRequest;
import dev.langchain4j.store.embedding.EmbeddingSearchResult;
import dev.langchain4j.store.embedding.EmbeddingStore;
import lombok.RequiredArgsConstructor;

import java.util.ArrayList;
import java.util.List;

import static dev.langchain4j.store.embedding.filter.MetadataFilterBuilder.metadataKey;

@SuppressWarnings("all")
@RequiredArgsConstructor
public class KnowledgeSearchTools {

    private final EmbeddingModel embeddingModel;
    private final EmbeddingStore<TextSegment> embeddingStore;
    private final Long knowledgeId;
    // 存本次检索结果，供外部组装 references 用
    private final List<EmbeddingMatch<TextSegment>> matches = new ArrayList<>();

    @Tool("在知识库中检索与问题相关的文档片段，返回片段内容")
    public String searchKnowledge(@P("要检索的问题")String query){
        // 1. 向量化 query
        Response<Embedding> embed = embeddingModel.embed(query);
        Embedding content = embed.content();

        // 2. Qdrant 检索
        EmbeddingSearchRequest.EmbeddingSearchRequestBuilder builder =
                EmbeddingSearchRequest.builder()
                        .queryEmbedding(content)
                        .maxResults(2);
        if (knowledgeId != null && knowledgeId > 0) {
            builder.filter(metadataKey("knowledgeId").isEqualTo(knowledgeId));
        }
        EmbeddingSearchRequest searchRequest = builder.build();
        EmbeddingSearchResult<TextSegment> searchResult = embeddingStore.search(searchRequest);

        // 3. 把 matchList 存进 this.matches
        List<EmbeddingMatch<TextSegment>> matchList = searchResult.matches();
        matches.addAll(matchList);

        // 4. 拼 context 字符串返回（搬第4步）
        StringBuilder context = new StringBuilder();
        for (int i = 0; i < matchList.size(); i++) {
            context.append("[").append(i + 1).append("]")
                    .append(matchList.get(i).embedded().text())
                    .append("\n\n");
        }

        return context.toString();
    }
    // 供 ask() 取出检索结果组装 references
    public List<EmbeddingMatch<TextSegment>> getMatches() {
        return matches;
    }
}
