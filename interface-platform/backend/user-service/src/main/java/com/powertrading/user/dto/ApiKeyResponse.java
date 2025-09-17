package com.powertrading.user.dto;

import com.fasterxml.jackson.annotation.JsonFormat;
import io.swagger.v3.oas.annotations.media.Schema;

import java.time.LocalDateTime;
import java.util.List;

/**
 * API密钥响应DTO
 *
 * @author PowerTrading Team
 * @since 2024-01-15
 */
@Schema(description = "API密钥响应")
public class ApiKeyResponse {

    @Schema(description = "AppId")
    private String appId;

    @Schema(description = "AppSecret")
    private String appSecret;

    @Schema(description = "密钥状态")
    private String status;

    @Schema(description = "创建时间")
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime createTime;

    @Schema(description = "最后使用时间")
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime lastUsedTime;

    @Schema(description = "权限范围")
    private List<String> permissions;

    // Constructors
    public ApiKeyResponse() {}

    public ApiKeyResponse(String appId, String appSecret, String status) {
        this.appId = appId;
        this.appSecret = appSecret;
        this.status = status;
        this.createTime = LocalDateTime.now();
    }

    // Getters and Setters
    public String getAppId() {
        return appId;
    }

    public void setAppId(String appId) {
        this.appId = appId;
    }

    public String getAppSecret() {
        return appSecret;
    }

    public void setAppSecret(String appSecret) {
        this.appSecret = appSecret;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    public LocalDateTime getCreateTime() {
        return createTime;
    }

    public void setCreateTime(LocalDateTime createTime) {
        this.createTime = createTime;
    }

    public LocalDateTime getLastUsedTime() {
        return lastUsedTime;
    }

    public void setLastUsedTime(LocalDateTime lastUsedTime) {
        this.lastUsedTime = lastUsedTime;
    }

    public List<String> getPermissions() {
        return permissions;
    }

    public void setPermissions(List<String> permissions) {
        this.permissions = permissions;
    }

    @Override
    public String toString() {
        return "ApiKeyResponse{" +
                "appId='" + appId + '\'' +
                ", appSecret='[PROTECTED]'" +
                ", status='" + status + '\'' +
                ", createTime=" + createTime +
                ", lastUsedTime=" + lastUsedTime +
                ", permissions=" + permissions +
                '}';
    }
}