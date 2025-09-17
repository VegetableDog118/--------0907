package com.powertrading.datasource.config;

import com.zaxxer.hikari.HikariConfig;
import com.zaxxer.hikari.HikariDataSource;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Primary;
import org.springframework.context.annotation.Profile;
import org.springframework.jdbc.datasource.embedded.EmbeddedDatabaseBuilder;
import org.springframework.jdbc.datasource.embedded.EmbeddedDatabaseType;

import javax.sql.DataSource;

/**
 * 测试配置类
 * 为单元测试和集成测试提供测试专用的配置
 * 
 * @author PowerTrading Team
 * @version 1.0.0
 */
@TestConfiguration
public class TestConfiguration {

    /**
     * 测试用内存数据库配置
     * 使用H2内存数据库进行测试，避免依赖外部数据库
     */
    @Bean
    @Primary
    @Profile("test")
    public DataSource testDataSource() {
        return new EmbeddedDatabaseBuilder()
                .setType(EmbeddedDatabaseType.H2)
                .setName("testdb")
                .addScript("classpath:schema-test.sql")
                .addScript("classpath:data-test.sql")
                .build();
    }

    /**
     * 测试用HikariCP配置
     * 针对测试环境优化的连接池配置
     */
    @Bean
    @Profile("test")
    public HikariConfig testHikariConfig() {
        HikariConfig config = new HikariConfig();
        
        // 基本配置
        config.setJdbcUrl("jdbc:h2:mem:testdb;DB_CLOSE_DELAY=-1;DB_CLOSE_ON_EXIT=FALSE");
        config.setDriverClassName("org.h2.Driver");
        config.setUsername("sa");
        config.setPassword("");
        
        // 连接池配置 - 测试环境使用较小的连接池
        config.setMaximumPoolSize(5);
        config.setMinimumIdle(1);
        config.setConnectionTimeout(5000);
        config.setIdleTimeout(300000);
        config.setMaxLifetime(600000);
        
        // 测试相关配置
        config.setConnectionTestQuery("SELECT 1");
        config.setValidationTimeout(3000);
        config.setLeakDetectionThreshold(10000);
        
        // 性能配置
        config.setAutoCommit(true);
        config.setReadOnly(false);
        config.setTransactionIsolation("TRANSACTION_READ_COMMITTED");
        
        // 连接池名称
        config.setPoolName("TestHikariPool");
        
        return config;
    }

    /**
     * 测试用HikariDataSource
     */
    @Bean
    @Profile("test")
    public HikariDataSource testHikariDataSource() {
        return new HikariDataSource(testHikariConfig());
    }
}