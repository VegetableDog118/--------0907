package com.powertrading.auth.service;

import com.powertrading.auth.dto.*;
import com.powertrading.auth.util.JwtTokenUtil;
import io.jsonwebtoken.Claims;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.*;
import java.util.concurrent.TimeUnit;

/**
 * 认证服务
 *
 * @author PowerTrading Team
 * @since 2024-01-15
 */
@Slf4j
@Service
public class AuthService {

    @Autowired
    private JwtTokenUtil jwtTokenUtil;

    @Autowired
    private TokenBlacklistService tokenBlacklistService;

    @Autowired
    private RedisTemplate<String, Object> redisTemplate;

    private static final String USER_TOKEN_KEY_PREFIX = "auth:user:token:";
    private static final String TOKEN_INFO_KEY_PREFIX = "auth:token:info:";

    /**
     * 生成Token
     */
    public TokenGenerateResponse generateToken(TokenGenerateRequest request) {
        try {
            // 生成访问Token
            String accessToken = jwtTokenUtil.generateAccessToken(
                    request.getUserId(),
                    request.getUsername(),
                    request.getCompanyName(),
                    request.getRoles(),
                    request.getPermissions()
            );

            // 生成刷新Token
            String refreshToken = jwtTokenUtil.generateRefreshToken(request.getUserId());

            // 计算过期时间
            LocalDateTime accessExpireTime = LocalDateTime.now().plusSeconds(86400); // 24小时
            LocalDateTime refreshExpireTime = LocalDateTime.now().plusSeconds(604800); // 7天

            // 缓存Token信息
            cacheTokenInfo(accessToken, request);
            cacheUserToken(request.getUserId(), accessToken);

            log.info("Token生成成功: userId={}, username={}", request.getUserId(), request.getUsername());

            return TokenGenerateResponse.success(
                    accessToken,
                    refreshToken,
                    request.getTokenType(),
                    86400L,
                    accessExpireTime,
                    refreshExpireTime
            );
        } catch (Exception e) {
            log.error("Token生成失败: userId={}", request.getUserId(), e);
            throw new RuntimeException("Token生成失败: " + e.getMessage());
        }
    }

    /**
     * 验证Token
     */
    public TokenValidateResponse validateToken(TokenValidateRequest request) {
        try {
            String token = request.getToken();
            
            // 检查Token格式
            if (!StringUtils.hasText(token)) {
                return TokenValidateResponse.failure("Token不能为空");
            }

            // 验证Token并获取Claims
            Claims claims = jwtTokenUtil.validateToken(token);
            String jti = claims.getId();

            // 检查Token是否在黑名单中
            if (tokenBlacklistService.isBlacklisted(jti)) {
                return TokenValidateResponse.failure("Token已被撤销");
            }

            // 提取Token信息
            String userId = claims.getSubject();
            String username = claims.get("username", String.class);
            String companyName = claims.get("companyName", String.class);
            @SuppressWarnings("unchecked")
            List<String> roles = (List<String>) claims.get("roles");
            @SuppressWarnings("unchecked")
            List<String> permissions = (List<String>) claims.get("permissions");

            // 计算剩余时间
            LocalDateTime expireTime = claims.getExpiration().toInstant()
                    .atZone(ZoneId.systemDefault())
                    .toLocalDateTime();
            long remainingTime = (claims.getExpiration().getTime() - System.currentTimeMillis()) / 1000;

            // 检查权限（如果需要）
            if (request.getCheckPermissions() && StringUtils.hasText(request.getRequiredPermission())) {
                if (permissions == null || !permissions.contains(request.getRequiredPermission())) {
                    return TokenValidateResponse.failure("权限不足");
                }
            }

            log.debug("Token验证成功: userId={}, remainingTime={}s", userId, remainingTime);

            return TokenValidateResponse.success(
                    userId, username, companyName, roles, permissions, expireTime, remainingTime
            );
        } catch (Exception e) {
            log.warn("Token验证失败: {}", e.getMessage());
            return TokenValidateResponse.failure(e.getMessage());
        }
    }

    /**
     * 刷新Token
     */
    public TokenGenerateResponse refreshToken(String refreshToken, String userId, String username,
                                            String companyName, List<String> roles, List<String> permissions) {
        try {
            // 验证刷新Token
            if (!jwtTokenUtil.isRefreshToken(refreshToken)) {
                throw new RuntimeException("无效的刷新Token");
            }

            // 检查刷新Token是否过期
            if (jwtTokenUtil.isTokenExpired(refreshToken)) {
                throw new RuntimeException("刷新Token已过期");
            }

            // 检查刷新Token是否在黑名单中
            String refreshJti = jwtTokenUtil.getJtiFromToken(refreshToken);
            if (tokenBlacklistService.isBlacklisted(refreshJti)) {
                throw new RuntimeException("刷新Token已被撤销");
            }

            // 生成新的访问Token
            String newAccessToken = jwtTokenUtil.refreshAccessToken(
                    refreshToken, username, companyName, roles, permissions
            );

            // 生成新的刷新Token
            String newRefreshToken = jwtTokenUtil.generateRefreshToken(userId);

            // 将旧的刷新Token加入黑名单
            long remainingTime = jwtTokenUtil.getRemainingTimeFromToken(refreshToken);
            tokenBlacklistService.addToBlacklist(refreshJti, remainingTime);

            // 缓存新Token信息
            TokenGenerateRequest cacheRequest = new TokenGenerateRequest();
            cacheRequest.setUserId(userId);
            cacheRequest.setUsername(username);
            cacheRequest.setCompanyName(companyName);
            cacheRequest.setRoles(roles);
            cacheRequest.setPermissions(permissions);
            cacheTokenInfo(newAccessToken, cacheRequest);
            cacheUserToken(userId, newAccessToken);

            // 计算过期时间
            LocalDateTime accessExpireTime = LocalDateTime.now().plusSeconds(86400);
            LocalDateTime refreshExpireTime = LocalDateTime.now().plusSeconds(604800);

            log.info("Token刷新成功: userId={}", userId);

            return TokenGenerateResponse.success(
                    newAccessToken,
                    newRefreshToken,
                    "Bearer",
                    86400L,
                    accessExpireTime,
                    refreshExpireTime
            );
        } catch (Exception e) {
            log.error("Token刷新失败: userId={}", userId, e);
            throw new RuntimeException("Token刷新失败: " + e.getMessage());
        }
    }

    /**
     * 撤销Token
     */
    public void revokeToken(String token) {
        try {
            Claims claims = jwtTokenUtil.validateToken(token);
            String jti = claims.getId();
            String userId = claims.getSubject();

            // 计算剩余有效时间
            long remainingTime = jwtTokenUtil.getRemainingTimeFromToken(token);
            
            // 加入黑名单
            tokenBlacklistService.addToBlacklist(jti, remainingTime);
            
            // 清除缓存
            clearTokenCache(userId, token);

            log.info("Token已撤销: userId={}, jti={}", userId, jti);
        } catch (Exception e) {
            log.error("撤销Token失败: {}", e.getMessage());
            throw new RuntimeException("撤销Token失败: " + e.getMessage());
        }
    }

    /**
     * 撤销用户所有Token
     */
    public void revokeUserAllTokens(String userId) {
        try {
            // 将用户所有Token加入黑名单
            tokenBlacklistService.blacklistUserTokens(userId);
            
            // 清除用户Token缓存
            clearUserTokenCache(userId);

            log.info("用户所有Token已撤销: userId={}", userId);
        } catch (Exception e) {
            log.error("撤销用户所有Token失败: userId={}", userId, e);
            throw new RuntimeException("撤销用户所有Token失败: " + e.getMessage());
        }
    }

    /**
     * 缓存Token信息
     */
    private void cacheTokenInfo(String token, TokenGenerateRequest request) {
        try {
            String jti = jwtTokenUtil.getJtiFromToken(token);
            String key = TOKEN_INFO_KEY_PREFIX + jti;
            
            redisTemplate.opsForHash().putAll(key, Map.of(
                    "userId", request.getUserId(),
                    "username", request.getUsername(),
                    "companyName", request.getCompanyName() != null ? request.getCompanyName() : "",
                    "clientIp", request.getClientIp() != null ? request.getClientIp() : "",
                    "userAgent", request.getUserAgent() != null ? request.getUserAgent() : "",
                    "createTime", System.currentTimeMillis()
            ));
            
            // 设置过期时间
            redisTemplate.expire(key, 86400, TimeUnit.SECONDS);
        } catch (Exception e) {
            log.warn("缓存Token信息失败", e);
        }
    }

    /**
     * 缓存用户Token映射
     */
    private void cacheUserToken(String userId, String token) {
        try {
            String jti = jwtTokenUtil.getJtiFromToken(token);
            String key = USER_TOKEN_KEY_PREFIX + userId;
            
            redisTemplate.opsForSet().add(key, jti);
            redisTemplate.expire(key, 86400, TimeUnit.SECONDS);
        } catch (Exception e) {
            log.warn("缓存用户Token映射失败", e);
        }
    }

    /**
     * 清除Token缓存
     */
    private void clearTokenCache(String userId, String token) {
        try {
            String jti = jwtTokenUtil.getJtiFromToken(token);
            
            // 清除Token信息缓存
            redisTemplate.delete(TOKEN_INFO_KEY_PREFIX + jti);
            
            // 从用户Token集合中移除
            redisTemplate.opsForSet().remove(USER_TOKEN_KEY_PREFIX + userId, jti);
        } catch (Exception e) {
            log.warn("清除Token缓存失败", e);
        }
    }

    /**
     * 清除用户所有Token缓存
     */
    private void clearUserTokenCache(String userId) {
        try {
            String userTokenKey = USER_TOKEN_KEY_PREFIX + userId;
            
            // 获取用户所有Token JTI
            Set<Object> jtis = redisTemplate.opsForSet().members(userTokenKey);
            
            if (jtis != null) {
                // 清除所有Token信息缓存
                for (Object jti : jtis) {
                    redisTemplate.delete(TOKEN_INFO_KEY_PREFIX + jti);
                }
            }
            
            // 清除用户Token集合
            redisTemplate.delete(userTokenKey);
        } catch (Exception e) {
            log.warn("清除用户Token缓存失败", e);
        }
    }
}