package com.powertrading.datasource.config;

import com.zaxxer.hikari.HikariConfig;
import com.zaxxer.hikari.HikariDataSource;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;

import javax.sql.DataSource;
import java.util.HashMap;
import java.util.Map;

/**
 * 数据源配置类
 * 
 * @author PowerTrading Team
 * @version 1.0.0
 */
@Configuration
public class DataSourceConfig {

    /**
     * 数据库类型与驱动类映射
     */
    private static final Map<String, String> DRIVER_CLASS_MAPPING = new HashMap<>();
    
    static {
        DRIVER_CLASS_MAPPING.put("mysql", "com.mysql.cj.jdbc.Driver");
        DRIVER_CLASS_MAPPING.put("postgresql", "org.postgresql.Driver");
        DRIVER_CLASS_MAPPING.put("oracle", "oracle.jdbc.OracleDriver");
        DRIVER_CLASS_MAPPING.put("sqlserver", "com.microsoft.sqlserver.jdbc.SQLServerDriver");
        DRIVER_CLASS_MAPPING.put("h2", "org.h2.Driver");
        DRIVER_CLASS_MAPPING.put("sqlite", "org.sqlite.JDBC");
    }

    /**
     * 数据库类型与URL模板映射
     */
    private static final Map<String, String> URL_TEMPLATE_MAPPING = new HashMap<>();
    
    static {
        URL_TEMPLATE_MAPPING.put("mysql", "jdbc:mysql://{host}:{port}/{database}?useUnicode=true&characterEncoding=utf8&useSSL=false&serverTimezone=Asia/Shanghai");
        URL_TEMPLATE_MAPPING.put("postgresql", "jdbc:postgresql://{host}:{port}/{database}");
        URL_TEMPLATE_MAPPING.put("oracle", "jdbc:oracle:thin:@{host}:{port}:{database}");
        URL_TEMPLATE_MAPPING.put("sqlserver", "jdbc:sqlserver://{host}:{port};databaseName={database}");
    }

    /**
     * 数据库类型与默认端口映射
     */
    private static final Map<String, Integer> DEFAULT_PORT_MAPPING = new HashMap<>();
    
    static {
        DEFAULT_PORT_MAPPING.put("mysql", 3306);
        DEFAULT_PORT_MAPPING.put("postgresql", 5432);
        DEFAULT_PORT_MAPPING.put("oracle", 1521);
        DEFAULT_PORT_MAPPING.put("sqlserver", 1433);
    }

    /**
     * 连接池默认配置属性
     */
    @Bean
    @ConfigurationProperties(prefix = "datasource.pool.default")
    public PoolConfig defaultPoolConfig() {
        return new PoolConfig();
    }

    /**
     * 获取数据库驱动类
     */
    public static String getDriverClass(String dbType) {
        return DRIVER_CLASS_MAPPING.get(dbType.toLowerCase());
    }

    /**
     * 获取URL模板
     */
    public static String getUrlTemplate(String dbType) {
        return URL_TEMPLATE_MAPPING.get(dbType.toLowerCase());
    }

    /**
     * 获取默认端口
     */
    public static Integer getDefaultPort(String dbType) {
        return DEFAULT_PORT_MAPPING.get(dbType.toLowerCase());
    }

    /**
     * 创建HikariCP数据源
     */
    public static HikariDataSource createHikariDataSource(com.powertrading.datasource.entity.DataSource dsConfig) {
        HikariConfig config = new HikariConfig();
        
        // 基本连接配置
        config.setJdbcUrl(dsConfig.getUrl());
        config.setUsername(dsConfig.getUsername());
        config.setPassword(dsConfig.getPassword());
        config.setDriverClassName(dsConfig.getDriverClass());
        
        // 连接池配置
        config.setPoolName("HikariPool-" + dsConfig.getName());
        config.setMinimumIdle(dsConfig.getMinPoolSize());
        config.setMaximumPoolSize(dsConfig.getMaxPoolSize());
        config.setConnectionTimeout(dsConfig.getConnectionTimeout());
        config.setIdleTimeout(dsConfig.getIdleTimeout());
        config.setMaxLifetime(dsConfig.getMaxLifetime());
        config.setValidationTimeout(dsConfig.getValidationTimeout());
        config.setLeakDetectionThreshold(dsConfig.getLeakDetectionThreshold());
        
        // 连接测试
        config.setConnectionTestQuery(getValidationQuery(dsConfig.getType()));
        
        // 性能优化配置
        config.setAutoCommit(true);
        config.setReadOnly(false);
        config.setIsolateInternalQueries(false);
        config.setRegisterMbeans(true);
        config.setAllowPoolSuspension(false);
        
        // 创建数据源
        return new HikariDataSource(config);
    }

    /**
     * 获取数据库验证查询语句
     */
    private static String getValidationQuery(String dbType) {
        switch (dbType.toLowerCase()) {
            case "mysql":
            case "h2":
                return "SELECT 1";
            case "postgresql":
                return "SELECT 1";
            case "oracle":
                return "SELECT 1 FROM DUAL";
            case "sqlserver":
                return "SELECT 1";
            default:
                return "SELECT 1";
        }
    }

    /**
     * 验证数据库类型是否支持
     */
    public static boolean isSupportedDbType(String dbType) {
        return DRIVER_CLASS_MAPPING.containsKey(dbType.toLowerCase());
    }

    /**
     * 获取所有支持的数据库类型
     */
    public static String[] getSupportedDbTypes() {
        return DRIVER_CLASS_MAPPING.keySet().toArray(new String[0]);
    }

    /**
     * 连接池配置类
     */
    public static class PoolConfig {
        private Integer minimumIdle = 2;
        private Integer maximumPoolSize = 10;
        private Long connectionTimeout = 30000L;
        private Long idleTimeout = 600000L;
        private Long maxLifetime = 1800000L;
        private Long validationTimeout = 5000L;
        private Long leakDetectionThreshold = 60000L;

        // Getters and Setters
        public Integer getMinimumIdle() {
            return minimumIdle;
        }

        public void setMinimumIdle(Integer minimumIdle) {
            this.minimumIdle = minimumIdle;
        }

        public Integer getMaximumPoolSize() {
            return maximumPoolSize;
        }

        public void setMaximumPoolSize(Integer maximumPoolSize) {
            this.maximumPoolSize = maximumPoolSize;
        }

        public Long getConnectionTimeout() {
            return connectionTimeout;
        }

        public void setConnectionTimeout(Long connectionTimeout) {
            this.connectionTimeout = connectionTimeout;
        }

        public Long getIdleTimeout() {
            return idleTimeout;
        }

        public void setIdleTimeout(Long idleTimeout) {
            this.idleTimeout = idleTimeout;
        }

        public Long getMaxLifetime() {
            return maxLifetime;
        }

        public void setMaxLifetime(Long maxLifetime) {
            this.maxLifetime = maxLifetime;
        }

        public Long getValidationTimeout() {
            return validationTimeout;
        }

        public void setValidationTimeout(Long validationTimeout) {
            this.validationTimeout = validationTimeout;
        }

        public Long getLeakDetectionThreshold() {
            return leakDetectionThreshold;
        }

        public void setLeakDetectionThreshold(Long leakDetectionThreshold) {
            this.leakDetectionThreshold = leakDetectionThreshold;
        }
    }
}