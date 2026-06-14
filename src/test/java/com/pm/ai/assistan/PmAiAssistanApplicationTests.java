package com.pm.ai.assistan;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * 应用上下文测试。
 * 这个测试直接使用 application.yml 中的 MySQL 配置连接本地 mc-agent。
 */
@SpringBootTest
class PmAiAssistanApplicationTests {

    @Autowired
    private CorsConfigurationSource corsConfigurationSource;

    @Test
    void contextLoads() {
    }

    /**
     * 验证局域网 Vite 前端地址被后端 CORS 白名单允许。
     * 前端使用 http://192.168.10.13:5173 访问时，浏览器会带这个 Origin。
     */
    @Test
    void corsAllowsLanViteOrigin() {
        MockHttpServletRequest request = new MockHttpServletRequest("POST", "/auth/register");
        request.addHeader("Origin", "http://192.168.10.13:5173");

        CorsConfiguration configuration = corsConfigurationSource.getCorsConfiguration(request);

        assertThat(configuration).isNotNull();
        assertThat(configuration.checkOrigin("http://192.168.10.13:5173"))
                .isEqualTo("http://192.168.10.13:5173");
    }
}
