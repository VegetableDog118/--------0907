package com.powertrading.auth.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import javax.validation.constraints.NotBlank;

/**
 * API密钥验证请求
 *
 * @author PowerTrading Team
 * @since 2024-01-15
 */
@Data
@Schema(description = "API密钥验证请求")
public class ApiKeyValidateRequest {

    @NotBlank(message = "AppId不能为空")
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

    @Schema(description = "请求接口路径", example = "/api/v1/interfaces/data")
    private String requestPath;

    @Schema(description = "请求方法", example = "GET")
    private String requestMethod;

    @Schema(description = "客户端IP", example = "192.168.1.100")
    private String clientIp;
}