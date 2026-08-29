package com.knowai.knowaibackend.config;

import dev.langchain4j.community.model.dashscope.QwenChatModel;
import dev.langchain4j.model.chat.ChatModel;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class ChatModelConfig {

    @Bean
    public ChatModel chatModel(){
        String API_KEY = System.getenv("DASHSCOPE_API_KEY");

        if (API_KEY == null || API_KEY.isBlank()) {
            throw new RuntimeException("❌ 没有读到环境变量 DASHSCOPE_API_KEY！检查名字+重启IDEA");
        }

        return QwenChatModel.builder().apiKey(API_KEY).modelName("qwen-plus").build();

    }
}
