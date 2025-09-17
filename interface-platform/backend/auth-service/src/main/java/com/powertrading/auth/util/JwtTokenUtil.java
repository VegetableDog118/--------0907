package com.powertrading.auth.util;

import io.jsonwebtoken.*;
import io.jsonwebtoken.security.Keys;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import javax.crypto.SecretKey;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.Date;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * JWT Token工具类
 *
 * @author PowerTrading Team
 * @since 2024-01-15
 */
@Slf4j
@Component
public class JwtTokenUtil {

    @Value("${jwt.secret}")
    private String jwtSecret;

    @Value("${jwt.expiration}")
    private Long jwtExpiration;

    @Value("${jwt.refresh-expiration}")
    private Long jwtRefreshExpiration;

    @Value("${jwt.issuer}")
    private String jwtIssuer;

    /**
     * 生成访问Token
     */
    public String generateAccessToken(String userId, String username, String companyName,
                                    List<String> roles, List<String> permissions) {
        return generateToken(userId, username, companyName, roles, permissions, jwtExpiration);
    }

    /**
     * 生成刷新Token
     */
    public String generateRefreshToken(String userId) {
        SecretKey key = Keys.hmacShaKeyFor(jwtSecret.getBytes());
        Date now = new Date();
        Date expiration = new Date(now.getTime() + jwtRefreshExpiration * 1000);

        return Jwts.builder()
                .setId(UUID.randomUUID().toString())
                .setSubject(userId)
                .setIssuer(jwtIssuer)
                .setIssuedAt(now)
                .setExpiration(expiration)
                .claim("type", "refresh")
                .signWith(key, SignatureAlgorithm.HS256)
                .compact();
    }

    /**
     * 生成Token
     */
    private String generateToken(String userId, String username, String companyName,
                               List<String> roles, List<String> permissions, Long expiration) {
        SecretKey key = Keys.hmacShaKeyFor(jwtSecret.getBytes());
        Date now = new Date();
        Date expirationDate = new Date(now.getTime() + expiration * 1000);

        return Jwts.builder()
                .setId(UUID.randomUUID().toString())
                .setSubject(userId)
                .setIssuer(jwtIssuer)
                .setIssuedAt(now)
                .setExpiration(expirationDate)
                .claim("username", username)
                .claim("companyName", companyName)
                .claim("roles", roles)
                .claim("permissions", permissions)
                .claim("type", "access")
                .signWith(key, SignatureAlgorithm.HS256)
                .compact();
    }

    /**
     * 验证Token
     */
    public Claims validateToken(String token) {
        try {
            SecretKey key = Keys.hmacShaKeyFor(jwtSecret.getBytes());
            return Jwts.parserBuilder()
                    .setSigningKey(key)
                    .build()
                    .parseClaimsJws(token)
                    .getBody();
        } catch (ExpiredJwtException e) {
            log.warn("JWT Token已过期: {}", e.getMessage());
            throw new RuntimeException("Token已过期", e);
        } catch (UnsupportedJwtException e) {
            log.warn("不支持的JWT Token: {}", e.getMessage());
            throw new RuntimeException("不支持的Token格式", e);
        } catch (MalformedJwtException e) {
            log.warn("JWT Token格式错误: {}", e.getMessage());
            throw new RuntimeException("Token格式错误", e);
        } catch (SignatureException e) {
            log.warn("JWT Token签名验证失败: {}", e.getMessage());
            throw new RuntimeException("Token签名无效", e);
        } catch (IllegalArgumentException e) {
            log.warn("JWT Token参数错误: {}", e.getMessage());
            throw new RuntimeException("Token无效", e);
        }
    }

    /**
     * 从Token中获取用户ID
     */
    public String getUserIdFromToken(String token) {
        Claims claims = validateToken(token);
        return claims.getSubject();
    }

    /**
     * 从Token中获取用户名
     */
    public String getUsernameFromToken(String token) {
        Claims claims = validateToken(token);
        return claims.get("username", String.class);
    }

    /**
     * 从Token中获取公司名称
     */
    public String getCompanyNameFromToken(String token) {
        Claims claims = validateToken(token);
        return claims.get("companyName", String.class);
    }

    /**
     * 从Token中获取角色列表
     */
    @SuppressWarnings("unchecked")
    public List<String> getRolesFromToken(String token) {
        Claims claims = validateToken(token);
        return (List<String>) claims.get("roles");
    }

    /**
     * 从Token中获取权限列表
     */
    @SuppressWarnings("unchecked")
    public List<String> getPermissionsFromToken(String token) {
        Claims claims = validateToken(token);
        return (List<String>) claims.get("permissions");
    }

    /**
     * 获取Token过期时间
     */
    public LocalDateTime getExpirationFromToken(String token) {
        Claims claims = validateToken(token);
        return claims.getExpiration().toInstant()
                .atZone(ZoneId.systemDefault())
                .toLocalDateTime();
    }

    /**
     * 获取Token剩余有效时间(秒)
     */
    public Long getRemainingTimeFromToken(String token) {
        Claims claims = validateToken(token);
        Date expiration = claims.getExpiration();
        Date now = new Date();
        return (expiration.getTime() - now.getTime()) / 1000;
    }

    /**
     * 检查Token是否过期
     */
    public Boolean isTokenExpired(String token) {
        try {
            Claims claims = validateToken(token);
            return claims.getExpiration().before(new Date());
        } catch (Exception e) {
            return true;
        }
    }

    /**
     * 检查是否为刷新Token
     */
    public Boolean isRefreshToken(String token) {
        try {
            Claims claims = validateToken(token);
            return "refresh".equals(claims.get("type", String.class));
        } catch (Exception e) {
            return false;
        }
    }

    /**
     * 从刷新Token生成新的访问Token
     */
    public String refreshAccessToken(String refreshToken, String username, String companyName,
                                   List<String> roles, List<String> permissions) {
        if (!isRefreshToken(refreshToken)) {
            throw new RuntimeException("无效的刷新Token");
        }
        
        String userId = getUserIdFromToken(refreshToken);
        return generateAccessToken(userId, username, companyName, roles, permissions);
    }

    /**
     * 获取Token的JTI(JWT ID)
     */
    public String getJtiFromToken(String token) {
        Claims claims = validateToken(token);
        return claims.getId();
    }

    /**
     * 获取Token的签发时间
     */
    public LocalDateTime getIssuedAtFromToken(String token) {
        Claims claims = validateToken(token);
        return claims.getIssuedAt().toInstant()
                .atZone(ZoneId.systemDefault())
                .toLocalDateTime();
    }
}