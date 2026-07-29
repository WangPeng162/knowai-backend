package com.knowai.knowaibackend.controller.system;


import com.baomidou.mybatisplus.core.metadata.IPage;
import com.knowai.knowaibackend.common.PageResult;
import com.knowai.knowaibackend.common.Result;
import com.knowai.knowaibackend.dto.knowledge.KnowledgeCreateDTO;
import com.knowai.knowaibackend.dto.knowledge.KnowledgeQueryDTO;
import com.knowai.knowaibackend.service.KnowledgeService;
import com.knowai.knowaibackend.vo.knowledge.KnowledgeListVO;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import jakarta.validation.Valid;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/knowledge")
public class KnowledgeController {

    @Autowired
    private KnowledgeService knowledgeService;

    @Operation(summary = "创建知识库")
    @PostMapping()
    public Result<String> createKnowledge(@Valid @RequestBody KnowledgeCreateDTO dto){
        boolean success = knowledgeService.createKnowledge(dto);
        return success ? Result.success("创建成功") : Result.fail("创建失败");
    }

    @Operation(summary = "获取知识库列表")
    @GetMapping("/list")
    public Result<PageResult<KnowledgeListVO>> page(KnowledgeQueryDTO dto){
        return Result.success(knowledgeService.listKnowledge(dto));
    }
}
