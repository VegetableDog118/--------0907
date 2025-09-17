package com.powertrading.datasource.monitor;

import com.powertrading.datasource.entity.DataSource;
import com.powertrading.datasource.manager.DataSourceManager;
import com.powertrading.datasource.repository.DataSourceRepository;
import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.Gauge;
import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.Timer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import javax.annotation.PostConstruct;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicLong;

/**
 * 数据源监控和健康检查服务
 * 
 * @author PowerTrading Team
 * @version 1.0.0
 */
@Service
public class DataSourceMonitorService {

    private static final Logger logger = LoggerFactory.getLogger(DataSourceMonitorService.class);

    @Autowired
    private DataSourceRepository dataSourceRepository;

    @Autowired
    private DataSourceManager dataSourceManager;

    @Autowired(required = false)
    private MeterRegistry meterRegistry;

    @Value("${datasource.monitor.enabled:true}")
    private boolean monitorEnabled;

    @Value("${datasource.monitor.health-check-interval:60000}")
    private long healthCheckInterval;

    @Value("${datasource.monitor.slow-query-threshold:5000}")
    private long slowQueryThreshold;

    // 监控指标
    private final Map<Long, DataSourceMetrics> metricsMap = new ConcurrentHashMap<>();
    
    // Micrometer指标
    private Counter connectionSuccessCounter;
    private Counter connectionFailureCounter;
    private Counter querySuccessCounter;
    private Counter queryFailureCounter;
    private Counter slowQueryCounter;
    private Timer connectionTimer;
    private Timer queryTimer;

    @PostConstruct
    public void init() {
        if (meterRegistry != null) {
            initMicrometerMetrics();
        }
        logger.info("数据源监控服务初始化完成: enabled={}, healthCheckInterval={}ms, slowQueryThreshold={}ms",
                   monitorEnabled, healthCheckInterval, slowQueryThreshold);
    }

    /**
     * 初始化Micrometer指标
     */
    private void initMicrometerMetrics() {
        connectionSuccessCounter = Counter.builder("datasource.connection.success")
            .description("数据源连接成功次数")
            .register(meterRegistry);
            
        connectionFailureCounter = Counter.builder("datasource.connection.failure")
            .description("数据源连接失败次数")
            .register(meterRegistry);
            
        querySuccessCounter = Counter.builder("datasource.query.success")
            .description("查询成功次数")
            .register(meterRegistry);
            
        queryFailureCounter = Counter.builder("datasource.query.failure")
            .description("查询失败次数")
            .register(meterRegistry);
            
        slowQueryCounter = Counter.builder("datasource.query.slow")
            .description("慢查询次数")
            .register(meterRegistry);
            
        connectionTimer = Timer.builder("datasource.connection.duration")
            .description("数据源连接耗时")
            .register(meterRegistry);
            
        queryTimer = Timer.builder("datasource.query.duration")
            .description("查询执行耗时")
            .register(meterRegistry);
    }

    /**
     * 定时健康检查
     */
    @Scheduled(fixedDelayString = "${datasource.monitor.health-check-interval:60000}")
    public void performHealthCheck() {
        if (!monitorEnabled) {
            return;
        }
        
        logger.debug("开始执行数据源健康检查");
        
        try {
            // 获取需要检查的数据源
            LocalDateTime threshold = LocalDateTime.now().minusNanos(healthCheckInterval * 1_000_000L);
            List<DataSource> dataSourcesToCheck = dataSourceRepository.findDataSourcesNeedHealthCheck(threshold);
            
            logger.debug("需要健康检查的数据源数量: {}", dataSourcesToCheck.size());
            
            for (DataSource dataSource : dataSourcesToCheck) {
                performSingleHealthCheck(dataSource);
            }
            
            // 更新监控指标
            updateMonitoringMetrics();
            
        } catch (Exception e) {
            logger.error("执行健康检查异常", e);
        }
    }

    /**
     * 执行单个数据源健康检查
     */
    private void performSingleHealthCheck(DataSource dataSource) {
        Long dataSourceId = dataSource.getId();
        String dataSourceName = dataSource.getName();
        
        logger.debug("执行数据源健康检查: id={}, name={}", dataSourceId, dataSourceName);
        
        Timer.Sample connectionSample = connectionTimer != null ? Timer.start(meterRegistry) : null;
        
        try {
            // 测试连接
            boolean isHealthy = testDataSourceHealth(dataSourceId);
            
            // 更新健康状态
            LocalDateTime now = LocalDateTime.now();
            int healthStatus = isHealthy ? 1 : 2; // 1-健康, 2-异常
            String errorMessage = isHealthy ? null : "健康检查失败";
            
            dataSourceRepository.updateHealthCheckInfo(dataSourceId, now, healthStatus, errorMessage);
            
            // 更新指标
            DataSourceMetrics metrics = getOrCreateMetrics(dataSourceId);
            if (isHealthy) {
                metrics.incrementConnectionSuccess();
                if (connectionSuccessCounter != null) {
                    connectionSuccessCounter.increment();
                }
            } else {
                metrics.incrementConnectionFailure();
                if (connectionFailureCounter != null) {
                    connectionFailureCounter.increment();
                }
            }
            
            logger.debug("数据源健康检查完成: id={}, name={}, healthy={}", 
                        dataSourceId, dataSourceName, isHealthy);
            
        } catch (Exception e) {
            logger.error("数据源健康检查异常: id={}, name={}", dataSourceId, dataSourceName, e);
            
            // 更新为异常状态
            dataSourceRepository.updateHealthCheckInfo(dataSourceId, LocalDateTime.now(), 2, e.getMessage());
            
            // 更新失败指标
            DataSourceMetrics metrics = getOrCreateMetrics(dataSourceId);
            metrics.incrementConnectionFailure();
            if (connectionFailureCounter != null) {
                connectionFailureCounter.increment();
            }
        } finally {
            if (connectionSample != null) {
                connectionSample.stop(connectionTimer);
            }
        }
    }

    /**
     * 测试数据源健康状态
     */
    private boolean testDataSourceHealth(Long dataSourceId) {
        try {
            // 获取连接并执行简单查询
            try (Connection connection = dataSourceManager.getConnection(dataSourceId)) {
                if (connection == null || connection.isClosed()) {
                    return false;
                }
                
                // 执行简单的健康检查查询
                String healthCheckSql = getHealthCheckSql(dataSourceId);
                try (PreparedStatement stmt = connection.prepareStatement(healthCheckSql);
                     ResultSet rs = stmt.executeQuery()) {
                    return rs.next();
                }
            }
        } catch (Exception e) {
            logger.warn("数据源健康检查失败: dataSourceId={}, error={}", dataSourceId, e.getMessage());
            return false;
        }
    }

    /**
     * 获取健康检查SQL
     */
    private String getHealthCheckSql(Long dataSourceId) {
        // 根据数据源类型返回不同的健康检查SQL
        DataSource dataSource = dataSourceManager.getDataSourceConfig(dataSourceId);
        if (dataSource != null) {
            String dbType = dataSource.getType().toLowerCase();
            switch (dbType) {
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
        return "SELECT 1";
    }

    /**
     * 记录查询执行指标
     */
    public void recordQueryExecution(Long dataSourceId, long executionTime, boolean success, String sql) {
        if (!monitorEnabled) {
            return;
        }
        
        DataSourceMetrics metrics = getOrCreateMetrics(dataSourceId);
        
        if (success) {
            metrics.incrementQuerySuccess();
            metrics.addQueryTime(executionTime);
            
            if (querySuccessCounter != null) {
                querySuccessCounter.increment();
            }
            
            if (queryTimer != null) {
                queryTimer.record(executionTime, java.util.concurrent.TimeUnit.MILLISECONDS);
            }
            
            // 检查是否为慢查询
            if (executionTime > slowQueryThreshold) {
                metrics.incrementSlowQuery();
                if (slowQueryCounter != null) {
                    slowQueryCounter.increment();
                }
                
                logger.warn("检测到慢查询: dataSourceId={}, executionTime={}ms, sql={}", 
                           dataSourceId, executionTime, sql);
            }
        } else {
            metrics.incrementQueryFailure();
            if (queryFailureCounter != null) {
                queryFailureCounter.increment();
            }
        }
    }

    /**
     * 记录连接获取指标
     */
    public void recordConnectionAcquisition(Long dataSourceId, long acquisitionTime, boolean success) {
        if (!monitorEnabled) {
            return;
        }
        
        DataSourceMetrics metrics = getOrCreateMetrics(dataSourceId);
        
        if (success) {
            metrics.incrementConnectionSuccess();
            metrics.addConnectionTime(acquisitionTime);
            
            if (connectionSuccessCounter != null) {
                connectionSuccessCounter.increment();
            }
        } else {
            metrics.incrementConnectionFailure();
            if (connectionFailureCounter != null) {
                connectionFailureCounter.increment();
            }
        }
    }

    /**
     * 获取数据源监控指标
     */
    public DataSourceMetrics getMetrics(Long dataSourceId) {
        return metricsMap.get(dataSourceId);
    }

    /**
     * 获取所有数据源监控指标
     */
    public Map<Long, DataSourceMetrics> getAllMetrics() {
        return new ConcurrentHashMap<>(metricsMap);
    }

    /**
     * 获取数据源健康状态报告
     */
    public HealthReport getHealthReport() {
        HealthReport report = new HealthReport();
        
        List<DataSource> allDataSources = dataSourceRepository.findEnabledDataSources();
        
        int totalCount = allDataSources.size();
        int healthyCount = 0;
        int unhealthyCount = 0;
        int unknownCount = 0;
        
        for (DataSource dataSource : allDataSources) {
            Integer healthStatus = dataSource.getHealthStatus();
            if (healthStatus == null || healthStatus == 0) {
                unknownCount++;
            } else if (healthStatus == 1) {
                healthyCount++;
            } else {
                unhealthyCount++;
            }
        }
        
        report.setTotalCount(totalCount);
        report.setHealthyCount(healthyCount);
        report.setUnhealthyCount(unhealthyCount);
        report.setUnknownCount(unknownCount);
        report.setHealthyRate(totalCount > 0 ? (double) healthyCount / totalCount : 0.0);
        report.setCheckTime(LocalDateTime.now());
        
        return report;
    }

    /**
     * 更新监控指标到Micrometer
     */
    private void updateMonitoringMetrics() {
        if (meterRegistry == null) {
            return;
        }
        
        // 注册连接池指标
        Map<Long, DataSourceManager.DataSourcePoolInfo> poolInfoMap = dataSourceManager.getAllPoolInfo();
        
        for (Map.Entry<Long, DataSourceManager.DataSourcePoolInfo> entry : poolInfoMap.entrySet()) {
            Long dataSourceId = entry.getKey();
            DataSourceManager.DataSourcePoolInfo poolInfo = entry.getValue();
            
            String dataSourceName = poolInfo.getName();
            
            // 活跃连接数
            Gauge.builder("datasource.pool.active", poolInfo, DataSourceManager.DataSourcePoolInfo::getActiveConnections)
                .description("活跃连接数")
                .tag("datasource", dataSourceName)
                .tag("id", dataSourceId.toString())
                .register(meterRegistry);
            
            // 空闲连接数
            Gauge.builder("datasource.pool.idle", poolInfo, DataSourceManager.DataSourcePoolInfo::getIdleConnections)
                .description("空闲连接数")
                .tag("datasource", dataSourceName)
                .tag("id", dataSourceId.toString())
                .register(meterRegistry);
            
            // 总连接数
            Gauge.builder("datasource.pool.total", poolInfo, DataSourceManager.DataSourcePoolInfo::getTotalConnections)
                .description("总连接数")
                .tag("datasource", dataSourceName)
                .tag("id", dataSourceId.toString())
                .register(meterRegistry);
            
            // 等待连接的线程数
            Gauge.builder("datasource.pool.waiting", poolInfo, DataSourceManager.DataSourcePoolInfo::getThreadsAwaitingConnection)
                .description("等待连接的线程数")
                .tag("datasource", dataSourceName)
                .tag("id", dataSourceId.toString())
                .register(meterRegistry);
        }
    }

    /**
     * 获取或创建监控指标
     */
    private DataSourceMetrics getOrCreateMetrics(Long dataSourceId) {
        return metricsMap.computeIfAbsent(dataSourceId, id -> new DataSourceMetrics(id));
    }

    /**
     * 清理数据源监控指标
     */
    public void removeMetrics(Long dataSourceId) {
        metricsMap.remove(dataSourceId);
    }

    /**
     * 数据源监控指标类
     */
    public static class DataSourceMetrics {
        private final Long dataSourceId;
        private final AtomicLong connectionSuccessCount = new AtomicLong(0);
        private final AtomicLong connectionFailureCount = new AtomicLong(0);
        private final AtomicLong querySuccessCount = new AtomicLong(0);
        private final AtomicLong queryFailureCount = new AtomicLong(0);
        private final AtomicLong slowQueryCount = new AtomicLong(0);
        private final AtomicLong totalConnectionTime = new AtomicLong(0);
        private final AtomicLong totalQueryTime = new AtomicLong(0);
        private final AtomicInteger connectionTimeCount = new AtomicInteger(0);
        private final AtomicInteger queryTimeCount = new AtomicInteger(0);
        private volatile LocalDateTime lastUpdateTime = LocalDateTime.now();

        public DataSourceMetrics(Long dataSourceId) {
            this.dataSourceId = dataSourceId;
        }

        public void incrementConnectionSuccess() {
            connectionSuccessCount.incrementAndGet();
            updateLastUpdateTime();
        }

        public void incrementConnectionFailure() {
            connectionFailureCount.incrementAndGet();
            updateLastUpdateTime();
        }

        public void incrementQuerySuccess() {
            querySuccessCount.incrementAndGet();
            updateLastUpdateTime();
        }

        public void incrementQueryFailure() {
            queryFailureCount.incrementAndGet();
            updateLastUpdateTime();
        }

        public void incrementSlowQuery() {
            slowQueryCount.incrementAndGet();
            updateLastUpdateTime();
        }

        public void addConnectionTime(long time) {
            totalConnectionTime.addAndGet(time);
            connectionTimeCount.incrementAndGet();
            updateLastUpdateTime();
        }

        public void addQueryTime(long time) {
            totalQueryTime.addAndGet(time);
            queryTimeCount.incrementAndGet();
            updateLastUpdateTime();
        }

        private void updateLastUpdateTime() {
            lastUpdateTime = LocalDateTime.now();
        }

        // Getters
        public Long getDataSourceId() { return dataSourceId; }
        public long getConnectionSuccessCount() { return connectionSuccessCount.get(); }
        public long getConnectionFailureCount() { return connectionFailureCount.get(); }
        public long getQuerySuccessCount() { return querySuccessCount.get(); }
        public long getQueryFailureCount() { return queryFailureCount.get(); }
        public long getSlowQueryCount() { return slowQueryCount.get(); }
        public double getAverageConnectionTime() {
            int count = connectionTimeCount.get();
            return count > 0 ? (double) totalConnectionTime.get() / count : 0.0;
        }
        public double getAverageQueryTime() {
            int count = queryTimeCount.get();
            return count > 0 ? (double) totalQueryTime.get() / count : 0.0;
        }
        public LocalDateTime getLastUpdateTime() { return lastUpdateTime; }
        
        public double getConnectionSuccessRate() {
            long total = connectionSuccessCount.get() + connectionFailureCount.get();
            return total > 0 ? (double) connectionSuccessCount.get() / total : 0.0;
        }
        
        public double getQuerySuccessRate() {
            long total = querySuccessCount.get() + queryFailureCount.get();
            return total > 0 ? (double) querySuccessCount.get() / total : 0.0;
        }
    }

    /**
     * 健康状态报告类
     */
    public static class HealthReport {
        private int totalCount;
        private int healthyCount;
        private int unhealthyCount;
        private int unknownCount;
        private double healthyRate;
        private LocalDateTime checkTime;

        // Getters and Setters
        public int getTotalCount() { return totalCount; }
        public void setTotalCount(int totalCount) { this.totalCount = totalCount; }
        public int getHealthyCount() { return healthyCount; }
        public void setHealthyCount(int healthyCount) { this.healthyCount = healthyCount; }
        public int getUnhealthyCount() { return unhealthyCount; }
        public void setUnhealthyCount(int unhealthyCount) { this.unhealthyCount = unhealthyCount; }
        public int getUnknownCount() { return unknownCount; }
        public void setUnknownCount(int unknownCount) { this.unknownCount = unknownCount; }
        public double getHealthyRate() { return healthyRate; }
        public void setHealthyRate(double healthyRate) { this.healthyRate = healthyRate; }
        public LocalDateTime getCheckTime() { return checkTime; }
        public void setCheckTime(LocalDateTime checkTime) { this.checkTime = checkTime; }
    }
}