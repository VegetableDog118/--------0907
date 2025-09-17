package com.powertrading.auth.service;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.concurrent.TimeUnit;

/**
 * 安全策略管理服务
 *
 * @author PowerTrading Team
 * @since 2024-01-15
 */
@Slf4j
@Service
public class SecurityPolicyService {

    @Autowired
    private RedisTemplate<String, Object> redisTemplate;

    @Autowired
    private TokenBlacklistService tokenBlacklistService;

    @Value("${auth.security.max-login-attempts:5}")
    private Integer maxLoginAttempts;

    @Value("${auth.security.lockout-duration:1800}")
    private Integer lockoutDuration; // 30分钟

    @Value("${jwt.expiration:86400}")
    private Long jwtExpiration;

    @Value("${jwt.refresh-expiration:604800}")
    private Long jwtRefreshExpiration;

    private static final String LOGIN_ATTEMPTS_PREFIX = "auth:login:attempts:";
    private static final String ACCOUNT_LOCKOUT_PREFIX = "auth:lockout:";
    private static final String IP_BLACKLIST_PREFIX = "auth:ip:blacklist:";
    private static final String SUSPICIOUS_ACTIVITY_PREFIX = "auth:suspicious:";
    private static final String SECURITY_POLICY_PREFIX = "auth:policy:";
    private static final DateTimeFormatter DATETIME_FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");

    /**
     * 记录登录失败尝试
     */
    public void recordLoginFailure(String userId, String clientIp) {
        try {
            String attemptsKey = LOGIN_ATTEMPTS_PREFIX + userId;
            
            // 增加失败次数
            Long attempts = redisTemplate.opsForValue().increment(attemptsKey);
            redisTemplate.expire(attemptsKey, lockoutDuration, TimeUnit.SECONDS);

            log.warn("用户登录失败: userId={}, clientIp={}, attempts={}", userId, clientIp, attempts);

            // 检查是否需要锁定账户
            if (attempts != null && attempts >= maxLoginAttempts) {
                lockAccount(userId, "连续登录失败次数过多", clientIp);
            }

            // 记录可疑活动
            recordSuspiciousActivity(userId, clientIp, "LOGIN_FAILURE", 
                    "连续登录失败 " + attempts + " 次");
        } catch (Exception e) {
            log.error("记录登录失败尝试失败", e);
        }
    }

    /**
     * 清除登录失败记录
     */
    public void clearLoginFailures(String userId) {
        try {
            String attemptsKey = LOGIN_ATTEMPTS_PREFIX + userId;
            redisTemplate.delete(attemptsKey);
            
            log.info("已清除用户登录失败记录: userId={}", userId);
        } catch (Exception e) {
            log.error("清除登录失败记录失败", e);
        }
    }

    /**
     * 锁定账户
     */
    public void lockAccount(String userId, String reason, String clientIp) {
        try {
            String lockoutKey = ACCOUNT_LOCKOUT_PREFIX + userId;
            
            Map<String, Object> lockoutInfo = new HashMap<>();
            lockoutInfo.put("userId", userId);
            lockoutInfo.put("reason", reason);
            lockoutInfo.put("clientIp", clientIp);
            lockoutInfo.put("lockTime", System.currentTimeMillis());
            lockoutInfo.put("lockDateTime", LocalDateTime.now().format(DATETIME_FORMATTER));
            lockoutInfo.put("unlockTime", System.currentTimeMillis() + (lockoutDuration * 1000L));
            
            redisTemplate.opsForHash().putAll(lockoutKey, lockoutInfo);
            redisTemplate.expire(lockoutKey, lockoutDuration, TimeUnit.SECONDS);

            log.warn("账户已锁定: userId={}, reason={}, duration={}s", userId, reason, lockoutDuration);

            // 撤销用户所有Token
            tokenBlacklistService.blacklistUserTokens(userId);
        } catch (Exception e) {
            log.error("锁定账户失败", e);
        }
    }

    /**
     * 解锁账户
     */
    public void unlockAccount(String userId) {
        try {
            String lockoutKey = ACCOUNT_LOCKOUT_PREFIX + userId;
            String attemptsKey = LOGIN_ATTEMPTS_PREFIX + userId;
            
            redisTemplate.delete(lockoutKey);
            redisTemplate.delete(attemptsKey);
            
            log.info("账户已解锁: userId={}", userId);
        } catch (Exception e) {
            log.error("解锁账户失败", e);
        }
    }

    /**
     * 检查账户是否被锁定
     */
    public boolean isAccountLocked(String userId) {
        try {
            String lockoutKey = ACCOUNT_LOCKOUT_PREFIX + userId;
            return Boolean.TRUE.equals(redisTemplate.hasKey(lockoutKey));
        } catch (Exception e) {
            log.error("检查账户锁定状态失败", e);
            return false;
        }
    }

    /**
     * 获取账户锁定信息
     */
    public Map<String, Object> getAccountLockoutInfo(String userId) {
        try {
            String lockoutKey = ACCOUNT_LOCKOUT_PREFIX + userId;
            Map<Object, Object> lockoutInfo = redisTemplate.opsForHash().entries(lockoutKey);
            
            if (!lockoutInfo.isEmpty()) {
                Map<String, Object> result = new HashMap<>();
                result.put("locked", true);
                result.put("reason", lockoutInfo.get("reason"));
                result.put("lockTime", lockoutInfo.get("lockTime"));
                result.put("lockDateTime", lockoutInfo.get("lockDateTime"));
                result.put("unlockTime", lockoutInfo.get("unlockTime"));
                
                // 计算剩余锁定时间
                Long unlockTime = (Long) lockoutInfo.get("unlockTime");
                if (unlockTime != null) {
                    long remainingTime = Math.max(0, unlockTime - System.currentTimeMillis());
                    result.put("remainingLockTime", remainingTime / 1000); // 转换为秒
                }
                
                return result;
            } else {
                return Map.of("locked", false);
            }
        } catch (Exception e) {
            log.error("获取账户锁定信息失败", e);
            return Map.of("locked", false);
        }
    }

    /**
     * 添加IP到黑名单
     */
    public void addIpToBlacklist(String clientIp, String reason, long durationSeconds) {
        try {
            String blacklistKey = IP_BLACKLIST_PREFIX + clientIp;
            
            Map<String, Object> blacklistInfo = new HashMap<>();
            blacklistInfo.put("ip", clientIp);
            blacklistInfo.put("reason", reason);
            blacklistInfo.put("addTime", System.currentTimeMillis());
            blacklistInfo.put("addDateTime", LocalDateTime.now().format(DATETIME_FORMATTER));
            blacklistInfo.put("expireTime", System.currentTimeMillis() + (durationSeconds * 1000));
            
            redisTemplate.opsForHash().putAll(blacklistKey, blacklistInfo);
            redisTemplate.expire(blacklistKey, durationSeconds, TimeUnit.SECONDS);
            
            log.warn("IP已加入黑名单: ip={}, reason={}, duration={}s", clientIp, reason, durationSeconds);
        } catch (Exception e) {
            log.error("添加IP到黑名单失败", e);
        }
    }

    /**
     * 从黑名单移除IP
     */
    public void removeIpFromBlacklist(String clientIp) {
        try {
            String blacklistKey = IP_BLACKLIST_PREFIX + clientIp;
            redisTemplate.delete(blacklistKey);
            
            log.info("IP已从黑名单移除: ip={}", clientIp);
        } catch (Exception e) {
            log.error("从黑名单移除IP失败", e);
        }
    }

    /**
     * 检查IP是否在黑名单中
     */
    public boolean isIpBlacklisted(String clientIp) {
        try {
            String blacklistKey = IP_BLACKLIST_PREFIX + clientIp;
            return Boolean.TRUE.equals(redisTemplate.hasKey(blacklistKey));
        } catch (Exception e) {
            log.error("检查IP黑名单状态失败", e);
            return false;
        }
    }

    /**
     * 记录可疑活动
     */
    public void recordSuspiciousActivity(String userId, String clientIp, String activityType, String description) {
        try {
            String suspiciousKey = SUSPICIOUS_ACTIVITY_PREFIX + userId + ":" + clientIp;
            
            Map<String, Object> activityInfo = new HashMap<>();
            activityInfo.put("userId", userId);
            activityInfo.put("clientIp", clientIp);
            activityInfo.put("activityType", activityType);
            activityInfo.put("description", description);
            activityInfo.put("timestamp", System.currentTimeMillis());
            activityInfo.put("datetime", LocalDateTime.now().format(DATETIME_FORMATTER));
            
            redisTemplate.opsForList().leftPush(suspiciousKey, activityInfo);
            redisTemplate.expire(suspiciousKey, 7, TimeUnit.DAYS); // 保留7天
            
            // 限制列表长度
            redisTemplate.opsForList().trim(suspiciousKey, 0, 99); // 最多保留100条记录
            
            log.warn("可疑活动已记录: userId={}, ip={}, type={}, desc={}", 
                    userId, clientIp, activityType, description);
        } catch (Exception e) {
            log.error("记录可疑活动失败", e);
        }
    }

    /**
     * 检查Token是否需要刷新
     */
    public boolean shouldRefreshToken(long remainingTimeSeconds) {
        // 如果剩余时间少于总有效期的1/4，建议刷新
        return remainingTimeSeconds < (jwtExpiration / 4);
    }

    /**
     * 检查Token是否即将过期
     */
    public boolean isTokenExpiringSoon(long remainingTimeSeconds, long thresholdSeconds) {
        return remainingTimeSeconds < thresholdSeconds;
    }

    /**
     * 获取安全策略配置
     */
    public Map<String, Object> getSecurityPolicyConfig() {
        Map<String, Object> config = new HashMap<>();
        config.put("maxLoginAttempts", maxLoginAttempts);
        config.put("lockoutDuration", lockoutDuration);
        config.put("jwtExpiration", jwtExpiration);
        config.put("jwtRefreshExpiration", jwtRefreshExpiration);
        config.put("blacklistEnabled", tokenBlacklistService.isBlacklistEnabled());
        return config;
    }

    /**
     * 更新安全策略配置
     */
    public void updateSecurityPolicyConfig(Map<String, Object> config) {
        try {
            String policyKey = SECURITY_POLICY_PREFIX + "config";
            redisTemplate.opsForHash().putAll(policyKey, config);
            
            log.info("安全策略配置已更新: {}", config);
        } catch (Exception e) {
            log.error("更新安全策略配置失败", e);
        }
    }

    /**
     * 获取安全统计信息
     */
    public Map<String, Object> getSecurityStatistics() {
        try {
            Map<String, Object> stats = new HashMap<>();
            
            // 获取当前锁定账户数量
            Set<String> lockoutKeys = redisTemplate.keys(ACCOUNT_LOCKOUT_PREFIX + "*");
            stats.put("lockedAccounts", lockoutKeys != null ? lockoutKeys.size() : 0);
            
            // 获取黑名单IP数量
            Set<String> blacklistKeys = redisTemplate.keys(IP_BLACKLIST_PREFIX + "*");
            stats.put("blacklistedIps", blacklistKeys != null ? blacklistKeys.size() : 0);
            
            // 获取Token黑名单大小
            stats.put("blacklistedTokens", tokenBlacklistService.getBlacklistSize());
            
            // 获取可疑活动数量
            Set<String> suspiciousKeys = redisTemplate.keys(SUSPICIOUS_ACTIVITY_PREFIX + "*");
            stats.put("suspiciousActivities", suspiciousKeys != null ? suspiciousKeys.size() : 0);
            
            return stats;
        } catch (Exception e) {
            log.error("获取安全统计信息失败", e);
            return new HashMap<>();
        }
    }

    /**
     * 定时清理过期的安全记录
     */
    @Scheduled(fixedRate = 3600000) // 每小时执行一次
    public void cleanupExpiredSecurityRecords() {
        try {
            // 清理过期的登录失败记录
            cleanupExpiredKeys(LOGIN_ATTEMPTS_PREFIX);
            
            // 清理过期的账户锁定记录
            cleanupExpiredKeys(ACCOUNT_LOCKOUT_PREFIX);
            
            // 清理过期的IP黑名单记录
            cleanupExpiredKeys(IP_BLACKLIST_PREFIX);
            
            log.info("安全记录清理完成");
        } catch (Exception e) {
            log.error("清理过期安全记录失败", e);
        }
    }

    /**
     * 清理过期的键
     */
    private void cleanupExpiredKeys(String keyPrefix) {
        try {
            Set<String> keys = redisTemplate.keys(keyPrefix + "*");
            if (keys != null && !keys.isEmpty()) {
                for (String key : keys) {
                    Long ttl = redisTemplate.getExpire(key);
                    if (ttl != null && ttl <= 0) {
                        redisTemplate.delete(key);
                    }
                }
            }
        } catch (Exception e) {
            log.error("清理过期键失败: prefix={}", keyPrefix, e);
        }
    }

    /**
     * 检查密码强度
     */
    public boolean isPasswordStrong(String password) {
        if (!StringUtils.hasText(password) || password.length() < 8) {
            return false;
        }
        
        // 检查是否包含大写字母、小写字母、数字和特殊字符
        boolean hasUpper = password.matches(".*[A-Z].*");
        boolean hasLower = password.matches(".*[a-z].*");
        boolean hasDigit = password.matches(".*\\d.*");
        boolean hasSpecial = password.matches(".*[!@#$%^&*()_+\\-=\\[\\]{};':,.<>?].*");
        
        return hasUpper && hasLower && hasDigit && hasSpecial;
    }

    /**
     * 生成安全报告
     */
    public Map<String, Object> generateSecurityReport() {
        try {
            Map<String, Object> report = new HashMap<>();
            report.put("reportTime", LocalDateTime.now().format(DATETIME_FORMATTER));
            report.put("statistics", getSecurityStatistics());
            report.put("policyConfig", getSecurityPolicyConfig());
            
            // 添加安全建议
            List<String> recommendations = new ArrayList<>();
            
            Map<String, Object> stats = getSecurityStatistics();
            Integer lockedAccounts = (Integer) stats.get("lockedAccounts");
            Integer blacklistedIps = (Integer) stats.get("blacklistedIps");
            
            if (lockedAccounts != null && lockedAccounts > 10) {
                recommendations.add("当前锁定账户数量较多，建议检查是否存在批量攻击");
            }
            
            if (blacklistedIps != null && blacklistedIps > 50) {
                recommendations.add("黑名单IP数量较多，建议加强网络安全防护");
            }
            
            report.put("recommendations", recommendations);
            
            return report;
        } catch (Exception e) {
            log.error("生成安全报告失败", e);
            return new HashMap<>();
        }
    }
}