package com.knowai.knowaibackend.controller.system;

import com.knowai.knowaibackend.common.Result;
import com.knowai.knowaibackend.utils.OssUtil;
import io.swagger.v3.oas.annotations.Operation;
import lombok.RequiredArgsConstructor;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;

@RestController
@RequestMapping("/upload")
@RequiredArgsConstructor
public class UploadController {

    private final OssUtil ossUtil;
    @Operation(summary = "文件上传")
    @PostMapping(consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public Result<String> upload(@RequestParam("file")MultipartFile file) throws IOException{

        String url = ossUtil.upload(file,"avatar");
        return Result.success(url);
    }
}
