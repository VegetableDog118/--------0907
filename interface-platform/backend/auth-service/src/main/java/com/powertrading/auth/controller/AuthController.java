package com.powertrading.auth.controller;

import com.powertrading.auth.dto.*;
import com.powertrading.auth.service.AuthService;
import com.powertrading.auth.service.TokenBlacklistService;
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
import java.util.List;
import java.util.Map;

/**
 * 认证控制器
 *
 * @author PowerTrading Team
 * @since 2024-01-15
 */
@Slf4j
@RestController
@RequestMapping("/api/v1/auth")
@Tag(name = "认证管理", description = "JWT Token认证相关接口")
public class AuthController {

    @Autowired
    private AuthService authService;

    @Autowired
    private TokenBlacklistService tokenBlacklistService;

    /**
     * 生成Token
     */
    @PostMapping("/token/generate")
    @Operation(summary = "生成Token", description = "为用户生成JWT访问Token和刷新Token")
    public ResponseEntity<Map<String, Object>> generateToken(
            @Valid @RequestBody TokenGenerateRequest request,
            HttpServletRequest httpRequest) {
        
        try {
            // 设置客户端信息
            request.setClientIp(getClientIp(httpRequest));
            request.setUserAgent(httpRequest.getHeader("User-Agent"));
            
            TokenGenerateResponse response = authService.generateToken(request);
            
            Map<String, Object> result = new HashMap<>();
            result.put("code", 200);
            result.put("message", "Token生成成功");
            result.put("data", response);
            
            return ResponseEntity.ok(result);
        } catch (Exception e) {
            log.error("生成Token失败", e);
            
            Map<String, Object> result = new HashMap<>();
            result.put("code", 500);
            result.put("message", "Token生成失败: " + e.getMessage());
            
            return ResponseEntity.status(500).body(result);
        }
    }

    /**
     * 验证Token
     */
    @PostMapping("/token/validate")
    @Operation(summary = "验证Token", description = "验证JWT Token的有效性和权限")
    public ResponseEntity<Map<String, Object>> validateToken(
            @Valid @RequestBody TokenValidateRequest request,
            HttpServletRequest httpRequest) {
        
        try {
            // 设置客户端IP
            request.setClientIp(getClientIp(httpRequest));
            
            TokenValidateResponse response = authService.validateToken(request);
            
            Map<String, Object> result = new HashMap<>();
            if (response.getValid()) {
                result.put("code", 200);
                result.put("message", "Token验证成功");
                result.put("data", response);
                return ResponseEntity.ok(result);
            } else {
                result.put("code", 401);
                result.put("message", response.getErrorMessage());
                return ResponseEntity.status(401).body(result);
            }
        } catch (Exception e) {
            log.error("验证Token失败", e);
            
            Map<String, Object> result = new HashMap<>();
            result.put("code", 401);
            result.put("message", "Token验证失败: " + e.getMessage());
            
            return ResponseEntity.status(401).body(result);
        }
    }

    /**
     * 刷新Token
     */
    @PostMapping("/token/refresh")
    @Operation(summary = "刷新Token", description = "使用刷新Token获取新的访问Token")
    public ResponseEntity<Map<String, Object>> refreshToken(
            @Parameter(description = "刷新Token") @RequestParam String refreshToken,
            @Parameter(description = "用户ID") @RequestParam String userId,
            @Parameter(description = "用户名") @RequestParam String username,
            @Parameter(description = "公司名称") @RequestParam(required = false) String companyName,
            @Parameter(description = "角色列表") @RequestParam List<String> roles,
            @Parameter(description = "权限列表") @RequestParam List<String> permissions) {
        
        try {
            TokenGenerateResponse response = authService.refreshToken(
                    refreshToken, userId, username, companyName, roles, permissions
            );
            
            Map<String, Object> result = new HashMap<>();
            result.put("code", 200);
            result.put("message", "Token刷新成功");
            result.put("data", response);
            
            return ResponseEntity.ok(result);
        } catch (Exception e) {
            log.error("刷新Token失败", e);
            
            Map<String, Object> result = new HashMap<>();
            result.put("code", 401);
            result.put("message", "Token刷新失败: " + e.getMessage());
            
            return ResponseEntity.status(401).body(result);
        }
    }

    /**
     * 撤销Token
     */
    @PostMapping("/token/revoke")
    @Operation(summary = "撤销Token", description = "撤销指定的JWT Token")
    public ResponseEntity<Map<String, Object>> revokeToken(
            @Parameter(description = "要撤销的Token") @RequestParam String token) {
        
        try {
            authService.revokeToken(token);
            
            Map<String, Object> result = new HashMap<>();
            result.put("code", 200);
            result.put("message", "Token撤销成功");
            
            return ResponseEntity.ok(result);
        } catch (Exception e) {
            log.error("撤销Token失败", e);
            
            Map<String, Object> result = new HashMap<>();
            result.put("code", 500);
            result.put("message", "Token撤销失败: " + e.getMessage());
            
            return ResponseEntity.status(500).body(result);
        }
    }

    /**
     * 撤销用户所有Token
     */
    @PostMapping("/token/revoke-all")
    @Operation(summary = "撤销用户所有Token", description = "撤销指定用户的所有JWT Token")
    public ResponseEntity<Map<String, Object>> revokeUserAllTokens(
            @Parameter(description = "用户ID") @RequestParam String userId) {
        
        try {
            authService.revokeUserAllTokens(userId);
            
            Map<String, Object> result = new HashMap<>();
            result.put("code", 200);
            result.put("message", "用户所有Token撤销成功");
            
            return ResponseEntity.ok(result);
        } catch (Exception e) {
            log.error("撤销用户所有Token失败", e);
            
            Map<String, Object> result = new HashMap<>();
            result.put("code", 500);
            result.put("message", "撤销用户所有Token失败: " + e.getMessage());
            
            return ResponseEntity.status(500).body(result);
        }
    }

    /**
     * 获取黑名单状态
     */
    @GetMapping("/blacklist/status")
    @Operation(summary = "获取黑名单状态", description = "获取Token黑名单的状态信息")
    public ResponseEntity<Map<String, Object>> getBlacklistStatus() {
        
        try {
            Map<String, Object> status = new HashMap<>();
            status.put("enabled", tokenBlacklistService.isBlacklistEnabled());
            status.put("size", tokenBlacklistService.getBlacklistSize());
            
            Map<String, Object> result = new HashMap<>();
            result.put("code", 200);
            result.put("message", "获取黑名单状态成功");
            result.put("data", status);
            
            return ResponseEntity.ok(result);
        } catch (Exception e) {
            log.error("获取黑名单状态失败", e);
            
            Map<String, Object> result = new HashMap<>();
            result.put("code", 500);
            result.put("message", "获取黑名单状态失败: " + e.getMessage());
            
            return ResponseEntity.status(500).body(result);
        }
    }

    /**
     * 清空黑名单
     */
    @PostMapping("/blacklist/clear")
    @Operation(summary = "清空黑名单", description = "清空所有Token黑名单记录")
    public ResponseEntity<Map<String, Object>> clearBlacklist() {
        
        try {
            tokenBlacklistService.clearAllBlacklist();
            
            Map<String, Object> result = new HashMap<>();
            result.put("code", 200);
            result.put("message", "黑名单清空成功");
            
            return ResponseEntity.ok(result);
        } catch (Exception e) {
            log.error("清空黑名单失败", e);
            
            Map<String, Object> result = new HashMap<>();
            result.put("code", 500);
            result.put("message", "清空黑名单失败: " + e.getMessage());
            
            return ResponseEntity.status(500).body(result);
        }
    }

    /**
     * 健康检查
     */
    @GetMapping("/health")
    @Operation(summary = "健康检查", description = "检查认证服务的健康状态")
    public ResponseEntity<Map<String, Object>> health() {
        Map<String, Object> result = new HashMap<>();
        result.put("code", 200);
        result.put("message", "认证服务运行正常");
        result.put("timestamp", System.currentTimeMillis());
        
        return ResponseEntity.ok(result);
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