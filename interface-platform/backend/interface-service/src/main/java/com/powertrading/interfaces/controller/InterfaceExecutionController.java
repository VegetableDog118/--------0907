package com.powertrading.interfaces.controller;

import com.powertrading.interfaces.service.InterfaceExecutionService;
import com.powertrading.interfaces.common.ApiResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import javax.servlet.http.HttpServletRequest;
import javax.validation.constraints.NotBlank;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * 接口执行控制器
 * 提供动态路由注册后的接口执行API
 *
 * @author PowerTrading Team
 * @since 2024-01-15
 */
@Slf4j
@RestController
@RequestMapping("/api/v1/execution")
@Tag(name = "接口执行", description = "动态路由接口执行相关API")
@Validated
public class InterfaceExecutionController {



    @Autowired
    private InterfaceExecutionService interfaceExecutionService;

    /**
     * 执行接口（POST方式）
     * 主要用于网关动态路由转发
     *
     * @param interfaceId 接口ID
     * @param requestBody 请求体参数
     * @param httpRequest HTTP请求
     * @return 执行结果
     */
    @PostMapping("/execute/{interfaceId}")
    @Operation(summary = "执行接口（POST）", description = "通过POST方式执行指定接口，支持请求体参数")
    public ApiResponse<InterfaceExecutionService.InterfaceExecutionResult> executeInterfacePost(
            @Parameter(description = "接口ID", required = true)
            @PathVariable @NotBlank(message = "接口ID不能为空") String interfaceId,
            @RequestBody(required = false) Map<String, Object> requestBody,
            HttpServletRequest httpRequest) {
        try {
            // 合并请求参数
            Map<String, Object> allParams = mergeRequestParameters(httpRequest, requestBody);
            
            // 执行接口
            InterfaceExecutionService.InterfaceExecutionResult result = 
                interfaceExecutionService.executeInterface(interfaceId, allParams);
            
            return ApiResponse.success(result);
            
        } catch (Exception e) {
            log.error("执行接口失败，接口ID: {}", interfaceId, e);
            return ApiResponse.error("执行接口失败: " + e.getMessage());
        }
    }

    /**
     * 执行接口（GET方式）
     * 主要用于网关动态路由转发
     *
     * @param interfaceId 接口ID
     * @param httpRequest HTTP请求
     * @return 执行结果
     */
    @GetMapping("/execute/{interfaceId}")
    @Operation(summary = "执行接口（GET）", description = "通过GET方式执行指定接口，支持查询参数")
    public ApiResponse<InterfaceExecutionService.InterfaceExecutionResult> executeInterfaceGet(
            @Parameter(description = "接口ID", required = true)
            @PathVariable @NotBlank(message = "接口ID不能为空") String interfaceId,
            HttpServletRequest httpRequest) {
        try {
            // 获取查询参数
            Map<String, Object> queryParams = extractQueryParameters(httpRequest);
            
            // 执行接口
            InterfaceExecutionService.InterfaceExecutionResult result = 
                interfaceExecutionService.executeInterface(interfaceId, queryParams);
            
            return ApiResponse.success(result);
            
        } catch (Exception e) {
            log.error("执行接口失败，接口ID: {}", interfaceId, e);
            return ApiResponse.error("执行接口失败: " + e.getMessage());
        }
    }

    /**
     * 执行接口（PUT方式）
     * 主要用于网关动态路由转发
     *
     * @param interfaceId 接口ID
     * @param requestBody 请求体参数
     * @param httpRequest HTTP请求
     * @return 执行结果
     */
    @PutMapping("/execute/{interfaceId}")
    @Operation(summary = "执行接口（PUT）", description = "通过PUT方式执行指定接口，支持请求体参数")
    public ApiResponse<InterfaceExecutionService.InterfaceExecutionResult> executeInterfacePut(
            @Parameter(description = "接口ID", required = true)
            @PathVariable @NotBlank(message = "接口ID不能为空") String interfaceId,
            @RequestBody(required = false) Map<String, Object> requestBody,
            HttpServletRequest httpRequest) {
        try {
            // 合并请求参数
            Map<String, Object> allParams = mergeRequestParameters(httpRequest, requestBody);
            
            // 执行接口
            InterfaceExecutionService.InterfaceExecutionResult result = 
                interfaceExecutionService.executeInterface(interfaceId, allParams);
            
            return ApiResponse.success(result);
            
        } catch (Exception e) {
            log.error("执行接口失败，接口ID: {}", interfaceId, e);
            return ApiResponse.error("执行接口失败: " + e.getMessage());
        }
    }

    /**
     * 测试接口执行
     * 用于接口开发和调试阶段的测试
     *
     * @param interfaceId 接口ID
     * @param request 测试请求
     * @return 执行结果
     */
    @PostMapping("/test/{interfaceId}")
    @Operation(summary = "测试接口执行", description = "用于接口开发和调试阶段的测试执行")
    public ApiResponse<InterfaceTestResult> testInterfaceExecution(
            @Parameter(description = "接口ID", required = true)
            @PathVariable @NotBlank(message = "接口ID不能为空") String interfaceId,
            @RequestBody InterfaceTestRequest request) {
        try {
            // 执行接口
            InterfaceExecutionService.InterfaceExecutionResult executionResult = 
                interfaceExecutionService.executeInterface(interfaceId, request.getParams());
            
            // 构建测试结果
            InterfaceTestResult testResult = new InterfaceTestResult();
            testResult.setInterfaceId(interfaceId);
            testResult.setTestTime(executionResult.getExecuteTime());
            testResult.setSuccess(executionResult.isSuccess());
            testResult.setErrorMessage(executionResult.getErrorMessage());
            testResult.setExecuteSql(executionResult.getExecuteSql());
            testResult.setResultData(executionResult.getData());
            testResult.setTotalCount(executionResult.getTotalCount());
            testResult.setRequestParams(request.getParams());
            
            return ApiResponse.success(testResult);
            
        } catch (Exception e) {
            log.error("测试接口执行失败，接口ID: {}", interfaceId, e);
            
            InterfaceTestResult testResult = new InterfaceTestResult();
            testResult.setInterfaceId(interfaceId);
            testResult.setSuccess(false);
            testResult.setErrorMessage(e.getMessage());
            testResult.setRequestParams(request.getParams());
            
            return ApiResponse.success(testResult);
        }
    }

    /**
     * 获取接口执行统计
     *
     * @param interfaceId 接口ID
     * @return 执行统计
     */
    @GetMapping("/{interfaceId}/execution/statistics")
    @Operation(summary = "获取接口执行统计", description = "获取指定接口的执行统计信息")
    public ApiResponse<InterfaceExecutionStatistics> getInterfaceExecutionStatistics(
            @Parameter(description = "接口ID", required = true)
            @PathVariable @NotBlank(message = "接口ID不能为空") String interfaceId) {
        try {
            // 这里应该实现具体的统计逻辑
            // 暂时返回模拟数据
            InterfaceExecutionStatistics statistics = new InterfaceExecutionStatistics();
            statistics.setInterfaceId(interfaceId);
            statistics.setTotalExecutions(0L);
            statistics.setSuccessExecutions(0L);
            statistics.setFailedExecutions(0L);
            statistics.setAverageResponseTime(0.0);
            statistics.setLastExecuteTime(null);
            
            return ApiResponse.success(statistics);
            
        } catch (Exception e) {
            log.error("获取接口执行统计失败，接口ID: {}", interfaceId, e);
            return ApiResponse.error("获取接口执行统计失败: " + e.getMessage());
        }
    }

    /**
     * 健康检查接口
     * 用于网关检查接口服务状态
     *
     * @return 健康状态
     */
    @GetMapping("/health")
    @Operation(summary = "健康检查", description = "检查接口执行服务的健康状态")
    public ApiResponse<Map<String, Object>> healthCheck() {
        try {
            Map<String, Object> health = new HashMap<>();
            health.put("status", "UP");
            health.put("service", "interface-execution-service");
            health.put("timestamp", System.currentTimeMillis());
            
            return ApiResponse.success(health);
            
        } catch (Exception e) {
            log.error("健康检查失败", e);
            
            Map<String, Object> health = new HashMap<>();
            health.put("status", "DOWN");
            health.put("error", e.getMessage());
            health.put("timestamp", System.currentTimeMillis());
            
            ApiResponse<Map<String, Object>> response = ApiResponse.error("服务不可用");
            response.setData(health);
            return response;
        }
    }

    /**
     * 合并请求参数
     */
    private Map<String, Object> mergeRequestParameters(HttpServletRequest httpRequest, 
                                                      Map<String, Object> requestBody) {
        Map<String, Object> allParams = new HashMap<>();
        
        // 添加查询参数
        Map<String, Object> queryParams = extractQueryParameters(httpRequest);
        allParams.putAll(queryParams);
        
        // 添加请求体参数（优先级更高）
        if (requestBody != null) {
            allParams.putAll(requestBody);
        }
        
        return allParams;
    }

    /**
     * 提取查询参数
     */
    private Map<String, Object> extractQueryParameters(HttpServletRequest httpRequest) {
        Map<String, Object> params = new HashMap<>();
        
        Enumeration<String> parameterNames = httpRequest.getParameterNames();
        while (parameterNames.hasMoreElements()) {
            String paramName = parameterNames.nextElement();
            String[] paramValues = httpRequest.getParameterValues(paramName);
            
            if (paramValues.length == 1) {
                params.put(paramName, paramValues[0]);
            } else {
                params.put(paramName, paramValues);
            }
        }
        
        return params;
    }

    /**
     * 接口测试请求
     */
    public static class InterfaceTestRequest {
        private Map<String, Object> params;
        
        public Map<String, Object> getParams() {
            return params;
        }
        
        public void setParams(Map<String, Object> params) {
            this.params = params;
        }
    }

    /**
     * 接口测试结果
     */
    public static class InterfaceTestResult {
        private String interfaceId;
        private String testTime;
        private boolean success;
        private String errorMessage;
        private String executeSql;
        private List<Map<String, Object>> resultData;
        private long totalCount;
        private Map<String, Object> requestParams;
        
        // getters and setters
        public String getInterfaceId() { return interfaceId; }
        public void setInterfaceId(String interfaceId) { this.interfaceId = interfaceId; }
        public String getTestTime() { return testTime; }
        public void setTestTime(String testTime) { this.testTime = testTime; }
        public boolean isSuccess() { return success; }
        public void setSuccess(boolean success) { this.success = success; }
        public String getErrorMessage() { return errorMessage; }
        public void setErrorMessage(String errorMessage) { this.errorMessage = errorMessage; }
        public String getExecuteSql() { return executeSql; }
        public void setExecuteSql(String executeSql) { this.executeSql = executeSql; }
        public List<Map<String, Object>> getResultData() { return resultData; }
        public void setResultData(List<Map<String, Object>> resultData) { this.resultData = resultData; }
        public long getTotalCount() { return totalCount; }
        public void setTotalCount(long totalCount) { this.totalCount = totalCount; }
        public Map<String, Object> getRequestParams() { return requestParams; }
        public void setRequestParams(Map<String, Object> requestParams) { this.requestParams = requestParams; }
    }

    /**
     * 接口执行统计
     */
    public static class InterfaceExecutionStatistics {
        private String interfaceId;
        private long totalExecutions;
        private long successExecutions;
        private long failedExecutions;
        private double averageResponseTime;
        private String lastExecuteTime;
        
        // getters and setters
        public String getInterfaceId() { return interfaceId; }
        public void setInterfaceId(String interfaceId) { this.interfaceId = interfaceId; }
        public long getTotalExecutions() { return totalExecutions; }
        public void setTotalExecutions(long totalExecutions) { this.totalExecutions = totalExecutions; }
        public long getSuccessExecutions() { return successExecutions; }
        public void setSuccessExecutions(long successExecutions) { this.successExecutions = successExecutions; }
        public long getFailedExecutions() { return failedExecutions; }
        public void setFailedExecutions(long failedExecutions) { this.failedExecutions = failedExecutions; }
        public double getAverageResponseTime() { return averageResponseTime; }
        public void setAverageResponseTime(double averageResponseTime) { this.averageResponseTime = averageResponseTime; }
        public String getLastExecuteTime() { return lastExecuteTime; }
        public void setLastExecuteTime(String lastExecuteTime) { this.lastExecuteTime = lastExecuteTime; }
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