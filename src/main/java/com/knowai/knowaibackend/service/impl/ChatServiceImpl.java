package com.knowai.knowaibackend.service.impl;

import com.knowai.knowaibackend.agent.KnowledgeAssistant;
import com.knowai.knowaibackend.agent.KnowledgeSearchTools;
import com.knowai.knowaibackend.entity.KnowledgeDocument;
import com.knowai.knowaibackend.service.ChatService;
import com.knowai.knowaibackend.service.KnowledgeDocumentService;
import com.knowai.knowaibackend.vo.chat.ChatResultVO;
import com.knowai.knowaibackend.vo.chat.ReferenceVO;
import dev.langchain4j.data.document.Metadata;
import dev.langchain4j.data.segment.TextSegment;
import dev.langchain4j.memory.ChatMemory;
import dev.langchain4j.memory.chat.MessageWindowChatMemory;
import dev.langchain4j.model.chat.ChatModel;
import dev.langchain4j.model.embedding.EmbeddingModel;
import dev.langchain4j.service.AiServices;
import dev.langchain4j.store.embedding.EmbeddingMatch;
import dev.langchain4j.store.embedding.EmbeddingStore;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;



@RequiredArgsConstructor
@Service
public class ChatServiceImpl implements ChatService {
    private final KnowledgeDocumentService knowledgeDocumentService;
    private final ChatModel chatModel;
    private final EmbeddingStore<TextSegment> embeddingStore;
    private final EmbeddingModel embeddingModel;
    private final ConcurrentHashMap<String, ChatMemory> sessionMemories =new ConcurrentHashMap<>();

    @Override
    public ChatResultVO ask(String question, Long knowledgeId,String sessionId) {
        //1.拿当前sessionId的memory（没有就创建一个）
        //（1）判断sessionId
        if (sessionId == null || sessionId.isEmpty()) {
            sessionId = "default-session"; // 兜底，所有不传 sessionId 的请求共用一个 memory
        }

        //（2）拿到sessionId的memory，没有就创建
        ChatMemory chatMemory = sessionMemories.computeIfAbsent(
                sessionId, k -> MessageWindowChatMemory.withMaxMessages(10));

        //2.构造工具（每次请求 new，knowledgeId 不同）
        KnowledgeSearchTools tools = new KnowledgeSearchTools(embeddingModel, embeddingStore, knowledgeId);

        //3.用 AiServices 构建 Agent
        KnowledgeAssistant assistant = AiServices.builder(KnowledgeAssistant.class)
                .chatModel(chatModel).tools(tools).chatMemory(chatMemory).build();

        // 4. 一行调用，Agent 内部自动处理"模型决定要不要调工具 → 调 → 拿结果 → 再生成"
        String answer = assistant.chat(question);

        //5.收集references(matchList 来源从 ask 里的检索结果改成 tools.getMatches())
        List<EmbeddingMatch<TextSegment>> matchList = tools.getMatches();
        List<ReferenceVO> references = new ArrayList<>();
        if (!matchList.isEmpty()) {
            //（1） 从matchList提取去重后的documentId
            Set<Long> documentIds = matchList.stream()
                    .map(m -> m.embedded().metadata().getLong("documentId"))
                    .collect(Collectors.toSet());

            //（2）批量查文档名（一次IN SQL，去重后查询）
            List<KnowledgeDocument> documents = knowledgeDocumentService.listByIds(documentIds);

            //（3）建 Map<documentId, fileName>，避免循环查
            Map<Long, String> nameMap = documents.stream().collect(Collectors.toMap(
                    KnowledgeDocument::getId, KnowledgeDocument::getFileName
            ));

            //（4）组装references列表
            for (EmbeddingMatch<TextSegment> match : matchList) {
                Metadata metadata = match.embedded().metadata();
                Long documentId = metadata.getLong("documentId");

                ReferenceVO ref = new ReferenceVO();
                ref.setDocumentId(documentId);
                ref.setDocumentName(nameMap.get(documentId));
                ref.setPageNumber(metadata.getInteger("pageNumber"));
                ref.setScore(match.score());
                references.add(ref);
            }
        }

        //6.组装返回对象
        ChatResultVO result = new ChatResultVO();
        result.setAnswer(answer);
        result.setReferences(references);

        return result;
    }
}
