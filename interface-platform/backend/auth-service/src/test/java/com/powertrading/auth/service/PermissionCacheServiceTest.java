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

import java.util.*;
import java.util.concurrent.TimeUnit;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

/**
 * 权限缓存服务测试
 *
 * @author PowerTrading Team
 * @since 2024-01-15
 */
@ExtendWith(MockitoExtension.class)
class PermissionCacheServiceTest {

    @Mock
    private RedisTemplate<String, Object> redisTemplate;
    
    @Mock
    private ValueOperations<String, Object> valueOperations;
    
    @Mock
    private SetOperations<String, Object> setOperations;
    
    @InjectMocks
    private PermissionCacheService permissionCacheService;
    
    private final String testUserId = "user-001";
    private final String testRoleId = "role-001";
    private final String testEndpoint = "/api/users";
    private final String testMethod = "GET";
    private final List<String> testPermissions = Arrays.asList("user:read", "user:write", "user:delete");
    private final List<String> testRoles = Arrays.asList("admin", "user", "manager");

    @BeforeEach
    void setUp() {
        when(redisTemplate.opsForValue()).thenReturn(valueOperations);
        when(redisTemplate.opsForSet()).thenReturn(setOperations);
    }

    @Test
    void testCacheUserPermissions() {
        // 执行缓存用户权限
        permissionCacheService.cacheUserPermissions(testUserId, testPermissions);
        
        // 验证Redis操作
        String permissionKey = "user_permissions:" + testUserId;
        verify(valueOperations).set(permissionKey, testPermissions, 30, TimeUnit.MINUTES);
    }

    @Test
    void testGetUserPermissions() {
        // Mock缓存数据
        String permissionKey = "user_permissions:" + testUserId;
        when(valueOperations.get(permissionKey)).thenReturn(testPermissions);
        
        // 执行获取用户权限
        List<String> permissions = permissionCacheService.getUserPermissions(testUserId);
        
        // 验证结果
        assertEquals(testPermissions, permissions);
    }

    @Test
    void testGetUserPermissionsNotCached() {
        // Mock缓存未命中
        String permissionKey = "user_permissions:" + testUserId;
        when(valueOperations.get(permissionKey)).thenReturn(null);
        
        // 执行获取用户权限
        List<String> permissions = permissionCacheService.getUserPermissions(testUserId);
        
        // 验证结果
        assertTrue(permissions.isEmpty());
    }

    @Test
    void testCacheUserRoles() {
        // 执行缓存用户角色
        permissionCacheService.cacheUserRoles(testUserId, testRoles);
        
        // 验证Redis操作
        String roleKey = "user_roles:" + testUserId;
        verify(valueOperations).set(roleKey, testRoles, 30, TimeUnit.MINUTES);
    }

    @Test
    void testGetUserRoles() {
        // Mock缓存数据
        String roleKey = "user_roles:" + testUserId;
        when(valueOperations.get(roleKey)).thenReturn(testRoles);
        
        // 执行获取用户角色
        List<String> roles = permissionCacheService.getUserRoles(testUserId);
        
        // 验证结果
        assertEquals(testRoles, roles);
    }

    @Test
    void testGetUserRolesNotCached() {
        // Mock缓存未命中
        String roleKey = "user_roles:" + testUserId;
        when(valueOperations.get(roleKey)).thenReturn(null);
        
        // 执行获取用户角色
        List<String> roles = permissionCacheService.getUserRoles(testUserId);
        
        // 验证结果
        assertTrue(roles.isEmpty());
    }

    @Test
    void testCacheRolePermissions() {
        // 执行缓存角色权限
        permissionCacheService.cacheRolePermissions(testRoleId, testPermissions);
        
        // 验证Redis操作
        String permissionKey = "role_permissions:" + testRoleId;
        verify(valueOperations).set(permissionKey, testPermissions, 60, TimeUnit.MINUTES);
    }

    @Test
    void testGetRolePermissions() {
        // Mock缓存数据
        String permissionKey = "role_permissions:" + testRoleId;
        when(valueOperations.get(permissionKey)).thenReturn(testPermissions);
        
        // 执行获取角色权限
        List<String> permissions = permissionCacheService.getRolePermissions(testRoleId);
        
        // 验证结果
        assertEquals(testPermissions, permissions);
    }

    @Test
    void testCacheApiPermission() {
        String requiredPermission = "user:read";
        
        // 执行缓存API权限
        permissionCacheService.cacheApiPermission(testEndpoint, testMethod, requiredPermission);
        
        // 验证Redis操作
        String apiKey = "api_permission:" + testEndpoint + ":" + testMethod;
        verify(valueOperations).set(apiKey, requiredPermission, 120, TimeUnit.MINUTES);
    }

    @Test
    void testGetApiPermission() {
        String requiredPermission = "user:read";
        
        // Mock缓存数据
        String apiKey = "api_permission:" + testEndpoint + ":" + testMethod;
        when(valueOperations.get(apiKey)).thenReturn(requiredPermission);
        
        // 执行获取API权限
        String permission = permissionCacheService.getApiPermission(testEndpoint, testMethod);
        
        // 验证结果
        assertEquals(requiredPermission, permission);
    }

    @Test
    void testGetApiPermissionNotCached() {
        // Mock缓存未命中
        String apiKey = "api_permission:" + testEndpoint + ":" + testMethod;
        when(valueOperations.get(apiKey)).thenReturn(null);
        
        // 执行获取API权限
        String permission = permissionCacheService.getApiPermission(testEndpoint, testMethod);
        
        // 验证结果
        assertNull(permission);
    }

    @Test
    void testCheckUserPermission() {
        String targetPermission = "user:read";
        
        // Mock用户权限缓存
        String permissionKey = "user_permissions:" + testUserId;
        when(valueOperations.get(permissionKey)).thenReturn(testPermissions);
        
        // 执行权限检查
        boolean hasPermission = permissionCacheService.checkUserPermission(testUserId, targetPermission);
        
        // 验证结果
        assertTrue(hasPermission);
    }

    @Test
    void testCheckUserPermissionNotFound() {
        String targetPermission = "admin:delete";
        
        // Mock用户权限缓存
        String permissionKey = "user_permissions:" + testUserId;
        when(valueOperations.get(permissionKey)).thenReturn(testPermissions);
        
        // 执行权限检查
        boolean hasPermission = permissionCacheService.checkUserPermission(testUserId, targetPermission);
        
        // 验证结果
        assertFalse(hasPermission);
    }

    @Test
    void testCheckUserRole() {
        String targetRole = "admin";
        
        // Mock用户角色缓存
        String roleKey = "user_roles:" + testUserId;
        when(valueOperations.get(roleKey)).thenReturn(testRoles);
        
        // 执行角色检查
        boolean hasRole = permissionCacheService.checkUserRole(testUserId, targetRole);
        
        // 验证结果
        assertTrue(hasRole);
    }

    @Test
    void testCheckUserRoleNotFound() {
        String targetRole = "superadmin";
        
        // Mock用户角色缓存
        String roleKey = "user_roles:" + testUserId;
        when(valueOperations.get(roleKey)).thenReturn(testRoles);
        
        // 执行角色检查
        boolean hasRole = permissionCacheService.checkUserRole(testUserId, targetRole);
        
        // 验证结果
        assertFalse(hasRole);
    }

    @Test
    void testRefreshUserPermissions() {
        List<String> newPermissions = Arrays.asList("user:read", "user:update");
        
        // 执行刷新用户权限
        permissionCacheService.refreshUserPermissions(testUserId, newPermissions);
        
        // 验证Redis操作
        String permissionKey = "user_permissions:" + testUserId;
        verify(valueOperations).set(permissionKey, newPermissions, 30, TimeUnit.MINUTES);
    }

    @Test
    void testRefreshUserRoles() {
        List<String> newRoles = Arrays.asList("user", "editor");
        
        // 执行刷新用户角色
        permissionCacheService.refreshUserRoles(testUserId, newRoles);
        
        // 验证Redis操作
        String roleKey = "user_roles:" + testUserId;
        verify(valueOperations).set(roleKey, newRoles, 30, TimeUnit.MINUTES);
    }

    @Test
    void testClearUserCache() {
        // 执行清除用户缓存
        permissionCacheService.clearUserCache(testUserId);
        
        // 验证Redis操作
        String permissionKey = "user_permissions:" + testUserId;
        String roleKey = "user_roles:" + testUserId;
        
        verify(redisTemplate).delete(permissionKey);
        verify(redisTemplate).delete(roleKey);
    }

    @Test
    void testClearRoleCache() {
        // 执行清除角色缓存
        permissionCacheService.clearRoleCache(testRoleId);
        
        // 验证Redis操作
        String permissionKey = "role_permissions:" + testRoleId;
        verify(redisTemplate).delete(permissionKey);
    }

    @Test
    void testClearApiCache() {
        // 执行清除API缓存
        permissionCacheService.clearApiCache(testEndpoint, testMethod);
        
        // 验证Redis操作
        String apiKey = "api_permission:" + testEndpoint + ":" + testMethod;
        verify(redisTemplate).delete(apiKey);
    }

    @Test
    void testClearAllCache() {
        // Mock Redis keys操作
        Set<String> mockKeys = Set.of(
                "user_permissions:user-001",
                "user_roles:user-001",
                "role_permissions:role-001",
                "api_permission:/api/users:GET"
        );
        when(redisTemplate.keys("user_permissions:*")).thenReturn(mockKeys);
        when(redisTemplate.keys("user_roles:*")).thenReturn(mockKeys);
        when(redisTemplate.keys("role_permissions:*")).thenReturn(mockKeys);
        when(redisTemplate.keys("api_permission:*")).thenReturn(mockKeys);
        
        // 执行清除所有缓存
        permissionCacheService.clearAllCache();
        
        // 验证Redis操作
        verify(redisTemplate, times(4)).keys(anyString());
        verify(redisTemplate, times(4)).delete(any(Collection.class));
    }

    @Test
    void testGetCacheStatistics() {
        // Mock统计数据
        when(valueOperations.get("cache_stats:user_permissions_hits")).thenReturn(1000L);
        when(valueOperations.get("cache_stats:user_permissions_misses")).thenReturn(100L);
        when(valueOperations.get("cache_stats:user_roles_hits")).thenReturn(800L);
        when(valueOperations.get("cache_stats:user_roles_misses")).thenReturn(80L);
        when(valueOperations.get("cache_stats:role_permissions_hits")).thenReturn(500L);
        when(valueOperations.get("cache_stats:role_permissions_misses")).thenReturn(50L);
        when(valueOperations.get("cache_stats:api_permissions_hits")).thenReturn(2000L);
        when(valueOperations.get("cache_stats:api_permissions_misses")).thenReturn(200L);
        
        // 执行获取缓存统计
        Map<String, Object> statistics = permissionCacheService.getCacheStatistics();
        
        // 验证结果
        assertNotNull(statistics);
        assertEquals(1000L, statistics.get("userPermissionsHits"));
        assertEquals(100L, statistics.get("userPermissionsMisses"));
        assertEquals(800L, statistics.get("userRolesHits"));
        assertEquals(80L, statistics.get("userRolesMisses"));
        assertEquals(500L, statistics.get("rolePermissionsHits"));
        assertEquals(50L, statistics.get("rolePermissionsMisses"));
        assertEquals(2000L, statistics.get("apiPermissionsHits"));
        assertEquals(200L, statistics.get("apiPermissionsMisses"));
        
        // 验证命中率计算
        assertEquals(90.91, (Double) statistics.get("userPermissionsHitRate"), 0.01);
        assertEquals(90.91, (Double) statistics.get("userRolesHitRate"), 0.01);
        assertEquals(90.91, (Double) statistics.get("rolePermissionsHitRate"), 0.01);
        assertEquals(90.91, (Double) statistics.get("apiPermissionsHitRate"), 0.01);
        assertEquals(90.91, (Double) statistics.get("overallHitRate"), 0.01);
    }

    @Test
    void testPrewarmCache() {
        // Mock预热数据
        Map<String, List<String>> userPermissions = Map.of(
                "user-001", Arrays.asList("user:read", "user:write"),
                "user-002", Arrays.asList("admin:read", "admin:write")
        );
        
        Map<String, List<String>> userRoles = Map.of(
                "user-001", Arrays.asList("user"),
                "user-002", Arrays.asList("admin")
        );
        
        Map<String, List<String>> rolePermissions = Map.of(
                "user", Arrays.asList("user:read"),
                "admin", Arrays.asList("admin:read", "admin:write")
        );
        
        Map<String, String> apiPermissions = Map.of(
                "/api/users:GET", "user:read",
                "/api/admin:GET", "admin:read"
        );
        
        // 执行缓存预热
        permissionCacheService.prewarmCache(userPermissions, userRoles, rolePermissions, apiPermissions);
        
        // 验证Redis操作
        verify(valueOperations, times(2)).set(startsWith("user_permissions:"), any(), eq(30L), eq(TimeUnit.MINUTES));
        verify(valueOperations, times(2)).set(startsWith("user_roles:"), any(), eq(30L), eq(TimeUnit.MINUTES));
        verify(valueOperations, times(2)).set(startsWith("role_permissions:"), any(), eq(60L), eq(TimeUnit.MINUTES));
        verify(valueOperations, times(2)).set(startsWith("api_permission:"), any(), eq(120L), eq(TimeUnit.MINUTES));
    }

    @Test
    void testRefreshCacheScheduled() {
        // 这个测试主要验证定时刷新方法不会抛出异常
        assertDoesNotThrow(() -> {
            permissionCacheService.refreshCacheScheduled();
        });
    }

    @Test
    void testRecordCacheHit() {
        String cacheType = "user_permissions";
        
        // 执行记录缓存命中
        permissionCacheService.recordCacheHit(cacheType);
        
        // 验证Redis操作
        String hitKey = "cache_stats:" + cacheType + "_hits";
        verify(valueOperations).increment(hitKey);
    }

    @Test
    void testRecordCacheMiss() {
        String cacheType = "user_permissions";
        
        // 执行记录缓存未命中
        permissionCacheService.recordCacheMiss(cacheType);
        
        // 验证Redis操作
        String missKey = "cache_stats:" + cacheType + "_misses";
        verify(valueOperations).increment(missKey);
    }

    @Test
    void testGetUserPermissionsWithCacheHit() {
        // Mock缓存命中
        String permissionKey = "user_permissions:" + testUserId;
        when(valueOperations.get(permissionKey)).thenReturn(testPermissions);
        
        // 执行获取用户权限
        List<String> permissions = permissionCacheService.getUserPermissions(testUserId);
        
        // 验证结果和缓存统计
        assertEquals(testPermissions, permissions);
        verify(valueOperations).increment("cache_stats:user_permissions_hits");
    }

    @Test
    void testGetUserPermissionsWithCacheMiss() {
        // Mock缓存未命中
        String permissionKey = "user_permissions:" + testUserId;
        when(valueOperations.get(permissionKey)).thenReturn(null);
        
        // 执行获取用户权限
        List<String> permissions = permissionCacheService.getUserPermissions(testUserId);
        
        // 验证结果和缓存统计
        assertTrue(permissions.isEmpty());
        verify(valueOperations).increment("cache_stats:user_permissions_misses");
    }
}