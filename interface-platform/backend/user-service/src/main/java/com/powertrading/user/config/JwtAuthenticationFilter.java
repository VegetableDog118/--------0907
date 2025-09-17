package com.powertrading.user.config;

import com.powertrading.user.dto.TokenInfo;
import com.powertrading.user.service.UserService;
import io.jsonwebtoken.ExpiredJwtException;
import io.jsonwebtoken.MalformedJwtException;
import io.jsonwebtoken.UnsupportedJwtException;
import io.jsonwebtoken.security.SignatureException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.web.authentication.WebAuthenticationDetailsSource;
import org.springframework.util.StringUtils;
import org.springframework.web.filter.OncePerRequestFilter;

import javax.servlet.FilterChain;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.util.List;
import java.util.stream.Collectors;

/**
 * JWT认证过滤器
 *
 * @author PowerTrading Team
 * @since 2024-01-15
 */
public class JwtAuthenticationFilter extends OncePerRequestFilter {

    private static final Logger logger = LoggerFactory.getLogger(JwtAuthenticationFilter.class);

    @Autowired
    private UserService userService;

    @Autowired
    private RedisTemplate<String, Object> redisTemplate;

    private static final String TOKEN_PREFIX = "Bearer ";
    private static final String HEADER_NAME = "Authorization";
    private static final String REDIS_TOKEN_BLACKLIST_PREFIX = "token:blacklist:";

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, 
                                  FilterChain filterChain) throws ServletException, IOException {
        
        try {
            String token = extractToken(request);
            
            if (StringUtils.hasText(token) && !isTokenBlacklisted(token)) {
                TokenInfo tokenInfo = userService.validateToken(token);
                
                if (tokenInfo != null) {
                    // 创建认证对象
                    List<SimpleGrantedAuthority> authorities = tokenInfo.getPermissions().stream()
                        .map(SimpleGrantedAuthority::new)
                        .collect(Collectors.toList());
                    
                    UsernamePasswordAuthenticationToken authentication = 
                        new UsernamePasswordAuthenticationToken(
                            tokenInfo.getUserId(), 
                            null, 
                            authorities
                        );
                    
                    authentication.setDetails(new WebAuthenticationDetailsSource().buildDetails(request));
                    
                    // 设置到安全上下文
                    SecurityContextHolder.getContext().setAuthentication(authentication);
                    
                    // 设置请求属性，供控制器使用
                    request.setAttribute("userId", tokenInfo.getUserId());
                    request.setAttribute("username", tokenInfo.getUsername());
                    request.setAttribute("companyName", tokenInfo.getCompanyName());
                    request.setAttribute("roles", tokenInfo.getRoles());
                    request.setAttribute("permissions", tokenInfo.getPermissions());
                    
                    // 如果只有一个角色，设置role属性
                    if (tokenInfo.getRoles() != null && !tokenInfo.getRoles().isEmpty()) {
                        request.setAttribute("role", tokenInfo.getRoles().get(0));
                    }
                    
                    logger.debug("JWT认证成功: userId={}, username={}", 
                        tokenInfo.getUserId(), tokenInfo.getUsername());
                }
            }
        } catch (ExpiredJwtException e) {
            logger.warn("JWT Token已过期: {}", e.getMessage());
            setErrorResponse(response, "TOKEN_EXPIRED", "Token已过期");
            return;
        } catch (UnsupportedJwtException e) {
            logger.warn("不支持的JWT Token: {}", e.getMessage());
            setErrorResponse(response, "TOKEN_UNSUPPORTED", "不支持的Token格式");
            return;
        } catch (MalformedJwtException e) {
            logger.warn("JWT Token格式错误: {}", e.getMessage());
            setErrorResponse(response, "TOKEN_MALFORMED", "Token格式错误");
            return;
        } catch (SignatureException e) {
            logger.warn("JWT Token签名验证失败: {}", e.getMessage());
            setErrorResponse(response, "TOKEN_SIGNATURE_INVALID", "Token签名无效");
            return;
        } catch (IllegalArgumentException e) {
            logger.warn("JWT Token参数错误: {}", e.getMessage());
            setErrorResponse(response, "TOKEN_INVALID", "Token无效");
            return;
        } catch (Exception e) {
            logger.error("JWT认证过程中发生异常: {}", e.getMessage(), e);
            setErrorResponse(response, "AUTHENTICATION_ERROR", "认证失败");
            return;
        }
        
        filterChain.doFilter(request, response);
    }

    /**
     * 从请求中提取Token
     */
    private String extractToken(HttpServletRequest request) {
        String bearerToken = request.getHeader(HEADER_NAME);
        if (StringUtils.hasText(bearerToken) && bearerToken.startsWith(TOKEN_PREFIX)) {
            return bearerToken.substring(TOKEN_PREFIX.length());
        }
        return null;
    }

    /**
     * 检查Token是否在黑名单中
     */
    private boolean isTokenBlacklisted(String token) {
        try {
            String blacklistKey = REDIS_TOKEN_BLACKLIST_PREFIX + token;
            return Boolean.TRUE.equals(redisTemplate.hasKey(blacklistKey));
        } catch (Exception e) {
            logger.warn("检查Token黑名单时发生异常: {}", e.getMessage());
            return false;
        }
    }

    /**
     * 设置错误响应
     */
    private void setErrorResponse(HttpServletResponse response, String errorCode, String errorMessage) 
            throws IOException {
        response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
        response.setContentType("application/json;charset=UTF-8");
        
        String jsonResponse = String.format(
            "{\"success\":false,\"errorCode\":\"%s\",\"message\":\"%s\",\"timestamp\":\"%s\"}",
            errorCode, errorMessage, java.time.LocalDateTime.now()
        );
        
        response.getWriter().write(jsonResponse);
    }

    /**
     * 判断是否跳过JWT认证
     */
    @Override
    protected boolean shouldNotFilter(HttpServletRequest request) throws ServletException {
        String path = request.getRequestURI();
        
        // 跳过公开接口 - 精确匹配或前缀匹配
        return path.equals("/user-service/api/v1/users/register") ||
               path.equals("/user-service/api/v1/users/login") ||
               path.equals("/user-service/api/v1/users/validate-token") ||
               path.equals("/api/v1/users/register") ||
               path.equals("/api/v1/users/login") ||
               path.equals("/api/v1/users/validate-token") ||
               // 内部服务调用接口
               path.startsWith("/user-service/api/v1/users/exists/") ||
               path.startsWith("/user-service/api/v1/users/role/") ||
               path.startsWith("/api/v1/users/exists/") ||
               path.startsWith("/api/v1/users/role/") ||
               path.startsWith("/user-service/swagger-ui") ||
               path.startsWith("/user-service/v3/api-docs") ||
               path.startsWith("/user-service/swagger-resources") ||
               path.startsWith("/user-service/webjars") ||
               path.startsWith("/user-service/actuator") ||
               path.startsWith("/swagger-ui") ||
               path.startsWith("/v3/api-docs") ||
               path.startsWith("/swagger-resources") ||
               path.startsWith("/webjars") ||
               path.startsWith("/actuator");
    }
}