package com.powertrading.auth.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import javax.validation.constraints.NotBlank;
import javax.validation.constraints.NotNull;
import java.util.List;

/**
 * Token生成请求
 *
 * @author PowerTrading Team
 * @since 2024-01-15
 */
@Data
@Schema(description = "Token生成请求")
public class TokenGenerateRequest {

    @NotBlank(message = "用户ID不能为空")
    @Schema(description = "用户ID", example = "1001")
    private String userId;

    @NotBlank(message = "用户名不能为空")
    @Schema(description = "用户名", example = "admin")
    private String username;

    @Schema(description = "公司名称", example = "电力交易中心")
    private String companyName;

    @NotNull(message = "角色列表不能为空")
    @Schema(description = "用户角色列表")
    private List<String> roles;

    @Schema(description = "用户权限列表")
    private List<String> permissions;

    @Schema(description = "Token类型", example = "Bearer")
    private String tokenType = "Bearer";

    @Schema(description = "客户端IP", example = "192.168.1.100")
    private String clientIp;

    @Schema(description = "用户代理", example = "Mozilla/5.0...")
    private String userAgent;
}