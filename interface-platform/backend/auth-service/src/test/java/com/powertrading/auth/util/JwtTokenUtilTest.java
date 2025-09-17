package com.powertrading.auth.util;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;

import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

/**
 * JWT Token工具类测试
 *
 * @author PowerTrading Team
 * @since 2024-01-15
 */
class JwtTokenUtilTest {

    private JwtTokenUtil jwtTokenUtil;
    
    private final String testSecret = "testSecretKeyForJwtTokenUtilTest2024";
    private final Long testExpiration = 3600L; // 1小时
    private final Long testRefreshExpiration = 86400L; // 24小时
    private final String testIssuer = "test-issuer";
    
    private final String testUserId = "test-user-001";
    private final String testUsername = "testuser";
    private final String testCompanyName = "Test Company";
    private final List<String> testRoles = Arrays.asList("user", "admin");
    private final List<String> testPermissions = Arrays.asList("read", "write", "delete");

    @BeforeEach
    void setUp() {
        jwtTokenUtil = new JwtTokenUtil();
        
        // 使用反射设置私有字段
        ReflectionTestUtils.setField(jwtTokenUtil, "jwtSecret", testSecret);
        ReflectionTestUtils.setField(jwtTokenUtil, "jwtExpiration", testExpiration);
        ReflectionTestUtils.setField(jwtTokenUtil, "jwtRefreshExpiration", testRefreshExpiration);
        ReflectionTestUtils.setField(jwtTokenUtil, "jwtIssuer", testIssuer);
    }

    @Test
    void testGenerateAccessToken() {
        // 生成访问Token
        String token = jwtTokenUtil.generateAccessToken(
                testUserId, testUsername, testCompanyName, testRoles, testPermissions
        );
        
        // 验证Token不为空
        assertNotNull(token);
        assertFalse(token.isEmpty());
        
        // 验证Token格式（JWT格式应该有两个点分隔三部分）
        String[] parts = token.split("\\.");
        assertEquals(3, parts.length);
    }

    @Test
    void testGenerateRefreshToken() {
        // 生成刷新Token
        String refreshToken = jwtTokenUtil.generateRefreshToken(testUserId);
        
        // 验证Token不为空
        assertNotNull(refreshToken);
        assertFalse(refreshToken.isEmpty());
        
        // 验证Token格式
        String[] parts = refreshToken.split("\\.");
        assertEquals(3, parts.length);
        
        // 验证是否为刷新Token
        assertTrue(jwtTokenUtil.isRefreshToken(refreshToken));
    }

    @Test
    void testValidateToken() {
        // 生成Token
        String token = jwtTokenUtil.generateAccessToken(
                testUserId, testUsername, testCompanyName, testRoles, testPermissions
        );
        
        // 验证Token
        assertDoesNotThrow(() -> {
            jwtTokenUtil.validateToken(token);
        });
    }

    @Test
    void testGetUserIdFromToken() {
        // 生成Token
        String token = jwtTokenUtil.generateAccessToken(
                testUserId, testUsername, testCompanyName, testRoles, testPermissions
        );
        
        // 从Token中获取用户ID
        String userId = jwtTokenUtil.getUserIdFromToken(token);
        
        assertEquals(testUserId, userId);
    }

    @Test
    void testGetUsernameFromToken() {
        // 生成Token
        String token = jwtTokenUtil.generateAccessToken(
                testUserId, testUsername, testCompanyName, testRoles, testPermissions
        );
        
        // 从Token中获取用户名
        String username = jwtTokenUtil.getUsernameFromToken(token);
        
        assertEquals(testUsername, username);
    }

    @Test
    void testGetCompanyNameFromToken() {
        // 生成Token
        String token = jwtTokenUtil.generateAccessToken(
                testUserId, testUsername, testCompanyName, testRoles, testPermissions
        );
        
        // 从Token中获取公司名称
        String companyName = jwtTokenUtil.getCompanyNameFromToken(token);
        
        assertEquals(testCompanyName, companyName);
    }

    @Test
    void testGetRolesFromToken() {
        // 生成Token
        String token = jwtTokenUtil.generateAccessToken(
                testUserId, testUsername, testCompanyName, testRoles, testPermissions
        );
        
        // 从Token中获取角色列表
        List<String> roles = jwtTokenUtil.getRolesFromToken(token);
        
        assertEquals(testRoles, roles);
    }

    @Test
    void testGetPermissionsFromToken() {
        // 生成Token
        String token = jwtTokenUtil.generateAccessToken(
                testUserId, testUsername, testCompanyName, testRoles, testPermissions
        );
        
        // 从Token中获取权限列表
        List<String> permissions = jwtTokenUtil.getPermissionsFromToken(token);
        
        assertEquals(testPermissions, permissions);
    }

    @Test
    void testGetExpirationFromToken() {
        // 生成Token
        String token = jwtTokenUtil.generateAccessToken(
                testUserId, testUsername, testCompanyName, testRoles, testPermissions
        );
        
        // 从Token中获取过期时间
        LocalDateTime expiration = jwtTokenUtil.getExpirationFromToken(token);
        
        assertNotNull(expiration);
        assertTrue(expiration.isAfter(LocalDateTime.now()));
    }

    @Test
    void testGetRemainingTimeFromToken() {
        // 生成Token
        String token = jwtTokenUtil.generateAccessToken(
                testUserId, testUsername, testCompanyName, testRoles, testPermissions
        );
        
        // 从Token中获取剩余时间
        Long remainingTime = jwtTokenUtil.getRemainingTimeFromToken(token);
        
        assertNotNull(remainingTime);
        assertTrue(remainingTime > 0);
        assertTrue(remainingTime <= testExpiration);
    }

    @Test
    void testIsTokenExpired() {
        // 生成Token
        String token = jwtTokenUtil.generateAccessToken(
                testUserId, testUsername, testCompanyName, testRoles, testPermissions
        );
        
        // 检查Token是否过期（新生成的Token不应该过期）
        assertFalse(jwtTokenUtil.isTokenExpired(token));
    }

    @Test
    void testIsRefreshToken() {
        // 生成访问Token
        String accessToken = jwtTokenUtil.generateAccessToken(
                testUserId, testUsername, testCompanyName, testRoles, testPermissions
        );
        
        // 生成刷新Token
        String refreshToken = jwtTokenUtil.generateRefreshToken(testUserId);
        
        // 验证Token类型
        assertFalse(jwtTokenUtil.isRefreshToken(accessToken));
        assertTrue(jwtTokenUtil.isRefreshToken(refreshToken));
    }

    @Test
    void testRefreshAccessToken() {
        // 生成刷新Token
        String refreshToken = jwtTokenUtil.generateRefreshToken(testUserId);
        
        // 使用刷新Token生成新的访问Token
        String newAccessToken = jwtTokenUtil.refreshAccessToken(
                refreshToken, testUsername, testCompanyName, testRoles, testPermissions
        );
        
        // 验证新Token
        assertNotNull(newAccessToken);
        assertFalse(newAccessToken.isEmpty());
        assertFalse(jwtTokenUtil.isRefreshToken(newAccessToken));
        
        // 验证新Token中的信息
        assertEquals(testUserId, jwtTokenUtil.getUserIdFromToken(newAccessToken));
        assertEquals(testUsername, jwtTokenUtil.getUsernameFromToken(newAccessToken));
    }

    @Test
    void testGetJtiFromToken() {
        // 生成Token
        String token = jwtTokenUtil.generateAccessToken(
                testUserId, testUsername, testCompanyName, testRoles, testPermissions
        );
        
        // 获取JTI
        String jti = jwtTokenUtil.getJtiFromToken(token);
        
        assertNotNull(jti);
        assertFalse(jti.isEmpty());
    }

    @Test
    void testGetIssuedAtFromToken() {
        // 生成Token
        String token = jwtTokenUtil.generateAccessToken(
                testUserId, testUsername, testCompanyName, testRoles, testPermissions
        );
        
        // 获取签发时间
        LocalDateTime issuedAt = jwtTokenUtil.getIssuedAtFromToken(token);
        
        assertNotNull(issuedAt);
        assertTrue(issuedAt.isBefore(LocalDateTime.now().plusSeconds(1))); // 允许1秒误差
    }

    @Test
    void testValidateInvalidToken() {
        String invalidToken = "invalid.token.here";
        
        // 验证无效Token应该抛出异常
        assertThrows(RuntimeException.class, () -> {
            jwtTokenUtil.validateToken(invalidToken);
        });
    }

    @Test
    void testRefreshAccessTokenWithInvalidRefreshToken() {
        String invalidRefreshToken = "invalid.refresh.token";
        
        // 使用无效刷新Token应该抛出异常
        assertThrows(RuntimeException.class, () -> {
            jwtTokenUtil.refreshAccessToken(
                    invalidRefreshToken, testUsername, testCompanyName, testRoles, testPermissions
            );
        });
    }

    @Test
    void testRefreshAccessTokenWithAccessToken() {
        // 生成访问Token
        String accessToken = jwtTokenUtil.generateAccessToken(
                testUserId, testUsername, testCompanyName, testRoles, testPermissions
        );
        
        // 使用访问Token作为刷新Token应该抛出异常
        assertThrows(RuntimeException.class, () -> {
            jwtTokenUtil.refreshAccessToken(
                    accessToken, testUsername, testCompanyName, testRoles, testPermissions
            );
        });
    }
}