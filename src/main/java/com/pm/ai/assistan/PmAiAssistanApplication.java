package com.pm.ai.assistan;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cloud.openfeign.EnableFeignClients;

/**
 * PM AI 助手启动类：统一扫描 assistant 与 tools 包下的 Spring 组件。
 */
@EnableFeignClients
@SpringBootApplication(scanBasePackages = "com.pm.ai")
public class PmAiAssistanApplication {

    /**
     * 应用启动入口：扫描 com.pm.ai 下的 assistant 与 tools 两个模块。
     */
    public static void main(String[] args) {
        SpringApplication.run(PmAiAssistanApplication.class, args);
    }
}
