package com.knowai.knowaibackend.common;

import java.util.Arrays;
import java.util.List;

public class WhiteList {

    public static final List<String> URL = Arrays.asList(
            "/auth/login",
            "/auth/register",
            // Swagger/OpenAPI 放行（含静态资源）
            "/swagger-ui/",
            "/v3/api-docs"
    );

    /**
     * 判断请求路径是否在白名单中（支持前缀匹配）
     */
    public static boolean isWhitelisted(String uri) {
        for (String path : URL) {
            if (uri.equals(path) || uri.startsWith(path)) {
                return true;
            }
        }
        return false;
    }
}
