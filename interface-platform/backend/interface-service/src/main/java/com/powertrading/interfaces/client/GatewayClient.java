package com.powertrading.interfaces.client;

import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

/**
 * 网关服务客户端
 *
 * @author PowerTrading Team
 * @since 2024-01-15
 */
@FeignClient(name = "gateway-service", path = "/api/v1/gateway")
public interface GatewayClient {

    /**
     * 注册接口路由
     *
     * @param request 路由注册请求
     * @return 注册结果
     */
    @PostMapping("/routes")
    ApiResponse<String> registerRoute(@RequestBody RouteRegistrationRequest request);

    /**
     * 更新接口路由
     *
     * @param routeId 路由ID
     * @param request 路由更新请求
     * @return 更新结果
     */
    @PutMapping("/routes/{routeId}")
    ApiResponse<String> updateRoute(@PathVariable("routeId") String routeId,
                                   @RequestBody RouteRegistrationRequest request);

    /**
     * 删除接口路由
     *
     * @param routeId 路由ID
     * @return 删除结果
     */
    @DeleteMapping("/routes/{routeId}")
    ApiResponse<String> deleteRoute(@PathVariable("routeId") String routeId);

    /**
     * 批量注册路由
     *
     * @param requests 批量路由注册请求
     * @return 注册结果
     */
    @PostMapping("/routes/batch")
    ApiResponse<BatchOperationResult> batchRegisterRoutes(@RequestBody List<RouteRegistrationRequest> requests);

    /**
     * 批量删除路由
     *
     * @param routeIds 路由ID列表
     * @return 删除结果
     */
    @DeleteMapping("/routes/batch")
    ApiResponse<BatchOperationResult> batchDeleteRoutes(@RequestBody List<String> routeIds);

    /**
     * 获取路由列表
     *
     * @return 路由列表
     */
    @GetMapping("/routes")
    ApiResponse<List<RouteInfo>> getRoutes();

    /**
     * 刷新路由配置
     *
     * @return 刷新结果
     */
    @PostMapping("/routes/refresh")
    ApiResponse<String> refreshRoutes();

    /**
     * API响应包装类
     */
    class ApiResponse<T> {
        private String code;
        private String message;
        private T data;
        private Boolean success;
        
        // getters and setters
        public String getCode() { return code; }
        public void setCode(String code) { this.code = code; }
        public String getMessage() { return message; }
        public void setMessage(String message) { this.message = message; }
        public T getData() { return data; }
        public void setData(T data) { this.data = data; }
        public Boolean getSuccess() { return success; }
        public void setSuccess(Boolean success) { this.success = success; }
        public boolean isSuccess() { return Boolean.TRUE.equals(success); }
    }

    /**
     * 路由注册请求
     */
    class RouteRegistrationRequest {
        private String routeId;
        private String interfaceId;
        private String interfacePath;
        private String targetUri;
        private String method;
        private Integer rateLimit;
        private Integer timeout;
        private List<String> requiredPermissions;
        private Map<String, String> metadata;
        
        // getters and setters
        public String getRouteId() { return routeId; }
        public void setRouteId(String routeId) { this.routeId = routeId; }
        public String getInterfaceId() { return interfaceId; }
        public void setInterfaceId(String interfaceId) { this.interfaceId = interfaceId; }
        public String getInterfacePath() { return interfacePath; }
        public void setInterfacePath(String interfacePath) { this.interfacePath = interfacePath; }
        public String getTargetUri() { return targetUri; }
        public void setTargetUri(String targetUri) { this.targetUri = targetUri; }
        public String getMethod() { return method; }
        public void setMethod(String method) { this.method = method; }
        public Integer getRateLimit() { return rateLimit; }
        public void setRateLimit(Integer rateLimit) { this.rateLimit = rateLimit; }
        public Integer getTimeout() { return timeout; }
        public void setTimeout(Integer timeout) { this.timeout = timeout; }
        public List<String> getRequiredPermissions() { return requiredPermissions; }
        public void setRequiredPermissions(List<String> requiredPermissions) { this.requiredPermissions = requiredPermissions; }
        public Map<String, String> getMetadata() { return metadata; }
        public void setMetadata(Map<String, String> metadata) { this.metadata = metadata; }
    }

    /**
     * 批量操作结果
     */
    class BatchOperationResult {
        private Integer successCount;
        private Integer failureCount;
        private List<String> successIds;
        private List<FailureInfo> failures;
        
        // getters and setters
        public Integer getSuccessCount() { return successCount; }
        public void setSuccessCount(Integer successCount) { this.successCount = successCount; }
        public Integer getFailureCount() { return failureCount; }
        public void setFailureCount(Integer failureCount) { this.failureCount = failureCount; }
        public List<String> getSuccessIds() { return successIds; }
        public void setSuccessIds(List<String> successIds) { this.successIds = successIds; }
        public List<FailureInfo> getFailures() { return failures; }
        public void setFailures(List<FailureInfo> failures) { this.failures = failures; }
    }

    /**
     * 失败信息
     */
    class FailureInfo {
        private String id;
        private String reason;
        
        // getters and setters
        public String getId() { return id; }
        public void setId(String id) { this.id = id; }
        public String getReason() { return reason; }
        public void setReason(String reason) { this.reason = reason; }
    }

    /**
     * 路由信息
     */
    class RouteInfo {
        private String routeId;
        private String interfaceId;
        private String interfacePath;
        private String targetUri;
        private String method;
        private Integer rateLimit;
        private Integer timeout;
        private String status;
        private String createTime;
        private String updateTime;
        
        // getters and setters
        public String getRouteId() { return routeId; }
        public void setRouteId(String routeId) { this.routeId = routeId; }
        public String getInterfaceId() { return interfaceId; }
        public void setInterfaceId(String interfaceId) { this.interfaceId = interfaceId; }
        public String getInterfacePath() { return interfacePath; }
        public void setInterfacePath(String interfacePath) { this.interfacePath = interfacePath; }
        public String getTargetUri() { return targetUri; }
        public void setTargetUri(String targetUri) { this.targetUri = targetUri; }
        public String getMethod() { return method; }
        public void setMethod(String method) { this.method = method; }
        public Integer getRateLimit() { return rateLimit; }
        public void setRateLimit(Integer rateLimit) { this.rateLimit = rateLimit; }
        public Integer getTimeout() { return timeout; }
        public void setTimeout(Integer timeout) { this.timeout = timeout; }
        public String getStatus() { return status; }
        public void setStatus(String status) { this.status = status; }
        public String getCreateTime() { return createTime; }
        public void setCreateTime(String createTime) { this.createTime = createTime; }
        public String getUpdateTime() { return updateTime; }
        public void setUpdateTime(String updateTime) { this.updateTime = updateTime; }
    }
}