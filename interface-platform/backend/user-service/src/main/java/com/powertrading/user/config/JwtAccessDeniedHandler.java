package com.powertrading.user.config;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.web.access.AccessDeniedHandler;
import org.springframework.stereotype.Component;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.time.LocalDateTime;

/**
 * JWT访问拒绝处理器
 * 处理已认证用户访问没有权限的资源时的情况
 *
 * @author PowerTrading Team
 * @since 2024-01-15
 */
@Component
public class JwtAccessDeniedHandler implements AccessDeniedHandler {

    private static final Logger logger = LoggerFactory.getLogger(JwtAccessDeniedHandler.class);

    @Override
    public void handle(HttpServletRequest request, HttpServletResponse response,
                      AccessDeniedException accessDeniedException) throws IOException, ServletException {
        
        String userId = (String) request.getAttribute("userId");
        String username = (String) request.getAttribute("username");
        
        logger.warn("用户 {}({}) 尝试访问无权限资源: {} {}", 
            username, userId, request.getMethod(), request.getRequestURI());
        
        response.setStatus(HttpServletResponse.SC_FORBIDDEN);
        response.setContentType("application/json;charset=UTF-8");
        
        String jsonResponse = String.format(
            "{\"success\":false,\"errorCode\":\"FORBIDDEN\",\"message\":\"权限不足，无法访问该资源\",\"timestamp\":\"%s\"}",
            LocalDateTime.now()
        );
        
        response.getWriter().write(jsonResponse);
    }
}