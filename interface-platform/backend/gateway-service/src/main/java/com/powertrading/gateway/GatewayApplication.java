package com.powertrading.gateway;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cloud.client.discovery.EnableDiscoveryClient;
import org.springframework.cloud.openfeign.EnableFeignClients;

/**
 * 网关服务启动类
 * 提供统一的API网关入口，负责路由转发、认证鉴权、限流控制等功能
 */
@SpringBootApplication(exclude = {
    org.springframework.boot.autoconfigure.jdbc.DataSourceAutoConfiguration.class,
    org.springframework.boot.autoconfigure.orm.jpa.HibernateJpaAutoConfiguration.class
})
@EnableDiscoveryClient
@EnableFeignClients
public class GatewayApplication {

    public static void main(String[] args) {
        SpringApplication.run(GatewayApplication.class, args);
        System.out.println("\n" +
                "   ____       _                           ____                  _          \n" +
                "  / ___| __ _| |_ _____      ____ _ _   _  / ___|  ___ _ ____   _(_) ___ ___ \n" +
                " | |  _ / _` | __/ _ \\ \\ /\\ / / _` | | | | \\___ \\ / _ \\ '__\\ \\ / / |/ __/ _ \\\n" +
                " | |_| | (_| | ||  __/\\ V  V / (_| | |_| |  ___) |  __/ |   \\ V /| | (_|  __/\n" +
                "  \\____|\\_\\_,|\\__\\___| \\_/\\_/ \\__,_|\\__, | |____/ \\___|_|    \\_/ |_|\\___\\___|\n" +
                "                                    |___/                                  \n" +
                "\n网关服务启动成功！端口: 8080");
    }
}