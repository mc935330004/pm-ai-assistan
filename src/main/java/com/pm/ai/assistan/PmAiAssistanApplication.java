package com.pm.ai.assistan;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cloud.openfeign.EnableFeignClients;

/**
 * PM AI 助手启动类。
 * 主应用扫描 com.pm.ai 下的业务模块；登录权限能力由 ai-auth 依赖提供。
 */
@EnableFeignClients
@SpringBootApplication(scanBasePackages = "com.pm.ai")
public class PmAiAssistanApplication {

    /**
     * 应用启动入口。
     */
    public static void main(String[] args) {
        SpringApplication.run(PmAiAssistanApplication.class, args);
    }
}
