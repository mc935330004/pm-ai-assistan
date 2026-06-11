package com.pm.ai.assistan.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.client.RestTemplate;

/**
 * HTTP 客户端配置：集中创建后端动态接口调用需要的 RestTemplate。
 */
@Configuration
public class HttpClientConfig {

    /**
     * 通用 HTTP 客户端 Bean，供 OpenAPI 拉取和 PM 接口动态调用复用。
     */
    @Bean
    public RestTemplate restTemplate() {
        return new RestTemplate();
    }
}
