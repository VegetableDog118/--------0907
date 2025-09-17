package com.powertrading.auth.service;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.Date;
import java.util.Set;
import java.util.concurrent.TimeUnit;

/**
 * Token黑名单管理服务
 *
 * @author PowerTrading Team
 * @since 2024-01-15
 */
@Slf4j
@Service
public class TokenBlacklistService {

    private static final String BLACKLIST_KEY_PREFIX = "auth:blacklist:";
    private static final String BLACKLIST_SET_KEY = "auth:blacklist:set";

    @Autowired
    private RedisTemplate<String, Object> redisTemplate;

    @Value("${auth.blacklist.enabled:true}")
    private Boolean blacklistEnabled;

    @Value("${jwt.expiration}")
    private Long jwtExpiration;

    /**
     * 将Token加入黑名单
     */
    public void addToBlacklist(String jti, Date expiration) {
        if (!blacklistEnabled) {
            return;
        }

        try {
            String key = BLACKLIST_KEY_PREFIX + jti;
            long ttl = expiration.getTime() - System.currentTimeMillis();
            
            if (ttl > 0) {
                // 设置过期时间，过期后自动删除
                redisTemplate.opsForValue().set(key, "blacklisted", ttl, TimeUnit.MILLISECONDS);
                
                // 同时加入黑名单集合，用于批量清理
                redisTemplate.opsForZSet().add(BLACKLIST_SET_KEY, jti, expiration.getTime());
                
                log.info("Token已加入黑名单: jti={}, expiration={}", jti, expiration);
            }
        } catch (Exception e) {
            log.error("添加Token到黑名单失败: jti={}", jti, e);
        }
    }

    /**
     * 将Token加入黑名单(使用JTI和剩余有效时间)
     */
    public void addToBlacklist(String jti, long remainingTimeSeconds) {
        if (!blacklistEnabled || remainingTimeSeconds <= 0) {
            return;
        }

        try {
            String key = BLACKLIST_KEY_PREFIX + jti;
            redisTemplate.opsForValue().set(key, "blacklisted", remainingTimeSeconds, TimeUnit.SECONDS);
            
            // 计算过期时间戳
            long expirationTimestamp = System.currentTimeMillis() + (remainingTimeSeconds * 1000);
            redisTemplate.opsForZSet().add(BLACKLIST_SET_KEY, jti, expirationTimestamp);
            
            log.info("Token已加入黑名单: jti={}, remainingTime={}s", jti, remainingTimeSeconds);
        } catch (Exception e) {
            log.error("添加Token到黑名单失败: jti={}", jti, e);
        }
    }

    /**
     * 检查Token是否在黑名单中
     */
    public boolean isBlacklisted(String jti) {
        if (!blacklistEnabled) {
            return false;
        }

        try {
            String key = BLACKLIST_KEY_PREFIX + jti;
            return Boolean.TRUE.equals(redisTemplate.hasKey(key));
        } catch (Exception e) {
            log.error("检查Token黑名单状态失败: jti={}", jti, e);
            // 出现异常时，为了安全起见，认为Token在黑名单中
            return true;
        }
    }

    /**
     * 从黑名单中移除Token
     */
    public void removeFromBlacklist(String jti) {
        if (!blacklistEnabled) {
            return;
        }

        try {
            String key = BLACKLIST_KEY_PREFIX + jti;
            redisTemplate.delete(key);
            redisTemplate.opsForZSet().remove(BLACKLIST_SET_KEY, jti);
            
            log.info("Token已从黑名单移除: jti={}", jti);
        } catch (Exception e) {
            log.error("从黑名单移除Token失败: jti={}", jti, e);
        }
    }

    /**
     * 批量将用户的所有Token加入黑名单
     */
    public void blacklistUserTokens(String userId) {
        if (!blacklistEnabled) {
            return;
        }

        try {
            // 这里可以根据实际需求实现
            // 例如：维护用户Token映射关系，然后批量加入黑名单
            String userTokenPattern = "auth:user:" + userId + ":*";
            Set<String> userTokenKeys = redisTemplate.keys(userTokenPattern);
            
            if (userTokenKeys != null && !userTokenKeys.isEmpty()) {
                for (String tokenKey : userTokenKeys) {
                    String jti = tokenKey.substring(tokenKey.lastIndexOf(":") + 1);
                    addToBlacklist(jti, jwtExpiration);
                }
                log.info("用户所有Token已加入黑名单: userId={}, tokenCount={}", userId, userTokenKeys.size());
            }
        } catch (Exception e) {
            log.error("批量加入用户Token到黑名单失败: userId={}", userId, e);
        }
    }

    /**
     * 获取黑名单Token数量
     */
    public long getBlacklistSize() {
        if (!blacklistEnabled) {
            return 0;
        }

        try {
            Long size = redisTemplate.opsForZSet().zCard(BLACKLIST_SET_KEY);
            return size != null ? size : 0;
        } catch (Exception e) {
            log.error("获取黑名单大小失败", e);
            return 0;
        }
    }

    /**
     * 定时清理过期的黑名单记录
     */
    @Scheduled(fixedRate = 3600000) // 每小时执行一次
    public void cleanupExpiredBlacklistEntries() {
        if (!blacklistEnabled) {
            return;
        }

        try {
            long currentTime = System.currentTimeMillis();
            
            // 移除已过期的黑名单记录
            Long removedCount = redisTemplate.opsForZSet()
                    .removeRangeByScore(BLACKLIST_SET_KEY, 0, currentTime);
            
            if (removedCount != null && removedCount > 0) {
                log.info("清理过期黑名单记录: count={}", removedCount);
            }
        } catch (Exception e) {
            log.error("清理过期黑名单记录失败", e);
        }
    }

    /**
     * 清空所有黑名单记录
     */
    public void clearAllBlacklist() {
        if (!blacklistEnabled) {
            return;
        }

        try {
            // 清空黑名单集合
            redisTemplate.delete(BLACKLIST_SET_KEY);
            
            // 清空所有黑名单键
            Set<String> blacklistKeys = redisTemplate.keys(BLACKLIST_KEY_PREFIX + "*");
            if (blacklistKeys != null && !blacklistKeys.isEmpty()) {
                redisTemplate.delete(blacklistKeys);
                log.info("已清空所有黑名单记录: count={}", blacklistKeys.size());
            }
        } catch (Exception e) {
            log.error("清空黑名单失败", e);
        }
    }

    /**
     * 检查黑名单功能是否启用
     */
    public boolean isBlacklistEnabled() {
        return blacklistEnabled;
    }
}