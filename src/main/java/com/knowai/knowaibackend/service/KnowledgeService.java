package com.knowai.knowaibackend.service;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.spring.service.IService;
import com.knowai.knowaibackend.common.PageResult;
import com.knowai.knowaibackend.dto.knowledge.KnowledgeCreateDTO;
import com.knowai.knowaibackend.dto.knowledge.KnowledgeQueryDTO;
import com.knowai.knowaibackend.entity.KnowledgeBase;
import com.knowai.knowaibackend.vo.knowledge.KnowledgeListVO;

@SuppressWarnings("all")
public interface KnowledgeService extends IService<KnowledgeBase> {

    boolean createKnowledge(KnowledgeCreateDTO dto);

    PageResult<KnowledgeListVO> listKnowledge(KnowledgeQueryDTO dto);
}
