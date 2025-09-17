package com.powertrading.auth.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.time.LocalDateTime;
import java.util.List;

/**
 * API密钥验证响应
 *
 * @author PowerTrading Team
 * @since 2024-01-15
 */
@Data
@Schema(description = "API密钥验证响应")
public class ApiKeyValidateResponse {

    @Schema(description = "验证是否成功", example = "true")
    private Boolean valid;

    @Schema(description = "用户ID", example = "1001")
    private String userId;

    @Schema(description = "应用ID", example = "app_12345678")
    private String appId;

    @Schema(description = "用户名", example = "admin")
    private String username;

    @Schema(description = "公司名称", example = "电力交易中心")
    private String companyName;

    @Schema(description = "API密钥权限列表")
    private List<String> permissions;

    @Schema(description = "可访问的接口列表")
    private List<String> allowedInterfaces;

    @Schema(description = "密钥过期时间")
    private LocalDateTime expireTime;

    @Schema(description = "剩余调用次数", example = "1000")
    private Long remainingCalls;

    @Schema(description = "每日调用限制", example = "10000")
    private Long dailyLimit;

    @Schema(description = "错误信息")
    private String errorMessage;

    @Schema(description = "错误代码")
    private String errorCode;

    public static ApiKeyValidateResponse success(String userId, String appId, String username, 
                                               String companyName, List<String> permissions,
                                               List<String> allowedInterfaces, LocalDateTime expireTime,
                                               Long remainingCalls, Long dailyLimit) {
        ApiKeyValidateResponse response = new ApiKeyValidateResponse();
        response.setValid(true);
        response.setUserId(userId);
        response.setAppId(appId);
        response.setUsername(username);
        response.setCompanyName(companyName);
        response.setPermissions(permissions);
        response.setAllowedInterfaces(allowedInterfaces);
        response.setExpireTime(expireTime);
        response.setRemainingCalls(remainingCalls);
        response.setDailyLimit(dailyLimit);
        return response;
    }

    public static ApiKeyValidateResponse failure(String errorCode, String errorMessage) {
        ApiKeyValidateResponse response = new ApiKeyValidateResponse();
        response.setValid(false);
        response.setErrorCode(errorCode);
        response.setErrorMessage(errorMessage);
        return response;
    }
}