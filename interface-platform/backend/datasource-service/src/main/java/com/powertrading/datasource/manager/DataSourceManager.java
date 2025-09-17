package com.powertrading.datasource.manager;

import com.powertrading.datasource.config.DataSourceConfig;
import com.powertrading.datasource.entity.DataSource;
import com.powertrading.datasource.exception.DataSourceException;
import com.zaxxer.hikari.HikariDataSource;
import com.zaxxer.hikari.HikariPoolMXBean;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import javax.annotation.PreDestroy;
import javax.management.JMX;
import javax.management.MBeanServer;
import javax.management.ObjectName;
import java.lang.management.ManagementFactory;
import java.sql.Connection;
import java.sql.SQLException;
import java.time.LocalDateTime;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.locks.ReentrantReadWriteLock;

/**
 * 多数据源连接管理器
 * 负责管理和维护多个数据源的连接池
 * 
 * @author PowerTrading Team
 * @version 1.0.0
 */
@Component
public class DataSourceManager {

    private static final Logger logger = LoggerFactory.getLogger(DataSourceManager.class);

    /**
     * 数据源连接池缓存
     * Key: 数据源ID, Value: HikariDataSource
     */
    private final Map<Long, HikariDataSource> dataSourceCache = new ConcurrentHashMap<>();

    /**
     * 数据源配置缓存
     * Key: 数据源ID, Value: DataSource配置
     */
    private final Map<Long, DataSource> configCache = new ConcurrentHashMap<>();

    /**
     * 读写锁，保证线程安全
     */
    private final ReentrantReadWriteLock lock = new ReentrantReadWriteLock();

    /**
     * MBean服务器，用于监控
     */
    private final MBeanServer mBeanServer = ManagementFactory.getPlatformMBeanServer();

    /**
     * 获取数据源连接
     * 
     * @param dataSourceId 数据源ID
     * @return 数据库连接
     * @throws DataSourceException 数据源异常
     */
    public Connection getConnection(Long dataSourceId) throws DataSourceException {
        lock.readLock().lock();
        try {
            HikariDataSource hikariDataSource = dataSourceCache.get(dataSourceId);
            if (hikariDataSource == null) {
                throw new DataSourceException("数据源不存在或未初始化: " + dataSourceId);
            }

            if (hikariDataSource.isClosed()) {
                throw new DataSourceException("数据源连接池已关闭: " + dataSourceId);
            }

            Connection connection = hikariDataSource.getConnection();
            logger.debug("获取数据源连接成功: dataSourceId={}, activeConnections={}", 
                        dataSourceId, hikariDataSource.getHikariPoolMXBean().getActiveConnections());
            
            return connection;
        } catch (SQLException e) {
            logger.error("获取数据源连接失败: dataSourceId={}", dataSourceId, e);
            throw new DataSourceException("获取数据源连接失败: " + e.getMessage(), e);
        } finally {
            lock.readLock().unlock();
        }
    }

    /**
     * 添加数据源
     * 
     * @param dataSource 数据源配置
     * @throws DataSourceException 数据源异常
     */
    public void addDataSource(DataSource dataSource) throws DataSourceException {
        lock.writeLock().lock();
        try {
            validateDataSourceConfig(dataSource);
            
            // 创建HikariCP数据源
            HikariDataSource hikariDataSource = DataSourceConfig.createHikariDataSource(dataSource);
            
            // 测试连接
            testConnection(hikariDataSource);
            
            // 缓存数据源
            dataSourceCache.put(dataSource.getId(), hikariDataSource);
            configCache.put(dataSource.getId(), dataSource);
            
            logger.info("数据源添加成功: id={}, name={}, type={}", 
                       dataSource.getId(), dataSource.getName(), dataSource.getType());
        } catch (Exception e) {
            logger.error("数据源添加失败: name={}", dataSource.getName(), e);
            throw new DataSourceException("数据源添加失败: " + e.getMessage(), e);
        } finally {
            lock.writeLock().unlock();
        }
    }

    /**
     * 更新数据源
     * 
     * @param dataSource 数据源配置
     * @throws DataSourceException 数据源异常
     */
    public void updateDataSource(DataSource dataSource) throws DataSourceException {
        lock.writeLock().lock();
        try {
            validateDataSourceConfig(dataSource);
            
            // 关闭旧的数据源
            removeDataSource(dataSource.getId());
            
            // 添加新的数据源
            addDataSource(dataSource);
            
            logger.info("数据源更新成功: id={}, name={}", dataSource.getId(), dataSource.getName());
        } catch (Exception e) {
            logger.error("数据源更新失败: id={}", dataSource.getId(), e);
            throw new DataSourceException("数据源更新失败: " + e.getMessage(), e);
        } finally {
            lock.writeLock().unlock();
        }
    }

    /**
     * 移除数据源
     * 
     * @param dataSourceId 数据源ID
     */
    public void removeDataSource(Long dataSourceId) {
        lock.writeLock().lock();
        try {
            HikariDataSource hikariDataSource = dataSourceCache.remove(dataSourceId);
            configCache.remove(dataSourceId);
            
            if (hikariDataSource != null && !hikariDataSource.isClosed()) {
                hikariDataSource.close();
                logger.info("数据源移除成功: dataSourceId={}", dataSourceId);
            }
        } catch (Exception e) {
            logger.error("数据源移除失败: dataSourceId={}", dataSourceId, e);
        } finally {
            lock.writeLock().unlock();
        }
    }

    /**
     * 测试数据源连接
     * 
     * @param dataSourceId 数据源ID
     * @return 连接测试结果
     */
    public boolean testConnection(Long dataSourceId) {
        lock.readLock().lock();
        try {
            HikariDataSource hikariDataSource = dataSourceCache.get(dataSourceId);
            if (hikariDataSource == null) {
                return false;
            }
            
            return testConnection(hikariDataSource);
        } finally {
            lock.readLock().unlock();
        }
    }

    /**
     * 测试数据源连接
     * 
     * @param hikariDataSource HikariCP数据源
     * @return 连接测试结果
     */
    private boolean testConnection(HikariDataSource hikariDataSource) {
        try (Connection connection = hikariDataSource.getConnection()) {
            return connection.isValid(5); // 5秒超时
        } catch (SQLException e) {
            logger.warn("数据源连接测试失败: {}", e.getMessage());
            return false;
        }
    }

    /**
     * 获取数据源连接池信息
     * 
     * @param dataSourceId 数据源ID
     * @return 连接池信息
     */
    public DataSourcePoolInfo getPoolInfo(Long dataSourceId) {
        lock.readLock().lock();
        try {
            HikariDataSource hikariDataSource = dataSourceCache.get(dataSourceId);
            DataSource config = configCache.get(dataSourceId);
            
            if (hikariDataSource == null || config == null) {
                return null;
            }
            
            HikariPoolMXBean poolMXBean = hikariDataSource.getHikariPoolMXBean();
            
            return new DataSourcePoolInfo(
                dataSourceId,
                config.getName(),
                config.getType(),
                poolMXBean.getActiveConnections(),
                poolMXBean.getIdleConnections(),
                poolMXBean.getTotalConnections(),
                poolMXBean.getThreadsAwaitingConnection(),
                hikariDataSource.isClosed(),
                LocalDateTime.now()
            );
        } finally {
            lock.readLock().unlock();
        }
    }

    /**
     * 获取所有数据源连接池信息
     * 
     * @return 连接池信息列表
     */
    public Map<Long, DataSourcePoolInfo> getAllPoolInfo() {
        lock.readLock().lock();
        try {
            Map<Long, DataSourcePoolInfo> poolInfoMap = new ConcurrentHashMap<>();
            
            for (Long dataSourceId : dataSourceCache.keySet()) {
                DataSourcePoolInfo poolInfo = getPoolInfo(dataSourceId);
                if (poolInfo != null) {
                    poolInfoMap.put(dataSourceId, poolInfo);
                }
            }
            
            return poolInfoMap;
        } finally {
            lock.readLock().unlock();
        }
    }

    /**
     * 检查数据源是否存在
     * 
     * @param dataSourceId 数据源ID
     * @return 是否存在
     */
    public boolean exists(Long dataSourceId) {
        lock.readLock().lock();
        try {
            return dataSourceCache.containsKey(dataSourceId);
        } finally {
            lock.readLock().unlock();
        }
    }

    /**
     * 获取数据源配置
     * 
     * @param dataSourceId 数据源ID
     * @return 数据源配置
     */
    public DataSource getDataSourceConfig(Long dataSourceId) {
        lock.readLock().lock();
        try {
            return configCache.get(dataSourceId);
        } finally {
            lock.readLock().unlock();
        }
    }

    /**
     * 验证数据源配置
     * 
     * @param dataSource 数据源配置
     * @throws DataSourceException 配置验证异常
     */
    private void validateDataSourceConfig(DataSource dataSource) throws DataSourceException {
        if (dataSource == null) {
            throw new DataSourceException("数据源配置不能为空");
        }
        
        if (dataSource.getName() == null || dataSource.getName().trim().isEmpty()) {
            throw new DataSourceException("数据源名称不能为空");
        }
        
        if (dataSource.getType() == null || dataSource.getType().trim().isEmpty()) {
            throw new DataSourceException("数据源类型不能为空");
        }
        
        if (!DataSourceConfig.isSupportedDbType(dataSource.getType())) {
            throw new DataSourceException("不支持的数据库类型: " + dataSource.getType());
        }
        
        if (dataSource.getUrl() == null || dataSource.getUrl().trim().isEmpty()) {
            throw new DataSourceException("数据库URL不能为空");
        }
        
        if (dataSource.getUsername() == null || dataSource.getUsername().trim().isEmpty()) {
            throw new DataSourceException("数据库用户名不能为空");
        }
        
        if (dataSource.getPassword() == null || dataSource.getPassword().trim().isEmpty()) {
            throw new DataSourceException("数据库密码不能为空");
        }
        
        // 设置默认驱动类
        if (dataSource.getDriverClass() == null || dataSource.getDriverClass().trim().isEmpty()) {
            dataSource.setDriverClass(DataSourceConfig.getDriverClass(dataSource.getType()));
        }
    }

    /**
     * 应用关闭时清理资源
     */
    @PreDestroy
    public void destroy() {
        lock.writeLock().lock();
        try {
            logger.info("开始关闭所有数据源连接池...");
            
            for (Map.Entry<Long, HikariDataSource> entry : dataSourceCache.entrySet()) {
                try {
                    HikariDataSource hikariDataSource = entry.getValue();
                    if (!hikariDataSource.isClosed()) {
                        hikariDataSource.close();
                        logger.info("数据源连接池关闭成功: dataSourceId={}", entry.getKey());
                    }
                } catch (Exception e) {
                    logger.error("数据源连接池关闭失败: dataSourceId={}", entry.getKey(), e);
                }
            }
            
            dataSourceCache.clear();
            configCache.clear();
            
            logger.info("所有数据源连接池关闭完成");
        } finally {
            lock.writeLock().unlock();
        }
    }

    /**
     * 数据源连接池信息
     */
    public static class DataSourcePoolInfo {
        private final Long dataSourceId;
        private final String name;
        private final String type;
        private final int activeConnections;
        private final int idleConnections;
        private final int totalConnections;
        private final int threadsAwaitingConnection;
        private final boolean closed;
        private final LocalDateTime checkTime;

        public DataSourcePoolInfo(Long dataSourceId, String name, String type, 
                                 int activeConnections, int idleConnections, int totalConnections,
                                 int threadsAwaitingConnection, boolean closed, LocalDateTime checkTime) {
            this.dataSourceId = dataSourceId;
            this.name = name;
            this.type = type;
            this.activeConnections = activeConnections;
            this.idleConnections = idleConnections;
            this.totalConnections = totalConnections;
            this.threadsAwaitingConnection = threadsAwaitingConnection;
            this.closed = closed;
            this.checkTime = checkTime;
        }

        // Getters
        public Long getDataSourceId() { return dataSourceId; }
        public String getName() { return name; }
        public String getType() { return type; }
        public int getActiveConnections() { return activeConnections; }
        public int getIdleConnections() { return idleConnections; }
        public int getTotalConnections() { return totalConnections; }
        public int getThreadsAwaitingConnection() { return threadsAwaitingConnection; }
        public boolean isClosed() { return closed; }
        public LocalDateTime getCheckTime() { return checkTime; }
    }
}