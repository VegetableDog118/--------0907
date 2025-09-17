package com.powertrading.datasource.service;

import com.powertrading.datasource.entity.DataSource;
import com.powertrading.datasource.exception.DataSourceException;
import com.powertrading.datasource.manager.DataSourceManager;
import com.powertrading.datasource.repository.DataSourceRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import javax.persistence.criteria.Predicate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;

/**
 * 数据源服务类
 * 
 * @author PowerTrading Team
 * @version 1.0.0
 */
@Service
@Transactional
public class DataSourceService {

    private static final Logger logger = LoggerFactory.getLogger(DataSourceService.class);

    @Autowired
    private DataSourceRepository dataSourceRepository;

    @Autowired
    private DataSourceManager dataSourceManager;

    /**
     * 创建数据源
     * 
     * @param dataSource 数据源配置
     * @return 创建的数据源
     * @throws DataSourceException 数据源异常
     */
    @CacheEvict(value = "datasources", allEntries = true)
    public DataSource createDataSource(DataSource dataSource) throws DataSourceException {
        logger.info("开始创建数据源: name={}, type={}", dataSource.getName(), dataSource.getType());
        
        try {
            // 检查名称是否重复
            if (dataSourceRepository.findByName(dataSource.getName()).isPresent()) {
                throw new DataSourceException("数据源名称已存在: " + dataSource.getName());
            }
            
            // 设置默认值
            setDefaultValues(dataSource);
            
            // 保存到数据库
            DataSource savedDataSource = dataSourceRepository.save(dataSource);
            
            // 添加到连接管理器
            dataSourceManager.addDataSource(savedDataSource);
            
            // 更新最后连接时间
            savedDataSource.setLastConnectedAt(LocalDateTime.now());
            savedDataSource.setHealthStatus(1); // 健康
            dataSourceRepository.save(savedDataSource);
            
            logger.info("数据源创建成功: id={}, name={}", savedDataSource.getId(), savedDataSource.getName());
            return savedDataSource;
        } catch (Exception e) {
            logger.error("数据源创建失败: name={}", dataSource.getName(), e);
            throw new DataSourceException("数据源创建失败: " + e.getMessage(), e);
        }
    }

    /**
     * 更新数据源
     * 
     * @param id 数据源ID
     * @param dataSource 数据源配置
     * @return 更新的数据源
     * @throws DataSourceException 数据源异常
     */
    @CacheEvict(value = "datasources", allEntries = true)
    public DataSource updateDataSource(Long id, DataSource dataSource) throws DataSourceException {
        logger.info("开始更新数据源: id={}, name={}", id, dataSource.getName());
        
        try {
            // 检查数据源是否存在
            DataSource existingDataSource = dataSourceRepository.findById(id)
                .orElseThrow(() -> new DataSourceException("数据源不存在: " + id));
            
            // 检查名称是否重复（排除自己）
            if (dataSourceRepository.existsByNameAndIdNot(dataSource.getName(), id)) {
                throw new DataSourceException("数据源名称已存在: " + dataSource.getName());
            }
            
            // 更新字段
            updateFields(existingDataSource, dataSource);
            
            // 保存到数据库
            DataSource savedDataSource = dataSourceRepository.save(existingDataSource);
            
            // 更新连接管理器
            dataSourceManager.updateDataSource(savedDataSource);
            
            logger.info("数据源更新成功: id={}, name={}", savedDataSource.getId(), savedDataSource.getName());
            return savedDataSource;
        } catch (Exception e) {
            logger.error("数据源更新失败: id={}", id, e);
            throw new DataSourceException("数据源更新失败: " + e.getMessage(), e);
        }
    }

    /**
     * 删除数据源
     * 
     * @param id 数据源ID
     * @throws DataSourceException 数据源异常
     */
    @CacheEvict(value = "datasources", allEntries = true)
    public void deleteDataSource(Long id) throws DataSourceException {
        logger.info("开始删除数据源: id={}", id);
        
        try {
            // 检查数据源是否存在
            DataSource dataSource = dataSourceRepository.findById(id)
                .orElseThrow(() -> new DataSourceException("数据源不存在: " + id));
            
            // 从连接管理器移除
            dataSourceManager.removeDataSource(id);
            
            // 从数据库删除
            dataSourceRepository.deleteById(id);
            
            logger.info("数据源删除成功: id={}, name={}", id, dataSource.getName());
        } catch (Exception e) {
            logger.error("数据源删除失败: id={}", id, e);
            throw new DataSourceException("数据源删除失败: " + e.getMessage(), e);
        }
    }

    /**
     * 根据ID获取数据源
     * 
     * @param id 数据源ID
     * @return 数据源
     */
    @Cacheable(value = "datasources", key = "#id")
    public Optional<DataSource> getDataSourceById(Long id) {
        return dataSourceRepository.findById(id);
    }

    /**
     * 根据名称获取数据源
     * 
     * @param name 数据源名称
     * @return 数据源
     */
    @Cacheable(value = "datasources", key = "'name:' + #name")
    public Optional<DataSource> getDataSourceByName(String name) {
        return dataSourceRepository.findByName(name);
    }

    /**
     * 分页查询数据源
     * 
     * @param name 数据源名称（可选）
     * @param type 数据源类型（可选）
     * @param status 状态（可选）
     * @param pageable 分页参数
     * @return 数据源分页结果
     */
    public Page<DataSource> getDataSources(String name, String type, Integer status, Pageable pageable) {
        Specification<DataSource> spec = (root, query, criteriaBuilder) -> {
            List<Predicate> predicates = new ArrayList<>();
            
            if (StringUtils.hasText(name)) {
                predicates.add(criteriaBuilder.like(root.get("name"), "%" + name + "%"));
            }
            
            if (StringUtils.hasText(type)) {
                predicates.add(criteriaBuilder.equal(root.get("type"), type));
            }
            
            if (status != null) {
                predicates.add(criteriaBuilder.equal(root.get("status"), status));
            }
            
            return criteriaBuilder.and(predicates.toArray(new Predicate[0]));
        };
        
        return dataSourceRepository.findAll(spec, pageable);
    }

    /**
     * 获取启用的数据源列表
     * 
     * @return 启用的数据源列表
     */
    @Cacheable(value = "datasources", key = "'enabled'")
    public List<DataSource> getEnabledDataSources() {
        return dataSourceRepository.findEnabledDataSources();
    }

    /**
     * 测试数据源连接
     * 
     * @param id 数据源ID
     * @return 连接测试结果
     * @throws DataSourceException 数据源异常
     */
    public boolean testConnection(Long id) throws DataSourceException {
        logger.info("开始测试数据源连接: id={}", id);
        
        try {
            // 检查数据源是否存在
            DataSource dataSource = dataSourceRepository.findById(id)
                .orElseThrow(() -> new DataSourceException("数据源不存在: " + id));
            
            boolean connected = dataSourceManager.testConnection(id);
            
            // 更新健康检查信息
            LocalDateTime now = LocalDateTime.now();
            if (connected) {
                dataSourceRepository.updateHealthCheckInfo(id, now, 1, null);
                dataSourceRepository.updateLastConnectedAt(id, now);
            } else {
                dataSourceRepository.updateHealthCheckInfo(id, now, 2, "连接测试失败");
            }
            
            logger.info("数据源连接测试完成: id={}, result={}", id, connected);
            return connected;
        } catch (Exception e) {
            logger.error("数据源连接测试失败: id={}", id, e);
            
            // 更新健康检查信息
            dataSourceRepository.updateHealthCheckInfo(id, LocalDateTime.now(), 2, e.getMessage());
            
            throw new DataSourceException("数据源连接测试失败: " + e.getMessage(), e);
        }
    }

    /**
     * 获取数据源连接池信息
     * 
     * @param id 数据源ID
     * @return 连接池信息
     */
    public DataSourceManager.DataSourcePoolInfo getPoolInfo(Long id) {
        return dataSourceManager.getPoolInfo(id);
    }

    /**
     * 获取所有数据源连接池信息
     * 
     * @return 连接池信息映射
     */
    public Map<Long, DataSourceManager.DataSourcePoolInfo> getAllPoolInfo() {
        return dataSourceManager.getAllPoolInfo();
    }

    /**
     * 启用数据源
     * 
     * @param id 数据源ID
     * @throws DataSourceException 数据源异常
     */
    @CacheEvict(value = "datasources", allEntries = true)
    public void enableDataSource(Long id) throws DataSourceException {
        updateDataSourceStatus(id, 1);
    }

    /**
     * 禁用数据源
     * 
     * @param id 数据源ID
     * @throws DataSourceException 数据源异常
     */
    @CacheEvict(value = "datasources", allEntries = true)
    public void disableDataSource(Long id) throws DataSourceException {
        updateDataSourceStatus(id, 0);
    }

    /**
     * 更新数据源状态
     * 
     * @param id 数据源ID
     * @param status 状态
     * @throws DataSourceException 数据源异常
     */
    private void updateDataSourceStatus(Long id, Integer status) throws DataSourceException {
        try {
            DataSource dataSource = dataSourceRepository.findById(id)
                .orElseThrow(() -> new DataSourceException("数据源不存在: " + id));
            
            dataSource.setStatus(status);
            dataSourceRepository.save(dataSource);
            
            if (status == 0) {
                // 禁用时从连接管理器移除
                dataSourceManager.removeDataSource(id);
            } else {
                // 启用时添加到连接管理器
                dataSourceManager.addDataSource(dataSource);
            }
            
            logger.info("数据源状态更新成功: id={}, status={}", id, status);
        } catch (Exception e) {
            logger.error("数据源状态更新失败: id={}, status={}", id, status, e);
            throw new DataSourceException("数据源状态更新失败: " + e.getMessage(), e);
        }
    }

    /**
     * 设置默认值
     * 
     * @param dataSource 数据源
     */
    private void setDefaultValues(DataSource dataSource) {
        if (dataSource.getStatus() == null) {
            dataSource.setStatus(1); // 默认启用
        }
        
        if (dataSource.getHealthStatus() == null) {
            dataSource.setHealthStatus(0); // 默认未知
        }
        
        if (dataSource.getMinPoolSize() == null) {
            dataSource.setMinPoolSize(2);
        }
        
        if (dataSource.getMaxPoolSize() == null) {
            dataSource.setMaxPoolSize(10);
        }
        
        if (dataSource.getConnectionTimeout() == null) {
            dataSource.setConnectionTimeout(30000L);
        }
        
        if (dataSource.getIdleTimeout() == null) {
            dataSource.setIdleTimeout(600000L);
        }
        
        if (dataSource.getMaxLifetime() == null) {
            dataSource.setMaxLifetime(1800000L);
        }
        
        if (dataSource.getValidationTimeout() == null) {
            dataSource.setValidationTimeout(5000L);
        }
        
        if (dataSource.getLeakDetectionThreshold() == null) {
            dataSource.setLeakDetectionThreshold(60000L);
        }
    }

    /**
     * 更新字段
     * 
     * @param existing 现有数据源
     * @param updated 更新的数据源
     */
    private void updateFields(DataSource existing, DataSource updated) {
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
}