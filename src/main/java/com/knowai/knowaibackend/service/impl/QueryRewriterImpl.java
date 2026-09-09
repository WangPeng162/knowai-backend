package com.knowai.knowaibackend.service.impl;

import com.knowai.knowaibackend.service.QueryRewriter;
import dev.langchain4j.data.message.ChatMessage;
import dev.langchain4j.data.message.SystemMessage;
import dev.langchain4j.data.message.UserMessage;
import dev.langchain4j.model.chat.ChatModel;
import dev.langchain4j.model.chat.response.ChatResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;

/**
 * 查询改写器（多轮对话指代消解）
 *
 * 设计要点：
 *  - 输入：对话历史 context + 当前问题 question（可能含"它/这个"等指代）
 *  - 输出：一句"可独立检索"的完整问题，丢给向量数据库
 *  - 改写只影响"检索"，不替换用户对话内容，Agent 生成仍看原 question
 *
 * 防呆：
 *  - chatModel 异常 → fallback 原 question（绝不让问答链路挂掉）
 *  - 改写返回空白 → fallback 原 question
 */
@Service
@RequiredArgsConstructor
public class QueryRewriterImpl implements QueryRewriter {

    private final ChatModel chatModel;

    private static final String SYSTEM_PROMPT = """
            你是查询改写器。把"当前问题"结合"对话历史"改写成一句"可独立检索的完整问题"，供向量数据库检索使用。
            规则：
            1. 含"它/这个/那个/这所学校/刚才说的"等指代 → 用历史里的具体实体替换（如"它"→"广西农业职业技术大学"）
            2. 问题本身已经完整无指代 → 原样返回，禁止改动
            3. 只输出改写后的文本，不要解释、不要引号、不要前缀
            4. 绝不添加历史里不存在的信息（防幻觉）
            """;

    @Override
    public String rewrite(String context, String question) {
        try {
            String userMsg = "对话历史：" + (context == null ? "" : context)
                    + "\n当前问题：" + question;

            List<ChatMessage> messages = List.of(
                    SystemMessage.from(SYSTEM_PROMPT),
                    UserMessage.from(userMsg)
            );

            ChatResponse response = chatModel.chat(messages);
            String rewritten = response.aiMessage().text().trim();

            // 防御性 fallback：改写器返回空白 → 不传改写结果
            return rewritten.isEmpty() ? question : rewritten;
        } catch (Exception e) {
            // 改写器是辅助层，失败必须降级，绝不能让问答链路挂掉
            return question;
        }
    }
}
