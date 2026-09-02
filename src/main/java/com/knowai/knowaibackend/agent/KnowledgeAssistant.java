package com.knowai.knowaibackend.agent;

import dev.langchain4j.service.SystemMessage;

public interface KnowledgeAssistant {

    @SystemMessage("你是一个知识库助手。\" +\n" +
            "        \"凡是涉及具体学校、人物、事件、数据、定义等需要准确信息的，\" +\n" +
            "        \"**必须**先调用 searchKnowledge 工具查询。\" +\n" +
            "        \"如果工具返回'资料中没有相关信息'，再告诉用户查不到。\" +\n" +
            "        \"绝对不要凭自己记忆回答事实性问题！")
    String chat(String userMessage);

}
