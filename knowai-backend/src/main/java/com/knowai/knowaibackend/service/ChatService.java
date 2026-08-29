package com.knowai.knowaibackend.service;

import com.knowai.knowaibackend.vo.chat.ChatResultVO;

public interface ChatService {
    ChatResultVO ask(String question, Long knowledgeId,String sessionId);
}
