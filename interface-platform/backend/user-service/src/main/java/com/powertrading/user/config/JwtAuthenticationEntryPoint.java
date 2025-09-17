package com.powertrading.user.config;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.web.AuthenticationEntryPoint;
import org.springframework.stereotype.Component;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.time.LocalDateTime;

/**
 * JWT认证入口点
 * 处理未认证用户访问需要认证的资源时的情况
 *
 * @author PowerTrading Team
 * @since 2024-01-15
 */
@Component
public class JwtAuthenticationEntryPoint implements AuthenticationEntryPoint {

    private static final Logger logger = LoggerFactory.getLogger(JwtAuthenticationEntryPoint.class);

    @Override
    public void commence(HttpServletRequest request, HttpServletResponse response,
                        AuthenticationException authException) throws IOException, ServletException {
        
        logger.warn("未认证用户尝试访问受保护资源: {} {}", request.getMethod(), request.getRequestURI());
        
        response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
        response.setContentType("application/json;charset=UTF-8");
        
        String jsonResponse = String.format(
            "{\"success\":false,\"errorCode\":\"UNAUTHORIZED\",\"message\":\"未认证，请先登录\",\"timestamp\":\"%s\"}",
            LocalDateTime.now()
        );
        
        response.getWriter().write(jsonResponse);
    }
}