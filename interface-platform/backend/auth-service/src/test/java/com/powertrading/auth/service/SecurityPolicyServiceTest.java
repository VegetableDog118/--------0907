package com.powertrading.auth.service;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.core.ValueOperations;
import org.springframework.data.redis.core.SetOperations;

import java.time.LocalDateTime;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.TimeUnit;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

/**
 * 安全策略服务测试
 *
 * @author PowerTrading Team
 * @since 2024-01-15
 */
@ExtendWith(MockitoExtension.class)
class SecurityPolicyServiceTest {

    @Mock
    private RedisTemplate<String, Object> redisTemplate;
    
    @Mock
    private TokenBlacklistService tokenBlacklistService;
    
    @Mock
    private ValueOperations<String, Object> valueOperations;
    
    @Mock
    private SetOperations<String, Object> setOperations;
    
    @InjectMocks
    private SecurityPolicyService securityPolicyService;
    
    private final String testUserId = "user-001";
    private final String testClientIp = "192.168.1.100";
    private final String testUserAgent = "Test-Agent/1.0";
    private final String testPassword = "TestPassword123!";

    @BeforeEach
    void setUp() {
        when(redisTemplate.opsForValue()).thenReturn(valueOperations);
        when(redisTemplate.opsForSet()).thenReturn(setOperations);
    }

    @Test
    void testRecordLoginFailure() {
        // 执行记录登录失败
        securityPolicyService.recordLoginFailure(testUserId, testClientIp, "Invalid password");
        
        // 验证Redis操作
        String failureKey = "login_failure:" + testUserId;
        verify(valueOperations).increment(failureKey);
        verify(redisTemplate).expire(failureKey, 1, TimeUnit.HOURS);
        
        String ipFailureKey = "ip_failure:" + testClientIp;
        verify(valueOperations).increment(ipFailureKey);
        verify(redisTemplate).expire(ipFailureKey, 1, TimeUnit.HOURS);
    }

    @Test
    void testIsAccountLocked() {
        // Mock账户锁定状态
        String lockKey = "account_locked:" + testUserId;
        when(valueOperations.get(lockKey)).thenReturn("true");
        
        // 执行检查
        boolean isLocked = securityPolicyService.isAccountLocked(testUserId);
        
        // 验证结果
        assertTrue(isLocked);
    }

    @Test
    void testIsAccountNotLocked() {
        // Mock账户未锁定状态
        String lockKey = "account_locked:" + testUserId;
        when(valueOperations.get(lockKey)).thenReturn(null);
        
        // 执行检查
        boolean isLocked = securityPolicyService.isAccountLocked(testUserId);
        
        // 验证结果
        assertFalse(isLocked);
    }

    @Test
    void testLockAccount() {
        // 执行锁定账户
        securityPolicyService.lockAccount(testUserId, "Too many failed login attempts");
        
        // 验证Redis操作
        String lockKey = "account_locked:" + testUserId;
        verify(valueOperations).set(lockKey, "true", 30, TimeUnit.MINUTES);
        
        String reasonKey = "lock_reason:" + testUserId;
        verify(valueOperations).set(reasonKey, "Too many failed login attempts", 30, TimeUnit.MINUTES);
    }

    @Test
    void testUnlockAccount() {
        // 执行解锁账户
        securityPolicyService.unlockAccount(testUserId);
        
        // 验证Redis操作
        String lockKey = "account_locked:" + testUserId;
        String reasonKey = "lock_reason:" + testUserId;
        String failureKey = "login_failure:" + testUserId;
        
        verify(redisTemplate).delete(lockKey);
        verify(redisTemplate).delete(reasonKey);
        verify(redisTemplate).delete(failureKey);
    }

    @Test
    void testAddIpToBlacklist() {
        String reason = "Suspicious activity detected";
        
        // 执行添加IP到黑名单
        securityPolicyService.addIpToBlacklist(testClientIp, reason);
        
        // 验证Redis操作
        String blacklistKey = "ip_blacklist";
        verify(setOperations).add(blacklistKey, testClientIp);
        
        String reasonKey = "ip_blacklist_reason:" + testClientIp;
        verify(valueOperations).set(reasonKey, reason, 24, TimeUnit.HOURS);
    }

    @Test
    void testRemoveIpFromBlacklist() {
        // 执行从黑名单移除IP
        securityPolicyService.removeIpFromBlacklist(testClientIp);
        
        // 验证Redis操作
        String blacklistKey = "ip_blacklist";
        verify(setOperations).remove(blacklistKey, testClientIp);
        
        String reasonKey = "ip_blacklist_reason:" + testClientIp;
        verify(redisTemplate).delete(reasonKey);
    }

    @Test
    void testIsIpBlacklisted() {
        // Mock IP在黑名单中
        String blacklistKey = "ip_blacklist";
        when(setOperations.isMember(blacklistKey, testClientIp)).thenReturn(true);
        
        // 执行检查
        boolean isBlacklisted = securityPolicyService.isIpBlacklisted(testClientIp);
        
        // 验证结果
        assertTrue(isBlacklisted);
    }

    @Test
    void testIsIpNotBlacklisted() {
        // Mock IP不在黑名单中
        String blacklistKey = "ip_blacklist";
        when(setOperations.isMember(blacklistKey, testClientIp)).thenReturn(false);
        
        // 执行检查
        boolean isBlacklisted = securityPolicyService.isIpBlacklisted(testClientIp);
        
        // 验证结果
        assertFalse(isBlacklisted);
    }

    @Test
    void testRecordSuspiciousActivity() {
        String activity = "Multiple failed login attempts";
        
        // 执行记录可疑活动
        securityPolicyService.recordSuspiciousActivity(testUserId, testClientIp, activity, testUserAgent);
        
        // 验证Redis操作
        String activityKey = "suspicious_activity:" + testUserId;
        verify(valueOperations).increment(activityKey);
        verify(redisTemplate).expire(activityKey, 24, TimeUnit.HOURS);
        
        String ipActivityKey = "suspicious_ip:" + testClientIp;
        verify(valueOperations).increment(ipActivityKey);
        verify(redisTemplate).expire(ipActivityKey, 24, TimeUnit.HOURS);
    }

    @Test
    void testCheckPasswordStrengthWeak() {
        String weakPassword = "123456";
        
        // 执行密码强度检查
        Map<String, Object> result = securityPolicyService.checkPasswordStrength(weakPassword);
        
        // 验证结果
        assertEquals("weak", result.get("strength"));
        assertEquals(1, result.get("score"));
        assertFalse((Boolean) result.get("isValid"));
        assertTrue(((String) result.get("suggestions")).contains("至少8个字符"));
    }

    @Test
    void testCheckPasswordStrengthMedium() {
        String mediumPassword = "Password123";
        
        // 执行密码强度检查
        Map<String, Object> result = securityPolicyService.checkPasswordStrength(mediumPassword);
        
        // 验证结果
        assertEquals("medium", result.get("strength"));
        assertEquals(3, result.get("score"));
        assertTrue((Boolean) result.get("isValid"));
    }

    @Test
    void testCheckPasswordStrengthStrong() {
        String strongPassword = "StrongPassword123!@#";
        
        // 执行密码强度检查
        Map<String, Object> result = securityPolicyService.checkPasswordStrength(strongPassword);
        
        // 验证结果
        assertEquals("strong", result.get("strength"));
        assertEquals(5, result.get("score"));
        assertTrue((Boolean) result.get("isValid"));
    }

    @Test
    void testGetSecurityPolicyConfig() {
        // Mock配置数据
        when(valueOperations.get("security_config:max_login_attempts")).thenReturn(5);
        when(valueOperations.get("security_config:account_lock_duration")).thenReturn(30);
        when(valueOperations.get("security_config:password_min_length")).thenReturn(8);
        when(valueOperations.get("security_config:password_require_uppercase")).thenReturn(true);
        when(valueOperations.get("security_config:password_require_lowercase")).thenReturn(true);
        when(valueOperations.get("security_config:password_require_numbers")).thenReturn(true);
        when(valueOperations.get("security_config:password_require_symbols")).thenReturn(true);
        when(valueOperations.get("security_config:session_timeout")).thenReturn(3600);
        when(valueOperations.get("security_config:enable_ip_whitelist")).thenReturn(false);
        when(valueOperations.get("security_config:enable_two_factor_auth")).thenReturn(false);
        
        // 执行获取配置
        Map<String, Object> config = securityPolicyService.getSecurityPolicyConfig();
        
        // 验证结果
        assertEquals(5, config.get("maxLoginAttempts"));
        assertEquals(30, config.get("accountLockDuration"));
        assertEquals(8, config.get("passwordMinLength"));
        assertTrue((Boolean) config.get("passwordRequireUppercase"));
        assertTrue((Boolean) config.get("passwordRequireLowercase"));
        assertTrue((Boolean) config.get("passwordRequireNumbers"));
        assertTrue((Boolean) config.get("passwordRequireSymbols"));
        assertEquals(3600, config.get("sessionTimeout"));
        assertFalse((Boolean) config.get("enableIpWhitelist"));
        assertFalse((Boolean) config.get("enableTwoFactorAuth"));
    }

    @Test
    void testUpdateSecurityPolicyConfig() {
        // 准备配置数据
        Map<String, Object> config = Map.of(
                "maxLoginAttempts", 3,
                "accountLockDuration", 60,
                "passwordMinLength", 10,
                "passwordRequireUppercase", true,
                "passwordRequireLowercase", true,
                "passwordRequireNumbers", true,
                "passwordRequireSymbols", false,
                "sessionTimeout", 7200,
                "enableIpWhitelist", true,
                "enableTwoFactorAuth", true
        );
        
        // 执行更新配置
        securityPolicyService.updateSecurityPolicyConfig(config);
        
        // 验证Redis操作
        verify(valueOperations).set("security_config:max_login_attempts", 3);
        verify(valueOperations).set("security_config:account_lock_duration", 60);
        verify(valueOperations).set("security_config:password_min_length", 10);
        verify(valueOperations).set("security_config:password_require_uppercase", true);
        verify(valueOperations).set("security_config:password_require_lowercase", true);
        verify(valueOperations).set("security_config:password_require_numbers", true);
        verify(valueOperations).set("security_config:password_require_symbols", false);
        verify(valueOperations).set("security_config:session_timeout", 7200);
        verify(valueOperations).set("security_config:enable_ip_whitelist", true);
        verify(valueOperations).set("security_config:enable_two_factor_auth", true);
    }

    @Test
    void testGetSecurityStatistics() {
        // Mock统计数据
        when(valueOperations.get("security_stats:total_login_attempts")).thenReturn(1000L);
        when(valueOperations.get("security_stats:failed_login_attempts")).thenReturn(50L);
        when(valueOperations.get("security_stats:locked_accounts")).thenReturn(5L);
        when(valueOperations.get("security_stats:blacklisted_ips")).thenReturn(10L);
        when(valueOperations.get("security_stats:suspicious_activities")).thenReturn(25L);
        when(valueOperations.get("security_stats:blocked_requests")).thenReturn(100L);
        
        // 执行获取统计信息
        Map<String, Object> statistics = securityPolicyService.getSecurityStatistics();
        
        // 验证结果
        assertEquals(1000L, statistics.get("totalLoginAttempts"));
        assertEquals(50L, statistics.get("failedLoginAttempts"));
        assertEquals(5L, statistics.get("lockedAccounts"));
        assertEquals(10L, statistics.get("blacklistedIps"));
        assertEquals(25L, statistics.get("suspiciousActivities"));
        assertEquals(100L, statistics.get("blockedRequests"));
        assertEquals(95.0, statistics.get("loginSuccessRate"));
    }

    @Test
    void testGenerateSecurityReport() {
        // Mock报告数据
        when(valueOperations.get("security_stats:total_login_attempts")).thenReturn(1000L);
        when(valueOperations.get("security_stats:failed_login_attempts")).thenReturn(50L);
        when(valueOperations.get("security_stats:locked_accounts")).thenReturn(5L);
        when(valueOperations.get("security_stats:blacklisted_ips")).thenReturn(10L);
        when(valueOperations.get("security_stats:suspicious_activities")).thenReturn(25L);
        when(valueOperations.get("security_stats:blocked_requests")).thenReturn(100L);
        
        // 执行生成安全报告
        Map<String, Object> report = securityPolicyService.generateSecurityReport();
        
        // 验证结果
        assertNotNull(report.get("reportTime"));
        assertNotNull(report.get("statistics"));
        assertNotNull(report.get("riskLevel"));
        assertNotNull(report.get("recommendations"));
        
        @SuppressWarnings("unchecked")
        Map<String, Object> statistics = (Map<String, Object>) report.get("statistics");
        assertEquals(1000L, statistics.get("totalLoginAttempts"));
        assertEquals(50L, statistics.get("failedLoginAttempts"));
        assertEquals(95.0, statistics.get("loginSuccessRate"));
    }

    @Test
    void testClearLoginFailures() {
        // 执行清除登录失败记录
        securityPolicyService.clearLoginFailures(testUserId);
        
        // 验证Redis操作
        String failureKey = "login_failure:" + testUserId;
        verify(redisTemplate).delete(failureKey);
    }

    @Test
    void testIsTokenExpiredWithValidToken() {
        LocalDateTime futureTime = LocalDateTime.now().plusHours(1);
        
        // 执行Token过期检查
        boolean isExpired = securityPolicyService.isTokenExpired(futureTime);
        
        // 验证结果
        assertFalse(isExpired);
    }

    @Test
    void testIsTokenExpiredWithExpiredToken() {
        LocalDateTime pastTime = LocalDateTime.now().minusHours(1);
        
        // 执行Token过期检查
        boolean isExpired = securityPolicyService.isTokenExpired(pastTime);
        
        // 验证结果
        assertTrue(isExpired);
    }

    @Test
    void testCheckAccountLockWithTooManyFailures() {
        // Mock失败次数超过限制
        String failureKey = "login_failure:" + testUserId;
        when(valueOperations.get(failureKey)).thenReturn(6L); // 超过默认的5次限制
        
        // 执行检查（这个方法会在recordLoginFailure中被调用）
        securityPolicyService.recordLoginFailure(testUserId, testClientIp, "Invalid password");
        
        // 验证账户被锁定
        String lockKey = "account_locked:" + testUserId;
        verify(valueOperations).set(eq(lockKey), eq("true"), eq(30L), eq(TimeUnit.MINUTES));
    }
}