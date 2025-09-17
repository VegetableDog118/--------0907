package com.powertrading.auth.service;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.TimeUnit;

/**
 * 审计日志服务
 *
 * @author PowerTrading Team
 * @since 2024-01-15
 */
@Slf4j
@Service
public class AuditLogService {

    @Autowired
    private RedisTemplate<String, Object> redisTemplate;

    private static final String AUDIT_LOG_PREFIX = "auth:audit:";
    private static final String DAILY_STATS_PREFIX = "auth:stats:daily:";
    private static final DateTimeFormatter DATE_FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd");
    private static final DateTimeFormatter DATETIME_FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");

    /**
     * 记录认证成功日志
     */
    @Async
    public void logAuthSuccess(String userId, String username, String authType, 
                              String clientIp, String userAgent) {
        try {
            Map<String, Object> logData = new HashMap<>();
            logData.put("userId", userId);
            logData.put("username", username);
            logData.put("authType", authType);
            logData.put("result", "SUCCESS");
            logData.put("clientIp", clientIp);
            logData.put("userAgent", userAgent);
            logData.put("timestamp", System.currentTimeMillis());
            logData.put("datetime", LocalDateTime.now().format(DATETIME_FORMATTER));

            // 存储详细日志
            String logKey = generateLogKey("success", userId);
            redisTemplate.opsForList().leftPush(logKey, logData);
            redisTemplate.expire(logKey, 30, TimeUnit.DAYS); // 保留30天

            // 更新统计信息
            updateDailyStats("success", authType);

            log.info("认证成功日志已记录: userId={}, authType={}, clientIp={}", 
                    userId, authType, clientIp);
        } catch (Exception e) {
            log.error("记录认证成功日志失败", e);
        }
    }

    /**
     * 记录认证失败日志
     */
    @Async
    public void logAuthFailure(String userId, String authType, String errorMessage, 
                              String clientIp, String userAgent) {
        try {
            Map<String, Object> logData = new HashMap<>();
            logData.put("userId", userId);
            logData.put("authType", authType);
            logData.put("result", "FAILURE");
            logData.put("errorMessage", errorMessage);
            logData.put("clientIp", clientIp);
            logData.put("userAgent", userAgent);
            logData.put("timestamp", System.currentTimeMillis());
            logData.put("datetime", LocalDateTime.now().format(DATETIME_FORMATTER));

            // 存储详细日志
            String logKey = generateLogKey("failure", userId);
            redisTemplate.opsForList().leftPush(logKey, logData);
            redisTemplate.expire(logKey, 30, TimeUnit.DAYS);

            // 更新统计信息
            updateDailyStats("failure", authType);

            // 检查是否需要安全告警
            checkSecurityAlert(userId, clientIp);

            log.warn("认证失败日志已记录: userId={}, authType={}, error={}, clientIp={}", 
                    userId, authType, errorMessage, clientIp);
        } catch (Exception e) {
            log.error("记录认证失败日志失败", e);
        }
    }

    /**
     * 记录Token生成日志
     */
    @Async
    public void logTokenGeneration(String userId, String username, String tokenType, 
                                  String clientIp, String userAgent) {
        try {
            Map<String, Object> logData = new HashMap<>();
            logData.put("userId", userId);
            logData.put("username", username);
            logData.put("action", "TOKEN_GENERATE");
            logData.put("tokenType", tokenType);
            logData.put("clientIp", clientIp);
            logData.put("userAgent", userAgent);
            logData.put("timestamp", System.currentTimeMillis());
            logData.put("datetime", LocalDateTime.now().format(DATETIME_FORMATTER));

            String logKey = generateLogKey("token", userId);
            redisTemplate.opsForList().leftPush(logKey, logData);
            redisTemplate.expire(logKey, 30, TimeUnit.DAYS);

            log.info("Token生成日志已记录: userId={}, tokenType={}", userId, tokenType);
        } catch (Exception e) {
            log.error("记录Token生成日志失败", e);
        }
    }

    /**
     * 记录Token撤销日志
     */
    @Async
    public void logTokenRevocation(String userId, String tokenType, String reason, 
                                  String clientIp, String userAgent) {
        try {
            Map<String, Object> logData = new HashMap<>();
            logData.put("userId", userId);
            logData.put("action", "TOKEN_REVOKE");
            logData.put("tokenType", tokenType);
            logData.put("reason", reason);
            logData.put("clientIp", clientIp);
            logData.put("userAgent", userAgent);
            logData.put("timestamp", System.currentTimeMillis());
            logData.put("datetime", LocalDateTime.now().format(DATETIME_FORMATTER));

            String logKey = generateLogKey("token", userId);
            redisTemplate.opsForList().leftPush(logKey, logData);
            redisTemplate.expire(logKey, 30, TimeUnit.DAYS);

            log.info("Token撤销日志已记录: userId={}, reason={}", userId, reason);
        } catch (Exception e) {
            log.error("记录Token撤销日志失败", e);
        }
    }

    /**
     * 记录API密钥操作日志
     */
    @Async
    public void logApiKeyOperation(String userId, String appId, String action, 
                                  String clientIp, String userAgent) {
        try {
            Map<String, Object> logData = new HashMap<>();
            logData.put("userId", userId);
            logData.put("appId", appId);
            logData.put("action", action);
            logData.put("clientIp", clientIp);
            logData.put("userAgent", userAgent);
            logData.put("timestamp", System.currentTimeMillis());
            logData.put("datetime", LocalDateTime.now().format(DATETIME_FORMATTER));

            String logKey = generateLogKey("apikey", userId);
            redisTemplate.opsForList().leftPush(logKey, logData);
            redisTemplate.expire(logKey, 30, TimeUnit.DAYS);

            log.info("API密钥操作日志已记录: userId={}, appId={}, action={}", 
                    userId, appId, action);
        } catch (Exception e) {
            log.error("记录API密钥操作日志失败", e);
        }
    }

    /**
     * 记录权限检查日志
     */
    @Async
    public void logPermissionCheck(String userId, String permission, boolean granted, 
                                  String resource, String clientIp) {
        try {
            Map<String, Object> logData = new HashMap<>();
            logData.put("userId", userId);
            logData.put("action", "PERMISSION_CHECK");
            logData.put("permission", permission);
            logData.put("granted", granted);
            logData.put("resource", resource);
            logData.put("clientIp", clientIp);
            logData.put("timestamp", System.currentTimeMillis());
            logData.put("datetime", LocalDateTime.now().format(DATETIME_FORMATTER));

            String logKey = generateLogKey("permission", userId);
            redisTemplate.opsForList().leftPush(logKey, logData);
            redisTemplate.expire(logKey, 7, TimeUnit.DAYS); // 权限检查日志保留7天

            if (!granted) {
                log.warn("权限检查失败: userId={}, permission={}, resource={}", 
                        userId, permission, resource);
            }
        } catch (Exception e) {
            log.error("记录权限检查日志失败", e);
        }
    }

    /**
     * 获取用户审计日志
     */
    public Map<String, Object> getUserAuditLogs(String userId, String logType, int limit) {
        try {
            String logKey = generateLogKey(logType, userId);
            
            // 获取日志列表
            Long totalCount = redisTemplate.opsForList().size(logKey);
            List<Object> logs = redisTemplate.opsForList().range(logKey, 0, limit - 1);

            Map<String, Object> result = new HashMap<>();
            result.put("userId", userId);
            result.put("logType", logType);
            result.put("totalCount", totalCount != null ? totalCount : 0);
            result.put("logs", logs != null ? logs : new ArrayList<>());
            result.put("queryTime", LocalDateTime.now().format(DATETIME_FORMATTER));

            return result;
        } catch (Exception e) {
            log.error("获取用户审计日志失败: userId={}, logType={}", userId, logType, e);
            return new HashMap<>();
        }
    }

    /**
     * 获取每日统计信息
     */
    public Map<String, Object> getDailyStats(String date) {
        try {
            String statsKey = DAILY_STATS_PREFIX + date;
            Map<Object, Object> stats = redisTemplate.opsForHash().entries(statsKey);

            Map<String, Object> result = new HashMap<>();
            result.put("date", date);
            result.put("totalAuthRequests", stats.getOrDefault("total", 0));
            result.put("successfulAuths", stats.getOrDefault("success", 0));
            result.put("failedAuths", stats.getOrDefault("failure", 0));
            result.put("jwtTokenAuths", stats.getOrDefault("JWT_TOKEN", 0));
            result.put("apiKeyAuths", stats.getOrDefault("API_KEY", 0));
            result.put("mixedAuths", stats.getOrDefault("MIXED", 0));

            return result;
        } catch (Exception e) {
            log.error("获取每日统计信息失败: date={}", date, e);
            return new HashMap<>();
        }
    }

    /**
     * 生成日志键
     */
    private String generateLogKey(String logType, String userId) {
        String today = LocalDateTime.now().format(DATE_FORMATTER);
        return AUDIT_LOG_PREFIX + logType + ":" + userId + ":" + today;
    }

    /**
     * 更新每日统计信息
     */
    private void updateDailyStats(String result, String authType) {
        try {
            String today = LocalDateTime.now().format(DATE_FORMATTER);
            String statsKey = DAILY_STATS_PREFIX + today;

            // 增加总数
            redisTemplate.opsForHash().increment(statsKey, "total", 1);
            
            // 增加结果统计
            redisTemplate.opsForHash().increment(statsKey, result, 1);
            
            // 增加认证类型统计
            if (authType != null) {
                redisTemplate.opsForHash().increment(statsKey, authType, 1);
            }

            // 设置过期时间（保留90天）
            redisTemplate.expire(statsKey, 90, TimeUnit.DAYS);
        } catch (Exception e) {
            log.error("更新每日统计信息失败", e);
        }
    }

    /**
     * 检查安全告警
     */
    private void checkSecurityAlert(String userId, String clientIp) {
        try {
            String alertKey = "auth:alert:" + userId + ":" + clientIp;
            
            // 增加失败计数
            Long failureCount = redisTemplate.opsForValue().increment(alertKey);
            redisTemplate.expire(alertKey, 1, TimeUnit.HOURS); // 1小时窗口

            // 如果1小时内失败次数超过10次，记录安全告警
            if (failureCount != null && failureCount > 10) {
                log.warn("安全告警: 用户 {} 从IP {} 在1小时内认证失败 {} 次", 
                        userId, clientIp, failureCount);
                
                // 这里可以发送告警通知
                // alertNotificationService.sendSecurityAlert(userId, clientIp, failureCount);
            }
        } catch (Exception e) {
            log.error("检查安全告警失败", e);
        }
    }
}