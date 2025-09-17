package com.powertrading.auth.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.time.LocalDateTime;
import java.util.List;

/**
 * Token验证响应
 *
 * @author PowerTrading Team
 * @since 2024-01-15
 */
@Data
@Schema(description = "Token验证响应")
public class TokenValidateResponse {

    @Schema(description = "验证是否成功", example = "true")
    private Boolean valid;

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

    @Schema(description = "Token过期时间")
    private LocalDateTime expireTime;

    @Schema(description = "剩余有效时间(秒)", example = "3600")
    private Long remainingTime;

    @Schema(description = "是否需要刷新", example = "false")
    private Boolean needRefresh;

    @Schema(description = "错误信息")
    private String errorMessage;

    public static TokenValidateResponse success(String userId, String username, String companyName,
                                              List<String> roles, List<String> permissions,
                                              LocalDateTime expireTime, Long remainingTime) {
        TokenValidateResponse response = new TokenValidateResponse();
        response.setValid(true);
        response.setUserId(userId);
        response.setUsername(username);
        response.setCompanyName(companyName);
        response.setRoles(roles);
        response.setPermissions(permissions);
        response.setExpireTime(expireTime);
        response.setRemainingTime(remainingTime);
        response.setNeedRefresh(remainingTime < 3600); // 小于1小时建议刷新
        return response;
    }

    public static TokenValidateResponse failure(String errorMessage) {
        TokenValidateResponse response = new TokenValidateResponse();
        response.setValid(false);
        response.setErrorMessage(errorMessage);
        return response;
    }
}