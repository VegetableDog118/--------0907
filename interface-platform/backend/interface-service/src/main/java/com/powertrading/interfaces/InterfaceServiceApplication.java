package com.powertrading.interfaces;

import org.mybatis.spring.annotation.MapperScan;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cloud.client.discovery.EnableDiscoveryClient;
import org.springframework.cloud.openfeign.EnableFeignClients;
import org.springframework.scheduling.annotation.EnableAsync;
import org.springframework.transaction.annotation.EnableTransactionManagement;

/**
 * 接口管理服务启动类
 *
 * @author PowerTrading Team
 * @since 2024-01-15
 */
@SpringBootApplication
@EnableDiscoveryClient
@EnableFeignClients
@EnableAsync
@EnableTransactionManagement
@MapperScan("com.powertrading.interfaces.mapper")
public class InterfaceServiceApplication {

    public static void main(String[] args) {
        SpringApplication.run(InterfaceServiceApplication.class, args);
    }
}