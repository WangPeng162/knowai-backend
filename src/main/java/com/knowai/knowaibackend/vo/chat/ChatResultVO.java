package com.knowai.knowaibackend.vo.chat;

import lombok.Data;
import java.util.List;

@Data
public class ChatResultVO {
    private String answer;                  // 模型生成的回答
    private List<ReferenceVO> references;   // 引用来源列表
}