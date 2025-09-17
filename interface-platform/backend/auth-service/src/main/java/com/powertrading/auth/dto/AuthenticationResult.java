package com.powertrading.auth.dto;

import com.powertrading.auth.service.MultiAuthService;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.time.LocalDateTime;
import java.util.List;

/**
 * 统一认证结果
 *
 * @author PowerTrading Team
 * @since 2024-01-15
 */
@Data
@Schema(description = "统一认证结果")
public class AuthenticationResult {

    @Schema(description = "认证是否成功", example = "true")
    private boolean success;

    @Schema(description = "认证类型")
    private MultiAuthService.AuthType authType;

    @Schema(description = "用户ID", example = "1001")
    private String userId;

    @Schema(description = "用户名", example = "admin")
    private String username;

    @Schema(description = "公司名称", example = "电力交易中心")
    private String companyName;

    @Schema(description = "用户角色列表")
    private List<String> roles;

    @Schema(description = "用户权限列表")
    private List<String> permissions;

    @Schema(description = "应用ID", example = "app_12345678")
    private String appId;

    @Schema(description = "过期时间")
    private LocalDateTime expireTime;

    @Schema(description = "剩余有效时间(秒)", example = "3600")
    private Long remainingTime;

    @Schema(description = "错误代码")
    private String errorCode;

    @Schema(description = "错误信息")
    private String errorMessage;

    @Schema(description = "认证时间")
    private LocalDateTime authTime;

    @Schema(description = "扩展信息")
    private Object extra;

    public static AuthenticationResult success(MultiAuthService.AuthType authType,
                                             String userId, String username, String companyName,
                                             List<String> roles, List<String> permissions,
                                             String appId, LocalDateTime expireTime, Long remainingTime) {
        AuthenticationResult result = new AuthenticationResult();
        result.setSuccess(true);
        result.setAuthType(authType);
        result.setUserId(userId);
        result.setUsername(username);
        result.setCompanyName(companyName);
        result.setRoles(roles);
        result.setPermissions(permissions);
        result.setAppId(appId);
        result.setExpireTime(expireTime);
        result.setRemainingTime(remainingTime);
        result.setAuthTime(LocalDateTime.now());
        return result;
    }

    public static AuthenticationResult failure(String errorCode, String errorMessage) {
        AuthenticationResult result = new AuthenticationResult();
        result.setSuccess(false);
        result.setErrorCode(errorCode);
        result.setErrorMessage(errorMessage);
        result.setAuthTime(LocalDateTime.now());
        return result;
    }

    /**
     * 检查是否有指定权限
     */
    public boolean hasPermission(String permission) {
        return permissions != null && permissions.contains(permission);
    }

    /**
     * 检查是否有指定角色
     */
    public boolean hasRole(String role) {
        return roles != null && roles.contains(role);
    }

    /**
     * 检查是否有任一权限
     */
    public boolean hasAnyPermission(String... permissions) {
        if (this.permissions == null || permissions == null) {
            return false;
        }
        
        for (String permission : permissions) {
            if (this.permissions.contains(permission)) {
                return true;
            }
        }
        return false;
    }

    /**
     * 检查是否有任一角色
     */
    public boolean hasAnyRole(String... roles) {
        if (this.roles == null || roles == null) {
            return false;
        }
        
        for (String role : roles) {
            if (this.roles.contains(role)) {
                return true;
            }
        }
        return false;
    }

    /**
     * 检查认证是否即将过期
     */
    public boolean isExpiringSoon(long thresholdSeconds) {
        return remainingTime != null && remainingTime < thresholdSeconds;
    }

    /**
     * 获取认证类型描述
     */
    public String getAuthTypeDescription() {
        if (authType == null) {
            return "未知";
        }
        
        switch (authType) {
            case JWT_TOKEN:
                return "JWT Token认证";
            case API_KEY:
                return "API密钥认证";
            case MIXED:
                return "混合认证";
            default:
                return "未知认证类型";
        }
    }
}