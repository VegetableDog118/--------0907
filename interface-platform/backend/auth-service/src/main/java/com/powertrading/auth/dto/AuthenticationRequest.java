package com.powertrading.auth.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

/**
 * 统一认证请求
 *
 * @author PowerTrading Team
 * @since 2024-01-15
 */
@Data
@Schema(description = "统一认证请求")
public class AuthenticationRequest {

    // JWT Token认证相关
    @Schema(description = "JWT Token", example = "eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9...")
    private String jwtToken;

    // API密钥认证相关
    @Schema(description = "应用ID", example = "app_12345678")
    private String appId;

    @Schema(description = "API密钥", example = "ak_abcdef1234567890")
    private String apiKey;

    @Schema(description = "签名", example = "signature_string")
    private String signature;

    @Schema(description = "时间戳", example = "1640995200")
    private Long timestamp;

    @Schema(description = "随机数", example = "nonce123")
    private String nonce;

    // 请求信息
    @Schema(description = "请求路径", example = "/api/v1/interfaces/data")
    private String requestPath;

    @Schema(description = "请求方法", example = "GET")
    private String requestMethod;

    @Schema(description = "客户端IP", example = "192.168.1.100")
    private String clientIp;

    @Schema(description = "用户代理", example = "Mozilla/5.0...")
    private String userAgent;

    // 权限检查相关
    @Schema(description = "是否检查权限", example = "true")
    private Boolean checkPermissions = false;

    @Schema(description = "需要验证的权限", example = "interface:read")
    private String requiredPermission;

    // 认证模式
    @Schema(description = "认证模式", example = "auto", allowableValues = {"jwt", "apikey", "mixed", "auto"})
    private String authMode = "auto"; // auto: 自动判断, jwt: 仅JWT, apikey: 仅API密钥, mixed: 混合认证

    @Schema(description = "是否严格模式", example = "false")
    private Boolean strictMode = false; // 严格模式下，混合认证要求两种认证都成功
}