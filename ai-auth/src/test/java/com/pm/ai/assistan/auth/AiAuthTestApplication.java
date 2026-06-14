package com.pm.ai.assistan.auth;

import org.springframework.boot.autoconfigure.SpringBootApplication;

/**
 * ai-auth 模块测试启动类。
 * 模块本身是认证库，不提供独立生产启动入口，测试时用这个类加载认证相关 Bean。
 */
@SpringBootApplication(scanBasePackages = "com.pm.ai")
public class AiAuthTestApplication {
}
