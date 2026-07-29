package com.knowai.knowaibackend;

import com.knowai.knowaibackend.common.properties.OssProperties;
import lombok.extern.slf4j.Slf4j;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

@Slf4j
@SpringBootTest
public class TestOss {

    @Autowired
    private OssProperties ossProperties;

    @Test
    public void printOssConfig() {
        log.info("读取到的 bucketName：{}", ossProperties.getBucketName());
    }
}
