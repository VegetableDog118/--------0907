package com.powertrading.datasource.monitor;

import com.powertrading.datasource.manager.DataSourceManager;
import com.powertrading.datasource.repository.DataSourceRepository;
import com.zaxxer.hikari.HikariPoolMXBean;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicLong;
import java.util.stream.Collectors;

/**
 * 连接池监控和慢查询分析服务
 * 
 * @author PowerTrading Team
 * @version 1.0.0
 */
@Service
public class PerformanceAnalysisService {

    private static final Logger logger = LoggerFactory.getLogger(PerformanceAnalysisService.class);

    @Autowired
    private DataSourceManager dataSourceManager;

    @Autowired
    private DataSourceRepository dataSourceRepository;

    @Value("${datasource.monitor.slow-query-threshold:5000}")
    private long slowQueryThreshold;

    @Value("${datasource.monitor.enabled:true}")
    private boolean monitorEnabled;

    // 连接池性能历史数据
    private final Map<Long, List<ConnectionPoolSnapshot>> poolHistoryMap = new ConcurrentHashMap<>();
    
    // 慢查询记录
    private final Map<Long, List<SlowQueryRecord>> slowQueryMap = new ConcurrentHashMap<>();
    
    // 查询统计信息
    private final Map<Long, QueryStatistics> queryStatsMap = new ConcurrentHashMap<>();
    
    // 最大历史记录数
    private static final int MAX_HISTORY_SIZE = 1000;
    private static final int MAX_SLOW_QUERY_SIZE = 500;

    /**
     * 定时收集连接池性能数据
     */
    @Scheduled(fixedRate = 30000) // 每30秒收集一次
    public void collectConnectionPoolMetrics() {
        if (!monitorEnabled) {
            return;
        }
        
        try {
            Map<Long, DataSourceManager.DataSourcePoolInfo> poolInfoMap = dataSourceManager.getAllPoolInfo();
            
            for (Map.Entry<Long, DataSourceManager.DataSourcePoolInfo> entry : poolInfoMap.entrySet()) {
                Long dataSourceId = entry.getKey();
                DataSourceManager.DataSourcePoolInfo poolInfo = entry.getValue();
                
                // 创建快照
                ConnectionPoolSnapshot snapshot = new ConnectionPoolSnapshot(
                    dataSourceId,
                    poolInfo.getName(),
                    poolInfo.getActiveConnections(),
                    poolInfo.getIdleConnections(),
                    poolInfo.getTotalConnections(),
                    poolInfo.getThreadsAwaitingConnection(),
                    LocalDateTime.now()
                );
                
                // 添加到历史记录
                addPoolSnapshot(dataSourceId, snapshot);
            }
            
        } catch (Exception e) {
            logger.error("收集连接池性能数据异常", e);
        }
    }

    /**
     * 记录查询执行信息
     * 
     * @param dataSourceId 数据源ID
     * @param sql SQL语句
     * @param executionTime 执行时间（毫秒）
     * @param success 是否成功
     * @param rowCount 返回行数
     */
    public void recordQueryExecution(Long dataSourceId, String sql, long executionTime, 
                                   boolean success, int rowCount) {
        if (!monitorEnabled) {
            return;
        }
        
        try {
            // 更新查询统计
            updateQueryStatistics(dataSourceId, executionTime, success, rowCount);
            
            // 记录慢查询
            if (executionTime > slowQueryThreshold) {
                recordSlowQuery(dataSourceId, sql, executionTime, success, rowCount);
            }
            
        } catch (Exception e) {
            logger.error("记录查询执行信息异常: dataSourceId={}", dataSourceId, e);
        }
    }

    /**
     * 获取连接池性能分析报告
     * 
     * @param dataSourceId 数据源ID
     * @param hours 分析时间范围（小时）
     * @return 性能分析报告
     */
    public ConnectionPoolAnalysisReport getConnectionPoolAnalysis(Long dataSourceId, int hours) {
        List<ConnectionPoolSnapshot> snapshots = getPoolSnapshots(dataSourceId, hours);
        
        if (snapshots.isEmpty()) {
            return new ConnectionPoolAnalysisReport(dataSourceId, "无数据");
        }
        
        ConnectionPoolAnalysisReport report = new ConnectionPoolAnalysisReport(dataSourceId, "正常");
        
        // 计算统计指标
        OptionalDouble avgActive = snapshots.stream().mapToInt(ConnectionPoolSnapshot::getActiveConnections).average();
        OptionalDouble avgIdle = snapshots.stream().mapToInt(ConnectionPoolSnapshot::getIdleConnections).average();
        OptionalDouble avgTotal = snapshots.stream().mapToInt(ConnectionPoolSnapshot::getTotalConnections).average();
        OptionalDouble avgWaiting = snapshots.stream().mapToInt(ConnectionPoolSnapshot::getThreadsAwaitingConnection).average();
        
        int maxActive = snapshots.stream().mapToInt(ConnectionPoolSnapshot::getActiveConnections).max().orElse(0);
        int maxWaiting = snapshots.stream().mapToInt(ConnectionPoolSnapshot::getThreadsAwaitingConnection).max().orElse(0);
        
        report.setAverageActiveConnections(avgActive.orElse(0));
        report.setAverageIdleConnections(avgIdle.orElse(0));
        report.setAverageTotalConnections(avgTotal.orElse(0));
        report.setAverageWaitingThreads(avgWaiting.orElse(0));
        report.setMaxActiveConnections(maxActive);
        report.setMaxWaitingThreads(maxWaiting);
        
        // 计算利用率
        if (avgTotal.isPresent() && avgTotal.getAsDouble() > 0) {
            report.setUtilizationRate(avgActive.getAsDouble() / avgTotal.getAsDouble());
        }
        
        // 分析性能问题
        List<String> issues = analyzeConnectionPoolIssues(snapshots);
        report.setPerformanceIssues(issues);
        
        // 设置状态
        if (!issues.isEmpty()) {
            report.setStatus("警告");
        }
        
        report.setAnalysisTime(LocalDateTime.now());
        report.setDataPoints(snapshots.size());
        
        return report;
    }

    /**
     * 获取慢查询分析报告
     * 
     * @param dataSourceId 数据源ID
     * @param hours 分析时间范围（小时）
     * @return 慢查询分析报告
     */
    public SlowQueryAnalysisReport getSlowQueryAnalysis(Long dataSourceId, int hours) {
        List<SlowQueryRecord> slowQueries = getSlowQueries(dataSourceId, hours);
        
        SlowQueryAnalysisReport report = new SlowQueryAnalysisReport();
        report.setDataSourceId(dataSourceId);
        report.setAnalysisTime(LocalDateTime.now());
        report.setTotalSlowQueries(slowQueries.size());
        
        if (slowQueries.isEmpty()) {
            report.setAverageExecutionTime(0);
            report.setMaxExecutionTime(0);
            report.setTopSlowQueries(Collections.emptyList());
            return report;
        }
        
        // 计算统计指标
        OptionalDouble avgTime = slowQueries.stream().mapToLong(SlowQueryRecord::getExecutionTime).average();
        long maxTime = slowQueries.stream().mapToLong(SlowQueryRecord::getExecutionTime).max().orElse(0);
        
        report.setAverageExecutionTime(avgTime.orElse(0));
        report.setMaxExecutionTime(maxTime);
        
        // 获取最慢的查询（前10个）
        List<SlowQueryRecord> topSlowQueries = slowQueries.stream()
            .sorted((a, b) -> Long.compare(b.getExecutionTime(), a.getExecutionTime()))
            .limit(10)
            .collect(Collectors.toList());
        report.setTopSlowQueries(topSlowQueries);
        
        // 分析查询模式
        Map<String, Long> queryPatterns = analyzeQueryPatterns(slowQueries);
        report.setQueryPatterns(queryPatterns);
        
        // 生成优化建议
        List<String> suggestions = generateOptimizationSuggestions(slowQueries);
        report.setOptimizationSuggestions(suggestions);
        
        return report;
    }

    /**
     * 获取查询统计信息
     * 
     * @param dataSourceId 数据源ID
     * @return 查询统计信息
     */
    public QueryStatistics getQueryStatistics(Long dataSourceId) {
        return queryStatsMap.getOrDefault(dataSourceId, new QueryStatistics(dataSourceId));
    }

    /**
     * 获取所有数据源的性能概览
     * 
     * @return 性能概览
     */
    public PerformanceOverview getPerformanceOverview() {
        PerformanceOverview overview = new PerformanceOverview();
        overview.setGenerateTime(LocalDateTime.now());
        
        Map<Long, DataSourceManager.DataSourcePoolInfo> poolInfoMap = dataSourceManager.getAllPoolInfo();
        
        int totalDataSources = poolInfoMap.size();
        int healthyDataSources = 0;
        int totalActiveConnections = 0;
        int totalIdleConnections = 0;
        int totalWaitingThreads = 0;
        
        for (DataSourceManager.DataSourcePoolInfo poolInfo : poolInfoMap.values()) {
            if (!poolInfo.isClosed()) {
                healthyDataSources++;
            }
            totalActiveConnections += poolInfo.getActiveConnections();
            totalIdleConnections += poolInfo.getIdleConnections();
            totalWaitingThreads += poolInfo.getThreadsAwaitingConnection();
        }
        
        overview.setTotalDataSources(totalDataSources);
        overview.setHealthyDataSources(healthyDataSources);
        overview.setTotalActiveConnections(totalActiveConnections);
        overview.setTotalIdleConnections(totalIdleConnections);
        overview.setTotalWaitingThreads(totalWaitingThreads);
        
        // 计算慢查询统计
        int totalSlowQueries = slowQueryMap.values().stream()
            .mapToInt(List::size)
            .sum();
        overview.setTotalSlowQueries(totalSlowQueries);
        
        // 计算查询统计
        long totalQueries = queryStatsMap.values().stream()
            .mapToLong(stats -> stats.getTotalQueries().get())
            .sum();
        long successfulQueries = queryStatsMap.values().stream()
            .mapToLong(stats -> stats.getSuccessfulQueries().get())
            .sum();
        
        overview.setTotalQueries(totalQueries);
        overview.setSuccessfulQueries(successfulQueries);
        overview.setSuccessRate(totalQueries > 0 ? (double) successfulQueries / totalQueries : 0.0);
        
        return overview;
    }

    /**
     * 添加连接池快照
     */
    private void addPoolSnapshot(Long dataSourceId, ConnectionPoolSnapshot snapshot) {
        poolHistoryMap.computeIfAbsent(dataSourceId, k -> new ArrayList<>()).add(snapshot);
        
        // 限制历史记录大小
        List<ConnectionPoolSnapshot> snapshots = poolHistoryMap.get(dataSourceId);
        if (snapshots.size() > MAX_HISTORY_SIZE) {
            snapshots.remove(0);
        }
    }

    /**
     * 记录慢查询
     */
    private void recordSlowQuery(Long dataSourceId, String sql, long executionTime, 
                               boolean success, int rowCount) {
        SlowQueryRecord record = new SlowQueryRecord(
            dataSourceId, sql, executionTime, success, rowCount, LocalDateTime.now()
        );
        
        slowQueryMap.computeIfAbsent(dataSourceId, k -> new ArrayList<>()).add(record);
        
        // 限制慢查询记录大小
        List<SlowQueryRecord> slowQueries = slowQueryMap.get(dataSourceId);
        if (slowQueries.size() > MAX_SLOW_QUERY_SIZE) {
            slowQueries.remove(0);
        }
        
        logger.warn("记录慢查询: dataSourceId={}, executionTime={}ms, sql={}", 
                   dataSourceId, executionTime, sql.length() > 100 ? sql.substring(0, 100) + "..." : sql);
    }

    /**
     * 更新查询统计
     */
    private void updateQueryStatistics(Long dataSourceId, long executionTime, boolean success, int rowCount) {
        QueryStatistics stats = queryStatsMap.computeIfAbsent(dataSourceId, QueryStatistics::new);
        
        stats.getTotalQueries().incrementAndGet();
        if (success) {
            stats.getSuccessfulQueries().incrementAndGet();
        } else {
            stats.getFailedQueries().incrementAndGet();
        }
        
        stats.getTotalExecutionTime().addAndGet(executionTime);
        stats.getTotalRowsReturned().addAndGet(rowCount);
        
        // 更新最大执行时间
        long currentMax = stats.getMaxExecutionTime().get();
        if (executionTime > currentMax) {
            stats.getMaxExecutionTime().set(executionTime);
        }
        
        // 更新最小执行时间
        long currentMin = stats.getMinExecutionTime().get();
        if (currentMin == 0 || executionTime < currentMin) {
            stats.getMinExecutionTime().set(executionTime);
        }
        
        stats.setLastUpdateTime(LocalDateTime.now());
    }

    /**
     * 获取连接池快照
     */
    private List<ConnectionPoolSnapshot> getPoolSnapshots(Long dataSourceId, int hours) {
        List<ConnectionPoolSnapshot> snapshots = poolHistoryMap.getOrDefault(dataSourceId, Collections.emptyList());
        
        if (hours <= 0) {
            return new ArrayList<>(snapshots);
        }
        
        LocalDateTime cutoff = LocalDateTime.now().minusHours(hours);
        return snapshots.stream()
            .filter(snapshot -> snapshot.getTimestamp().isAfter(cutoff))
            .collect(Collectors.toList());
    }

    /**
     * 获取慢查询记录
     */
    private List<SlowQueryRecord> getSlowQueries(Long dataSourceId, int hours) {
        List<SlowQueryRecord> slowQueries = slowQueryMap.getOrDefault(dataSourceId, Collections.emptyList());
        
        if (hours <= 0) {
            return new ArrayList<>(slowQueries);
        }
        
        LocalDateTime cutoff = LocalDateTime.now().minusHours(hours);
        return slowQueries.stream()
            .filter(record -> record.getExecuteTime().isAfter(cutoff))
            .collect(Collectors.toList());
    }

    /**
     * 分析连接池问题
     */
    private List<String> analyzeConnectionPoolIssues(List<ConnectionPoolSnapshot> snapshots) {
        List<String> issues = new ArrayList<>();
        
        // 检查连接池利用率过高
        double avgUtilization = snapshots.stream()
            .mapToDouble(s -> s.getTotalConnections() > 0 ? 
                (double) s.getActiveConnections() / s.getTotalConnections() : 0)
            .average().orElse(0);
        
        if (avgUtilization > 0.8) {
            issues.add("连接池利用率过高 (" + String.format("%.1f%%", avgUtilization * 100) + ")，建议增加最大连接数");
        }
        
        // 检查等待线程过多
        double avgWaiting = snapshots.stream()
            .mapToInt(ConnectionPoolSnapshot::getThreadsAwaitingConnection)
            .average().orElse(0);
        
        if (avgWaiting > 5) {
            issues.add("平均等待连接的线程数过多 (" + String.format("%.1f", avgWaiting) + ")，可能存在连接泄漏或连接池配置不当");
        }
        
        // 检查连接数波动
        int maxActive = snapshots.stream().mapToInt(ConnectionPoolSnapshot::getActiveConnections).max().orElse(0);
        int minActive = snapshots.stream().mapToInt(ConnectionPoolSnapshot::getActiveConnections).min().orElse(0);
        
        if (maxActive - minActive > 10) {
            issues.add("活跃连接数波动较大 (" + minActive + "-" + maxActive + ")，建议检查应用负载模式");
        }
        
        return issues;
    }

    /**
     * 分析查询模式
     */
    private Map<String, Long> analyzeQueryPatterns(List<SlowQueryRecord> slowQueries) {
        Map<String, Long> patterns = new HashMap<>();
        
        for (SlowQueryRecord record : slowQueries) {
            String sql = record.getSql().toUpperCase().trim();
            
            // 提取查询类型
            String queryType = "OTHER";
            if (sql.startsWith("SELECT")) {
                queryType = "SELECT";
            } else if (sql.startsWith("INSERT")) {
                queryType = "INSERT";
            } else if (sql.startsWith("UPDATE")) {
                queryType = "UPDATE";
            } else if (sql.startsWith("DELETE")) {
                queryType = "DELETE";
            }
            
            patterns.merge(queryType, 1L, Long::sum);
        }
        
        return patterns;
    }

    /**
     * 生成优化建议
     */
    private List<String> generateOptimizationSuggestions(List<SlowQueryRecord> slowQueries) {
        List<String> suggestions = new ArrayList<>();
        
        // 分析慢查询特征
        long avgExecutionTime = (long) slowQueries.stream()
            .mapToLong(SlowQueryRecord::getExecutionTime)
            .average().orElse(0);
        
        if (avgExecutionTime > 10000) {
            suggestions.add("平均执行时间超过10秒，建议检查SQL语句是否存在全表扫描");
        }
        
        // 检查是否有重复的慢查询
        Map<String, Long> sqlCounts = slowQueries.stream()
            .collect(Collectors.groupingBy(
                record -> record.getSql().substring(0, Math.min(record.getSql().length(), 100)),
                Collectors.counting()
            ));
        
        long maxCount = sqlCounts.values().stream().mapToLong(Long::longValue).max().orElse(0);
        if (maxCount > 5) {
            suggestions.add("存在重复执行的慢查询，建议添加索引或优化查询逻辑");
        }
        
        // 检查返回行数
        double avgRowCount = slowQueries.stream()
            .mapToInt(SlowQueryRecord::getRowCount)
            .average().orElse(0);
        
        if (avgRowCount > 10000) {
            suggestions.add("查询返回行数过多，建议添加分页或限制返回结果集大小");
        }
        
        if (suggestions.isEmpty()) {
            suggestions.add("建议检查数据库索引配置和查询执行计划");
        }
        
        return suggestions;
    }

    // 内部类定义
    public static class ConnectionPoolSnapshot {
        private final Long dataSourceId;
        private final String dataSourceName;
        private final int activeConnections;
        private final int idleConnections;
        private final int totalConnections;
        private final int threadsAwaitingConnection;
        private final LocalDateTime timestamp;

        public ConnectionPoolSnapshot(Long dataSourceId, String dataSourceName, int activeConnections, 
                                    int idleConnections, int totalConnections, int threadsAwaitingConnection, 
                                    LocalDateTime timestamp) {
            this.dataSourceId = dataSourceId;
            this.dataSourceName = dataSourceName;
            this.activeConnections = activeConnections;
            this.idleConnections = idleConnections;
            this.totalConnections = totalConnections;
            this.threadsAwaitingConnection = threadsAwaitingConnection;
            this.timestamp = timestamp;
        }

        // Getters
        public Long getDataSourceId() { return dataSourceId; }
        public String getDataSourceName() { return dataSourceName; }
        public int getActiveConnections() { return activeConnections; }
        public int getIdleConnections() { return idleConnections; }
        public int getTotalConnections() { return totalConnections; }
        public int getThreadsAwaitingConnection() { return threadsAwaitingConnection; }
        public LocalDateTime getTimestamp() { return timestamp; }
    }

    public static class SlowQueryRecord {
        private final Long dataSourceId;
        private final String sql;
        private final long executionTime;
        private final boolean success;
        private final int rowCount;
        private final LocalDateTime executeTime;

        public SlowQueryRecord(Long dataSourceId, String sql, long executionTime, 
                             boolean success, int rowCount, LocalDateTime executeTime) {
            this.dataSourceId = dataSourceId;
            this.sql = sql;
            this.executionTime = executionTime;
            this.success = success;
            this.rowCount = rowCount;
            this.executeTime = executeTime;
        }

        // Getters
        public Long getDataSourceId() { return dataSourceId; }
        public String getSql() { return sql; }
        public long getExecutionTime() { return executionTime; }
        public boolean isSuccess() { return success; }
        public int getRowCount() { return rowCount; }
        public LocalDateTime getExecuteTime() { return executeTime; }
    }

    public static class QueryStatistics {
        private final Long dataSourceId;
        private final AtomicLong totalQueries = new AtomicLong(0);
        private final AtomicLong successfulQueries = new AtomicLong(0);
        private final AtomicLong failedQueries = new AtomicLong(0);
        private final AtomicLong totalExecutionTime = new AtomicLong(0);
        private final AtomicLong totalRowsReturned = new AtomicLong(0);
        private final AtomicLong maxExecutionTime = new AtomicLong(0);
        private final AtomicLong minExecutionTime = new AtomicLong(0);
        private volatile LocalDateTime lastUpdateTime = LocalDateTime.now();

        public QueryStatistics(Long dataSourceId) {
            this.dataSourceId = dataSourceId;
        }

        public double getAverageExecutionTime() {
            long total = totalQueries.get();
            return total > 0 ? (double) totalExecutionTime.get() / total : 0.0;
        }

        public double getSuccessRate() {
            long total = totalQueries.get();
            return total > 0 ? (double) successfulQueries.get() / total : 0.0;
        }

        // Getters
        public Long getDataSourceId() { return dataSourceId; }
        public AtomicLong getTotalQueries() { return totalQueries; }
        public AtomicLong getSuccessfulQueries() { return successfulQueries; }
        public AtomicLong getFailedQueries() { return failedQueries; }
        public AtomicLong getTotalExecutionTime() { return totalExecutionTime; }
        public AtomicLong getTotalRowsReturned() { return totalRowsReturned; }
        public AtomicLong getMaxExecutionTime() { return maxExecutionTime; }
        public AtomicLong getMinExecutionTime() { return minExecutionTime; }
        public LocalDateTime getLastUpdateTime() { return lastUpdateTime; }
        public void setLastUpdateTime(LocalDateTime lastUpdateTime) { this.lastUpdateTime = lastUpdateTime; }
    }

    public static class ConnectionPoolAnalysisReport {
        private Long dataSourceId;
        private String status;
        private double averageActiveConnections;
        private double averageIdleConnections;
        private double averageTotalConnections;
        private double averageWaitingThreads;
        private int maxActiveConnections;
        private int maxWaitingThreads;
        private double utilizationRate;
        private List<String> performanceIssues;
        private LocalDateTime analysisTime;
        private int dataPoints;

        public ConnectionPoolAnalysisReport(Long dataSourceId, String status) {
            this.dataSourceId = dataSourceId;
            this.status = status;
            this.performanceIssues = new ArrayList<>();
        }

        // Getters and Setters
        public Long getDataSourceId() { return dataSourceId; }
        public void setDataSourceId(Long dataSourceId) { this.dataSourceId = dataSourceId; }
        public String getStatus() { return status; }
        public void setStatus(String status) { this.status = status; }
        public double getAverageActiveConnections() { return averageActiveConnections; }
        public void setAverageActiveConnections(double averageActiveConnections) { this.averageActiveConnections = averageActiveConnections; }
        public double getAverageIdleConnections() { return averageIdleConnections; }
        public void setAverageIdleConnections(double averageIdleConnections) { this.averageIdleConnections = averageIdleConnections; }
        public double getAverageTotalConnections() { return averageTotalConnections; }
        public void setAverageTotalConnections(double averageTotalConnections) { this.averageTotalConnections = averageTotalConnections; }
        public double getAverageWaitingThreads() { return averageWaitingThreads; }
        public void setAverageWaitingThreads(double averageWaitingThreads) { this.averageWaitingThreads = averageWaitingThreads; }
        public int getMaxActiveConnections() { return maxActiveConnections; }
        public void setMaxActiveConnections(int maxActiveConnections) { this.maxActiveConnections = maxActiveConnections; }
        public int getMaxWaitingThreads() { return maxWaitingThreads; }
        public void setMaxWaitingThreads(int maxWaitingThreads) { this.maxWaitingThreads = maxWaitingThreads; }
        public double getUtilizationRate() { return utilizationRate; }
        public void setUtilizationRate(double utilizationRate) { this.utilizationRate = utilizationRate; }
        public List<String> getPerformanceIssues() { return performanceIssues; }
        public void setPerformanceIssues(List<String> performanceIssues) { this.performanceIssues = performanceIssues; }
        public LocalDateTime getAnalysisTime() { return analysisTime; }
        public void setAnalysisTime(LocalDateTime analysisTime) { this.analysisTime = analysisTime; }
        public int getDataPoints() { return dataPoints; }
        public void setDataPoints(int dataPoints) { this.dataPoints = dataPoints; }
    }

    public static class SlowQueryAnalysisReport {
        private Long dataSourceId;
        private LocalDateTime analysisTime;
        private int totalSlowQueries;
        private double averageExecutionTime;
        private long maxExecutionTime;
        private List<SlowQueryRecord> topSlowQueries;
        private Map<String, Long> queryPatterns;
        private List<String> optimizationSuggestions;

        // Getters and Setters
        public Long getDataSourceId() { return dataSourceId; }
        public void setDataSourceId(Long dataSourceId) { this.dataSourceId = dataSourceId; }
        public LocalDateTime getAnalysisTime() { return analysisTime; }
        public void setAnalysisTime(LocalDateTime analysisTime) { this.analysisTime = analysisTime; }
        public int getTotalSlowQueries() { return totalSlowQueries; }
        public void setTotalSlowQueries(int totalSlowQueries) { this.totalSlowQueries = totalSlowQueries; }
        public double getAverageExecutionTime() { return averageExecutionTime; }
        public void setAverageExecutionTime(double averageExecutionTime) { this.averageExecutionTime = averageExecutionTime; }
        public long getMaxExecutionTime() { return maxExecutionTime; }
        public void setMaxExecutionTime(long maxExecutionTime) { this.maxExecutionTime = maxExecutionTime; }
        public List<SlowQueryRecord> getTopSlowQueries() { return topSlowQueries; }
        public void setTopSlowQueries(List<SlowQueryRecord> topSlowQueries) { this.topSlowQueries = topSlowQueries; }
        public Map<String, Long> getQueryPatterns() { return queryPatterns; }
        public void setQueryPatterns(Map<String, Long> queryPatterns) { this.queryPatterns = queryPatterns; }
        public List<String> getOptimizationSuggestions() { return optimizationSuggestions; }
        public void setOptimizationSuggestions(List<String> optimizationSuggestions) { this.optimizationSuggestions = optimizationSuggestions; }
    }

    public static class PerformanceOverview {
        private LocalDateTime generateTime;
        private int totalDataSources;
        private int healthyDataSources;
        private int totalActiveConnections;
        private int totalIdleConnections;
        private int totalWaitingThreads;
        private int totalSlowQueries;
        private long totalQueries;
        private long successfulQueries;
        private double successRate;

        // Getters and Setters
        public LocalDateTime getGenerateTime() { return generateTime; }
        public void setGenerateTime(LocalDateTime generateTime) { this.generateTime = generateTime; }
        public int getTotalDataSources() { return totalDataSources; }
        public void setTotalDataSources(int totalDataSources) { this.totalDataSources = totalDataSources; }
        public int getHealthyDataSources() { return healthyDataSources; }
        public void setHealthyDataSources(int healthyDataSources) { this.healthyDataSources = healthyDataSources; }
        public int getTotalActiveConnections() { return totalActiveConnections; }
        public void setTotalActiveConnections(int totalActiveConnections) { this.totalActiveConnections = totalActiveConnections; }
        public int getTotalIdleConnections() { return totalIdleConnections; }
        public void setTotalIdleConnections(int totalIdleConnections) { this.totalIdleConnections = totalIdleConnections; }
        public int getTotalWaitingThreads() { return totalWaitingThreads; }
        public void setTotalWaitingThreads(int totalWaitingThreads) { this.totalWaitingThreads = totalWaitingThreads; }
        public int getTotalSlowQueries() { return totalSlowQueries; }
        public void setTotalSlowQueries(int totalSlowQueries) { this.totalSlowQueries = totalSlowQueries; }
        public long getTotalQueries() { return totalQueries; }
        public void setTotalQueries(long totalQueries) { this.totalQueries = totalQueries; }
        public long getSuccessfulQueries() { return successfulQueries; }
        public void setSuccessfulQueries(long successfulQueries) { this.successfulQueries = successfulQueries; }
        public double getSuccessRate() { return successRate; }
        public void setSuccessRate(double successRate) { this.successRate = successRate; }
    }
}