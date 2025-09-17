package com.powertrading.interfaces.controller;

import com.powertrading.interfaces.service.InterfaceStatusService;
import com.powertrading.interfaces.common.ApiResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import javax.servlet.http.HttpServletRequest;
import javax.validation.Valid;
import javax.validation.constraints.NotBlank;
import javax.validation.constraints.NotEmpty;
import java.util.List;

/**
 * 接口状态管理控制器
 * 提供接口生命周期管理API：未上架 → 已上架 → 已下架
 *
 * @author PowerTrading Team
 * @since 2024-01-15
 */
@Slf4j
@RestController
@RequestMapping("/api/v1/status")
@Tag(name = "接口状态管理", description = "接口生命周期管理相关API")
@Validated
public class InterfaceStatusController {



    @Autowired
    private InterfaceStatusService interfaceStatusService;

    /**
     * 上架接口
     *
     * @param interfaceId 接口ID
     * @param httpRequest HTTP请求
     * @return 操作结果
     */
    @PostMapping("/{interfaceId}/publish")
    @Operation(summary = "上架接口", description = "将接口状态从未上架或已下架变更为已上架")
    public ApiResponse<Void> publishInterface(
            @Parameter(description = "接口ID", required = true)
            @PathVariable @NotBlank(message = "接口ID不能为空") String interfaceId,
            HttpServletRequest httpRequest) {
        try {
            String publishBy = getUserFromRequest(httpRequest);
            interfaceStatusService.publishInterface(interfaceId, publishBy);
            return ApiResponse.success();
        } catch (Exception e) {
            log.error("上架接口失败，接口ID: {}", interfaceId, e);
            return ApiResponse.error("上架接口失败: " + e.getMessage());
        }
    }

    /**
     * 下架接口
     *
     * @param interfaceId 接口ID
     * @param request 下架请求
     * @param httpRequest HTTP请求
     * @return 操作结果
     */
    @PostMapping("/{interfaceId}/offline")
    @Operation(summary = "下架接口", description = "将接口状态从已上架变更为已下架")
    public ApiResponse<Void> offlineInterface(
            @Parameter(description = "接口ID", required = true)
            @PathVariable @NotBlank(message = "接口ID不能为空") String interfaceId,
            @RequestBody @Valid OfflineInterfaceRequest request,
            HttpServletRequest httpRequest) {
        try {
            String offlineBy = getUserFromRequest(httpRequest);
            interfaceStatusService.offlineInterface(interfaceId, offlineBy, request.getOfflineReason());
            return ApiResponse.success();
        } catch (Exception e) {
            log.error("下架接口失败，接口ID: {}", interfaceId, e);
            return ApiResponse.error("下架接口失败: " + e.getMessage());
        }
    }

    /**
     * 重新上架接口
     *
     * @param interfaceId 接口ID
     * @param httpRequest HTTP请求
     * @return 操作结果
     */
    @PostMapping("/{interfaceId}/republish")
    @Operation(summary = "重新上架接口", description = "将接口状态从已下架变更为已上架")
    public ApiResponse<Void> republishInterface(
            @Parameter(description = "接口ID", required = true)
            @PathVariable @NotBlank(message = "接口ID不能为空") String interfaceId,
            HttpServletRequest httpRequest) {
        try {
            String republishBy = getUserFromRequest(httpRequest);
            interfaceStatusService.republishInterface(interfaceId, republishBy);
            return ApiResponse.success();
        } catch (Exception e) {
            log.error("重新上架接口失败，接口ID: {}", interfaceId, e);
            return ApiResponse.error("重新上架接口失败: " + e.getMessage());
        }
    }
    
    /**
     * 测试重新上架接口（简化版本）
     *
     * @param interfaceId 接口ID
     * @param httpRequest HTTP请求
     * @return 操作结果
     */
    @PostMapping("/{interfaceId}/test-republish")
    @Operation(summary = "测试重新上架接口", description = "测试版本的重新上架接口")
    public ApiResponse<Void> testRepublishInterface(
            @Parameter(description = "接口ID", required = true)
            @PathVariable @NotBlank(message = "接口ID不能为空") String interfaceId,
            HttpServletRequest httpRequest) {
        try {
            String republishBy = getUserFromRequest(httpRequest);
            interfaceStatusService.testRepublishInterface(interfaceId, republishBy);
            return ApiResponse.success();
        } catch (Exception e) {
            log.error("测试重新上架接口失败，接口ID: {}", interfaceId, e);
            return ApiResponse.error("测试重新上架接口失败: " + e.getMessage());
        }
    }
    
    /**
     * 测试上架接口（简化版本）
     *
     * @param interfaceId 接口ID
     * @param httpRequest HTTP请求
     * @return 操作结果
     */
    @PostMapping("/{interfaceId}/test-publish")
    @Operation(summary = "测试上架接口", description = "测试版本的上架接口")
    public ApiResponse<Void> testPublishInterface(
            @Parameter(description = "接口ID", required = true)
            @PathVariable @NotBlank(message = "接口ID不能为空") String interfaceId,
            HttpServletRequest httpRequest) {
        try {
            String publishBy = getUserFromRequest(httpRequest);
            interfaceStatusService.testPublishInterface(interfaceId, publishBy);
            return ApiResponse.success();
        } catch (Exception e) {
            log.error("测试上架接口失败，接口ID: {}", interfaceId, e);
            return ApiResponse.error("测试上架接口失败: " + e.getMessage());
        }
    }

    /**
     * 批量上架接口
     *
     * @param request 批量上架请求
     * @param httpRequest HTTP请求
     * @return 操作结果
     */
    @PostMapping("/batch/publish")
    @Operation(summary = "批量上架接口", description = "批量将接口状态变更为已上架")
    public ApiResponse<InterfaceStatusService.BatchOperationResult> batchPublishInterfaces(
            @RequestBody @Valid BatchPublishRequest request,
            HttpServletRequest httpRequest) {
        try {
            String publishBy = getUserFromRequest(httpRequest);
            InterfaceStatusService.BatchOperationResult result = 
                interfaceStatusService.batchPublishInterfaces(request.getInterfaceIds(), publishBy);
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
    @PostMapping("/batch/offline")
    @Operation(summary = "批量下架接口", description = "批量将接口状态变更为已下架")
    public ApiResponse<InterfaceStatusService.BatchOperationResult> batchOfflineInterfaces(
            @RequestBody @Valid BatchOfflineRequest request,
            HttpServletRequest httpRequest) {
        try {
            String offlineBy = getUserFromRequest(httpRequest);
            InterfaceStatusService.BatchOperationResult result = 
                interfaceStatusService.batchOfflineInterfaces(
                    request.getInterfaceIds(), offlineBy, request.getOfflineReason());
            return ApiResponse.success(result);
        } catch (Exception e) {
            log.error("批量下架接口失败", e);
            return ApiResponse.error("批量下架接口失败: " + e.getMessage());
        }
    }

    /**
     * 获取接口状态统计
     *
     * @return 状态统计信息
     */
    @GetMapping("/status/statistics")
    @Operation(summary = "获取接口状态统计", description = "获取各状态接口的数量统计")
    public ApiResponse<InterfaceStatusService.InterfaceStatusStatistics> getInterfaceStatusStatistics() {
        try {
            InterfaceStatusService.InterfaceStatusStatistics statistics = 
                interfaceStatusService.getInterfaceStatusStatistics();
            return ApiResponse.success(statistics);
        } catch (Exception e) {
            log.error("获取接口状态统计失败", e);
            return ApiResponse.error("获取接口状态统计失败: " + e.getMessage());
        }
    }

    /**
     * 检查接口状态变更权限
     *
     * @param interfaceId 接口ID
     * @param operation 操作类型
     * @return 权限检查结果
     */
    @GetMapping("/{interfaceId}/status/check")
    @Operation(summary = "检查接口状态变更权限", description = "检查当前用户是否有权限执行指定的状态变更操作")
    public ApiResponse<StatusOperationPermission> checkStatusOperationPermission(
            @Parameter(description = "接口ID", required = true)
            @PathVariable @NotBlank(message = "接口ID不能为空") String interfaceId,
            @Parameter(description = "操作类型：publish, offline, republish")
            @RequestParam @NotBlank(message = "操作类型不能为空") String operation) {
        try {
            // 这里应该实现具体的权限检查逻辑
            // 暂时返回允许所有操作
            StatusOperationPermission permission = new StatusOperationPermission();
            permission.setInterfaceId(interfaceId);
            permission.setOperation(operation);
            permission.setAllowed(true);
            permission.setReason("权限检查通过");
            
            return ApiResponse.success(permission);
        } catch (Exception e) {
            log.error("检查接口状态变更权限失败，接口ID: {}, 操作: {}", interfaceId, operation, e);
            return ApiResponse.error("检查接口状态变更权限失败: " + e.getMessage());
        }
    }

    /**
     * 获取接口状态变更历史
     *
     * @param interfaceId 接口ID
     * @return 状态变更历史
     */
    @GetMapping("/{interfaceId}/status/history")
    @Operation(summary = "获取接口状态变更历史", description = "获取指定接口的状态变更历史记录")
    public ApiResponse<List<StatusChangeHistory>> getInterfaceStatusHistory(
            @Parameter(description = "接口ID", required = true)
            @PathVariable @NotBlank(message = "接口ID不能为空") String interfaceId) {
        try {
            // 这里应该实现具体的历史记录查询逻辑
            // 暂时返回空列表
            List<StatusChangeHistory> history = List.of();
            return ApiResponse.success(history);
        } catch (Exception e) {
            log.error("获取接口状态变更历史失败，接口ID: {}", interfaceId, e);
            return ApiResponse.error("获取接口状态变更历史失败: " + e.getMessage());
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

    /**
     * 下架接口请求
     */
    public static class OfflineInterfaceRequest {
        @NotBlank(message = "下架原因不能为空")
        private String offlineReason;
        
        public String getOfflineReason() {
            return offlineReason;
        }
        
        public void setOfflineReason(String offlineReason) {
            this.offlineReason = offlineReason;
        }
    }

    /**
     * 批量上架请求
     */
    public static class BatchPublishRequest {
        @NotEmpty(message = "接口ID列表不能为空")
        private List<String> interfaceIds;
        
        public List<String> getInterfaceIds() {
            return interfaceIds;
        }
        
        public void setInterfaceIds(List<String> interfaceIds) {
            this.interfaceIds = interfaceIds;
        }
    }

    /**
     * 批量下架请求
     */
    public static class BatchOfflineRequest {
        @NotEmpty(message = "接口ID列表不能为空")
        private List<String> interfaceIds;
        
        @NotBlank(message = "下架原因不能为空")
        private String offlineReason;
        
        public List<String> getInterfaceIds() {
            return interfaceIds;
        }
        
        public void setInterfaceIds(List<String> interfaceIds) {
            this.interfaceIds = interfaceIds;
        }
        
        public String getOfflineReason() {
            return offlineReason;
        }
        
        public void setOfflineReason(String offlineReason) {
            this.offlineReason = offlineReason;
        }
    }

    /**
     * 状态操作权限
     */
    public static class StatusOperationPermission {
        private String interfaceId;
        private String operation;
        private boolean allowed;
        private String reason;
        
        public String getInterfaceId() { return interfaceId; }
        public void setInterfaceId(String interfaceId) { this.interfaceId = interfaceId; }
        public String getOperation() { return operation; }
        public void setOperation(String operation) { this.operation = operation; }
        public boolean isAllowed() { return allowed; }
        public void setAllowed(boolean allowed) { this.allowed = allowed; }
        public String getReason() { return reason; }
        public void setReason(String reason) { this.reason = reason; }
    }

    /**
     * 状态变更历史
     */
    public static class StatusChangeHistory {
        private String id;
        private String interfaceId;
        private String fromStatus;
        private String toStatus;
        private String changeBy;
        private String changeTime;
        private String reason;
        
        public String getId() { return id; }
        public void setId(String id) { this.id = id; }
        public String getInterfaceId() { return interfaceId; }
        public void setInterfaceId(String interfaceId) { this.interfaceId = interfaceId; }
        public String getFromStatus() { return fromStatus; }
        public void setFromStatus(String fromStatus) { this.fromStatus = fromStatus; }
        public String getToStatus() { return toStatus; }
        public void setToStatus(String toStatus) { this.toStatus = toStatus; }
        public String getChangeBy() { return changeBy; }
        public void setChangeBy(String changeBy) { this.changeBy = changeBy; }
        public String getChangeTime() { return changeTime; }
        public void setChangeTime(String changeTime) { this.changeTime = changeTime; }
        public String getReason() { return reason; }
        public void setReason(String reason) { this.reason = reason; }
    }


}