package com.knowai.knowaibackend.config;

import com.knowai.knowaibackend.filter.JwtAuthenticationFilter;
import jakarta.servlet.Filter;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.web.servlet.FilterRegistrationBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * Web通用配置类
 * 用于注册自定义Servlet过滤器、配置过滤器执行顺序、拦截路径等
 */
@Configuration
public class WebConfig {

    /**
     * 注入自定义JWT认证过滤器
     */
    @Autowired
    private JwtAuthenticationFilter jwtAuthenticationFilter;

    @Bean
    public FilterRegistrationBean<JwtAuthenticationFilter> jwtFilter(){
        // 创建过滤器注册实例
        FilterRegistrationBean<JwtAuthenticationFilter> bean = new FilterRegistrationBean<>();
        // 设置要注册的过滤器：JWT登录认证拦截器
        bean.setFilter(jwtAuthenticationFilter);
        // 配置拦截路径：拦截所有请求 /*
        bean.addUrlPatterns("/*");
        // 设置过滤器执行优先级，数值越小优先级越高
        bean.setOrder(1);
        return bean;
    }
}
