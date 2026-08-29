package com.knowai.knowaibackend.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.spring.service.impl.ServiceImpl;
import com.knowai.knowaibackend.common.PageResult;
import com.knowai.knowaibackend.common.UserContext;
import com.knowai.knowaibackend.dto.knowledge.KnowledgeCreateDTO;
import com.knowai.knowaibackend.dto.knowledge.KnowledgeQueryDTO;
import com.knowai.knowaibackend.entity.KnowledgeBase;
import com.knowai.knowaibackend.exception.BusinessException;
import com.knowai.knowaibackend.mapper.KnowledgeMapper;
import com.knowai.knowaibackend.service.KnowledgeService;
import com.knowai.knowaibackend.vo.knowledge.KnowledgeListVO;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.util.StringUtils;

import java.time.LocalDateTime;
import java.util.List;

@Service
public class KnowledgeServiceImpl extends ServiceImpl<KnowledgeMapper, KnowledgeBase> implements KnowledgeService {

    @Autowired
    private KnowledgeMapper knowledgeMapper;

    private static final Logger log = LoggerFactory.getLogger(KnowledgeServiceImpl.class);

    @Override
    public boolean createKnowledge(KnowledgeCreateDTO dto) {
        //1.获取用户id
        Long userId = UserContext.getUserId();

        //2. 判断该用户是否已有同名知识库
        KnowledgeBase exist = knowledgeMapper.selectOne(
                new LambdaQueryWrapper<KnowledgeBase>()
                        .eq(KnowledgeBase::getUserId, userId)
                        .eq(KnowledgeBase::getName, dto.getName())
        );
        if (exist != null){
            throw new BusinessException("该名称知识库已存在");
        }


        //3.创建 KnowledgeBase 实体对象
        KnowledgeBase knowledge = new KnowledgeBase();
        BeanUtils.copyProperties(dto,knowledge);

        //4.设置userId
        knowledge.setUserId(userId);

        //5.创建时间和更新时间(这里创建更新时间是因为查询知识库列表的时候永远是最新时间)
        knowledge.setCreateTime(LocalDateTime.now());
        knowledge.setUpdateTime(LocalDateTime.now());

        //6.保存
        boolean success = this.save(knowledge);
        if (success){
            log.info("用户创建知识库成功：userId={}, name={}", knowledge.getUserId(),knowledge.getName());
        }
        return success;
    }

    @Override
    public PageResult<KnowledgeListVO> listKnowledge(KnowledgeQueryDTO dto) {
        //1. 创建分页对象（Page）
        Page<KnowledgeBase> page = new Page<>(dto.getPageNum(), dto.getPageSize());

        //2.创建查询条件
        LambdaQueryWrapper<KnowledgeBase> wrapper = new LambdaQueryWrapper<KnowledgeBase>()
                .eq(KnowledgeBase::getUserId, UserContext.getUserId())
                .like(StringUtils.hasText(dto.getKeyword()),KnowledgeBase::getName, dto.getKeyword())
                .orderByDesc(KnowledgeBase::getUpdateTime);

        //2. 执行分页查询

        baseMapper.selectPage(page,wrapper);

        //3. Entity 转 VO
        List<KnowledgeListVO> voList = page.getRecords().stream().map(
                knowledge ->{
                    KnowledgeListVO vo = new KnowledgeListVO();
                    BeanUtils.copyProperties(knowledge,vo);
                    return vo;
                }
        ).toList();


        //4. 封装 PageResult
        return new PageResult<>(page.getTotal(), voList);
    }
}
