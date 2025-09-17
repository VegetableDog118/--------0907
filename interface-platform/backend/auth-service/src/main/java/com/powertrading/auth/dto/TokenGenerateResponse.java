package com.powertrading.auth.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * Token生成响应
 *
 * @author PowerTrading Team
 * @since 2024-01-15
 */
@Data
@Schema(description = "Token生成响应")
public class TokenGenerateResponse {

    @Schema(description = "访问Token", example = "eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9...")
    private String accessToken;

    @Schema(description = "刷新Token", example = "refresh_token_string")
    private String refreshToken;

    @Schema(description = "Token类型", example = "Bearer")
    private String tokenType;

    @Schema(description = "过期时间(秒)", example = "86400")
    private Long expiresIn;

    @Schema(description = "Token过期时间")
    private LocalDateTime expireTime;

    @Schema(description = "刷新Token过期时间")
    private LocalDateTime refreshExpireTime;

    @Schema(description = "Token范围", example = "read write")
    private String scope;

    public static TokenGenerateResponse success(String accessToken, String refreshToken, 
                                              String tokenType, Long expiresIn, 
                                              LocalDateTime expireTime, LocalDateTime refreshExpireTime) {
        TokenGenerateResponse response = new TokenGenerateResponse();
        response.setAccessToken(accessToken);
        response.setRefreshToken(refreshToken);
        response.setTokenType(tokenType);
        response.setExpiresIn(expiresIn);
        response.setExpireTime(expireTime);
        response.setRefreshExpireTime(refreshExpireTime);
        response.setScope("read write");
        return response;
    }
}