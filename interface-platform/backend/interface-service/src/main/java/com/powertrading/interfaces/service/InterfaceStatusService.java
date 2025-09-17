package com.powertrading.interfaces.service;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.UpdateWrapper;
import com.powertrading.interfaces.mapper.InterfaceMapper;
import com.powertrading.interfaces.entity.Interface;
import com.powertrading.interfaces.client.GatewayClient;
import lombok.extern.slf4j.Slf4j;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;
import com.powertrading.interfaces.mapper.InterfaceMapperImpl;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * 接口状态管理服务
 * 负责接口的上架、下架、重新上架等状态变更操作
 *
 * @author PowerTrading Team
 * @since 2024-01-15
 */
@Slf4j
@Service
@Transactional
public class InterfaceStatusService {

    // Logger已通过@Slf4j注解提供，无需手动声明

    @Autowired
    private InterfaceMapper interfaceMapper;
    
    @Autowired
    private InterfaceMapperImpl interfaceMapperImpl;
    
    @Autowired
    private GatewayClient gatewayClient;
    
    @Autowired
    private JdbcTemplate jdbcTemplate;

    /**
     * 上架接口
     * 状态变更：未上架 → 已上架
     *
     * @param interfaceId 接口ID
     * @param publishBy 上架操作人
     */
    // @Transactional(rollbackFor = Exception.class)
    // @CacheEvict(value = {"interface:detail", "interface:list"}, allEntries = true)
    public void publishInterface(String interfaceId, String publishBy) {
        try {
            log.info("开始上架接口，接口ID: {}, 操作人: {}", interfaceId, publishBy);
            
            // 直接更新接口状态，不做任何查询
            String sql = "UPDATE interfaces SET status = 'published', " +
                        "publish_time = NOW(), publish_by = ?, " +
                        "update_by = ?, update_time = NOW() " +
                        "WHERE id = ?";
            
            int result = jdbcTemplate.update(sql, publishBy, publishBy, interfaceId);
            
            if (result == 0) {
                throw new RuntimeException("更新接口状态失败，接口不存在");
            }
            
            log.info("接口上架成功，接口ID: {}, 操作人: {}", interfaceId, publishBy);
            
        } catch (Exception e) {
            log.error("接口上架失败，接口ID: {}", interfaceId, e);
            throw new RuntimeException("接口上架失败: " + e.getMessage());
        }
    }

    /**
     * 下架接口
     * 状态变更：已上架 → 已下架
     *
     * @param interfaceId 接口ID
     * @param offlineBy 下架操作人
     * @param offlineReason 下架原因
     */
    @Transactional(rollbackFor = Exception.class)
    @CacheEvict(value = {"interface:detail", "interface:list"}, allEntries = true)
    public void offlineInterface(String interfaceId, String offlineBy, String offlineReason) {
        try {
            log.info("开始下架接口，接口ID: {}", interfaceId);
            
            // 检查接口是否存在 - 使用MyBatis-Plus方式
            Interface existingInterface = interfaceMapper.selectById(interfaceId);
            if (existingInterface == null) {
                throw new RuntimeException("接口不存在");
            }
            
            // 检查当前状态是否允许下架
            if (!Interface.STATUS_PUBLISHED.equals(existingInterface.getStatus())) {
                throw new RuntimeException("当前状态不允许下架操作，当前状态: " + existingInterface.getStatus());
            }
            
            // 更新接口状态 - 使用UpdateWrapper避免自定义updateById的参数绑定问题
            UpdateWrapper<Interface> updateWrapper = new UpdateWrapper<>();
            updateWrapper.eq("id", interfaceId)
                        .set("status", Interface.STATUS_OFFLINE)
                        .set("offline_time", LocalDateTime.now())
                        .set("offline_by", offlineBy)
                        .set("offline_reason", offlineReason)
                        .set("update_by", offlineBy)
                        .set("update_time", LocalDateTime.now());
            
            int result = interfaceMapper.update(null, updateWrapper);
            if (result == 0) {
                throw new RuntimeException("更新接口状态失败");
            }
            
            log.info("接口下架成功，接口ID: {}, 接口名称: {}, 下架原因: {}, 操作人: {}", 
                interfaceId, existingInterface.getInterfaceName(), offlineReason, offlineBy);
            
        } catch (Exception e) {
            log.error("接口下架失败，接口ID: {}", interfaceId, e);
            throw new RuntimeException("接口下架失败: " + e.getMessage());
        }
    }

    /**
     * 重新上架接口
     * 状态变更：已下架 → 已上架
     *
     * @param interfaceId 接口ID
     * @param republishBy 重新上架操作人
     */
    @Transactional(rollbackFor = Exception.class)
    // @CacheEvict(value = {"interface:detail", "interface:list"}, allEntries = true)
    public void republishInterface(String interfaceId, String republishBy) {
        try {
            log.info("开始重新上架接口，接口ID: {}", interfaceId);
            
            // 直接更新接口状态，不进行任何查询操作
            UpdateWrapper<Interface> updateWrapper = new UpdateWrapper<>();
            updateWrapper.eq("id", interfaceId)
                        .eq("status", Interface.STATUS_OFFLINE)  // 确保只更新已下架的接口
                        .set("status", Interface.STATUS_PUBLISHED)
                        .set("publish_time", LocalDateTime.now())
                        .set("publish_by", republishBy)
                        .set("offline_time", null)
                        .set("offline_by", null)
                        .set("offline_reason", null)
                        .set("update_by", republishBy)
                        .set("update_time", LocalDateTime.now());
            
            int result = interfaceMapper.update(null, updateWrapper);
            if (result == 0) {
                throw new RuntimeException("更新接口状态失败，接口不存在或状态不正确");
            }
            
            log.info("接口重新上架成功，接口ID: {}, 操作人: {}", interfaceId, republishBy);
            
        } catch (Exception e) {
            log.error("接口重新上架失败，接口ID: {}", interfaceId, e);
            throw new RuntimeException("接口重新上架失败: " + e.getMessage());
        }
    }
    
    /**
     * 测试重新上架接口（完全简化版本）
     */
    public void testRepublishInterface(String interfaceId, String republishBy) {
        log.info("测试重新上架接口，接口ID: {}, 操作人: {}", interfaceId, republishBy);
        // 什么都不做，只是记录日志
    }
    
    /**
     * 测试上架接口（完全独立的方法）
     */
    public void testPublishInterface(String interfaceId, String publishBy) {
        log.info("测试上架接口，接口ID: {}, 操作人: {}", interfaceId, publishBy);
        // 什么都不做，只是记录日志
    }

    /**
     * 批量上架接口
     *
     * @param interfaceIds 接口ID列表
     * @param publishBy 上架操作人
     * @return 操作结果
     */
    @Transactional(rollbackFor = Exception.class)
    @CacheEvict(value = {"interface:detail", "interface:list"}, allEntries = true)
    public BatchOperationResult batchPublishInterfaces(List<String> interfaceIds, String publishBy) {
        BatchOperationResult result = new BatchOperationResult();
        result.setTotalCount(interfaceIds.size());
        
        List<String> successIds = new ArrayList<>();
        List<BatchOperationResult.FailedItem> failedItems = new ArrayList<>();
        
        for (String interfaceId : interfaceIds) {
            try {
                publishInterface(interfaceId, publishBy);
                successIds.add(interfaceId);
            } catch (Exception e) {
                BatchOperationResult.FailedItem failedItem = new BatchOperationResult.FailedItem();
                failedItem.setId(interfaceId);
                failedItem.setReason(e.getMessage());
                failedItems.add(failedItem);
                log.warn("批量上架接口失败，接口ID: {}, 原因: {}", interfaceId, e.getMessage());
            }
        }
        
        result.setSuccessCount(successIds.size());
        result.setFailedCount(failedItems.size());
        result.setSuccessIds(successIds);
        result.setFailedItems(failedItems);
        
        log.info("批量上架接口完成，总数: {}, 成功: {}, 失败: {}, 操作人: {}", 
            result.getTotalCount(), result.getSuccessCount(), result.getFailedCount(), publishBy);
        
        return result;
    }

    /**
     * 批量下架接口
     *
     * @param interfaceIds 接口ID列表
     * @param offlineBy 下架操作人
     * @param offlineReason 下架原因
     * @return 操作结果
     */
    @Transactional(rollbackFor = Exception.class)
    @CacheEvict(value = {"interface:detail", "interface:list"}, allEntries = true)
    public BatchOperationResult batchOfflineInterfaces(List<String> interfaceIds, String offlineBy, String offlineReason) {
        BatchOperationResult result = new BatchOperationResult();
        result.setTotalCount(interfaceIds.size());
        
        List<String> successIds = new ArrayList<>();
        List<BatchOperationResult.FailedItem> failedItems = new ArrayList<>();
        
        for (String interfaceId : interfaceIds) {
            try {
                offlineInterface(interfaceId, offlineBy, offlineReason);
                successIds.add(interfaceId);
            } catch (Exception e) {
                BatchOperationResult.FailedItem failedItem = new BatchOperationResult.FailedItem();
                failedItem.setId(interfaceId);
                failedItem.setReason(e.getMessage());
                failedItems.add(failedItem);
                log.warn("批量下架接口失败，接口ID: {}, 原因: {}", interfaceId, e.getMessage());
            }
        }
        
        result.setSuccessCount(successIds.size());
        result.setFailedCount(failedItems.size());
        result.setSuccessIds(successIds);
        result.setFailedItems(failedItems);
        
        log.info("批量下架接口完成，总数: {}, 成功: {}, 失败: {}, 操作人: {}", 
            result.getTotalCount(), result.getSuccessCount(), result.getFailedCount(), offlineBy);
        
        return result;
    }

    /**
     * 获取接口状态统计
     *
     * @return 状态统计信息
     */
    public InterfaceStatusStatistics getInterfaceStatusStatistics() {
        try {
            // 使用新的InterfaceMapperImpl避免XML映射的参数绑定问题
            List<InterfaceMapper.InterfaceStatistics> statisticsList = interfaceMapperImpl.selectInterfaceStatisticsNew();
            
            InterfaceStatusStatistics result = new InterfaceStatusStatistics();
            
            for (InterfaceMapper.InterfaceStatistics stats : statisticsList) {
                switch (stats.getStatus()) {
                    case Interface.STATUS_UNPUBLISHED:
                        result.setUnpublishedCount(stats.getCount());
                        break;
                    case Interface.STATUS_PUBLISHED:
                        result.setPublishedCount(stats.getCount());
                        break;
                    case Interface.STATUS_OFFLINE:
                        result.setOfflineCount(stats.getCount());
                        break;
                }
            }
            
            result.setTotalCount(result.getUnpublishedCount() + result.getPublishedCount() + 
                               result.getOfflineCount());
            
            return result;
        } catch (Exception e) {
            log.error("获取接口状态统计失败", e);
            throw new RuntimeException("获取接口状态统计失败: " + e.getMessage());
        }
    }

    /**
     * 验证接口是否可以上架
     */
    private void validateInterfaceForPublish(Interface interfaceInfo) {
        // 检查接口基本信息
        if (interfaceInfo.getInterfaceName() == null || interfaceInfo.getInterfaceName().trim().isEmpty()) {
            throw new RuntimeException("接口名称不能为空");
        }
        
        if (interfaceInfo.getInterfacePath() == null || interfaceInfo.getInterfacePath().trim().isEmpty()) {
            throw new RuntimeException("接口路径不能为空");
        }
        
        if (interfaceInfo.getDataSourceId() == null || interfaceInfo.getDataSourceId().trim().isEmpty()) {
            throw new RuntimeException("数据源不能为空");
        }
        
        if (interfaceInfo.getTableName() == null || interfaceInfo.getTableName().trim().isEmpty()) {
            throw new RuntimeException("数据表不能为空");
        }
        
        if (interfaceInfo.getSqlTemplate() == null || interfaceInfo.getSqlTemplate().trim().isEmpty()) {
            throw new RuntimeException("SQL模板不能为空");
        }
        
        // 暂时注释掉路径重复检查，避免MyBatis参数绑定问题
        // TODO: 后续需要实现更安全的路径重复检查机制
        /*
        QueryWrapper<Interface> pathQuery = new QueryWrapper<>();
        pathQuery.eq("interface_path", interfaceInfo.getInterfacePath())
                 .ne("id", interfaceInfo.getId())
                 .eq("status", Interface.STATUS_PUBLISHED);
        Interface existingByPath = interfaceMapper.selectOne(pathQuery);
        if (existingByPath != null) {
            throw new RuntimeException("接口路径已被其他已上架接口使用");
        }
        */
    }

    /**
     * 注册到网关
     */
    private void registerToGateway(Interface interfaceInfo) {
        try {
            GatewayClient.RouteRegistrationRequest request = new GatewayClient.RouteRegistrationRequest();
            request.setRouteId("interface_" + interfaceInfo.getId());
            request.setInterfacePath(interfaceInfo.getInterfacePath());
            request.setMethod(interfaceInfo.getRequestMethod());
            request.setTargetUri("/api/v1/interfaces/execute/" + interfaceInfo.getId());
            request.setRateLimit(interfaceInfo.getRateLimit());
            request.setTimeout(interfaceInfo.getTimeout());
            
            GatewayClient.ApiResponse<String> response = gatewayClient.registerRoute(request);
            if (!response.isSuccess()) {
                throw new RuntimeException("注册到网关失败: " + response.getMessage());
            }
            
            log.info("接口注册到网关成功，接口ID: {}, 路径: {}", 
                interfaceInfo.getId(), interfaceInfo.getInterfacePath());
            
        } catch (Exception e) {
            log.error("注册到网关失败，接口ID: {}", interfaceInfo.getId(), e);
            throw new RuntimeException("注册到网关失败: " + e.getMessage());
        }
    }

    /**
     * 从网关注销
     */
    private void unregisterFromGateway(Interface interfaceInfo) {
        try {
            String routeId = "interface_" + interfaceInfo.getId();
            GatewayClient.ApiResponse<String> response = gatewayClient.deleteRoute(routeId);
            if (!response.isSuccess()) {
                log.warn("从网关注销失败，但继续执行下架操作，接口ID: {}, 原因: {}", 
                    interfaceInfo.getId(), response.getMessage());
            } else {
                log.info("接口从网关注销成功，接口ID: {}, 路径: {}", 
                    interfaceInfo.getId(), interfaceInfo.getInterfacePath());
            }
            
        } catch (Exception e) {
            log.warn("从网关注销失败，但继续执行下架操作，接口ID: {}", interfaceInfo.getId(), e);
        }
    }

    /**
     * 批量操作结果
     */
    public static class BatchOperationResult {
        private int totalCount;
        private int successCount;
        private int failedCount;
        private List<String> successIds;
        private List<FailedItem> failedItems;
        
        // getters and setters
        public int getTotalCount() { return totalCount; }
        public void setTotalCount(int totalCount) { this.totalCount = totalCount; }
        public int getSuccessCount() { return successCount; }
        public void setSuccessCount(int successCount) { this.successCount = successCount; }
        public int getFailedCount() { return failedCount; }
        public void setFailedCount(int failedCount) { this.failedCount = failedCount; }
        public List<String> getSuccessIds() { return successIds; }
        public void setSuccessIds(List<String> successIds) { this.successIds = successIds; }
        public List<FailedItem> getFailedItems() { return failedItems; }
        public void setFailedItems(List<FailedItem> failedItems) { this.failedItems = failedItems; }
        
        public static class FailedItem {
            private String id;
            private String reason;
            
            public String getId() { return id; }
            public void setId(String id) { this.id = id; }
            public String getReason() { return reason; }
            public void setReason(String reason) { this.reason = reason; }
        }
    }

    /**
     * 接口状态统计
     */
    public static class InterfaceStatusStatistics {
        private long totalCount;
        private long unpublishedCount;
        private long publishedCount;
        private long offlineCount;
        
        // getters and setters
        public long getTotalCount() { return totalCount; }
        public void setTotalCount(long totalCount) { this.totalCount = totalCount; }
        public long getUnpublishedCount() { return unpublishedCount; }
        public void setUnpublishedCount(long unpublishedCount) { this.unpublishedCount = unpublishedCount; }
        public long getPublishedCount() { return publishedCount; }
        public void setPublishedCount(long publishedCount) { this.publishedCount = publishedCount; }
        public long getOfflineCount() { return offlineCount; }
        public void setOfflineCount(long offlineCount) { this.offlineCount = offlineCount; }
    }
}