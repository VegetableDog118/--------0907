package com.powertrading.datasource;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cache.annotation.EnableCaching;
import org.springframework.scheduling.annotation.EnableAsync;
import org.springframework.scheduling.annotation.EnableScheduling;
import org.springframework.transaction.annotation.EnableTransactionManagement;

/**
 * 数据源服务应用程序主类
 * 
 * 提供多数据源连接管理、查询执行、监控和缓存等功能的微服务应用
 * 
 * 主要功能：
 * - 多数据源连接管理(MySQL、PostgreSQL、Oracle等)
 * - HikariCP高性能连接池配置和优化
 * - SQL查询执行引擎，支持动态SQL生成
 * - 数据源监控和健康检查
 * - 查询结果缓存机制
 * - 连接池监控和慢查询分析
 * - 数据源配置的动态添加和更新
 * - 表结构数据接口集成
 * 
 * @author PowerTrading Team
 * @version 1.0.0
 * @since 2024-01-01
 */
@SpringBootApplication
@EnableCaching
@EnableAsync
@EnableScheduling
@EnableTransactionManagement
public class DataSourceServiceApplication {

    /**
     * 应用程序入口点
     * 
     * @param args 命令行参数
     */
    public static void main(String[] args) {
        // 设置系统属性
        System.setProperty("spring.application.name", "datasource-service");
        System.setProperty("server.port", "8088");
        
        // 启动Spring Boot应用
        SpringApplication application = new SpringApplication(DataSourceServiceApplication.class);
        
        // 设置默认配置文件
        application.setAdditionalProfiles("datasource");
        
        // 启动应用
        var context = application.run(args);
        
        // 输出启动信息
        String port = context.getEnvironment().getProperty("server.port", "8081");
        String contextPath = context.getEnvironment().getProperty("server.servlet.context-path", "");
        
        System.out.println("\n" +
                "====================================================================\n" +
                "  数据源服务 (DataSource Service) 启动成功!\n" +
                "  应用访问地址: http://localhost:" + port + contextPath + "\n" +
                "  API文档地址: http://localhost:" + port + contextPath + "/swagger-ui.html\n" +
                "  健康检查地址: http://localhost:" + port + contextPath + "/actuator/health\n" +
                "  监控面板地址: http://localhost:" + port + contextPath + "/actuator\n" +
                "====================================================================");
    }
}