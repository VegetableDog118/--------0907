package com.powertrading.approval.config;

import org.springframework.context.annotation.Configuration;
import org.springframework.retry.annotation.EnableRetry;

/**
 * 重试配置
 *
 * @author PowerTrading
 * @since 2024-01-15
 */
@Configuration
@EnableRetry
public class RetryConfig {
    // Spring Retry配置
}