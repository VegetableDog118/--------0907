package com.powertrading.auth.controller;

import com.powertrading.auth.dto.ApiKeyValidateRequest;
import com.powertrading.auth.dto.ApiKeyValidateResponse;
import com.powertrading.auth.service.ApiKeyService;
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
 * API密钥管理控制器
 *
 * @author PowerTrading Team
 * @since 2024-01-15
 */
@Slf4j
@RestController
@RequestMapping("/api/v1/apikey")
@Tag(name = "API密钥管理", description = "API密钥生成、验证、管理相关接口")
public class ApiKeyController {

    @Autowired
    private ApiKeyService apiKeyService;

    /**
     * 生成API密钥
     */
    @PostMapping("/generate")
    @Operation(summary = "生成API密钥", description = "为用户生成新的API密钥和AppId")
    public ResponseEntity<Map<String, Object>> generateApiKey(
            @Parameter(description = "用户ID") @RequestParam String userId,
            @Parameter(description = "用户名") @RequestParam String username,
            @Parameter(description = "公司名称") @RequestParam(required = false) String companyName,
            @Parameter(description = "权限列表") @RequestParam(required = false) List<String> permissions,
            @Parameter(description = "允许访问的接口列表") @RequestParam(required = false) List<String> allowedInterfaces,
            @Parameter(description = "每日调用限制") @RequestParam(required = false) Long dailyLimit) {
        
        try {
            Map<String, String> result = apiKeyService.generateApiKey(
                    userId, username, companyName, permissions, allowedInterfaces, dailyLimit
            );
            
            Map<String, Object> response = new HashMap<>();
            response.put("code", 200);
            response.put("message", "API密钥生成成功");
            response.put("data", result);
            
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            log.error("生成API密钥失败", e);
            
            Map<String, Object> response = new HashMap<>();
            response.put("code", 500);
            response.put("message", "API密钥生成失败: " + e.getMessage());
            
            return ResponseEntity.status(500).body(response);
        }
    }

    /**
     * 验证API密钥
     */
    @PostMapping("/validate")
    @Operation(summary = "验证API密钥", description = "验证AppId和API密钥的有效性")
    public ResponseEntity<Map<String, Object>> validateApiKey(
            @Valid @RequestBody ApiKeyValidateRequest request,
            HttpServletRequest httpRequest) {
        
        try {
            // 设置客户端IP
            request.setClientIp(getClientIp(httpRequest));
            
            ApiKeyValidateResponse response = apiKeyService.validateApiKey(request);
            
            Map<String, Object> result = new HashMap<>();
            if (response.getValid()) {
                result.put("code", 200);
                result.put("message", "API密钥验证成功");
                result.put("data", response);
                return ResponseEntity.ok(result);
            } else {
                result.put("code", 401);
                result.put("message", response.getErrorMessage());
                result.put("errorCode", response.getErrorCode());
                return ResponseEntity.status(401).body(result);
            }
        } catch (Exception e) {
            log.error("验证API密钥失败", e);
            
            Map<String, Object> result = new HashMap<>();
            result.put("code", 401);
            result.put("message", "API密钥验证失败: " + e.getMessage());
            
            return ResponseEntity.status(401).body(result);
        }
    }

    /**
     * 禁用API密钥
     */
    @PostMapping("/disable")
    @Operation(summary = "禁用API密钥", description = "禁用指定的API密钥")
    public ResponseEntity<Map<String, Object>> disableApiKey(
            @Parameter(description = "API密钥") @RequestParam String apiKey) {
        
        try {
            apiKeyService.disableApiKey(apiKey);
            
            Map<String, Object> response = new HashMap<>();
            response.put("code", 200);
            response.put("message", "API密钥禁用成功");
            
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            log.error("禁用API密钥失败", e);
            
            Map<String, Object> response = new HashMap<>();
            response.put("code", 500);
            response.put("message", "API密钥禁用失败: " + e.getMessage());
            
            return ResponseEntity.status(500).body(response);
        }
    }

    /**
     * 启用API密钥
     */
    @PostMapping("/enable")
    @Operation(summary = "启用API密钥", description = "启用指定的API密钥")
    public ResponseEntity<Map<String, Object>> enableApiKey(
            @Parameter(description = "API密钥") @RequestParam String apiKey) {
        
        try {
            apiKeyService.enableApiKey(apiKey);
            
            Map<String, Object> response = new HashMap<>();
            response.put("code", 200);
            response.put("message", "API密钥启用成功");
            
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            log.error("启用API密钥失败", e);
            
            Map<String, Object> response = new HashMap<>();
            response.put("code", 500);
            response.put("message", "API密钥启用失败: " + e.getMessage());
            
            return ResponseEntity.status(500).body(response);
        }
    }

    /**
     * 获取用户API密钥列表
     */
    @GetMapping("/list")
    @Operation(summary = "获取用户API密钥列表", description = "获取指定用户的所有API密钥")
    public ResponseEntity<Map<String, Object>> getUserApiKeys(
            @Parameter(description = "用户ID") @RequestParam String userId) {
        
        try {
            List<Map<String, Object>> apiKeys = apiKeyService.getUserApiKeys(userId);
            
            Map<String, Object> response = new HashMap<>();
            response.put("code", 200);
            response.put("message", "获取API密钥列表成功");
            response.put("data", apiKeys);
            
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            log.error("获取用户API密钥列表失败", e);
            
            Map<String, Object> response = new HashMap<>();
            response.put("code", 500);
            response.put("message", "获取API密钥列表失败: " + e.getMessage());
            
            return ResponseEntity.status(500).body(response);
        }
    }

    /**
     * 简化的API密钥验证接口（仅验证AppId）
     */
    @PostMapping("/validate-simple")
    @Operation(summary = "简化API密钥验证", description = "仅验证AppId的有效性，用于快速认证")
    public ResponseEntity<Map<String, Object>> validateSimple(
            @Parameter(description = "应用ID") @RequestParam String appId,
            @Parameter(description = "请求路径") @RequestParam(required = false) String requestPath,
            HttpServletRequest httpRequest) {
        
        try {
            ApiKeyValidateRequest request = new ApiKeyValidateRequest();
            request.setAppId(appId);
            request.setRequestPath(requestPath);
            request.setClientIp(getClientIp(httpRequest));
            
            ApiKeyValidateResponse response = apiKeyService.validateApiKey(request);
            
            Map<String, Object> result = new HashMap<>();
            if (response.getValid()) {
                // 只返回必要的信息
                Map<String, Object> data = new HashMap<>();
                data.put("valid", true);
                data.put("userId", response.getUserId());
                data.put("username", response.getUsername());
                data.put("remainingCalls", response.getRemainingCalls());
                
                result.put("code", 200);
                result.put("message", "验证成功");
                result.put("data", data);
                return ResponseEntity.ok(result);
            } else {
                result.put("code", 401);
                result.put("message", response.getErrorMessage());
                result.put("errorCode", response.getErrorCode());
                return ResponseEntity.status(401).body(result);
            }
        } catch (Exception e) {
            log.error("简化API密钥验证失败", e);
            
            Map<String, Object> result = new HashMap<>();
            result.put("code", 401);
            result.put("message", "验证失败: " + e.getMessage());
            
            return ResponseEntity.status(401).body(result);
        }
    }

    /**
     * 批量验证API密钥
     */
    @PostMapping("/validate-batch")
    @Operation(summary = "批量验证API密钥", description = "批量验证多个AppId的有效性")
    public ResponseEntity<Map<String, Object>> validateBatch(
            @Parameter(description = "AppId列表") @RequestParam List<String> appIds,
            HttpServletRequest httpRequest) {
        
        try {
            Map<String, Object> results = new HashMap<>();
            
            for (String appId : appIds) {
                ApiKeyValidateRequest request = new ApiKeyValidateRequest();
                request.setAppId(appId);
                request.setClientIp(getClientIp(httpRequest));
                
                ApiKeyValidateResponse response = apiKeyService.validateApiKey(request);
                
                Map<String, Object> result = new HashMap<>();
                result.put("valid", response.getValid());
                if (response.getValid()) {
                    result.put("userId", response.getUserId());
                    result.put("username", response.getUsername());
                } else {
                    result.put("errorCode", response.getErrorCode());
                    result.put("errorMessage", response.getErrorMessage());
                }
                
                results.put(appId, result);
            }
            
            Map<String, Object> response = new HashMap<>();
            response.put("code", 200);
            response.put("message", "批量验证完成");
            response.put("data", results);
            
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            log.error("批量验证API密钥失败", e);
            
            Map<String, Object> response = new HashMap<>();
            response.put("code", 500);
            response.put("message", "批量验证失败: " + e.getMessage());
            
            return ResponseEntity.status(500).body(response);
        }
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