package com.knowai.knowaibackend.config;

import dev.langchain4j.data.message.ChatMessage;
import dev.langchain4j.data.message.ChatMessageDeserializer;
import dev.langchain4j.data.message.ChatMessageSerializer;
import dev.langchain4j.store.memory.chat.ChatMemoryStore;
import lombok.RequiredArgsConstructor;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Component;

import java.time.Duration;
import java.util.ArrayList;
import java.util.List;


@RequiredArgsConstructor
@Component
public class RedisChatMemoryStore implements ChatMemoryStore {

    private static final String KEY_PREFIX = "knowai:chat:memory:";

    private static final Duration TTL = Duration.ofDays(1);

    private final StringRedisTemplate redisTemplate;

    @Override
    public List<ChatMessage> getMessages(Object memoryId) {
        String key = KEY_PREFIX + memoryId;
        String json = redisTemplate.opsForValue().get(key);
        if (json == null || json.isBlank()) {
            return new ArrayList<>();
        }
        return ChatMessageDeserializer.messagesFromJson(json);
    }

    @Override
    public void updateMessages(Object memoryId, List<ChatMessage> messages) {
        String json = ChatMessageSerializer.messagesToJson(messages);
        String key = KEY_PREFIX + memoryId;
        redisTemplate.opsForValue().set(key, json,TTL);
    }

    @Override
    public void deleteMessages(Object o) {
        redisTemplate.delete(KEY_PREFIX + o);
    }
}
