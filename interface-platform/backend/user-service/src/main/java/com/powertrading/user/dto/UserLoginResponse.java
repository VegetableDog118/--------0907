package com.powertrading.user.dto;

import io.swagger.v3.oas.annotations.media.Schema;

import java.util.List;

/**
 * 用户登录响应DTO
 *
 * @author PowerTrading Team
 * @since 2024-01-15
 */
@Schema(description = "用户登录响应")
public class UserLoginResponse {

    @Schema(description = "JWT Token")
    private String token;

    @Schema(description = "Token类型")
    private String tokenType = "Bearer";

    @Schema(description = "Token过期时间（秒）")
    private Long expiresIn;

    @Schema(description = "用户基本信息")
    private UserInfoResponse userInfo;

    @Schema(description = "权限列表")
    private List<String> permissions;

    // Constructors
    public UserLoginResponse() {}

    public UserLoginResponse(String token, Long expiresIn, UserInfoResponse userInfo, List<String> permissions) {
        this.token = token;
        this.expiresIn = expiresIn;
        this.userInfo = userInfo;
        this.permissions = permissions;
    }

    // Getters and Setters
    public String getToken() {
        return token;
    }

    public void setToken(String token) {
        this.token = token;
    }

    public String getTokenType() {
        return tokenType;
    }

    public void setTokenType(String tokenType) {
        this.tokenType = tokenType;
    }

    public Long getExpiresIn() {
        return expiresIn;
    }

    public void setExpiresIn(Long expiresIn) {
        this.expiresIn = expiresIn;
    }

    public UserInfoResponse getUserInfo() {
        return userInfo;
    }

    public void setUserInfo(UserInfoResponse userInfo) {
        this.userInfo = userInfo;
    }

    public List<String> getPermissions() {
        return permissions;
    }

    public void setPermissions(List<String> permissions) {
        this.permissions = permissions;
    }

    @Override
    public String toString() {
        return "UserLoginResponse{" +
                "tokenType='" + tokenType + '\'' +
                ", expiresIn=" + expiresIn +
                ", userInfo=" + userInfo +
                ", permissions=" + permissions +
                '}';
    }
}