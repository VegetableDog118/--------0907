package com.powertrading.auth.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import javax.validation.constraints.NotBlank;

/**
 * Token验证请求
 *
 * @author PowerTrading Team
 * @since 2024-01-15
 */
@Data
@Schema(description = "Token验证请求")
public class TokenValidateRequest {

    @NotBlank(message = "Token不能为空")
    @Schema(description = "JWT Token", example = "eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9...")
    private String token;

    @Schema(description = "是否检查权限", example = "true")
    private Boolean checkPermissions = true;

    @Schema(description = "需要验证的权限", example = "interface:read")
    private String requiredPermission;

    @Schema(description = "客户端IP", example = "192.168.1.100")
    private String clientIp;
}