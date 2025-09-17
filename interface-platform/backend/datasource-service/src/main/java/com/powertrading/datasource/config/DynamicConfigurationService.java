package com.powertrading.datasource.config;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.powertrading.datasource.entity.DataSource;
import com.powertrading.datasource.exception.DataSourceException;
import com.powertrading.datasource.manager.DataSourceManager;
import com.powertrading.datasource.repository.DataSourceRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.time.LocalDateTime;
import java.util.*;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

/**
 * 动态配置管理服务
 * 提供数据源配置的动态添加、更新、热重载等功能
 * 
 * @author PowerTrading Team
 * @version 1.0.0
 */
@Service
@Transactional
public class DynamicConfigurationService {

    private static final Logger logger = LoggerFactory.getLogger(DynamicConfigurationService.class);

    @Autowired
    private DataSourceRepository dataSourceRepository;

    @Autowired
    private DataSourceManager dataSourceManager;

    @Autowired
    private RedisTemplate<String, Object> redisTemplate;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private ApplicationEventPublisher eventPublisher;

    // 配置变更锁
    private final Map<Long, Object> configLocks = new ConcurrentHashMap<>();
    
    // 配置变更历史
    private final Map<Long, List<ConfigurationChange>> changeHistoryMap = new ConcurrentHashMap<>();
    
    // Redis键前缀
    private static final String CONFIG_LOCK_PREFIX = "datasource:config:lock:";
    private static final String CONFIG_BACKUP_PREFIX = "datasource:config:backup:";
    private static final String CONFIG_VALIDATION_PREFIX = "datasource:config:validation:";

    /**
     * 动态添加数据源配置
     * 
     * @param dataSource 数据源配置
     * @param validateOnly 是否仅验证不保存
     * @return 配置结果
     * @throws DataSourceException 配置异常
     */
    public ConfigurationResult addDataSourceDynamically(DataSource dataSource, boolean validateOnly) 
            throws DataSourceException {
        
        logger.info("开始动态添加数据源: name={}, validateOnly={}", dataSource.getName(), validateOnly);
        
        ConfigurationResult result = new ConfigurationResult();
        result.setOperation("ADD");
        result.setDataSourceName(dataSource.getName());
        result.setStartTime(LocalDateTime.now());
        
        try {
            // 1. 验证配置
            ValidationResult validation = validateDataSourceConfiguration(dataSource);
            result.setValidationResult(validation);
            
            if (!validation.isValid()) {
                result.setSuccess(false);
                result.setErrorMessage("配置验证失败: " + String.join(", ", validation.getErrors()));
                return result;
            }
            
            if (validateOnly) {
                result.setSuccess(true);
                result.setMessage("配置验证通过");
                return result;
            }
            
            // 2. 获取分布式锁
            String lockKey = CONFIG_LOCK_PREFIX + "add:" + dataSource.getName();
            if (!acquireDistributedLock(lockKey, 30)) {
                throw new DataSourceException("获取配置锁失败，可能有其他操作正在进行");
            }
            
            try {
                // 3. 检查名称冲突
                if (dataSourceRepository.findByName(dataSource.getName()).isPresent()) {
                    throw new DataSourceException("数据源名称已存在: " + dataSource.getName());
                }
                
                // 4. 保存配置到数据库
                DataSource savedDataSource = dataSourceRepository.save(dataSource);
                
                // 5. 动态加载到连接管理器
                dataSourceManager.addDataSource(savedDataSource);
                
                // 6. 测试连接
                boolean connected = dataSourceManager.testConnection(savedDataSource.getId());
                if (!connected) {
                    logger.warn("数据源连接测试失败，但配置已保存: id={}", savedDataSource.getId());
                }
                
                // 7. 记录配置变更
                recordConfigurationChange(savedDataSource.getId(), "ADD", null, savedDataSource, "动态添加数据源");
                
                // 8. 发布配置变更事件
                publishConfigurationEvent(new DataSourceConfigurationEvent(
                    DataSourceConfigurationEvent.EventType.ADDED, savedDataSource));
                
                result.setSuccess(true);
                result.setDataSourceId(savedDataSource.getId());
                result.setMessage("数据源添加成功");
                result.setConnectionTestResult(connected);
                
            } finally {
                releaseDistributedLock(lockKey);
            }
            
        } catch (Exception e) {
            logger.error("动态添加数据源失败: name={}", dataSource.getName(), e);
            result.setSuccess(false);
            result.setErrorMessage(e.getMessage());
        } finally {
            result.setEndTime(LocalDateTime.now());
        }
        
        return result;
    }

    /**
     * 动态更新数据源配置
     * 
     * @param id 数据源ID
     * @param updatedDataSource 更新的配置
     * @param hotReload 是否热重载
     * @return 配置结果
     * @throws DataSourceException 配置异常
     */
    public ConfigurationResult updateDataSourceDynamically(Long id, DataSource updatedDataSource, boolean hotReload) 
            throws DataSourceException {
        
        logger.info("开始动态更新数据源: id={}, hotReload={}", id, hotReload);
        
        ConfigurationResult result = new ConfigurationResult();
        result.setOperation("UPDATE");
        result.setDataSourceId(id);
        result.setStartTime(LocalDateTime.now());
        
        try {
            // 1. 获取原始配置
            DataSource originalDataSource = dataSourceRepository.findById(id)
                .orElseThrow(() -> new DataSourceException("数据源不存在: " + id));
            
            result.setDataSourceName(originalDataSource.getName());
            
            // 2. 验证新配置
            ValidationResult validation = validateDataSourceConfiguration(updatedDataSource);
            result.setValidationResult(validation);
            
            if (!validation.isValid()) {
                result.setSuccess(false);
                result.setErrorMessage("配置验证失败: " + String.join(", ", validation.getErrors()));
                return result;
            }
            
            // 3. 获取分布式锁
            String lockKey = CONFIG_LOCK_PREFIX + "update:" + id;
            if (!acquireDistributedLock(lockKey, 30)) {
                throw new DataSourceException("获取配置锁失败，可能有其他操作正在进行");
            }
            
            try {
                // 4. 备份原始配置
                backupConfiguration(id, originalDataSource);
                
                // 5. 检查名称冲突（排除自己）
                if (dataSourceRepository.existsByNameAndIdNot(updatedDataSource.getName(), id)) {
                    throw new DataSourceException("数据源名称已存在: " + updatedDataSource.getName());
                }
                
                // 6. 更新数据库配置
                updateDataSourceFields(originalDataSource, updatedDataSource);
                DataSource savedDataSource = dataSourceRepository.save(originalDataSource);
                
                if (hotReload) {
                    // 7. 热重载连接池
                    dataSourceManager.updateDataSource(savedDataSource);
                    
                    // 8. 测试新连接
                    boolean connected = dataSourceManager.testConnection(id);
                    result.setConnectionTestResult(connected);
                    
                    if (!connected) {
                        logger.warn("数据源连接测试失败，尝试回滚: id={}", id);
                        // 可以选择回滚到原始配置
                    }
                }
                
                // 9. 记录配置变更
                recordConfigurationChange(id, "UPDATE", originalDataSource, savedDataSource, "动态更新数据源");
                
                // 10. 发布配置变更事件
                publishConfigurationEvent(new DataSourceConfigurationEvent(
                    DataSourceConfigurationEvent.EventType.UPDATED, savedDataSource));
                
                result.setSuccess(true);
                result.setMessage(hotReload ? "数据源热更新成功" : "数据源配置更新成功（需重启生效）");
                
            } finally {
                releaseDistributedLock(lockKey);
            }
            
        } catch (Exception e) {
            logger.error("动态更新数据源失败: id={}", id, e);
            result.setSuccess(false);
            result.setErrorMessage(e.getMessage());
        } finally {
            result.setEndTime(LocalDateTime.now());
        }
        
        return result;
    }

    /**
     * 批量配置操作
     * 
     * @param operations 批量操作列表
     * @return 批量操作结果
     */
    public BatchConfigurationResult batchConfigurationOperations(List<BatchOperation> operations) {
        logger.info("开始批量配置操作: operationCount={}", operations.size());
        
        BatchConfigurationResult batchResult = new BatchConfigurationResult();
        batchResult.setStartTime(LocalDateTime.now());
        batchResult.setTotalOperations(operations.size());
        
        List<ConfigurationResult> results = new ArrayList<>();
        int successCount = 0;
        
        for (BatchOperation operation : operations) {
            try {
                ConfigurationResult result = null;
                
                switch (operation.getOperationType()) {
                    case ADD:
                        result = addDataSourceDynamically(operation.getDataSource(), false);
                        break;
                    case UPDATE:
                        result = updateDataSourceDynamically(operation.getDataSourceId(), 
                                                           operation.getDataSource(), operation.isHotReload());
                        break;
                    case DELETE:
                        result = deleteDataSourceDynamically(operation.getDataSourceId());
                        break;
                }
                
                if (result != null) {
                    results.add(result);
                    if (result.isSuccess()) {
                        successCount++;
                    }
                }
                
            } catch (Exception e) {
                logger.error("批量操作失败: operation={}", operation, e);
                ConfigurationResult errorResult = new ConfigurationResult();
                errorResult.setSuccess(false);
                errorResult.setErrorMessage(e.getMessage());
                results.add(errorResult);
            }
        }
        
        batchResult.setResults(results);
        batchResult.setSuccessCount(successCount);
        batchResult.setFailureCount(operations.size() - successCount);
        batchResult.setEndTime(LocalDateTime.now());
        
        logger.info("批量配置操作完成: total={}, success={}, failure={}", 
                   operations.size(), successCount, operations.size() - successCount);
        
        return batchResult;
    }

    /**
     * 动态删除数据源
     * 
     * @param id 数据源ID
     * @return 配置结果
     * @throws DataSourceException 配置异常
     */
    public ConfigurationResult deleteDataSourceDynamically(Long id) throws DataSourceException {
        logger.info("开始动态删除数据源: id={}", id);
        
        ConfigurationResult result = new ConfigurationResult();
        result.setOperation("DELETE");
        result.setDataSourceId(id);
        result.setStartTime(LocalDateTime.now());
        
        try {
            // 1. 获取原始配置
            DataSource dataSource = dataSourceRepository.findById(id)
                .orElseThrow(() -> new DataSourceException("数据源不存在: " + id));
            
            result.setDataSourceName(dataSource.getName());
            
            // 2. 获取分布式锁
            String lockKey = CONFIG_LOCK_PREFIX + "delete:" + id;
            if (!acquireDistributedLock(lockKey, 30)) {
                throw new DataSourceException("获取配置锁失败，可能有其他操作正在进行");
            }
            
            try {
                // 3. 备份配置
                backupConfiguration(id, dataSource);
                
                // 4. 从连接管理器移除
                dataSourceManager.removeDataSource(id);
                
                // 5. 从数据库删除
                dataSourceRepository.deleteById(id);
                
                // 6. 记录配置变更
                recordConfigurationChange(id, "DELETE", dataSource, null, "动态删除数据源");
                
                // 7. 发布配置变更事件
                publishConfigurationEvent(new DataSourceConfigurationEvent(
                    DataSourceConfigurationEvent.EventType.DELETED, dataSource));
                
                result.setSuccess(true);
                result.setMessage("数据源删除成功");
                
            } finally {
                releaseDistributedLock(lockKey);
            }
            
        } catch (Exception e) {
            logger.error("动态删除数据源失败: id={}", id, e);
            result.setSuccess(false);
            result.setErrorMessage(e.getMessage());
        } finally {
            result.setEndTime(LocalDateTime.now());
        }
        
        return result;
    }

    /**
     * 重新加载所有数据源配置
     * 
     * @return 重载结果
     */
    public CompletableFuture<ReloadResult> reloadAllDataSources() {
        return CompletableFuture.supplyAsync(() -> {
            logger.info("开始重新加载所有数据源配置");
            
            ReloadResult result = new ReloadResult();
            result.setStartTime(LocalDateTime.now());
            
            try {
                List<DataSource> allDataSources = dataSourceRepository.findEnabledDataSources();
                result.setTotalDataSources(allDataSources.size());
                
                int successCount = 0;
                List<String> errors = new ArrayList<>();
                
                for (DataSource dataSource : allDataSources) {
                    try {
                        dataSourceManager.updateDataSource(dataSource);
                        successCount++;
                    } catch (Exception e) {
                        logger.error("重载数据源失败: id={}, name={}", dataSource.getId(), dataSource.getName(), e);
                        errors.add("数据源 " + dataSource.getName() + ": " + e.getMessage());
                    }
                }
                
                result.setSuccessCount(successCount);
                result.setFailureCount(allDataSources.size() - successCount);
                result.setErrors(errors);
                result.setSuccess(errors.isEmpty());
                
                logger.info("重新加载数据源配置完成: total={}, success={}, failure={}", 
                           allDataSources.size(), successCount, allDataSources.size() - successCount);
                
            } catch (Exception e) {
                logger.error("重新加载数据源配置异常", e);
                result.setSuccess(false);
                result.getErrors().add("重载异常: " + e.getMessage());
            } finally {
                result.setEndTime(LocalDateTime.now());
            }
            
            return result;
        });
    }

    /**
     * 获取配置变更历史
     * 
     * @param dataSourceId 数据源ID
     * @param limit 限制数量
     * @return 变更历史
     */
    public List<ConfigurationChange> getConfigurationHistory(Long dataSourceId, int limit) {
        List<ConfigurationChange> history = changeHistoryMap.getOrDefault(dataSourceId, Collections.emptyList());
        
        return history.stream()
            .sorted((a, b) -> b.getChangeTime().compareTo(a.getChangeTime()))
            .limit(limit > 0 ? limit : 50)
            .collect(Collectors.toList());
    }

    /**
     * 验证数据源配置
     */
    private ValidationResult validateDataSourceConfiguration(DataSource dataSource) {
        ValidationResult result = new ValidationResult();
        List<String> errors = new ArrayList<>();
        List<String> warnings = new ArrayList<>();
        
        // 基本字段验证
        if (!StringUtils.hasText(dataSource.getName())) {
            errors.add("数据源名称不能为空");
        }
        
        if (!StringUtils.hasText(dataSource.getType())) {
            errors.add("数据源类型不能为空");
        } else if (!DataSourceConfig.isSupportedDbType(dataSource.getType())) {
            errors.add("不支持的数据库类型: " + dataSource.getType());
        }
        
        if (!StringUtils.hasText(dataSource.getUrl())) {
            errors.add("数据库URL不能为空");
        }
        
        if (!StringUtils.hasText(dataSource.getUsername())) {
            errors.add("用户名不能为空");
        }
        
        if (!StringUtils.hasText(dataSource.getPassword())) {
            errors.add("密码不能为空");
        }
        
        // 连接池配置验证
        if (dataSource.getMinPoolSize() != null && dataSource.getMinPoolSize() < 1) {
            errors.add("最小连接池大小不能小于1");
        }
        
        if (dataSource.getMaxPoolSize() != null && dataSource.getMaxPoolSize() < 1) {
            errors.add("最大连接池大小不能小于1");
        }
        
        if (dataSource.getMinPoolSize() != null && dataSource.getMaxPoolSize() != null 
            && dataSource.getMinPoolSize() > dataSource.getMaxPoolSize()) {
            errors.add("最小连接池大小不能大于最大连接池大小");
        }
        
        // 性能建议
        if (dataSource.getMaxPoolSize() != null && dataSource.getMaxPoolSize() > 50) {
            warnings.add("最大连接池大小过大，可能影响数据库性能");
        }
        
        if (dataSource.getConnectionTimeout() != null && dataSource.getConnectionTimeout() > 60000) {
            warnings.add("连接超时时间过长，可能影响应用响应性能");
        }
        
        result.setValid(errors.isEmpty());
        result.setErrors(errors);
        result.setWarnings(warnings);
        
        return result;
    }

    /**
     * 获取分布式锁
     */
    private boolean acquireDistributedLock(String lockKey, int timeoutSeconds) {
        try {
            Boolean acquired = redisTemplate.opsForValue().setIfAbsent(lockKey, "locked", timeoutSeconds, TimeUnit.SECONDS);
            return Boolean.TRUE.equals(acquired);
        } catch (Exception e) {
            logger.error("获取分布式锁失败: key={}", lockKey, e);
            return false;
        }
    }

    /**
     * 释放分布式锁
     */
    private void releaseDistributedLock(String lockKey) {
        try {
            redisTemplate.delete(lockKey);
        } catch (Exception e) {
            logger.error("释放分布式锁失败: key={}", lockKey, e);
        }
    }

    /**
     * 备份配置
     */
    private void backupConfiguration(Long dataSourceId, DataSource dataSource) {
        try {
            String backupKey = CONFIG_BACKUP_PREFIX + dataSourceId + ":" + System.currentTimeMillis();
            redisTemplate.opsForValue().set(backupKey, dataSource, 7, TimeUnit.DAYS);
        } catch (Exception e) {
            logger.error("备份配置失败: dataSourceId={}", dataSourceId, e);
        }
    }

    /**
     * 更新数据源字段
     */
    private void updateDataSourceFields(DataSource existing, DataSource updated) {
        existing.setName(updated.getName());
        existing.setDescription(updated.getDescription());
        existing.setType(updated.getType());
        existing.setUrl(updated.getUrl());
        existing.setUsername(updated.getUsername());
        
        if (StringUtils.hasText(updated.getPassword())) {
            existing.setPassword(updated.getPassword());
        }
        
        existing.setDriverClass(updated.getDriverClass());
        existing.setMinPoolSize(updated.getMinPoolSize());
        existing.setMaxPoolSize(updated.getMaxPoolSize());
        existing.setConnectionTimeout(updated.getConnectionTimeout());
        existing.setIdleTimeout(updated.getIdleTimeout());
        existing.setMaxLifetime(updated.getMaxLifetime());
        existing.setValidationTimeout(updated.getValidationTimeout());
        existing.setLeakDetectionThreshold(updated.getLeakDetectionThreshold());
        existing.setConfigJson(updated.getConfigJson());
        existing.setUpdatedBy(updated.getUpdatedBy());
    }

    /**
     * 记录配置变更
     */
    private void recordConfigurationChange(Long dataSourceId, String operation, 
                                         DataSource oldConfig, DataSource newConfig, String description) {
        try {
            ConfigurationChange change = new ConfigurationChange();
            change.setDataSourceId(dataSourceId);
            change.setOperation(operation);
            change.setOldConfiguration(oldConfig);
            change.setNewConfiguration(newConfig);
            change.setDescription(description);
            change.setChangeTime(LocalDateTime.now());
            change.setOperator("system"); // 可以从上下文获取当前用户
            
            changeHistoryMap.computeIfAbsent(dataSourceId, k -> new ArrayList<>()).add(change);
            
            // 限制历史记录数量
            List<ConfigurationChange> history = changeHistoryMap.get(dataSourceId);
            if (history.size() > 100) {
                history.remove(0);
            }
            
        } catch (Exception e) {
            logger.error("记录配置变更失败: dataSourceId={}", dataSourceId, e);
        }
    }

    /**
     * 发布配置变更事件
     */
    private void publishConfigurationEvent(DataSourceConfigurationEvent event) {
        try {
            eventPublisher.publishEvent(event);
        } catch (Exception e) {
            logger.error("发布配置变更事件失败", e);
        }
    }

    // 内部类定义
    public static class ConfigurationResult {
        private String operation;
        private Long dataSourceId;
        private String dataSourceName;
        private boolean success;
        private String message;
        private String errorMessage;
        private ValidationResult validationResult;
        private Boolean connectionTestResult;
        private LocalDateTime startTime;
        private LocalDateTime endTime;

        // Getters and Setters
        public String getOperation() { return operation; }
        public void setOperation(String operation) { this.operation = operation; }
        public Long getDataSourceId() { return dataSourceId; }
        public void setDataSourceId(Long dataSourceId) { this.dataSourceId = dataSourceId; }
        public String getDataSourceName() { return dataSourceName; }
        public void setDataSourceName(String dataSourceName) { this.dataSourceName = dataSourceName; }
        public boolean isSuccess() { return success; }
        public void setSuccess(boolean success) { this.success = success; }
        public String getMessage() { return message; }
        public void setMessage(String message) { this.message = message; }
        public String getErrorMessage() { return errorMessage; }
        public void setErrorMessage(String errorMessage) { this.errorMessage = errorMessage; }
        public ValidationResult getValidationResult() { return validationResult; }
        public void setValidationResult(ValidationResult validationResult) { this.validationResult = validationResult; }
        public Boolean getConnectionTestResult() { return connectionTestResult; }
        public void setConnectionTestResult(Boolean connectionTestResult) { this.connectionTestResult = connectionTestResult; }
        public LocalDateTime getStartTime() { return startTime; }
        public void setStartTime(LocalDateTime startTime) { this.startTime = startTime; }
        public LocalDateTime getEndTime() { return endTime; }
        public void setEndTime(LocalDateTime endTime) { this.endTime = endTime; }
    }

    public static class ValidationResult {
        private boolean valid;
        private List<String> errors = new ArrayList<>();
        private List<String> warnings = new ArrayList<>();

        // Getters and Setters
        public boolean isValid() { return valid; }
        public void setValid(boolean valid) { this.valid = valid; }
        public List<String> getErrors() { return errors; }
        public void setErrors(List<String> errors) { this.errors = errors; }
        public List<String> getWarnings() { return warnings; }
        public void setWarnings(List<String> warnings) { this.warnings = warnings; }
    }

    public static class BatchOperation {
        public enum OperationType { ADD, UPDATE, DELETE }
        
        private OperationType operationType;
        private Long dataSourceId;
        private DataSource dataSource;
        private boolean hotReload = true;

        // Getters and Setters
        public OperationType getOperationType() { return operationType; }
        public void setOperationType(OperationType operationType) { this.operationType = operationType; }
        public Long getDataSourceId() { return dataSourceId; }
        public void setDataSourceId(Long dataSourceId) { this.dataSourceId = dataSourceId; }
        public DataSource getDataSource() { return dataSource; }
        public void setDataSource(DataSource dataSource) { this.dataSource = dataSource; }
        public boolean isHotReload() { return hotReload; }
        public void setHotReload(boolean hotReload) { this.hotReload = hotReload; }
    }

    public static class BatchConfigurationResult {
        private int totalOperations;
        private int successCount;
        private int failureCount;
        private List<ConfigurationResult> results;
        private LocalDateTime startTime;
        private LocalDateTime endTime;

        // Getters and Setters
        public int getTotalOperations() { return totalOperations; }
        public void setTotalOperations(int totalOperations) { this.totalOperations = totalOperations; }
        public int getSuccessCount() { return successCount; }
        public void setSuccessCount(int successCount) { this.successCount = successCount; }
        public int getFailureCount() { return failureCount; }
        public void setFailureCount(int failureCount) { this.failureCount = failureCount; }
        public List<ConfigurationResult> getResults() { return results; }
        public void setResults(List<ConfigurationResult> results) { this.results = results; }
        public LocalDateTime getStartTime() { return startTime; }
        public void setStartTime(LocalDateTime startTime) { this.startTime = startTime; }
        public LocalDateTime getEndTime() { return endTime; }
        public void setEndTime(LocalDateTime endTime) { this.endTime = endTime; }
    }

    public static class ReloadResult {
        private boolean success;
        private int totalDataSources;
        private int successCount;
        private int failureCount;
        private List<String> errors = new ArrayList<>();
        private LocalDateTime startTime;
        private LocalDateTime endTime;

        // Getters and Setters
        public boolean isSuccess() { return success; }
        public void setSuccess(boolean success) { this.success = success; }
        public int getTotalDataSources() { return totalDataSources; }
        public void setTotalDataSources(int totalDataSources) { this.totalDataSources = totalDataSources; }
        public int getSuccessCount() { return successCount; }
        public void setSuccessCount(int successCount) { this.successCount = successCount; }
        public int getFailureCount() { return failureCount; }
        public void setFailureCount(int failureCount) { this.failureCount = failureCount; }
        public List<String> getErrors() { return errors; }
        public void setErrors(List<String> errors) { this.errors = errors; }
        public LocalDateTime getStartTime() { return startTime; }
        public void setStartTime(LocalDateTime startTime) { this.startTime = startTime; }
        public LocalDateTime getEndTime() { return endTime; }
        public void setEndTime(LocalDateTime endTime) { this.endTime = endTime; }
    }

    public static class ConfigurationChange {
        private Long dataSourceId;
        private String operation;
        private DataSource oldConfiguration;
        private DataSource newConfiguration;
        private String description;
        private String operator;
        private LocalDateTime changeTime;

        // Getters and Setters
        public Long getDataSourceId() { return dataSourceId; }
        public void setDataSourceId(Long dataSourceId) { this.dataSourceId = dataSourceId; }
        public String getOperation() { return operation; }
        public void setOperation(String operation) { this.operation = operation; }
        public DataSource getOldConfiguration() { return oldConfiguration; }
        public void setOldConfiguration(DataSource oldConfiguration) { this.oldConfiguration = oldConfiguration; }
        public DataSource getNewConfiguration() { return newConfiguration; }
        public void setNewConfiguration(DataSource newConfiguration) { this.newConfiguration = newConfiguration; }
        public String getDescription() { return description; }
        public void setDescription(String description) { this.description = description; }
        public String getOperator() { return operator; }
        public void setOperator(String operator) { this.operator = operator; }
        public LocalDateTime getChangeTime() { return changeTime; }
        public void setChangeTime(LocalDateTime changeTime) { this.changeTime = changeTime; }
    }

    public static class DataSourceConfigurationEvent {
        public enum EventType { ADDED, UPDATED, DELETED }
        
        private EventType eventType;
        private DataSource dataSource;
        private LocalDateTime eventTime;

        public DataSourceConfigurationEvent(EventType eventType, DataSource dataSource) {
            this.eventType = eventType;
            this.dataSource = dataSource;
            this.eventTime = LocalDateTime.now();
        }

        // Getters and Setters
        public EventType getEventType() { return eventType; }
        public void setEventType(EventType eventType) { this.eventType = eventType; }
        public DataSource getDataSource() { return dataSource; }
        public void setDataSource(DataSource dataSource) { this.dataSource = dataSource; }
        public LocalDateTime getEventTime() { return eventTime; }
        public void setEventTime(LocalDateTime eventTime) { this.eventTime = eventTime; }
    }
}