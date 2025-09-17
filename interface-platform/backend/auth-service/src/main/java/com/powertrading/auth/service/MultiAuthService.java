package com.powertrading.auth.service;

import com.powertrading.auth.dto.*;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * 多重认证管理服务
 * 支持用户登录认证和API调用认证的统一管理
 *
 * @author PowerTrading Team
 * @since 2024-01-15
 */
@Slf4j
@Service
public class MultiAuthService {

    @Autowired
    private AuthService authService;

    @Autowired
    private ApiKeyService apiKeyService;

    @Autowired
    private AuditLogService auditLogService;

    /**
     * 认证类型枚举
     */
    public enum AuthType {
        JWT_TOKEN,      // JWT Token认证
        API_KEY,        // API密钥认证
        MIXED          // 混合认证
    }

    /**
     * 统一认证接口
     */
    public AuthenticationResult authenticate(AuthenticationRequest request) {
        try {
            AuthType authType = determineAuthType(request);
            
            switch (authType) {
                case JWT_TOKEN:
                    return authenticateWithJwtToken(request);
                case API_KEY:
                    return authenticateWithApiKey(request);
                case MIXED:
                    return authenticateWithMixed(request);
                default:
                    return AuthenticationResult.failure("UNSUPPORTED_AUTH_TYPE", "不支持的认证类型");
            }
        } catch (Exception e) {
            log.error("统一认证失败", e);
            return AuthenticationResult.failure("AUTHENTICATION_ERROR", "认证过程中发生错误: " + e.getMessage());
        }
    }

    /**
     * JWT Token认证
     */
    private AuthenticationResult authenticateWithJwtToken(AuthenticationRequest request) {
        try {
            TokenValidateRequest tokenRequest = new TokenValidateRequest();
            tokenRequest.setToken(request.getJwtToken());
            tokenRequest.setCheckPermissions(request.getCheckPermissions());
            tokenRequest.setRequiredPermission(request.getRequiredPermission());
            tokenRequest.setClientIp(request.getClientIp());

            TokenValidateResponse tokenResponse = authService.validateToken(tokenRequest);

            if (tokenResponse.getValid()) {
                // 记录认证成功日志
                auditLogService.logAuthSuccess(
                        tokenResponse.getUserId(),
                        tokenResponse.getUsername(),
                        "JWT_TOKEN",
                        request.getClientIp(),
                        request.getUserAgent()
                );

                return AuthenticationResult.success(
                        AuthType.JWT_TOKEN,
                        tokenResponse.getUserId(),
                        tokenResponse.getUsername(),
                        tokenResponse.getCompanyName(),
                        tokenResponse.getRoles(),
                        tokenResponse.getPermissions(),
                        null, // API密钥认证才有appId
                        tokenResponse.getExpireTime(),
                        tokenResponse.getRemainingTime()
                );
            } else {
                // 记录认证失败日志
                auditLogService.logAuthFailure(
                        "unknown",
                        "JWT_TOKEN",
                        tokenResponse.getErrorMessage(),
                        request.getClientIp(),
                        request.getUserAgent()
                );

                return AuthenticationResult.failure("JWT_TOKEN_INVALID", tokenResponse.getErrorMessage());
            }
        } catch (Exception e) {
            log.error("JWT Token认证失败", e);
            return AuthenticationResult.failure("JWT_TOKEN_ERROR", "JWT Token认证失败: " + e.getMessage());
        }
    }

    /**
     * API密钥认证
     */
    private AuthenticationResult authenticateWithApiKey(AuthenticationRequest request) {
        try {
            ApiKeyValidateRequest apiKeyRequest = new ApiKeyValidateRequest();
            apiKeyRequest.setAppId(request.getAppId());
            apiKeyRequest.setApiKey(request.getApiKey());
            apiKeyRequest.setSignature(request.getSignature());
            apiKeyRequest.setTimestamp(request.getTimestamp());
            apiKeyRequest.setNonce(request.getNonce());
            apiKeyRequest.setRequestPath(request.getRequestPath());
            apiKeyRequest.setRequestMethod(request.getRequestMethod());
            apiKeyRequest.setClientIp(request.getClientIp());

            ApiKeyValidateResponse apiKeyResponse = apiKeyService.validateApiKey(apiKeyRequest);

            if (apiKeyResponse.getValid()) {
                // 记录认证成功日志
                auditLogService.logAuthSuccess(
                        apiKeyResponse.getUserId(),
                        apiKeyResponse.getUsername(),
                        "API_KEY",
                        request.getClientIp(),
                        request.getUserAgent()
                );

                return AuthenticationResult.success(
                        AuthType.API_KEY,
                        apiKeyResponse.getUserId(),
                        apiKeyResponse.getUsername(),
                        apiKeyResponse.getCompanyName(),
                        null, // API密钥认证没有角色信息
                        apiKeyResponse.getPermissions(),
                        apiKeyResponse.getAppId(),
                        apiKeyResponse.getExpireTime(),
                        apiKeyResponse.getRemainingCalls()
                );
            } else {
                // 记录认证失败日志
                auditLogService.logAuthFailure(
                        "unknown",
                        "API_KEY",
                        apiKeyResponse.getErrorMessage(),
                        request.getClientIp(),
                        request.getUserAgent()
                );

                return AuthenticationResult.failure(apiKeyResponse.getErrorCode(), apiKeyResponse.getErrorMessage());
            }
        } catch (Exception e) {
            log.error("API密钥认证失败", e);
            return AuthenticationResult.failure("API_KEY_ERROR", "API密钥认证失败: " + e.getMessage());
        }
    }

    /**
     * 混合认证（同时验证JWT Token和API密钥）
     */
    private AuthenticationResult authenticateWithMixed(AuthenticationRequest request) {
        try {
            // 先尝试JWT Token认证
            AuthenticationResult jwtResult = authenticateWithJwtToken(request);
            
            if (jwtResult.isSuccess()) {
                // JWT认证成功，再验证API密钥（如果提供）
                if (StringUtils.hasText(request.getAppId())) {
                    AuthenticationResult apiKeyResult = authenticateWithApiKey(request);
                    
                    if (apiKeyResult.isSuccess()) {
                        // 两种认证都成功，合并结果
                        return mergeAuthResults(jwtResult, apiKeyResult);
                    } else {
                        // API密钥认证失败
                        return AuthenticationResult.failure("MIXED_AUTH_API_KEY_FAILED", 
                                "混合认证中API密钥验证失败: " + apiKeyResult.getErrorMessage());
                    }
                } else {
                    // 只有JWT认证成功
                    return jwtResult;
                }
            } else {
                // JWT认证失败，尝试API密钥认证
                if (StringUtils.hasText(request.getAppId())) {
                    return authenticateWithApiKey(request);
                } else {
                    return AuthenticationResult.failure("MIXED_AUTH_FAILED", 
                            "混合认证失败: JWT Token和API密钥都无效");
                }
            }
        } catch (Exception e) {
            log.error("混合认证失败", e);
            return AuthenticationResult.failure("MIXED_AUTH_ERROR", "混合认证失败: " + e.getMessage());
        }
    }

    /**
     * 合并认证结果
     */
    private AuthenticationResult mergeAuthResults(AuthenticationResult jwtResult, AuthenticationResult apiKeyResult) {
        // 以JWT认证结果为主，补充API密钥信息
        AuthenticationResult mergedResult = new AuthenticationResult();
        mergedResult.setSuccess(true);
        mergedResult.setAuthType(AuthType.MIXED);
        mergedResult.setUserId(jwtResult.getUserId());
        mergedResult.setUsername(jwtResult.getUsername());
        mergedResult.setCompanyName(jwtResult.getCompanyName());
        mergedResult.setRoles(jwtResult.getRoles());
        
        // 合并权限（JWT权限 + API密钥权限）
        Set<String> mergedPermissions = new HashSet<>();
        if (jwtResult.getPermissions() != null) {
            mergedPermissions.addAll(jwtResult.getPermissions());
        }
        if (apiKeyResult.getPermissions() != null) {
            mergedPermissions.addAll(apiKeyResult.getPermissions());
        }
        mergedResult.setPermissions(new ArrayList<>(mergedPermissions));
        
        mergedResult.setAppId(apiKeyResult.getAppId());
        mergedResult.setExpireTime(jwtResult.getExpireTime()); // 使用JWT的过期时间
        mergedResult.setRemainingTime(Math.min(jwtResult.getRemainingTime(), apiKeyResult.getRemainingTime()));
        
        return mergedResult;
    }

    /**
     * 确定认证类型
     */
    private AuthType determineAuthType(AuthenticationRequest request) {
        boolean hasJwtToken = StringUtils.hasText(request.getJwtToken());
        boolean hasApiKey = StringUtils.hasText(request.getAppId());
        
        if (hasJwtToken && hasApiKey) {
            return AuthType.MIXED;
        } else if (hasJwtToken) {
            return AuthType.JWT_TOKEN;
        } else if (hasApiKey) {
            return AuthType.API_KEY;
        } else {
            throw new IllegalArgumentException("缺少认证信息：需要提供JWT Token或API密钥");
        }
    }

    /**
     * 检查用户权限
     */
    public boolean hasPermission(String userId, String permission) {
        try {
            // 这里可以实现权限缓存查询
            // 暂时返回true，具体实现可以查询用户权限缓存
            return true;
        } catch (Exception e) {
            log.error("检查用户权限失败: userId={}, permission={}", userId, permission, e);
            return false;
        }
    }

    /**
     * 检查接口访问权限
     */
    public boolean hasInterfaceAccess(String userId, String appId, String interfacePath) {
        try {
            // 检查用户是否有访问该接口的权限
            // 可以结合JWT Token权限和API密钥权限进行判断
            return true;
        } catch (Exception e) {
            log.error("检查接口访问权限失败: userId={}, appId={}, interfacePath={}", 
                    userId, appId, interfacePath, e);
            return false;
        }
    }

    /**
     * 获取认证统计信息
     */
    public Map<String, Object> getAuthStatistics() {
        try {
            Map<String, Object> statistics = new HashMap<>();
            
            // 这里可以实现认证统计功能
            statistics.put("totalAuthRequests", 0);
            statistics.put("successfulAuths", 0);
            statistics.put("failedAuths", 0);
            statistics.put("jwtTokenAuths", 0);
            statistics.put("apiKeyAuths", 0);
            statistics.put("mixedAuths", 0);
            
            return statistics;
        } catch (Exception e) {
            log.error("获取认证统计信息失败", e);
            return new HashMap<>();
        }
    }
}