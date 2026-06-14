package com.pm.ai.assistan.auth.config;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
/**
 * 认证模块 Jackson 配置。
 * 单独拆出来，避免 SecurityConfig 构造时和 Bearer 过滤器形成循环依赖。
 */
public class AuthJacksonConfig {

    /**
     * 认证模块内部使用的 Jackson 2 ObjectMapper。
     * 用于 Redis 会话序列化和 Spring Security 异常 JSON 输出。
     */
    @Bean
    public ObjectMapper authObjectMapper() {
        return new ObjectMapper().findAndRegisterModules();
    }
}
