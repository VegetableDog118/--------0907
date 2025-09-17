package com.powertrading.auth;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cache.annotation.EnableCaching;
import org.springframework.scheduling.annotation.EnableAsync;
import org.springframework.scheduling.annotation.EnableScheduling;

/**
 * 认证服务启动类
 * 
 * PowerTrading接口平台认证服务主启动类
 * 提供统一的认证和授权功能，包括JWT Token管理、API密钥认证、
 * 多重认证支持、安全策略、权限缓存、审计日志等核心功能。
 *
 * @author PowerTrading Team
 * @version 2.0.0
 * @since 2024-01-15
 */
@SpringBootApplication
@EnableCaching
@EnableAsync
@EnableScheduling
public class AuthServiceApplication {

    public static void main(String[] args) {
        SpringApplication.run(AuthServiceApplication.class, args);
    }
}