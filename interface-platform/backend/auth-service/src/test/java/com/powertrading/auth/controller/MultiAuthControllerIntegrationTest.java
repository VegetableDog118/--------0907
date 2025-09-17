package com.powertrading.auth.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.powertrading.auth.dto.AuthenticationRequest;
import com.powertrading.auth.dto.AuthenticationResult;
import com.powertrading.auth.service.AuditLogService;
import com.powertrading.auth.service.MultiAuthService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

/**
 * 多重认证控制器集成测试
 *
 * @author PowerTrading Team
 * @since 2024-01-15
 */
@WebMvcTest(MultiAuthController.class)
class MultiAuthControllerIntegrationTest {

    @Autowired
    private MockMvc mockMvc;
    
    @Autowired
    private ObjectMapper objectMapper;
    
    @MockBean
    private MultiAuthService multiAuthService;
    
    @MockBean
    private AuditLogService auditLogService;
    
    private final String testToken = "test.jwt.token";
    private final String testApiKey = "test-api-key-123";
    private final String testUserId = "user-001";
    private final String testUsername = "testuser";
    private final String testCompanyName = "Test Company";
    private final List<String> testRoles = Arrays.asList("user", "admin");
    private final List<String> testPermissions = Arrays.asList("read", "write");

    @BeforeEach
    void setUp() {
        // 通用Mock设置
    }

    @Test
    void testAuthenticateWithValidJwtToken() throws Exception {
        // 准备测试数据
        AuthenticationRequest request = new AuthenticationRequest();
        request.setJwtToken(testToken);
        request.setAuthMode("jwt");
        request.setClientIp("192.168.1.100");
        request.setUserAgent("Test-Agent/1.0");
        
        AuthenticationResult result = AuthenticationResult.success(
                testUserId, testUsername, testCompanyName, testRoles, testPermissions,
                "JWT", LocalDateTime.now().plusHours(1), 3600L
        );
        
        // Mock服务行为
        when(multiAuthService.authenticate(any(AuthenticationRequest.class))).thenReturn(result);
        
        // 执行请求
        mockMvc.perform(post("/api/auth/authenticate")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request))
                        .header("X-Forwarded-For", "192.168.1.100")
                        .header("User-Agent", "Test-Agent/1.0"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.userId").value(testUserId))
                .andExpect(jsonPath("$.username").value(testUsername))
                .andExpect(jsonPath("$.companyName").value(testCompanyName))
                .andExpect(jsonPath("$.authType").value("JWT"))
                .andExpect(jsonPath("$.roles").isArray())
                .andExpect(jsonPath("$.permissions").isArray());
        
        // 验证服务调用
        verify(multiAuthService).authenticate(any(AuthenticationRequest.class));
        verify(auditLogService).recordAuthSuccess(eq(testUserId), eq(testUsername), eq("JWT"), anyString(), anyString());
    }

    @Test
    void testAuthenticateWithInvalidToken() throws Exception {
        // 准备测试数据
        AuthenticationRequest request = new AuthenticationRequest();
        request.setJwtToken("invalid.token");
        request.setAuthMode("jwt");
        
        AuthenticationResult result = AuthenticationResult.failure("JWT Token验证失败");
        
        // Mock服务行为
        when(multiAuthService.authenticate(any(AuthenticationRequest.class))).thenReturn(result);
        
        // 执行请求
        mockMvc.perform(post("/api/auth/authenticate")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.success").value(false))
                .andExpect(jsonPath("$.errorMessage").value("JWT Token验证失败"));
        
        // 验证服务调用
        verify(multiAuthService).authenticate(any(AuthenticationRequest.class));
        verify(auditLogService).recordAuthFailure(isNull(), eq("JWT Token验证失败"), anyString(), anyString());
    }

    @Test
    void testAuthenticateWithValidApiKey() throws Exception {
        // 准备测试数据
        AuthenticationRequest request = new AuthenticationRequest();
        request.setApiKey(testApiKey);
        request.setAuthMode("apikey");
        
        AuthenticationResult result = AuthenticationResult.success(
                "app-001", "Test App", testCompanyName, Arrays.asList(), testPermissions,
                "API_KEY", LocalDateTime.now().plusDays(30), 2592000L
        );
        result.setAppId("app-001");
        result.setAppName("Test App");
        
        // Mock服务行为
        when(multiAuthService.authenticate(any(AuthenticationRequest.class))).thenReturn(result);
        
        // 执行请求
        mockMvc.perform(post("/api/auth/authenticate")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.appId").value("app-001"))
                .andExpect(jsonPath("$.appName").value("Test App"))
                .andExpect(jsonPath("$.authType").value("API_KEY"));
        
        // 验证服务调用
        verify(multiAuthService).authenticate(any(AuthenticationRequest.class));
        verify(auditLogService).recordAuthSuccess(eq("app-001"), eq("Test App"), eq("API_KEY"), anyString(), anyString());
    }

    @Test
    void testQuickAuthenticate() throws Exception {
        // Mock服务行为
        when(multiAuthService.quickAuthenticate(testToken)).thenReturn(true);
        
        // 执行请求
        mockMvc.perform(get("/api/auth/quick")
                        .param("token", testToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.valid").value(true));
        
        // 验证服务调用
        verify(multiAuthService).quickAuthenticate(testToken);
    }

    @Test
    void testQuickAuthenticateInvalid() throws Exception {
        // Mock服务行为
        when(multiAuthService.quickAuthenticate("invalid.token")).thenReturn(false);
        
        // 执行请求
        mockMvc.perform(get("/api/auth/quick")
                        .param("token", "invalid.token"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.valid").value(false));
        
        // 验证服务调用
        verify(multiAuthService).quickAuthenticate("invalid.token");
    }

    @Test
    void testCheckPermission() throws Exception {
        // 准备测试数据
        List<String> userPermissions = Arrays.asList("user:read", "user:write");
        String requiredPermission = "user:read";
        
        // Mock服务行为
        when(multiAuthService.checkPermission(userPermissions, requiredPermission)).thenReturn(true);
        
        // 执行请求
        mockMvc.perform(post("/api/auth/check-permission")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(Map.of(
                                "permissions", userPermissions,
                                "requiredPermission", requiredPermission
                        ))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.hasPermission").value(true));
        
        // 验证服务调用
        verify(multiAuthService).checkPermission(userPermissions, requiredPermission);
    }

    @Test
    void testCheckApiPermission() throws Exception {
        // 准备测试数据
        List<String> userPermissions = Arrays.asList("user:read", "user:write");
        String endpoint = "/api/users";
        String method = "GET";
        
        // Mock服务行为
        when(multiAuthService.checkApiPermission(userPermissions, endpoint, method)).thenReturn(true);
        
        // 执行请求
        mockMvc.perform(post("/api/auth/check-api-permission")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(Map.of(
                                "permissions", userPermissions,
                                "endpoint", endpoint,
                                "method", method
                        ))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.hasPermission").value(true));
        
        // 验证服务调用
        verify(multiAuthService).checkApiPermission(userPermissions, endpoint, method);
    }

    @Test
    void testGetAuthStatistics() throws Exception {
        // 准备测试数据
        Map<String, Object> statistics = new HashMap<>();
        statistics.put("totalRequests", 1000L);
        statistics.put("successRequests", 950L);
        statistics.put("failedRequests", 50L);
        statistics.put("successRate", 95.0);
        
        // Mock服务行为
        when(multiAuthService.getAuthStatistics()).thenReturn(statistics);
        
        // 执行请求
        mockMvc.perform(get("/api/auth/statistics"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.totalRequests").value(1000))
                .andExpect(jsonPath("$.successRequests").value(950))
                .andExpect(jsonPath("$.failedRequests").value(50))
                .andExpect(jsonPath("$.successRate").value(95.0));
        
        // 验证服务调用
        verify(multiAuthService).getAuthStatistics();
    }

    @Test
    void testGetUserAuditLogs() throws Exception {
        // 准备测试数据
        List<Map<String, Object>> auditLogs = Arrays.asList(
                Map.of(
                        "timestamp", "2024-01-15T10:00:00",
                        "action", "LOGIN_SUCCESS",
                        "userId", testUserId,
                        "clientIp", "192.168.1.100"
                ),
                Map.of(
                        "timestamp", "2024-01-15T11:00:00",
                        "action", "API_CALL",
                        "userId", testUserId,
                        "endpoint", "/api/users"
                )
        );
        
        // Mock服务行为
        when(auditLogService.getUserAuditLogs(testUserId, 10)).thenReturn(auditLogs);
        
        // 执行请求
        mockMvc.perform(get("/api/auth/audit-logs/{userId}", testUserId)
                        .param("limit", "10"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$").isArray())
                .andExpect(jsonPath("$[0].action").value("LOGIN_SUCCESS"))
                .andExpect(jsonPath("$[1].action").value("API_CALL"));
        
        // 验证服务调用
        verify(auditLogService).getUserAuditLogs(testUserId, 10);
    }

    @Test
    void testGetDailyStatistics() throws Exception {
        // 准备测试数据
        Map<String, Object> dailyStats = Map.of(
                "date", "2024-01-15",
                "totalLogins", 100L,
                "successfulLogins", 95L,
                "failedLogins", 5L,
                "uniqueUsers", 80L
        );
        
        // Mock服务行为
        when(auditLogService.getDailyStatistics("2024-01-15")).thenReturn(dailyStats);
        
        // 执行请求
        mockMvc.perform(get("/api/auth/daily-statistics")
                        .param("date", "2024-01-15"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.date").value("2024-01-15"))
                .andExpect(jsonPath("$.totalLogins").value(100))
                .andExpect(jsonPath("$.successfulLogins").value(95))
                .andExpect(jsonPath("$.failedLogins").value(5))
                .andExpect(jsonPath("$.uniqueUsers").value(80));
        
        // 验证服务调用
        verify(auditLogService).getDailyStatistics("2024-01-15");
    }

    @Test
    void testHealthCheck() throws Exception {
        // 执行请求
        mockMvc.perform(get("/api/auth/health"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("UP"))
                .andExpect(jsonPath("$.service").value("Multi-Auth Service"))
                .andExpect(jsonPath("$.timestamp").exists());
    }

    @Test
    void testAuthenticateWithMissingToken() throws Exception {
        // 准备测试数据（缺少Token）
        AuthenticationRequest request = new AuthenticationRequest();
        request.setAuthMode("jwt");
        
        AuthenticationResult result = AuthenticationResult.failure("未提供有效的认证信息");
        
        // Mock服务行为
        when(multiAuthService.authenticate(any(AuthenticationRequest.class))).thenReturn(result);
        
        // 执行请求
        mockMvc.perform(post("/api/auth/authenticate")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.success").value(false))
                .andExpect(jsonPath("$.errorMessage").value("未提供有效的认证信息"));
    }

    @Test
    void testAuthenticateWithInvalidRequestBody() throws Exception {
        // 执行请求（无效的JSON）
        mockMvc.perform(post("/api/auth/authenticate")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("invalid json"))
                .andExpect(status().isBadRequest());
    }

    @Test
    void testQuickAuthenticateWithMissingToken() throws Exception {
        // 执行请求（缺少token参数）
        mockMvc.perform(get("/api/auth/quick"))
                .andExpect(status().isBadRequest());
    }

    @Test
    void testCheckPermissionWithInvalidRequest() throws Exception {
        // 执行请求（缺少必要参数）
        mockMvc.perform(post("/api/auth/check-permission")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(Map.of(
                                "permissions", Arrays.asList("user:read")
                                // 缺少 requiredPermission
                        ))))
                .andExpect(status().isBadRequest());
    }

    @Test
    void testGetUserAuditLogsWithInvalidUserId() throws Exception {
        // Mock服务行为（用户不存在）
        when(auditLogService.getUserAuditLogs("invalid-user", 10))
                .thenThrow(new RuntimeException("用户不存在"));
        
        // 执行请求
        mockMvc.perform(get("/api/auth/audit-logs/{userId}", "invalid-user")
                        .param("limit", "10"))
                .andExpect(status().isNotFound());
    }

    @Test
    void testAuthenticateWithMixedMode() throws Exception {
        // 准备测试数据
        AuthenticationRequest request = new AuthenticationRequest();
        request.setJwtToken(testToken);
        request.setApiKey(testApiKey);
        request.setAuthMode("mixed");
        
        AuthenticationResult result = AuthenticationResult.success(
                testUserId, testUsername, testCompanyName, testRoles, testPermissions,
                "MIXED", LocalDateTime.now().plusHours(1), 3600L
        );
        result.setAppId("app-001");
        result.setAppName("Test App");
        
        // Mock服务行为
        when(multiAuthService.authenticate(any(AuthenticationRequest.class))).thenReturn(result);
        
        // 执行请求
        mockMvc.perform(post("/api/auth/authenticate")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.authType").value("MIXED"))
                .andExpect(jsonPath("$.userId").value(testUserId))
                .andExpect(jsonPath("$.appId").value("app-001"));
        
        // 验证服务调用
        verify(multiAuthService).authenticate(any(AuthenticationRequest.class));
        verify(auditLogService).recordAuthSuccess(eq(testUserId), eq(testUsername), eq("MIXED"), anyString(), anyString());
    }
}