package com.pm.ai.assistan.auth.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;

import java.time.Duration;
import java.util.List;

@Data
@ConfigurationProperties(prefix = "auth")
/**
 * auth.* 配置项的类型安全绑定。
 * 登录 token、默认管理员、跨域白名单等参数都集中放在这里，方便后续从 application.yml 调整。
 */
public class AuthProperties {

    /**
     * 本地登录 token 在 Redis 会话中的有效期。
     */
    private Duration tokenTtl = Duration.ofHours(24);

    /**
     * 前端跨域访问配置。
     */
    private Cors cors = new Cors();

    /**
     * 账号申请相关配置。
     */
    private Registration registration = new Registration();

    /**
     * 启动时自动创建默认管理员的配置。
     */
    private DefaultAdmin defaultAdmin = new DefaultAdmin();

    @Data
    /**
     * 控制浏览器允许从哪些前端地址访问后端接口。
     */
    public static class Cors {

        /**
         * 允许的前端 Origin 模式。
         * 本地开发既可能用 localhost，也可能用局域网 IP，例如 http://192.168.10.13:5173。
         */
        private List<String> allowedOriginPatterns = List.of(
                "http://localhost:*",
                "http://127.0.0.1:*",
                "http://192.168.*.*:*"
        );
    }

    @Data
    /**
     * 控制账号申请入口是否开放。
     */
    public static class Registration {

        /**
         * true 时允许 POST /auth/register 创建 PENDING 用户。
         */
        private boolean enabled = true;
    }

    @Data
    /**
     * 默认管理员配置。
     * 密码建议通过 AUTH_DEFAULT_ADMIN_PASSWORD 环境变量覆盖。
     */
    public static class DefaultAdmin {

        /**
         * 是否启动时创建默认管理员。
         */
        private boolean enabled = true;

        /**
         * 默认管理员账号。
         */
        private String username = "admin";

        /**
         * 默认管理员密码，仅用于首次启动初始化。
         * 默认值留空，具体环境通过 application-dev.yml 或环境变量提供。
         */
        private String password = "";

        /**
         * 默认管理员展示名称。
         */
        private String displayName = "系统管理员";
    }
}
