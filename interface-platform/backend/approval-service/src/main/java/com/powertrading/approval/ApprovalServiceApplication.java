package com.powertrading.approval;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cloud.client.discovery.EnableDiscoveryClient;
import org.springframework.cloud.openfeign.EnableFeignClients;

/**
 * 审批服务启动类
 *
 * @author PowerTrading
 * @since 2024-01-15
 */
@SpringBootApplication
@EnableDiscoveryClient
@EnableFeignClients
public class ApprovalServiceApplication {

    public static void main(String[] args) {
        SpringApplication.run(ApprovalServiceApplication.class, args);
    }
}