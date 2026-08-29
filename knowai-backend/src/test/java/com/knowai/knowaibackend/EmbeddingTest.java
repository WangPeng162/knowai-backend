package com.knowai.knowaibackend;

import com.knowai.knowaibackend.entity.KnowledgeEmbedding;
import com.knowai.knowaibackend.service.ChatService;
import com.knowai.knowaibackend.service.KnowledgeEmbeddingService;
import dev.langchain4j.community.model.dashscope.QwenEmbeddingModel;
import dev.langchain4j.data.embedding.Embedding;
import dev.langchain4j.data.segment.TextSegment;
import dev.langchain4j.model.output.Response;
import dev.langchain4j.store.embedding.EmbeddingMatch;
import dev.langchain4j.store.embedding.EmbeddingSearchRequest;
import dev.langchain4j.store.embedding.EmbeddingSearchResult;
import dev.langchain4j.store.embedding.EmbeddingStore;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import java.util.List;

@SpringBootTest
public class EmbeddingTest {

    @Autowired
    private EmbeddingStore<TextSegment> embeddingStore;

    @Autowired
    private KnowledgeEmbeddingService knowledgeEmbeddingService;

    @Autowired
    private ChatService chatService;

    @Test
    public void testQdrantSearch(){

        String API_KEY = System.getenv("DASHSCOPE_API_KEY");

        if (API_KEY == null || API_KEY.isBlank()) {
            throw new RuntimeException("❌ 没有读到环境变量 DASHSCOPE_API_KEY！检查名字+重启IDEA");
        }

        QwenEmbeddingModel embeddingModel = QwenEmbeddingModel.builder()
                .apiKey(API_KEY)
                .modelName("text-embedding-v4")
                .build();

        List<TextSegment> segments = List.of(
                TextSegment.from("Redis 是一个高性能的内存数据库"),
                TextSegment.from("Spring Boot 简化了 Java 应用开发"),
                TextSegment.from("向量数据库用于相似度检索")
        );

        Response<List<Embedding>> batchResponse = embeddingModel.embedAll(segments);
        List<Embedding> embeddings = batchResponse.content();

        List<String> vectorIds = embeddingStore.addAll(embeddings, segments);
        System.out.println("批量入库完成，共"+segments.size()+"条Chunk\n");

        //插入knowledge_embedding
        for (int i = 0; i < segments.size(); i++) {
            KnowledgeEmbedding knowledgeEmbedding = new KnowledgeEmbedding();
            //先随机id做过测试先
            long randomChunkId = System.currentTimeMillis() + i;
            knowledgeEmbedding.setChunkId(randomChunkId);
            knowledgeEmbedding.setVectorId(vectorIds.get(i));
            knowledgeEmbedding.setDimension(embeddings.get(i).dimension());
            knowledgeEmbedding.setStatus(2);
            knowledgeEmbeddingService.save(knowledgeEmbedding);
        }

        for (String vectorId : vectorIds) {
            System.out.println("vectorId="+vectorId);
        }

        //调用ChatService的ask方法
        String userQuery = "Redis 是干什么的？";
        //String resultContext = chatService.ask(userQuery,1L);

        System.out.println("用户提问："+ userQuery);
        //System.out.println(resultContext);

//        String userQuery = "Redis 是干什么的？";
//        Embedding queryEmbedding = embeddingModel.embed(userQuery).content();
//
//        EmbeddingSearchRequest searchRequest = EmbeddingSearchRequest.builder().queryEmbedding(queryEmbedding)
//                .maxResults(2)
//                .build();
//
//        EmbeddingSearchResult<TextSegment> searchResult = embeddingStore.search(searchRequest);
//        List<EmbeddingMatch<TextSegment>> matchList = searchResult.matches();
//
//        System.out.println("🔍 用户提问：" + userQuery);
//        System.out.println("-------- 相似度检索结果 --------");
//        for (EmbeddingMatch<TextSegment> match : matchList) {
//            System.out.printf("相似度分数：%.4f%n", match.score());
//            System.out.println("原文Chunk：" + match.embedded().text());
//            System.out.println("--------------------------------");
        //}
    }
}
