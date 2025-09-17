package com.powertrading.user.dto;

import com.fasterxml.jackson.annotation.JsonFormat;
import io.swagger.v3.oas.annotations.media.Schema;

import java.time.LocalDateTime;

/**
 * 用户注册响应DTO
 *
 * @author PowerTrading Team
 * @since 2024-01-15
 */
@Schema(description = "用户注册响应")
public class UserRegisterResponse {

    @Schema(description = "用户ID")
    private String userId;

    @Schema(description = "注册状态")
    private String status;

    @Schema(description = "创建时间")
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime createTime;

    @Schema(description = "响应消息")
    private String message;

    // Constructors
    public UserRegisterResponse() {}

    public UserRegisterResponse(String userId, String status, LocalDateTime createTime, String message) {
        this.userId = userId;
        this.status = status;
        this.createTime = createTime;
        this.message = message;
    }

    // Getters and Setters
    public String getUserId() {
        return userId;
    }

    public void setUserId(String userId) {
        this.userId = userId;
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

    public String getMessage() {
        return message;
    }

    public void setMessage(String message) {
        this.message = message;
    }

    @Override
    public String toString() {
        return "UserRegisterResponse{" +
                "userId='" + userId + '\'' +
                ", status='" + status + '\'' +
                ", createTime=" + createTime +
                ", message='" + message + '\'' +
                '}';
    }
}