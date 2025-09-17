package com.powertrading.auth.service;

import com.powertrading.auth.dto.AuthenticationRequest;
import com.powertrading.auth.dto.AuthenticationResult;
import com.powertrading.auth.util.JwtTokenUtil;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.core.ValueOperations;

import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.List;
import java.util.concurrent.TimeUnit;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

/**
 * 多重认证服务测试
 *
 * @author PowerTrading Team
 * @since 2024-01-15
 */
@ExtendWith(MockitoExtension.class)
class MultiAuthServiceTest {

    @Mock
    private JwtTokenUtil jwtTokenUtil;
    
    @Mock
    private TokenBlacklistService tokenBlacklistService;
    
    @Mock
    private RedisTemplate<String, Object> redisTemplate;
    
    @Mock
    private ValueOperations<String, Object> valueOperations;
    
    @InjectMocks
    private MultiAuthService multiAuthService;
    
    private final String testToken = "test.jwt.token";
    private final String testApiKey = "test-api-key-123";
    private final String testUserId = "user-001";
    private final String testUsername = "testuser";
    private final String testCompanyName = "Test Company";
    private final List<String> testRoles = Arrays.asList("user", "admin");
    private final List<String> testPermissions = Arrays.asList("read", "write");
    private final String testClientIp = "192.168.1.100";
    private final String testUserAgent = "Test-Agent/1.0";

    @BeforeEach
    void setUp() {
        when(redisTemplate.opsForValue()).thenReturn(valueOperations);
    }

    @Test
    void testAuthenticateWithValidJwtToken() {
        // 准备测试数据
        AuthenticationRequest request = new AuthenticationRequest();
        request.setJwtToken(testToken);
        request.setAuthMode("jwt");
        request.setClientIp(testClientIp);
        request.setUserAgent(testUserAgent);
        
        // Mock JWT工具类行为
        when(jwtTokenUtil.validateToken(testToken)).thenReturn(true);
        when(jwtTokenUtil.getUserIdFromToken(testToken)).thenReturn(testUserId);
        when(jwtTokenUtil.getUsernameFromToken(testToken)).thenReturn(testUsername);
        when(jwtTokenUtil.getCompanyNameFromToken(testToken)).thenReturn(testCompanyName);
        when(jwtTokenUtil.getRolesFromToken(testToken)).thenReturn(testRoles);
        when(jwtTokenUtil.getPermissionsFromToken(testToken)).thenReturn(testPermissions);
        when(jwtTokenUtil.getExpirationFromToken(testToken)).thenReturn(LocalDateTime.now().plusHours(1));
        when(jwtTokenUtil.getRemainingTimeFromToken(testToken)).thenReturn(3600L);
        when(jwtTokenUtil.isTokenExpired(testToken)).thenReturn(false);
        
        // Mock黑名单检查
        when(tokenBlacklistService.isTokenBlacklisted(testToken)).thenReturn(false);
        
        // 执行认证
        AuthenticationResult result = multiAuthService.authenticate(request);
        
        // 验证结果
        assertTrue(result.isSuccess());
        assertEquals(testUserId, result.getUserId());
        assertEquals(testUsername, result.getUsername());
        assertEquals(testCompanyName, result.getCompanyName());
        assertEquals(testRoles, result.getRoles());
        assertEquals(testPermissions, result.getPermissions());
        assertEquals("JWT", result.getAuthType());
        assertNotNull(result.getExpirationTime());
        assertNotNull(result.getRemainingTime());
    }

    @Test
    void testAuthenticateWithInvalidJwtToken() {
        // 准备测试数据
        AuthenticationRequest request = new AuthenticationRequest();
        request.setJwtToken(testToken);
        request.setAuthMode("jwt");
        request.setClientIp(testClientIp);
        request.setUserAgent(testUserAgent);
        
        // Mock JWT工具类行为（Token无效）
        when(jwtTokenUtil.validateToken(testToken)).thenThrow(new RuntimeException("Invalid token"));
        
        // 执行认证
        AuthenticationResult result = multiAuthService.authenticate(request);
        
        // 验证结果
        assertFalse(result.isSuccess());
        assertEquals("JWT Token验证失败: Invalid token", result.getErrorMessage());
    }

    @Test
    void testAuthenticateWithBlacklistedToken() {
        // 准备测试数据
        AuthenticationRequest request = new AuthenticationRequest();
        request.setJwtToken(testToken);
        request.setAuthMode("jwt");
        request.setClientIp(testClientIp);
        request.setUserAgent(testUserAgent);
        
        // Mock JWT工具类行为
        when(jwtTokenUtil.validateToken(testToken)).thenReturn(true);
        
        // Mock黑名单检查（Token在黑名单中）
        when(tokenBlacklistService.isTokenBlacklisted(testToken)).thenReturn(true);
        
        // 执行认证
        AuthenticationResult result = multiAuthService.authenticate(request);
        
        // 验证结果
        assertFalse(result.isSuccess());
        assertEquals("Token已被加入黑名单", result.getErrorMessage());
    }

    @Test
    void testAuthenticateWithValidApiKey() {
        // 准备测试数据
        AuthenticationRequest request = new AuthenticationRequest();
        request.setApiKey(testApiKey);
        request.setAuthMode("apikey");
        request.setClientIp(testClientIp);
        request.setUserAgent(testUserAgent);
        
        // Mock API密钥验证
        String apiKeyInfo = "{\"appId\":\"app-001\",\"appName\":\"Test App\",\"companyName\":\"Test Company\",\"permissions\":[\"read\",\"write\"],\"status\":\"active\",\"expirationTime\":\"2024-12-31T23:59:59\"}";
        when(valueOperations.get("api_key:" + testApiKey)).thenReturn(apiKeyInfo);
        
        // 执行认证
        AuthenticationResult result = multiAuthService.authenticate(request);
        
        // 验证结果
        assertTrue(result.isSuccess());
        assertEquals("app-001", result.getAppId());
        assertEquals("Test App", result.getAppName());
        assertEquals("Test Company", result.getCompanyName());
        assertEquals("API_KEY", result.getAuthType());
        assertNotNull(result.getPermissions());
        assertTrue(result.getPermissions().contains("read"));
        assertTrue(result.getPermissions().contains("write"));
    }

    @Test
    void testAuthenticateWithInvalidApiKey() {
        // 准备测试数据
        AuthenticationRequest request = new AuthenticationRequest();
        request.setApiKey(testApiKey);
        request.setAuthMode("apikey");
        request.setClientIp(testClientIp);
        request.setUserAgent(testUserAgent);
        
        // Mock API密钥验证（密钥不存在）
        when(valueOperations.get("api_key:" + testApiKey)).thenReturn(null);
        
        // 执行认证
        AuthenticationResult result = multiAuthService.authenticate(request);
        
        // 验证结果
        assertFalse(result.isSuccess());
        assertEquals("API密钥无效或不存在", result.getErrorMessage());
    }

    @Test
    void testAuthenticateWithMixedMode() {
        // 准备测试数据
        AuthenticationRequest request = new AuthenticationRequest();
        request.setJwtToken(testToken);
        request.setApiKey(testApiKey);
        request.setAuthMode("mixed");
        request.setClientIp(testClientIp);
        request.setUserAgent(testUserAgent);
        
        // Mock JWT认证成功
        when(jwtTokenUtil.validateToken(testToken)).thenReturn(true);
        when(jwtTokenUtil.getUserIdFromToken(testToken)).thenReturn(testUserId);
        when(jwtTokenUtil.getUsernameFromToken(testToken)).thenReturn(testUsername);
        when(jwtTokenUtil.getCompanyNameFromToken(testToken)).thenReturn(testCompanyName);
        when(jwtTokenUtil.getRolesFromToken(testToken)).thenReturn(testRoles);
        when(jwtTokenUtil.getPermissionsFromToken(testToken)).thenReturn(testPermissions);
        when(jwtTokenUtil.getExpirationFromToken(testToken)).thenReturn(LocalDateTime.now().plusHours(1));
        when(jwtTokenUtil.getRemainingTimeFromToken(testToken)).thenReturn(3600L);
        when(jwtTokenUtil.isTokenExpired(testToken)).thenReturn(false);
        when(tokenBlacklistService.isTokenBlacklisted(testToken)).thenReturn(false);
        
        // Mock API密钥认证成功
        String apiKeyInfo = "{\"appId\":\"app-001\",\"appName\":\"Test App\",\"companyName\":\"Test Company\",\"permissions\":[\"admin\"],\"status\":\"active\",\"expirationTime\":\"2024-12-31T23:59:59\"}";
        when(valueOperations.get("api_key:" + testApiKey)).thenReturn(apiKeyInfo);
        
        // 执行认证
        AuthenticationResult result = multiAuthService.authenticate(request);
        
        // 验证结果
        assertTrue(result.isSuccess());
        assertEquals("MIXED", result.getAuthType());
        assertEquals(testUserId, result.getUserId());
        assertEquals("app-001", result.getAppId());
        // 权限应该是合并后的结果
        assertTrue(result.getPermissions().contains("read"));
        assertTrue(result.getPermissions().contains("write"));
        assertTrue(result.getPermissions().contains("admin"));
    }

    @Test
    void testQuickAuthenticateWithValidToken() {
        // Mock JWT工具类行为
        when(jwtTokenUtil.validateToken(testToken)).thenReturn(true);
        when(jwtTokenUtil.getUserIdFromToken(testToken)).thenReturn(testUserId);
        when(jwtTokenUtil.isTokenExpired(testToken)).thenReturn(false);
        when(tokenBlacklistService.isTokenBlacklisted(testToken)).thenReturn(false);
        
        // 执行快速认证
        boolean result = multiAuthService.quickAuthenticate(testToken);
        
        // 验证结果
        assertTrue(result);
    }

    @Test
    void testQuickAuthenticateWithInvalidToken() {
        // Mock JWT工具类行为（Token无效）
        when(jwtTokenUtil.validateToken(testToken)).thenThrow(new RuntimeException("Invalid token"));
        
        // 执行快速认证
        boolean result = multiAuthService.quickAuthenticate(testToken);
        
        // 验证结果
        assertFalse(result);
    }

    @Test
    void testCheckPermission() {
        // 准备测试数据
        List<String> userPermissions = Arrays.asList("read", "write", "delete");
        
        // 测试权限检查
        assertTrue(multiAuthService.checkPermission(userPermissions, "read"));
        assertTrue(multiAuthService.checkPermission(userPermissions, "write"));
        assertTrue(multiAuthService.checkPermission(userPermissions, "delete"));
        assertFalse(multiAuthService.checkPermission(userPermissions, "admin"));
        assertFalse(multiAuthService.checkPermission(userPermissions, "execute"));
    }

    @Test
    void testCheckApiPermission() {
        // 准备测试数据
        String endpoint = "/api/users";
        String method = "GET";
        
        // Mock权限缓存
        String permissionKey = "api_permission:" + endpoint + ":" + method;
        String requiredPermission = "user:read";
        when(valueOperations.get(permissionKey)).thenReturn(requiredPermission);
        
        List<String> userPermissions = Arrays.asList("user:read", "user:write");
        
        // 执行API权限检查
        boolean result = multiAuthService.checkApiPermission(userPermissions, endpoint, method);
        
        // 验证结果
        assertTrue(result);
    }

    @Test
    void testCheckApiPermissionWithoutRequiredPermission() {
        // 准备测试数据
        String endpoint = "/api/public";
        String method = "GET";
        
        // Mock权限缓存（无需权限）
        String permissionKey = "api_permission:" + endpoint + ":" + method;
        when(valueOperations.get(permissionKey)).thenReturn(null);
        
        List<String> userPermissions = Arrays.asList("user:read");
        
        // 执行API权限检查
        boolean result = multiAuthService.checkApiPermission(userPermissions, endpoint, method);
        
        // 验证结果（无需权限的接口应该返回true）
        assertTrue(result);
    }

    @Test
    void testGetAuthStatistics() {
        // Mock统计数据
        when(valueOperations.get("auth_stats:total_requests")).thenReturn(1000L);
        when(valueOperations.get("auth_stats:success_requests")).thenReturn(950L);
        when(valueOperations.get("auth_stats:failed_requests")).thenReturn(50L);
        when(valueOperations.get("auth_stats:jwt_requests")).thenReturn(600L);
        when(valueOperations.get("auth_stats:apikey_requests")).thenReturn(300L);
        when(valueOperations.get("auth_stats:mixed_requests")).thenReturn(100L);
        
        // 执行获取统计信息
        var statistics = multiAuthService.getAuthStatistics();
        
        // 验证结果
        assertNotNull(statistics);
        assertEquals(1000L, statistics.get("totalRequests"));
        assertEquals(950L, statistics.get("successRequests"));
        assertEquals(50L, statistics.get("failedRequests"));
        assertEquals(600L, statistics.get("jwtRequests"));
        assertEquals(300L, statistics.get("apikeyRequests"));
        assertEquals(100L, statistics.get("mixedRequests"));
        assertEquals(95.0, statistics.get("successRate"));
    }

    @Test
    void testDetermineAuthType() {
        // 测试自动模式
        AuthenticationRequest request1 = new AuthenticationRequest();
        request1.setJwtToken(testToken);
        request1.setAuthMode("auto");
        assertEquals("jwt", multiAuthService.determineAuthType(request1));
        
        AuthenticationRequest request2 = new AuthenticationRequest();
        request2.setApiKey(testApiKey);
        request2.setAuthMode("auto");
        assertEquals("apikey", multiAuthService.determineAuthType(request2));
        
        AuthenticationRequest request3 = new AuthenticationRequest();
        request3.setJwtToken(testToken);
        request3.setApiKey(testApiKey);
        request3.setAuthMode("auto");
        assertEquals("mixed", multiAuthService.determineAuthType(request3));
        
        // 测试指定模式
        AuthenticationRequest request4 = new AuthenticationRequest();
        request4.setAuthMode("jwt");
        assertEquals("jwt", multiAuthService.determineAuthType(request4));
    }

    @Test
    void testAuthenticateWithEmptyRequest() {
        // 准备空的认证请求
        AuthenticationRequest request = new AuthenticationRequest();
        request.setAuthMode("auto");
        
        // 执行认证
        AuthenticationResult result = multiAuthService.authenticate(request);
        
        // 验证结果
        assertFalse(result.isSuccess());
        assertEquals("未提供有效的认证信息", result.getErrorMessage());
    }
}