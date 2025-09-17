package com.powertrading.auth.controller;

import com.powertrading.auth.dto.AuthenticationRequest;
import com.powertrading.auth.dto.AuthenticationResult;
import com.powertrading.auth.service.AuditLogService;
import com.powertrading.auth.service.MultiAuthService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import javax.servlet.http.HttpServletRequest;
import javax.validation.Valid;
import java.util.HashMap;
import java.util.Map;

/**
 * 多重认证控制器
 *
 * @author PowerTrading Team
 * @since 2024-01-15
 */
@Slf4j
@RestController
@RequestMapping("/api/v1/multi-auth")
@Tag(name = "多重认证管理", description = "统一认证接口，支持JWT Token和API密钥认证")
public class MultiAuthController {

    @Autowired
    private MultiAuthService multiAuthService;

    @Autowired
    private AuditLogService auditLogService;

    /**
     * 统一认证接口
     */
    @PostMapping("/authenticate")
    @Operation(summary = "统一认证", description = "支持JWT Token、API密钥、混合认证等多种认证方式")
    public ResponseEntity<Map<String, Object>> authenticate(
            @Valid @RequestBody AuthenticationRequest request,
            HttpServletRequest httpRequest) {
        
        try {
            // 设置客户端信息
            request.setClientIp(getClientIp(httpRequest));
            request.setUserAgent(httpRequest.getHeader("User-Agent"));
            
            // 执行认证
            AuthenticationResult result = multiAuthService.authenticate(request);
            
            Map<String, Object> response = new HashMap<>();
            if (result.isSuccess()) {
                response.put("code", 200);
                response.put("message", "认证成功");
                response.put("data", result);
                return ResponseEntity.ok(response);
            } else {
                response.put("code", 401);
                response.put("message", result.getErrorMessage());
                response.put("errorCode", result.getErrorCode());
                return ResponseEntity.status(401).body(response);
            }
        } catch (Exception e) {
            log.error("统一认证失败", e);
            
            Map<String, Object> response = new HashMap<>();
            response.put("code", 500);
            response.put("message", "认证过程中发生错误: " + e.getMessage());
            
            return ResponseEntity.status(500).body(response);
        }
    }

    /**
     * 快速认证接口（仅返回认证状态）
     */
    @PostMapping("/quick-auth")
    @Operation(summary = "快速认证", description = "快速认证接口，仅返回认证成功或失败")
    public ResponseEntity<Map<String, Object>> quickAuth(
            @Parameter(description = "JWT Token") @RequestParam(required = false) String jwtToken,
            @Parameter(description = "应用ID") @RequestParam(required = false) String appId,
            @Parameter(description = "API密钥") @RequestParam(required = false) String apiKey,
            @Parameter(description = "请求路径") @RequestParam(required = false) String requestPath,
            HttpServletRequest httpRequest) {
        
        try {
            AuthenticationRequest request = new AuthenticationRequest();
            request.setJwtToken(jwtToken);
            request.setAppId(appId);
            request.setApiKey(apiKey);
            request.setRequestPath(requestPath);
            request.setClientIp(getClientIp(httpRequest));
            request.setUserAgent(httpRequest.getHeader("User-Agent"));
            
            AuthenticationResult result = multiAuthService.authenticate(request);
            
            Map<String, Object> response = new HashMap<>();
            if (result.isSuccess()) {
                Map<String, Object> data = new HashMap<>();
                data.put("authenticated", true);
                data.put("userId", result.getUserId());
                data.put("username", result.getUsername());
                data.put("authType", result.getAuthType());
                
                response.put("code", 200);
                response.put("message", "认证成功");
                response.put("data", data);
                return ResponseEntity.ok(response);
            } else {
                response.put("code", 401);
                response.put("message", "认证失败");
                response.put("data", Map.of("authenticated", false));
                return ResponseEntity.status(401).body(response);
            }
        } catch (Exception e) {
            log.error("快速认证失败", e);
            
            Map<String, Object> response = new HashMap<>();
            response.put("code", 500);
            response.put("message", "认证失败");
            response.put("data", Map.of("authenticated", false));
            
            return ResponseEntity.status(500).body(response);
        }
    }

    /**
     * 权限检查接口
     */
    @PostMapping("/check-permission")
    @Operation(summary = "权限检查", description = "检查用户是否具有指定权限")
    public ResponseEntity<Map<String, Object>> checkPermission(
            @Parameter(description = "用户ID") @RequestParam String userId,
            @Parameter(description = "权限名称") @RequestParam String permission,
            @Parameter(description = "资源路径") @RequestParam(required = false) String resource,
            HttpServletRequest httpRequest) {
        
        try {
            boolean hasPermission = multiAuthService.hasPermission(userId, permission);
            
            // 记录权限检查日志
            auditLogService.logPermissionCheck(
                    userId, permission, hasPermission, resource, getClientIp(httpRequest)
            );
            
            Map<String, Object> data = new HashMap<>();
            data.put("userId", userId);
            data.put("permission", permission);
            data.put("granted", hasPermission);
            data.put("resource", resource);
            
            Map<String, Object> response = new HashMap<>();
            response.put("code", 200);
            response.put("message", hasPermission ? "权限检查通过" : "权限不足");
            response.put("data", data);
            
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            log.error("权限检查失败", e);
            
            Map<String, Object> response = new HashMap<>();
            response.put("code", 500);
            response.put("message", "权限检查失败: " + e.getMessage());
            
            return ResponseEntity.status(500).body(response);
        }
    }

    /**
     * 接口访问权限检查
     */
    @PostMapping("/check-interface-access")
    @Operation(summary = "接口访问权限检查", description = "检查用户是否可以访问指定接口")
    public ResponseEntity<Map<String, Object>> checkInterfaceAccess(
            @Parameter(description = "用户ID") @RequestParam String userId,
            @Parameter(description = "应用ID") @RequestParam(required = false) String appId,
            @Parameter(description = "接口路径") @RequestParam String interfacePath,
            HttpServletRequest httpRequest) {
        
        try {
            boolean hasAccess = multiAuthService.hasInterfaceAccess(userId, appId, interfacePath);
            
            Map<String, Object> data = new HashMap<>();
            data.put("userId", userId);
            data.put("appId", appId);
            data.put("interfacePath", interfacePath);
            data.put("hasAccess", hasAccess);
            
            Map<String, Object> response = new HashMap<>();
            response.put("code", 200);
            response.put("message", hasAccess ? "接口访问权限检查通过" : "无权限访问该接口");
            response.put("data", data);
            
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            log.error("接口访问权限检查失败", e);
            
            Map<String, Object> response = new HashMap<>();
            response.put("code", 500);
            response.put("message", "接口访问权限检查失败: " + e.getMessage());
            
            return ResponseEntity.status(500).body(response);
        }
    }

    /**
     * 获取认证统计信息
     */
    @GetMapping("/statistics")
    @Operation(summary = "获取认证统计信息", description = "获取认证相关的统计数据")
    public ResponseEntity<Map<String, Object>> getStatistics() {
        
        try {
            Map<String, Object> statistics = multiAuthService.getAuthStatistics();
            
            Map<String, Object> response = new HashMap<>();
            response.put("code", 200);
            response.put("message", "获取统计信息成功");
            response.put("data", statistics);
            
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            log.error("获取认证统计信息失败", e);
            
            Map<String, Object> response = new HashMap<>();
            response.put("code", 500);
            response.put("message", "获取统计信息失败: " + e.getMessage());
            
            return ResponseEntity.status(500).body(response);
        }
    }

    /**
     * 获取用户审计日志
     */
    @GetMapping("/audit-logs")
    @Operation(summary = "获取用户审计日志", description = "获取指定用户的审计日志")
    public ResponseEntity<Map<String, Object>> getAuditLogs(
            @Parameter(description = "用户ID") @RequestParam String userId,
            @Parameter(description = "日志类型") @RequestParam(defaultValue = "success") String logType,
            @Parameter(description = "返回条数") @RequestParam(defaultValue = "50") int limit) {
        
        try {
            Map<String, Object> auditLogs = auditLogService.getUserAuditLogs(userId, logType, limit);
            
            Map<String, Object> response = new HashMap<>();
            response.put("code", 200);
            response.put("message", "获取审计日志成功");
            response.put("data", auditLogs);
            
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            log.error("获取用户审计日志失败", e);
            
            Map<String, Object> response = new HashMap<>();
            response.put("code", 500);
            response.put("message", "获取审计日志失败: " + e.getMessage());
            
            return ResponseEntity.status(500).body(response);
        }
    }

    /**
     * 获取每日统计信息
     */
    @GetMapping("/daily-stats")
    @Operation(summary = "获取每日统计信息", description = "获取指定日期的认证统计信息")
    public ResponseEntity<Map<String, Object>> getDailyStats(
            @Parameter(description = "日期", example = "2024-01-15") @RequestParam String date) {
        
        try {
            Map<String, Object> dailyStats = auditLogService.getDailyStats(date);
            
            Map<String, Object> response = new HashMap<>();
            response.put("code", 200);
            response.put("message", "获取每日统计信息成功");
            response.put("data", dailyStats);
            
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            log.error("获取每日统计信息失败", e);
            
            Map<String, Object> response = new HashMap<>();
            response.put("code", 500);
            response.put("message", "获取每日统计信息失败: " + e.getMessage());
            
            return ResponseEntity.status(500).body(response);
        }
    }

    /**
     * 健康检查
     */
    @GetMapping("/health")
    @Operation(summary = "健康检查", description = "检查多重认证服务的健康状态")
    public ResponseEntity<Map<String, Object>> health() {
        Map<String, Object> response = new HashMap<>();
        response.put("code", 200);
        response.put("message", "多重认证服务运行正常");
        response.put("timestamp", System.currentTimeMillis());
        
        return ResponseEntity.ok(response);
    }

    /**
     * 获取客户端IP地址
     */
    private String getClientIp(HttpServletRequest request) {
        String xForwardedFor = request.getHeader("X-Forwarded-For");
        if (xForwardedFor != null && !xForwardedFor.isEmpty() && !"unknown".equalsIgnoreCase(xForwardedFor)) {
            return xForwardedFor.split(",")[0].trim();
        }
        
        String xRealIp = request.getHeader("X-Real-IP");
        if (xRealIp != null && !xRealIp.isEmpty() && !"unknown".equalsIgnoreCase(xRealIp)) {
            return xRealIp;
        }
        
        return request.getRemoteAddr();
    }
}