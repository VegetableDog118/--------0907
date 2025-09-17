package com.powertrading.datasource.config;

import com.zaxxer.hikari.HikariConfig;
import com.zaxxer.hikari.HikariDataSource;
import com.zaxxer.hikari.metrics.micrometer.MicrometerMetricsTrackerFactory;
import io.micrometer.core.instrument.MeterRegistry;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;
import org.springframework.stereotype.Component;

import java.util.Properties;

/**
 * HikariCP连接池优化配置
 * 
 * @author PowerTrading Team
 * @version 1.0.0
 */
@Configuration
public class HikariOptimizationConfig {

    private static final Logger logger = LoggerFactory.getLogger(HikariOptimizationConfig.class);

    @Autowired(required = false)
    private MeterRegistry meterRegistry;

    /**
     * 创建优化的HikariCP数据源
     * 
     * @param dsConfig 数据源配置
     * @param optimizationSettings 优化设置
     * @return 优化的HikariDataSource
     */
    public HikariDataSource createOptimizedDataSource(
            com.powertrading.datasource.entity.DataSource dsConfig,
            OptimizationSettings optimizationSettings) {
        
        HikariConfig config = new HikariConfig();
        
        // 基本连接配置
        setupBasicConfig(config, dsConfig);
        
        // 连接池优化配置
        setupPoolOptimization(config, dsConfig, optimizationSettings);
        
        // 性能优化配置
        setupPerformanceOptimization(config, dsConfig.getType(), optimizationSettings);
        
        // 监控配置
        setupMonitoring(config, dsConfig.getName());
        
        // 数据库特定优化
        setupDatabaseSpecificOptimization(config, dsConfig.getType());
        
        logger.info("创建优化的HikariCP数据源: name={}, type={}", dsConfig.getName(), dsConfig.getType());
        
        return new HikariDataSource(config);
    }

    /**
     * 设置基本连接配置
     */
    private void setupBasicConfig(HikariConfig config, com.powertrading.datasource.entity.DataSource dsConfig) {
        config.setJdbcUrl(dsConfig.getUrl());
        config.setUsername(dsConfig.getUsername());
        config.setPassword(dsConfig.getPassword());
        config.setDriverClassName(dsConfig.getDriverClass());
        config.setPoolName("OptimizedHikariPool-" + dsConfig.getName());
    }

    /**
     * 设置连接池优化配置
     */
    private void setupPoolOptimization(HikariConfig config, 
                                     com.powertrading.datasource.entity.DataSource dsConfig,
                                     OptimizationSettings settings) {
        
        // 连接池大小优化
        int minIdle = calculateOptimalMinIdle(dsConfig.getMinPoolSize(), settings);
        int maxPoolSize = calculateOptimalMaxPoolSize(dsConfig.getMaxPoolSize(), settings);
        
        config.setMinimumIdle(minIdle);
        config.setMaximumPoolSize(maxPoolSize);
        
        // 超时配置优化
        config.setConnectionTimeout(optimizeConnectionTimeout(dsConfig.getConnectionTimeout(), settings));
        config.setIdleTimeout(optimizeIdleTimeout(dsConfig.getIdleTimeout(), settings));
        config.setMaxLifetime(optimizeMaxLifetime(dsConfig.getMaxLifetime(), settings));
        config.setValidationTimeout(dsConfig.getValidationTimeout());
        
        // 泄漏检测
        if (settings.isEnableLeakDetection()) {
            config.setLeakDetectionThreshold(dsConfig.getLeakDetectionThreshold());
        }
        
        logger.debug("连接池优化配置: minIdle={}, maxPoolSize={}, connectionTimeout={}, idleTimeout={}, maxLifetime={}",
                    minIdle, maxPoolSize, config.getConnectionTimeout(), config.getIdleTimeout(), config.getMaxLifetime());
    }

    /**
     * 设置性能优化配置
     */
    private void setupPerformanceOptimization(HikariConfig config, String dbType, OptimizationSettings settings) {
        // 自动提交优化
        config.setAutoCommit(settings.isAutoCommit());
        
        // 只读优化
        config.setReadOnly(false);
        
        // 事务隔离级别优化
        if (settings.getTransactionIsolation() != null) {
            config.setTransactionIsolation(settings.getTransactionIsolation());
        }
        
        // 连接初始化SQL
        String initSql = getInitializationSql(dbType, settings);
        if (initSql != null) {
            config.setConnectionInitSql(initSql);
        }
        
        // 连接测试查询优化
        config.setConnectionTestQuery(getOptimizedTestQuery(dbType));
        
        // 内部查询隔离
        config.setIsolateInternalQueries(settings.isIsolateInternalQueries());
        
        // 允许池暂停
        config.setAllowPoolSuspension(settings.isAllowPoolSuspension());
        
        // 注册MBeans
        config.setRegisterMbeans(settings.isRegisterMbeans());
    }

    /**
     * 设置监控配置
     */
    private void setupMonitoring(HikariConfig config, String dataSourceName) {
        if (meterRegistry != null) {
            config.setMetricsTrackerFactory(new MicrometerMetricsTrackerFactory(meterRegistry));
            config.setMetricRegistry(null); // 使用Micrometer而不是Dropwizard
            logger.debug("启用Micrometer监控: dataSource={}", dataSourceName);
        }
    }

    /**
     * 设置数据库特定优化
     */
    private void setupDatabaseSpecificOptimization(HikariConfig config, String dbType) {
        Properties props = new Properties();
        
        switch (dbType.toLowerCase()) {
            case "mysql":
                setupMySQLOptimization(props);
                break;
            case "postgresql":
                setupPostgreSQLOptimization(props);
                break;
            case "oracle":
                setupOracleOptimization(props);
                break;
            case "sqlserver":
                setupSQLServerOptimization(props);
                break;
        }
        
        if (!props.isEmpty()) {
            config.setDataSourceProperties(props);
            logger.debug("应用数据库特定优化: dbType={}, properties={}", dbType, props.size());
        }
    }

    /**
     * MySQL优化配置
     */
    private void setupMySQLOptimization(Properties props) {
        props.setProperty("cachePrepStmts", "true");
        props.setProperty("prepStmtCacheSize", "250");
        props.setProperty("prepStmtCacheSqlLimit", "2048");
        props.setProperty("useServerPrepStmts", "true");
        props.setProperty("useLocalSessionState", "true");
        props.setProperty("rewriteBatchedStatements", "true");
        props.setProperty("cacheResultSetMetadata", "true");
        props.setProperty("cacheServerConfiguration", "true");
        props.setProperty("elideSetAutoCommits", "true");
        props.setProperty("maintainTimeStats", "false");
        props.setProperty("netTimeoutForStreamingResults", "0");
    }

    /**
     * PostgreSQL优化配置
     */
    private void setupPostgreSQLOptimization(Properties props) {
        props.setProperty("prepareThreshold", "5");
        props.setProperty("preparedStatementCacheQueries", "256");
        props.setProperty("preparedStatementCacheSizeMiB", "5");
        props.setProperty("defaultRowFetchSize", "1000");
        props.setProperty("logUnclosedConnections", "false");
        props.setProperty("tcpKeepAlive", "true");
        props.setProperty("ApplicationName", "PowerTradingDataSource");
    }

    /**
     * Oracle优化配置
     */
    private void setupOracleOptimization(Properties props) {
        props.setProperty("oracle.jdbc.implicitStatementCacheSize", "25");
        props.setProperty("oracle.jdbc.defaultRowPrefetch", "20");
        props.setProperty("oracle.net.CONNECT_TIMEOUT", "10000");
        props.setProperty("oracle.net.READ_TIMEOUT", "30000");
        props.setProperty("oracle.jdbc.ReadTimeout", "30000");
    }

    /**
     * SQL Server优化配置
     */
    private void setupSQLServerOptimization(Properties props) {
        props.setProperty("selectMethod", "cursor");
        props.setProperty("responseBuffering", "adaptive");
        props.setProperty("packetSize", "8192");
        props.setProperty("loginTimeout", "30");
        props.setProperty("socketTimeout", "30000");
    }

    /**
     * 计算最优最小空闲连接数
     */
    private int calculateOptimalMinIdle(Integer configuredMinIdle, OptimizationSettings settings) {
        if (configuredMinIdle == null) {
            return settings.getDefaultMinIdle();
        }
        
        // 根据负载模式调整
        switch (settings.getLoadPattern()) {
            case HIGH_CONCURRENCY:
                return Math.max(configuredMinIdle, 5);
            case BATCH_PROCESSING:
                return Math.max(configuredMinIdle, 2);
            case MIXED:
            default:
                return configuredMinIdle;
        }
    }

    /**
     * 计算最优最大连接池大小
     */
    private int calculateOptimalMaxPoolSize(Integer configuredMaxPoolSize, OptimizationSettings settings) {
        if (configuredMaxPoolSize == null) {
            return settings.getDefaultMaxPoolSize();
        }
        
        // 根据负载模式调整
        switch (settings.getLoadPattern()) {
            case HIGH_CONCURRENCY:
                return Math.max(configuredMaxPoolSize, 20);
            case BATCH_PROCESSING:
                return Math.min(configuredMaxPoolSize, 5);
            case MIXED:
            default:
                return configuredMaxPoolSize;
        }
    }

    /**
     * 优化连接超时时间
     */
    private long optimizeConnectionTimeout(Long configuredTimeout, OptimizationSettings settings) {
        if (configuredTimeout == null) {
            return 30000L;
        }
        
        // 根据网络环境调整
        switch (settings.getNetworkEnvironment()) {
            case LAN:
                return Math.min(configuredTimeout, 10000L);
            case WAN:
                return Math.max(configuredTimeout, 30000L);
            case CLOUD:
            default:
                return configuredTimeout;
        }
    }

    /**
     * 优化空闲超时时间
     */
    private long optimizeIdleTimeout(Long configuredTimeout, OptimizationSettings settings) {
        if (configuredTimeout == null) {
            return 600000L;
        }
        
        // 根据负载模式调整
        switch (settings.getLoadPattern()) {
            case HIGH_CONCURRENCY:
                return Math.max(configuredTimeout, 300000L); // 至少5分钟
            case BATCH_PROCESSING:
                return Math.min(configuredTimeout, 180000L); // 最多3分钟
            case MIXED:
            default:
                return configuredTimeout;
        }
    }

    /**
     * 优化最大生命周期
     */
    private long optimizeMaxLifetime(Long configuredLifetime, OptimizationSettings settings) {
        if (configuredLifetime == null) {
            return 1800000L; // 30分钟
        }
        
        // 确保不超过数据库连接超时时间
        return Math.min(configuredLifetime, 1800000L);
    }

    /**
     * 获取初始化SQL
     */
    private String getInitializationSql(String dbType, OptimizationSettings settings) {
        if (!settings.isUseInitializationSql()) {
            return null;
        }
        
        switch (dbType.toLowerCase()) {
            case "mysql":
                return "SET SESSION sql_mode='STRICT_TRANS_TABLES,NO_ZERO_DATE,NO_ZERO_IN_DATE,ERROR_FOR_DIVISION_BY_ZERO'";
            case "postgresql":
                return "SET application_name = 'PowerTradingDataSource'";
            case "oracle":
                return "ALTER SESSION SET NLS_DATE_FORMAT = 'YYYY-MM-DD HH24:MI:SS'";
            default:
                return null;
        }
    }

    /**
     * 获取优化的测试查询
     */
    private String getOptimizedTestQuery(String dbType) {
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
     * 优化设置配置类
     */
    @Component
    @ConfigurationProperties(prefix = "datasource.optimization")
    public static class OptimizationSettings {
        
        // 负载模式
        public enum LoadPattern {
            HIGH_CONCURRENCY,  // 高并发
            BATCH_PROCESSING,  // 批处理
            MIXED             // 混合
        }
        
        // 网络环境
        public enum NetworkEnvironment {
            LAN,    // 局域网
            WAN,    // 广域网
            CLOUD   // 云环境
        }
        
        private LoadPattern loadPattern = LoadPattern.MIXED;
        private NetworkEnvironment networkEnvironment = NetworkEnvironment.CLOUD;
        private boolean enableLeakDetection = true;
        private boolean autoCommit = true;
        private String transactionIsolation;
        private boolean isolateInternalQueries = false;
        private boolean allowPoolSuspension = false;
        private boolean registerMbeans = true;
        private boolean useInitializationSql = true;
        private int defaultMinIdle = 2;
        private int defaultMaxPoolSize = 10;
        
        // Getters and Setters
        public LoadPattern getLoadPattern() {
            return loadPattern;
        }
        
        public void setLoadPattern(LoadPattern loadPattern) {
            this.loadPattern = loadPattern;
        }
        
        public NetworkEnvironment getNetworkEnvironment() {
            return networkEnvironment;
        }
        
        public void setNetworkEnvironment(NetworkEnvironment networkEnvironment) {
            this.networkEnvironment = networkEnvironment;
        }
        
        public boolean isEnableLeakDetection() {
            return enableLeakDetection;
        }
        
        public void setEnableLeakDetection(boolean enableLeakDetection) {
            this.enableLeakDetection = enableLeakDetection;
        }
        
        public boolean isAutoCommit() {
            return autoCommit;
        }
        
        public void setAutoCommit(boolean autoCommit) {
            this.autoCommit = autoCommit;
        }
        
        public String getTransactionIsolation() {
            return transactionIsolation;
        }
        
        public void setTransactionIsolation(String transactionIsolation) {
            this.transactionIsolation = transactionIsolation;
        }
        
        public boolean isIsolateInternalQueries() {
            return isolateInternalQueries;
        }
        
        public void setIsolateInternalQueries(boolean isolateInternalQueries) {
            this.isolateInternalQueries = isolateInternalQueries;
        }
        
        public boolean isAllowPoolSuspension() {
            return allowPoolSuspension;
        }
        
        public void setAllowPoolSuspension(boolean allowPoolSuspension) {
            this.allowPoolSuspension = allowPoolSuspension;
        }
        
        public boolean isRegisterMbeans() {
            return registerMbeans;
        }
        
        public void setRegisterMbeans(boolean registerMbeans) {
            this.registerMbeans = registerMbeans;
        }
        
        public boolean isUseInitializationSql() {
            return useInitializationSql;
        }
        
        public void setUseInitializationSql(boolean useInitializationSql) {
            this.useInitializationSql = useInitializationSql;
        }
        
        public int getDefaultMinIdle() {
            return defaultMinIdle;
        }
        
        public void setDefaultMinIdle(int defaultMinIdle) {
            this.defaultMinIdle = defaultMinIdle;
        }
        
        public int getDefaultMaxPoolSize() {
            return defaultMaxPoolSize;
        }
        
        public void setDefaultMaxPoolSize(int defaultMaxPoolSize) {
            this.defaultMaxPoolSize = defaultMaxPoolSize;
        }
    }
}