package com.powertrading.interfaces.service;

import com.powertrading.interfaces.entity.Interface;
import com.powertrading.interfaces.mapper.InterfaceMapper;
import com.powertrading.interfaces.mapper.InterfaceParameterMapper;
import lombok.extern.slf4j.Slf4j;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.CollectionUtils;

import java.time.LocalDateTime;
import java.util.*;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.stream.Collectors;

/**
 * 接口批量操作服务
 * 实现接口的批量上架、下架、删除等操作
 *
 * @author PowerTrading Team
 * @since 2024-01-15
 */
@Slf4j
@Service
public class InterfaceBatchOperationService {

    private static final Logger log = LoggerFactory.getLogger(InterfaceBatchOperationService.class);

    @Autowired
    private InterfaceMapper interfaceMapper;

    @Autowired
    private InterfaceParameterMapper parameterMapper;

    @Autowired
    private InterfaceStatusService interfaceStatusService;

    @Autowired
    private InterfaceManagementService interfaceManagementService;

    // 批量操作线程池
    private final ExecutorService batchExecutor = Executors.newFixedThreadPool(10);

    /**
     * 批量上架接口
     *
     * @param request 批量上架请求
     * @return 操作结果
     */
    @Transactional(rollbackFor = Exception.class)
    @CacheEvict(value = {"interface:detail", "interface:list"}, allEntries = true)
    public BatchOperationResult batchPublishInterfaces(BatchPublishRequest request) {
        try {
            log.info("开始批量上架接口，接口数量: {}, 操作人: {}", 
                request.getInterfaceIds().size(), request.getOperateBy());
            
            // 验证请求参数
            validateBatchRequest(request.getInterfaceIds(), "批量上架");
            
            // 预检查接口状态
            List<String> validInterfaceIds = preCheckForPublish(request.getInterfaceIds());
            
            // 执行批量上架
            BatchOperationResult result = executeBatchOperation(
                validInterfaceIds, 
                request.getOperateBy(),
                "PUBLISH",
                request.getForceMode(),
                null
            );
            
            log.info("批量上架接口完成，总数: {}, 成功: {}, 失败: {}", 
                result.getTotalCount(), result.getSuccessCount(), result.getFailedCount());
            
            return result;
            
        } catch (Exception e) {
            log.error("批量上架接口失败", e);
            throw new RuntimeException("批量上架接口失败: " + e.getMessage());
        }
    }

    /**
     * 批量下架接口
     *
     * @param request 批量下架请求
     * @return 操作结果
     */
    @Transactional(rollbackFor = Exception.class)
    @CacheEvict(value = {"interface:detail", "interface:list"}, allEntries = true)
    public BatchOperationResult batchOfflineInterfaces(BatchOfflineRequest request) {
        try {
            log.info("开始批量下架接口，接口数量: {}, 操作人: {}, 下架原因: {}", 
                request.getInterfaceIds().size(), request.getOperateBy(), request.getOfflineReason());
            
            // 验证请求参数
            validateBatchRequest(request.getInterfaceIds(), "批量下架");
            
            // 预检查接口状态
            List<String> validInterfaceIds = preCheckForOffline(request.getInterfaceIds());
            
            // 执行批量下架
            BatchOperationResult result = executeBatchOperation(
                validInterfaceIds, 
                request.getOperateBy(),
                "OFFLINE",
                request.getForceMode(),
                request.getOfflineReason()
            );
            
            log.info("批量下架接口完成，总数: {}, 成功: {}, 失败: {}", 
                result.getTotalCount(), result.getSuccessCount(), result.getFailedCount());
            
            return result;
            
        } catch (Exception e) {
            log.error("批量下架接口失败", e);
            throw new RuntimeException("批量下架接口失败: " + e.getMessage());
        }
    }

    /**
     * 批量删除接口
     *
     * @param request 批量删除请求
     * @return 操作结果
     */
    @Transactional(rollbackFor = Exception.class)
    @CacheEvict(value = {"interface:detail", "interface:list"}, allEntries = true)
    public BatchOperationResult batchDeleteInterfaces(BatchDeleteRequest request) {
        try {
            log.info("开始批量删除接口，接口数量: {}, 操作人: {}", 
                request.getInterfaceIds().size(), request.getOperateBy());
            
            // 验证请求参数
            validateBatchRequest(request.getInterfaceIds(), "批量删除");
            
            // 预检查接口状态
            List<String> validInterfaceIds = preCheckForDelete(request.getInterfaceIds());
            
            // 执行批量删除
            BatchOperationResult result = executeBatchOperation(
                validInterfaceIds, 
                request.getOperateBy(),
                "DELETE",
                request.getForceMode(),
                null
            );
            
            log.info("批量删除接口完成，总数: {}, 成功: {}, 失败: {}", 
                result.getTotalCount(), result.getSuccessCount(), result.getFailedCount());
            
            return result;
            
        } catch (Exception e) {
            log.error("批量删除接口失败", e);
            throw new RuntimeException("批量删除接口失败: " + e.getMessage());
        }
    }

    /**
     * 批量复制接口
     *
     * @param request 批量复制请求
     * @return 操作结果
     */
    @Transactional(rollbackFor = Exception.class)
    public BatchOperationResult batchCopyInterfaces(BatchCopyRequest request) {
        try {
            log.info("开始批量复制接口，接口数量: {}, 操作人: {}", 
                request.getSourceInterfaceIds().size(), request.getOperateBy());
            
            // 验证请求参数
            validateBatchRequest(request.getSourceInterfaceIds(), "批量复制");
            
            BatchOperationResult result = new BatchOperationResult();
            result.setTotalCount(request.getSourceInterfaceIds().size());
            result.setOperationType("COPY");
            result.setOperateBy(request.getOperateBy());
            result.setOperateTime(LocalDateTime.now().toString());
            
            List<String> successIds = new ArrayList<>();
            List<BatchOperationResult.FailedItem> failedItems = new ArrayList<>();
            
            for (int i = 0; i < request.getSourceInterfaceIds().size(); i++) {
                String sourceInterfaceId = request.getSourceInterfaceIds().get(i);
                String newInterfaceName = request.getNewInterfaceNames().get(i);
                
                try {
                    String newInterfaceId = interfaceManagementService.copyInterface(
                        sourceInterfaceId, newInterfaceName, request.getOperateBy());
                    successIds.add(newInterfaceId);
                } catch (Exception e) {
                    BatchOperationResult.FailedItem failedItem = new BatchOperationResult.FailedItem();
                    failedItem.setId(sourceInterfaceId);
                    failedItem.setReason(e.getMessage());
                    failedItems.add(failedItem);
                    log.warn("复制接口失败，源接口ID: {}, 原因: {}", sourceInterfaceId, e.getMessage());
                }
            }
            
            result.setSuccessCount(successIds.size());
            result.setFailedCount(failedItems.size());
            result.setSuccessIds(successIds);
            result.setFailedItems(failedItems);
            
            log.info("批量复制接口完成，总数: {}, 成功: {}, 失败: {}", 
                result.getTotalCount(), result.getSuccessCount(), result.getFailedCount());
            
            return result;
            
        } catch (Exception e) {
            log.error("批量复制接口失败", e);
            throw new RuntimeException("批量复制接口失败: " + e.getMessage());
        }
    }

    /**
     * 批量更新接口分类
     *
     * @param request 批量更新分类请求
     * @return 操作结果
     */
    @Transactional(rollbackFor = Exception.class)
    @CacheEvict(value = {"interface:detail", "interface:list"}, allEntries = true)
    public BatchOperationResult batchUpdateInterfaceCategory(BatchUpdateCategoryRequest request) {
        try {
            log.info("开始批量更新接口分类，接口数量: {}, 目标分类: {}, 操作人: {}", 
                request.getInterfaceIds().size(), request.getTargetCategoryId(), request.getOperateBy());
            
            // 验证请求参数
            validateBatchRequest(request.getInterfaceIds(), "批量更新分类");
            
            BatchOperationResult result = new BatchOperationResult();
            result.setTotalCount(request.getInterfaceIds().size());
            result.setOperationType("UPDATE_CATEGORY");
            result.setOperateBy(request.getOperateBy());
            result.setOperateTime(LocalDateTime.now().toString());
            
            List<String> successIds = new ArrayList<>();
            List<BatchOperationResult.FailedItem> failedItems = new ArrayList<>();
            
            for (String interfaceId : request.getInterfaceIds()) {
                try {
                    // 检查接口是否存在
                    Interface existingInterface = interfaceMapper.selectById(interfaceId);
                    if (existingInterface == null) {
                        throw new RuntimeException("接口不存在");
                    }
                    
                    // 检查接口状态（已上架的接口不能修改分类）
                    if (Interface.STATUS_PUBLISHED.equals(existingInterface.getStatus())) {
                        throw new RuntimeException("已上架的接口不能修改分类");
                    }
                    
                    // 更新分类
                    Interface updateInterface = new Interface();
                    updateInterface.setId(interfaceId);
                    updateInterface.setCategoryId(request.getTargetCategoryId());
                    updateInterface.setUpdateBy(request.getOperateBy());
                    
                    interfaceMapper.updateById(updateInterface);
                    successIds.add(interfaceId);
                    
                } catch (Exception e) {
                    BatchOperationResult.FailedItem failedItem = new BatchOperationResult.FailedItem();
                    failedItem.setId(interfaceId);
                    failedItem.setReason(e.getMessage());
                    failedItems.add(failedItem);
                    log.warn("更新接口分类失败，接口ID: {}, 原因: {}", interfaceId, e.getMessage());
                }
            }
            
            result.setSuccessCount(successIds.size());
            result.setFailedCount(failedItems.size());
            result.setSuccessIds(successIds);
            result.setFailedItems(failedItems);
            
            log.info("批量更新接口分类完成，总数: {}, 成功: {}, 失败: {}", 
                result.getTotalCount(), result.getSuccessCount(), result.getFailedCount());
            
            return result;
            
        } catch (Exception e) {
            log.error("批量更新接口分类失败", e);
            throw new RuntimeException("批量更新接口分类失败: " + e.getMessage());
        }
    }

    /**
     * 获取批量操作历史
     *
     * @param operateBy 操作人
     * @param operationType 操作类型
     * @param limit 限制数量
     * @return 操作历史列表
     */
    public List<BatchOperationHistory> getBatchOperationHistory(String operateBy, String operationType, int limit) {
        try {
            // 这里应该从数据库查询批量操作历史
            // 暂时返回空列表
            return new ArrayList<>();
        } catch (Exception e) {
            log.error("获取批量操作历史失败", e);
            throw new RuntimeException("获取批量操作历史失败: " + e.getMessage());
        }
    }

    /**
     * 验证批量请求参数
     */
    private void validateBatchRequest(List<String> interfaceIds, String operationType) {
        if (CollectionUtils.isEmpty(interfaceIds)) {
            throw new RuntimeException(operationType + "的接口ID列表不能为空");
        }
        
        if (interfaceIds.size() > 100) {
            throw new RuntimeException(operationType + "的接口数量不能超过100个");
        }
        
        // 检查是否有重复的接口ID
        Set<String> uniqueIds = new HashSet<>(interfaceIds);
        if (uniqueIds.size() != interfaceIds.size()) {
            throw new RuntimeException(operationType + "的接口ID列表中存在重复项");
        }
    }

    /**
     * 预检查上架条件
     */
    private List<String> preCheckForPublish(List<String> interfaceIds) {
        List<String> validIds = new ArrayList<>();
        
        for (String interfaceId : interfaceIds) {
            try {
                Interface interfaceInfo = interfaceMapper.selectById(interfaceId);
                if (interfaceInfo != null && 
                    (Interface.STATUS_UNPUBLISHED.equals(interfaceInfo.getStatus()) ||
                     Interface.STATUS_OFFLINE.equals(interfaceInfo.getStatus()))) {
                    validIds.add(interfaceId);
                }
            } catch (Exception e) {
                log.warn("预检查接口失败，接口ID: {}", interfaceId, e);
            }
        }
        
        return validIds;
    }

    /**
     * 预检查下架条件
     */
    private List<String> preCheckForOffline(List<String> interfaceIds) {
        List<String> validIds = new ArrayList<>();
        
        for (String interfaceId : interfaceIds) {
            try {
                Interface interfaceInfo = interfaceMapper.selectById(interfaceId);
                if (interfaceInfo != null && Interface.STATUS_PUBLISHED.equals(interfaceInfo.getStatus())) {
                    validIds.add(interfaceId);
                }
            } catch (Exception e) {
                log.warn("预检查接口失败，接口ID: {}", interfaceId, e);
            }
        }
        
        return validIds;
    }

    /**
     * 预检查删除条件
     */
    private List<String> preCheckForDelete(List<String> interfaceIds) {
        List<String> validIds = new ArrayList<>();
        
        for (String interfaceId : interfaceIds) {
            try {
                Interface interfaceInfo = interfaceMapper.selectById(interfaceId);
                if (interfaceInfo != null && !Interface.STATUS_PUBLISHED.equals(interfaceInfo.getStatus())) {
                    validIds.add(interfaceId);
                }
            } catch (Exception e) {
                log.warn("预检查接口失败，接口ID: {}", interfaceId, e);
            }
        }
        
        return validIds;
    }

    /**
     * 执行批量操作
     */
    private BatchOperationResult executeBatchOperation(List<String> interfaceIds, String operateBy, 
                                                      String operationType, Boolean forceMode, String reason) {
        BatchOperationResult result = new BatchOperationResult();
        result.setTotalCount(interfaceIds.size());
        result.setOperationType(operationType);
        result.setOperateBy(operateBy);
        result.setOperateTime(LocalDateTime.now().toString());
        
        List<String> successIds = new ArrayList<>();
        List<BatchOperationResult.FailedItem> failedItems = new ArrayList<>();
        
        // 根据操作类型执行相应的操作
        for (String interfaceId : interfaceIds) {
            try {
                switch (operationType) {
                    case "PUBLISH":
                        interfaceStatusService.publishInterface(interfaceId, operateBy);
                        break;
                    case "OFFLINE":
                        interfaceStatusService.offlineInterface(interfaceId, operateBy, reason);
                        break;
                    case "DELETE":
                        interfaceManagementService.deleteInterface(interfaceId, operateBy);
                        break;
                    default:
                        throw new RuntimeException("不支持的操作类型: " + operationType);
                }
                successIds.add(interfaceId);
            } catch (Exception e) {
                if (forceMode != null && forceMode) {
                    log.warn("强制模式下忽略错误，接口ID: {}, 错误: {}", interfaceId, e.getMessage());
                    successIds.add(interfaceId);
                } else {
                    BatchOperationResult.FailedItem failedItem = new BatchOperationResult.FailedItem();
                    failedItem.setId(interfaceId);
                    failedItem.setReason(e.getMessage());
                    failedItems.add(failedItem);
                    log.warn("批量操作失败，接口ID: {}, 操作类型: {}, 原因: {}", 
                        interfaceId, operationType, e.getMessage());
                }
            }
        }
        
        result.setSuccessCount(successIds.size());
        result.setFailedCount(failedItems.size());
        result.setSuccessIds(successIds);
        result.setFailedItems(failedItems);
        
        return result;
    }

    // 请求和响应类定义
    
    public static class BatchPublishRequest {
        private List<String> interfaceIds;
        private String operateBy;
        private Boolean forceMode = false;
        
        // getters and setters
        public List<String> getInterfaceIds() { return interfaceIds; }
        public void setInterfaceIds(List<String> interfaceIds) { this.interfaceIds = interfaceIds; }
        public String getOperateBy() { return operateBy; }
        public void setOperateBy(String operateBy) { this.operateBy = operateBy; }
        public Boolean getForceMode() { return forceMode; }
        public void setForceMode(Boolean forceMode) { this.forceMode = forceMode; }
    }

    public static class BatchOfflineRequest {
        private List<String> interfaceIds;
        private String operateBy;
        private String offlineReason;
        private Boolean forceMode = false;
        
        // getters and setters
        public List<String> getInterfaceIds() { return interfaceIds; }
        public void setInterfaceIds(List<String> interfaceIds) { this.interfaceIds = interfaceIds; }
        public String getOperateBy() { return operateBy; }
        public void setOperateBy(String operateBy) { this.operateBy = operateBy; }
        public String getOfflineReason() { return offlineReason; }
        public void setOfflineReason(String offlineReason) { this.offlineReason = offlineReason; }
        public Boolean getForceMode() { return forceMode; }
        public void setForceMode(Boolean forceMode) { this.forceMode = forceMode; }
    }

    public static class BatchDeleteRequest {
        private List<String> interfaceIds;
        private String operateBy;
        private Boolean forceMode = false;
        
        // getters and setters
        public List<String> getInterfaceIds() { return interfaceIds; }
        public void setInterfaceIds(List<String> interfaceIds) { this.interfaceIds = interfaceIds; }
        public String getOperateBy() { return operateBy; }
        public void setOperateBy(String operateBy) { this.operateBy = operateBy; }
        public Boolean getForceMode() { return forceMode; }
        public void setForceMode(Boolean forceMode) { this.forceMode = forceMode; }
    }

    public static class BatchCopyRequest {
        private List<String> sourceInterfaceIds;
        private List<String> newInterfaceNames;
        private String operateBy;
        
        // getters and setters
        public List<String> getSourceInterfaceIds() { return sourceInterfaceIds; }
        public void setSourceInterfaceIds(List<String> sourceInterfaceIds) { this.sourceInterfaceIds = sourceInterfaceIds; }
        public List<String> getNewInterfaceNames() { return newInterfaceNames; }
        public void setNewInterfaceNames(List<String> newInterfaceNames) { this.newInterfaceNames = newInterfaceNames; }
        public String getOperateBy() { return operateBy; }
        public void setOperateBy(String operateBy) { this.operateBy = operateBy; }
    }

    public static class BatchUpdateCategoryRequest {
        private List<String> interfaceIds;
        private String targetCategoryId;
        private String operateBy;
        
        // getters and setters
        public List<String> getInterfaceIds() { return interfaceIds; }
        public void setInterfaceIds(List<String> interfaceIds) { this.interfaceIds = interfaceIds; }
        public String getTargetCategoryId() { return targetCategoryId; }
        public void setTargetCategoryId(String targetCategoryId) { this.targetCategoryId = targetCategoryId; }
        public String getOperateBy() { return operateBy; }
        public void setOperateBy(String operateBy) { this.operateBy = operateBy; }
    }

    public static class BatchOperationResult {
        private int totalCount;
        private int successCount;
        private int failedCount;
        private String operationType;
        private String operateBy;
        private String operateTime;
        private List<String> successIds;
        private List<FailedItem> failedItems;
        
        // getters and setters
        public int getTotalCount() { return totalCount; }
        public void setTotalCount(int totalCount) { this.totalCount = totalCount; }
        public int getSuccessCount() { return successCount; }
        public void setSuccessCount(int successCount) { this.successCount = successCount; }
        public int getFailedCount() { return failedCount; }
        public void setFailedCount(int failedCount) { this.failedCount = failedCount; }
        public String getOperationType() { return operationType; }
        public void setOperationType(String operationType) { this.operationType = operationType; }
        public String getOperateBy() { return operateBy; }
        public void setOperateBy(String operateBy) { this.operateBy = operateBy; }
        public String getOperateTime() { return operateTime; }
        public void setOperateTime(String operateTime) { this.operateTime = operateTime; }
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

    public static class BatchOperationHistory {
        private String id;
        private String operationType;
        private String operateBy;
        private String operateTime;
        private int totalCount;
        private int successCount;
        private int failedCount;
        
        // getters and setters
        public String getId() { return id; }
        public void setId(String id) { this.id = id; }
        public String getOperationType() { return operationType; }
        public void setOperationType(String operationType) { this.operationType = operationType; }
        public String getOperateBy() { return operateBy; }
        public void setOperateBy(String operateBy) { this.operateBy = operateBy; }
        public String getOperateTime() { return operateTime; }
        public void setOperateTime(String operateTime) { this.operateTime = operateTime; }
        public int getTotalCount() { return totalCount; }
        public void setTotalCount(int totalCount) { this.totalCount = totalCount; }
        public int getSuccessCount() { return successCount; }
        public void setSuccessCount(int successCount) { this.successCount = successCount; }
        public int getFailedCount() { return failedCount; }
        public void setFailedCount(int failedCount) { this.failedCount = failedCount; }
    }
}