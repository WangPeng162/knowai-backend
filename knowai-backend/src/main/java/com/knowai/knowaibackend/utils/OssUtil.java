package com.knowai.knowaibackend.utils;

import com.aliyun.oss.OSS;
import com.aliyun.oss.OSSClientBuilder;
import com.knowai.knowaibackend.common.properties.OssProperties;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.io.InputStream;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.UUID;

@Component
@RequiredArgsConstructor
public class OssUtil {
    private final OssProperties ossProperties;

    public String upload(MultipartFile file, String folder) throws IOException {
        OSS ossClient  = new OSSClientBuilder().build(
                ossProperties.getEndpoint(), ossProperties.getAccessKeyId()
                , ossProperties.getAccessKeySecret()
        );

        try {
            //获取原始文件名
            String originalFilename = file.getOriginalFilename();
            if (originalFilename == null || originalFilename.isBlank()){
                throw new IOException("文件名不能为空");
            }
            //获取后缀
            int index = originalFilename.lastIndexOf(".");
            String suffix = index == -1 ? "" : originalFilename.substring(index);
            //生成UUID
            String uuid = UUID.randomUUID().toString().replace("-","");
            //生成日期目录
            String datePath = LocalDate.now().format(DateTimeFormatter.ofPattern("yyyy/MM/dd"));
            //拼接objectName
            String objectName = folder + "/" + datePath + "/" + uuid + suffix;

            //上传图片
            ossClient.putObject(
                    ossProperties.getBucketName(),objectName,file.getInputStream());

            //返回url
            String url =  "https://" + ossProperties.getBucketName() + "."
                    + ossProperties.getEndpoint() + "/" + objectName;
            return url;
        } finally {
            //资源关闭
            ossClient.shutdown();
        }
    }

    /**
     * 从OSS下载文件，返回输入流
     * @param url OSS文件的完整URL
     * @return 文件输入流
     */
    public InputStream download(String url) throws IOException {
        OSS ossClient = new OSSClientBuilder().build(
                ossProperties.getEndpoint(), ossProperties.getAccessKeyId(),
                ossProperties.getAccessKeySecret()
        );

        // 从URL中提取objectKey
        // URL格式: https://{bucketName}.{endpoint}/{objectKey}
        String prefix = "https://" + ossProperties.getBucketName() + "." + ossProperties.getEndpoint() + "/";
        if (!url.startsWith(prefix)) {
            ossClient.shutdown();
            throw new IOException("URL不匹配当前OSS配置: " + url);
        }
        String objectKey = url.substring(prefix.length());

        // 返回包装流，关闭时同时关闭OSS客户端
        InputStream rawStream = ossClient.getObject(ossProperties.getBucketName(), objectKey).getObjectContent();
        return new InputStream() {
            @Override
            public int read() throws IOException {
                return rawStream.read();
            }

            @Override
            public int read(byte[] b, int off, int len) throws IOException {
                return rawStream.read(b, off, len);
            }

            @Override
            public void close() throws IOException {
                try {
                    rawStream.close();
                } finally {
                    ossClient.shutdown();
                }
            }
        };
    }
}
