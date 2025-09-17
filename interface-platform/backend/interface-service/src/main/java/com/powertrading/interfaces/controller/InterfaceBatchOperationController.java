package com.powertrading.interfaces.controller;

import com.powertrading.interfaces.service.InterfaceBatchOperationService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.extern.slf4j.Slf4j;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import javax.servlet.http.HttpServletRequest;
import javax.validation.Valid;
import javax.validation.constraints.Max;
import javax.validation.constraints.Min;
import java.util.List;

/**
 * 接口批量操作控制器
 * 提供接口的批量上架、下架、删除等操作API
 *
 * @author PowerTrading Team
 * @since 2024-01-15
 */
@Slf4j
@RestController
@RequestMapping("/api/v1/interfaces/batch")
@Tag(name = "接口批量操作", description = "接口批量操作相关API")
@Validated
public class InterfaceBatchOperationController {

    private static final Logger log = LoggerFactory.getLogger(InterfaceBatchOperationController.class);

    @Autowired
    private InterfaceBatchOperationService batchOperationService;

    /**
     * 批量上架接口
     *
     * @param request 批量上架请求
     * @param httpRequest HTTP请求
     * @return 操作结果
     */
    @PostMapping("/publish")
    @Operation(summary = "批量上架接口", description = "批量将接口状态变更为已上架")
    public ApiResponse<InterfaceBatchOperationService.BatchOperationResult> batchPublishInterfaces(
            @RequestBody @Valid BatchPublishRequest request,
            HttpServletRequest httpRequest) {
        try {
            String operateBy = getUserFromRequest(httpRequest);
            
            InterfaceBatchOperationService.BatchPublishRequest serviceRequest = 
                new InterfaceBatchOperationService.BatchPublishRequest();
            serviceRequest.setInterfaceIds(request.getInterfaceIds());
            serviceRequest.setOperateBy(operateBy);
            serviceRequest.setForceMode(request.getForceMode());
            
            InterfaceBatchOperationService.BatchOperationResult result = 
                batchOperationService.batchPublishInterfaces(serviceRequest);
            
            return ApiResponse.success(result);
            
        } catch (Exception e) {
            log.error("批量上架接口失败", e);
            return ApiResponse.error("批量上架接口失败: " + e.getMessage());
        }
    }

    /**
     * 批量下架接口
     *
     * @param request 批量下架请求
     * @param httpRequest HTTP请求
     * @return 操作结果
     */
    @PostMapping("/offline")
    @Operation(summary = "批量下架接口", description = "批量将接口状态变更为已下架")
    public ApiResponse<InterfaceBatchOperationService.BatchOperationResult> batchOfflineInterfaces(
            @RequestBody @Valid BatchOfflineRequest request,
            HttpServletRequest httpRequest) {
        try {
            String operateBy = getUserFromRequest(httpRequest);
            
            InterfaceBatchOperationService.BatchOfflineRequest serviceRequest = 
                new InterfaceBatchOperationService.BatchOfflineRequest();
            serviceRequest.setInterfaceIds(request.getInterfaceIds());
            serviceRequest.setOperateBy(operateBy);
            serviceRequest.setOfflineReason(request.getOfflineReason());
            serviceRequest.setForceMode(request.getForceMode());
            
            InterfaceBatchOperationService.BatchOperationResult result = 
                batchOperationService.batchOfflineInterfaces(serviceRequest);
            
            return ApiResponse.success(result);
            
        } catch (Exception e) {
            log.error("批量下架接口失败", e);
            return ApiResponse.error("批量下架接口失败: " + e.getMessage());
        }
    }

    /**
     * 批量删除接口
     *
     * @param request 批量删除请求
     * @param httpRequest HTTP请求
     * @return 操作结果
     */
    @PostMapping("/delete")
    @Operation(summary = "批量删除接口", description = "批量删除指定的接口")
    public ApiResponse<InterfaceBatchOperationService.BatchOperationResult> batchDeleteInterfaces(
            @RequestBody @Valid BatchDeleteRequest request,
            HttpServletRequest httpRequest) {
        try {
            String operateBy = getUserFromRequest(httpRequest);
            
            InterfaceBatchOperationService.BatchDeleteRequest serviceRequest = 
                new InterfaceBatchOperationService.BatchDeleteRequest();
            serviceRequest.setInterfaceIds(request.getInterfaceIds());
            serviceRequest.setOperateBy(operateBy);
            serviceRequest.setForceMode(request.getForceMode());
            
            InterfaceBatchOperationService.BatchOperationResult result = 
                batchOperationService.batchDeleteInterfaces(serviceRequest);
            
            return ApiResponse.success(result);
            
        } catch (Exception e) {
            log.error("批量删除接口失败", e);
            return ApiResponse.error("批量删除接口失败: " + e.getMessage());
        }
    }

    /**
     * 批量复制接口
     *
     * @param request 批量复制请求
     * @param httpRequest HTTP请求
     * @return 操作结果
     */
    @PostMapping("/copy")
    @Operation(summary = "批量复制接口", description = "批量复制指定的接口")
    public ApiResponse<InterfaceBatchOperationService.BatchOperationResult> batchCopyInterfaces(
            @RequestBody @Valid BatchCopyRequest request,
            HttpServletRequest httpRequest) {
        try {
            String operateBy = getUserFromRequest(httpRequest);
            
            InterfaceBatchOperationService.BatchCopyRequest serviceRequest = 
                new InterfaceBatchOperationService.BatchCopyRequest();
            serviceRequest.setSourceInterfaceIds(request.getSourceInterfaceIds());
            serviceRequest.setNewInterfaceNames(request.getNewInterfaceNames());
            serviceRequest.setOperateBy(operateBy);
            
            InterfaceBatchOperationService.BatchOperationResult result = 
                batchOperationService.batchCopyInterfaces(serviceRequest);
            
            return ApiResponse.success(result);
            
        } catch (Exception e) {
            log.error("批量复制接口失败", e);
            return ApiResponse.error("批量复制接口失败: " + e.getMessage());
        }
    }

    /**
     * 批量更新接口分类
     *
     * @param request 批量更新分类请求
     * @param httpRequest HTTP请求
     * @return 操作结果
     */
    @PostMapping("/update-category")
    @Operation(summary = "批量更新接口分类", description = "批量更新指定接口的分类")
    public ApiResponse<InterfaceBatchOperationService.BatchOperationResult> batchUpdateInterfaceCategory(
            @RequestBody @Valid BatchUpdateCategoryRequest request,
            HttpServletRequest httpRequest) {
        try {
            String operateBy = getUserFromRequest(httpRequest);
            
            InterfaceBatchOperationService.BatchUpdateCategoryRequest serviceRequest = 
                new InterfaceBatchOperationService.BatchUpdateCategoryRequest();
            serviceRequest.setInterfaceIds(request.getInterfaceIds());
            serviceRequest.setTargetCategoryId(request.getTargetCategoryId());
            serviceRequest.setOperateBy(operateBy);
            
            InterfaceBatchOperationService.BatchOperationResult result = 
                batchOperationService.batchUpdateInterfaceCategory(serviceRequest);
            
            return ApiResponse.success(result);
            
        } catch (Exception e) {
            log.error("批量更新接口分类失败", e);
            return ApiResponse.error("批量更新接口分类失败: " + e.getMessage());
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
    @GetMapping("/history")
    @Operation(summary = "获取批量操作历史", description = "获取批量操作的历史记录")
    public ApiResponse<List<InterfaceBatchOperationService.BatchOperationHistory>> getBatchOperationHistory(
            @Parameter(description = "操作人")
            @RequestParam(required = false) String operateBy,
            @Parameter(description = "操作类型")
            @RequestParam(required = false) String operationType,
            @Parameter(description = "限制数量")
            @RequestParam(defaultValue = "20") @Min(1) @Max(100) int limit) {
        try {
            List<InterfaceBatchOperationService.BatchOperationHistory> history = 
                batchOperationService.getBatchOperationHistory(operateBy, operationType, limit);
            return ApiResponse.success(history);
        } catch (Exception e) {
            log.error("获取批量操作历史失败", e);
            return ApiResponse.error("获取批量操作历史失败: " + e.getMessage());
        }
    }

    /**
     * 获取批量操作支持的操作类型
     *
     * @return 操作类型列表
     */
    @GetMapping("/operation-types")
    @Operation(summary = "获取批量操作类型", description = "获取系统支持的批量操作类型列表")
    public ApiResponse<List<BatchOperationType>> getBatchOperationTypes() {
        try {
            List<BatchOperationType> types = List.of(
                new BatchOperationType("PUBLISH", "批量上架", "将接口状态变更为已上架"),
                new BatchOperationType("OFFLINE", "批量下架", "将接口状态变更为已下架"),
                new BatchOperationType("DELETE", "批量删除", "删除指定的接口"),
                new BatchOperationType("COPY", "批量复制", "复制指定的接口"),
                new BatchOperationType("UPDATE_CATEGORY", "批量更新分类", "更新接口的分类")
            );
            return ApiResponse.success(types);
        } catch (Exception e) {
            log.error("获取批量操作类型失败", e);
            return ApiResponse.error("获取批量操作类型失败: " + e.getMessage());
        }
    }

    /**
     * 验证批量操作的前置条件
     *
     * @param request 验证请求
     * @return 验证结果
     */
    @PostMapping("/validate")
    @Operation(summary = "验证批量操作前置条件", description = "验证批量操作是否满足前置条件")
    public ApiResponse<BatchOperationValidationResult> validateBatchOperation(
            @RequestBody @Valid BatchOperationValidationRequest request) {
        try {
            // 这里应该实现具体的验证逻辑
            // 暂时返回通过验证的结果
            BatchOperationValidationResult result = new BatchOperationValidationResult();
            result.setValid(true);
            result.setValidInterfaceIds(request.getInterfaceIds());
            result.setInvalidInterfaceIds(List.of());
            result.setWarnings(List.of());
            
            return ApiResponse.success(result);
        } catch (Exception e) {
            log.error("验证批量操作前置条件失败", e);
            return ApiResponse.error("验证批量操作前置条件失败: " + e.getMessage());
        }
    }

    /**
     * 从请求中获取用户信息
     */
    private String getUserFromRequest(HttpServletRequest request) {
        String user = request.getHeader("X-User-Id");
        if (user == null) {
            user = "system";
        }
        return user;
    }

    // 请求和响应类定义
    
    public static class BatchPublishRequest {
        private List<String> interfaceIds;
        private Boolean forceMode = false;
        
        public List<String> getInterfaceIds() { return interfaceIds; }
        public void setInterfaceIds(List<String> interfaceIds) { this.interfaceIds = interfaceIds; }
        public Boolean getForceMode() { return forceMode; }
        public void setForceMode(Boolean forceMode) { this.forceMode = forceMode; }
    }

    public static class BatchOfflineRequest {
        private List<String> interfaceIds;
        private String offlineReason;
        private Boolean forceMode = false;
        
        public List<String> getInterfaceIds() { return interfaceIds; }
        public void setInterfaceIds(List<String> interfaceIds) { this.interfaceIds = interfaceIds; }
        public String getOfflineReason() { return offlineReason; }
        public void setOfflineReason(String offlineReason) { this.offlineReason = offlineReason; }
        public Boolean getForceMode() { return forceMode; }
        public void setForceMode(Boolean forceMode) { this.forceMode = forceMode; }
    }

    public static class BatchDeleteRequest {
        private List<String> interfaceIds;
        private Boolean forceMode = false;
        
        public List<String> getInterfaceIds() { return interfaceIds; }
        public void setInterfaceIds(List<String> interfaceIds) { this.interfaceIds = interfaceIds; }
        public Boolean getForceMode() { return forceMode; }
        public void setForceMode(Boolean forceMode) { this.forceMode = forceMode; }
    }

    public static class BatchCopyRequest {
        private List<String> sourceInterfaceIds;
        private List<String> newInterfaceNames;
        
        public List<String> getSourceInterfaceIds() { return sourceInterfaceIds; }
        public void setSourceInterfaceIds(List<String> sourceInterfaceIds) { this.sourceInterfaceIds = sourceInterfaceIds; }
        public List<String> getNewInterfaceNames() { return newInterfaceNames; }
        public void setNewInterfaceNames(List<String> newInterfaceNames) { this.newInterfaceNames = newInterfaceNames; }
    }

    public static class BatchUpdateCategoryRequest {
        private List<String> interfaceIds;
        private String targetCategoryId;
        
        public List<String> getInterfaceIds() { return interfaceIds; }
        public void setInterfaceIds(List<String> interfaceIds) { this.interfaceIds = interfaceIds; }
        public String getTargetCategoryId() { return targetCategoryId; }
        public void setTargetCategoryId(String targetCategoryId) { this.targetCategoryId = targetCategoryId; }
    }

    public static class BatchOperationValidationRequest {
        private List<String> interfaceIds;
        private String operationType;
        
        public List<String> getInterfaceIds() { return interfaceIds; }
        public void setInterfaceIds(List<String> interfaceIds) { this.interfaceIds = interfaceIds; }
        public String getOperationType() { return operationType; }
        public void setOperationType(String operationType) { this.operationType = operationType; }
    }

    public static class BatchOperationValidationResult {
        private boolean valid;
        private List<String> validInterfaceIds;
        private List<String> invalidInterfaceIds;
        private List<String> warnings;
        
        public boolean isValid() { return valid; }
        public void setValid(boolean valid) { this.valid = valid; }
        public List<String> getValidInterfaceIds() { return validInterfaceIds; }
        public void setValidInterfaceIds(List<String> validInterfaceIds) { this.validInterfaceIds = validInterfaceIds; }
        public List<String> getInvalidInterfaceIds() { return invalidInterfaceIds; }
        public void setInvalidInterfaceIds(List<String> invalidInterfaceIds) { this.invalidInterfaceIds = invalidInterfaceIds; }
        public List<String> getWarnings() { return warnings; }
        public void setWarnings(List<String> warnings) { this.warnings = warnings; }
    }

    public static class BatchOperationType {
        private String code;
        private String name;
        private String description;
        
        public BatchOperationType(String code, String name, String description) {
            this.code = code;
            this.name = name;
            this.description = description;
        }
        
        public String getCode() { return code; }
        public void setCode(String code) { this.code = code; }
        public String getName() { return name; }
        public void setName(String name) { this.name = name; }
        public String getDescription() { return description; }
        public void setDescription(String description) { this.description = description; }
    }

    /**
     * 统一响应结果
     */
    public static class ApiResponse<T> {
        private boolean success;
        private String message;
        private T data;
        private long timestamp;
        
        public ApiResponse() {
            this.timestamp = System.currentTimeMillis();
        }
        
        public static <T> ApiResponse<T> success() {
            ApiResponse<T> response = new ApiResponse<>();
            response.setSuccess(true);
            response.setMessage("操作成功");
            return response;
        }
        
        public static <T> ApiResponse<T> success(T data) {
            ApiResponse<T> response = new ApiResponse<>();
            response.setSuccess(true);
            response.setMessage("操作成功");
            response.setData(data);
            return response;
        }
        
        public static <T> ApiResponse<T> error(String message) {
            ApiResponse<T> response = new ApiResponse<>();
            response.setSuccess(false);
            response.setMessage(message);
            return response;
        }
        
        // getters and setters
        public boolean isSuccess() { return success; }
        public void setSuccess(boolean success) { this.success = success; }
        public String getMessage() { return message; }
        public void setMessage(String message) { this.message = message; }
        public T getData() { return data; }
        public void setData(T data) { this.data = data; }
        public long getTimestamp() { return timestamp; }
        public void setTimestamp(long timestamp) { this.timestamp = timestamp; }
    }
}