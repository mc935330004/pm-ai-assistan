package com.pm.ai.assistan.auth.config;

import org.flywaydb.core.Flyway;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import javax.sql.DataSource;

@Configuration
/**
 * 认证模块的 Flyway 显式配置。
 * 确保 classpath:db/migration 下的权限表结构先于种子数据初始化执行。
 */
public class FlywayConfig {

    /**
     * 执行数据库迁移脚本。
     * spring.flyway.enabled=false 时可关闭，方便特殊环境手动建表。
     */
    @Bean(initMethod = "migrate")
    @ConditionalOnProperty(prefix = "spring.flyway", name = "enabled", havingValue = "true", matchIfMissing = true)
    public Flyway flyway(DataSource dataSource) {
        return Flyway.configure()
                .dataSource(dataSource)
                .locations("classpath:db/migration")
                .baselineOnMigrate(true)
                .load();
    }
}
