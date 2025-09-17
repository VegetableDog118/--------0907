package com.powertrading.auth.filter;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.powertrading.auth.dto.AuthenticationRequest;
import com.powertrading.auth.dto.AuthenticationResult;
import com.powertrading.auth.service.MultiAuthService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

import javax.servlet.*;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * 网关认证过滤器
 * 用于在网关层进行统一的认证处理
 *
 * @author PowerTrading Team
 * @since 2024-01-15
 */
@Slf4j
@Component
public class GatewayAuthFilter implements Filter {

    @Autowired
    private MultiAuthService multiAuthService;

    private final ObjectMapper objectMapper = new ObjectMapper();

    // 不需要认证的路径
    private static final List<String> EXCLUDED_PATHS = Arrays.asList(
            "/api/v1/auth/health",
            "/api/v1/multi-auth/health",
            "/api/v1/security/health",
            "/api/v1/permission-cache/health",
            "/swagger-ui",
            "/api-docs",
            "/actuator/health",
            "/favicon.ico"
    );

    // 需要特殊处理的认证路径
    private static final List<String> AUTH_PATHS = Arrays.asList(
            "/api/v1/auth/token/generate",
            "/api/v1/auth/token/refresh",
            "/api/v1/apikey/generate"
    );

    @Override
    public void init(FilterConfig filterConfig) throws ServletException {
        log.info("网关认证过滤器初始化完成");
    }

    @Override
    public void doFilter(ServletRequest request, ServletResponse response, FilterChain chain)
            throws IOException, ServletException {
        
        HttpServletRequest httpRequest = (HttpServletRequest) request;
        HttpServletResponse httpResponse = (HttpServletResponse) response;
        
        String requestPath = httpRequest.getRequestURI();
        String method = httpRequest.getMethod();
        
        try {
            // 检查是否为排除路径
            if (isExcludedPath(requestPath)) {
                chain.doFilter(request, response);
                return;
            }
            
            // 检查是否为认证相关路径（特殊处理）
            if (isAuthPath(requestPath)) {
                // 认证相关路径可能需要特殊的验证逻辑
                handleAuthPath(httpRequest, httpResponse, chain);
                return;
            }
            
            // 执行认证检查
            AuthenticationResult authResult = performAuthentication(httpRequest);
            
            if (authResult.isSuccess()) {
                // 认证成功，设置用户信息到请求属性中
                setUserAttributes(httpRequest, authResult);
                
                // 继续处理请求
                chain.doFilter(request, response);
            } else {
                // 认证失败，返回错误响应
                sendAuthenticationError(httpResponse, authResult);
            }
        } catch (Exception e) {
            log.error("网关认证过滤器处理异常: path={}, method={}", requestPath, method, e);
            sendInternalError(httpResponse, "认证过程中发生内部错误");
        }
    }

    @Override
    public void destroy() {
        log.info("网关认证过滤器销毁");
    }

    /**
     * 检查是否为排除路径
     */
    private boolean isExcludedPath(String requestPath) {
        return EXCLUDED_PATHS.stream().anyMatch(requestPath::startsWith);
    }

    /**
     * 检查是否为认证路径
     */
    private boolean isAuthPath(String requestPath) {
        return AUTH_PATHS.stream().anyMatch(requestPath::startsWith);
    }

    /**
     * 处理认证相关路径
     */
    private void handleAuthPath(HttpServletRequest request, HttpServletResponse response, 
                               FilterChain chain) throws IOException, ServletException {
        // 认证相关路径的特殊处理逻辑
        // 例如：Token生成不需要认证，但可能需要其他验证
        
        String requestPath = request.getRequestURI();
        
        if (requestPath.contains("/token/generate") || requestPath.contains("/apikey/generate")) {
            // Token生成和API密钥生成需要验证用户身份，但不需要现有Token
            // 这里可以添加额外的验证逻辑，如IP白名单、频率限制等
            
            if (checkRateLimit(request)) {
                chain.doFilter(request, response);
            } else {
                sendRateLimitError(response);
            }
        } else {
            // 其他认证路径正常处理
            chain.doFilter(request, response);
        }
    }

    /**
     * 执行认证检查
     */
    private AuthenticationResult performAuthentication(HttpServletRequest request) {
        try {
            // 构建认证请求
            AuthenticationRequest authRequest = buildAuthenticationRequest(request);
            
            // 执行认证
            return multiAuthService.authenticate(authRequest);
        } catch (Exception e) {
            log.error("执行认证检查失败", e);
            return AuthenticationResult.failure("AUTHENTICATION_ERROR", "认证检查失败: " + e.getMessage());
        }
    }

    /**
     * 构建认证请求
     */
    private AuthenticationRequest buildAuthenticationRequest(HttpServletRequest request) {
        AuthenticationRequest authRequest = new AuthenticationRequest();
        
        // 从Header中获取JWT Token
        String authHeader = request.getHeader("Authorization");
        if (StringUtils.hasText(authHeader) && authHeader.startsWith("Bearer ")) {
            authRequest.setJwtToken(authHeader.substring(7));
        }
        
        // 从Header或参数中获取API密钥信息
        String appId = request.getHeader("X-App-Id");
        if (!StringUtils.hasText(appId)) {
            appId = request.getParameter("appId");
        }
        authRequest.setAppId(appId);
        
        String apiKey = request.getHeader("X-Api-Key");
        if (!StringUtils.hasText(apiKey)) {
            apiKey = request.getParameter("apiKey");
        }
        authRequest.setApiKey(apiKey);
        
        // 获取签名信息
        authRequest.setSignature(request.getHeader("X-Signature"));
        authRequest.setTimestamp(getLongHeader(request, "X-Timestamp"));
        authRequest.setNonce(request.getHeader("X-Nonce"));
        
        // 设置请求信息
        authRequest.setRequestPath(request.getRequestURI());
        authRequest.setRequestMethod(request.getMethod());
        authRequest.setClientIp(getClientIp(request));
        authRequest.setUserAgent(request.getHeader("User-Agent"));
        
        // 设置权限检查
        authRequest.setCheckPermissions(true);
        
        return authRequest;
    }

    /**
     * 设置用户属性到请求中
     */
    private void setUserAttributes(HttpServletRequest request, AuthenticationResult authResult) {
        request.setAttribute("userId", authResult.getUserId());
        request.setAttribute("username", authResult.getUsername());
        request.setAttribute("companyName", authResult.getCompanyName());
        request.setAttribute("roles", authResult.getRoles());
        request.setAttribute("permissions", authResult.getPermissions());
        request.setAttribute("appId", authResult.getAppId());
        request.setAttribute("authType", authResult.getAuthType());
        request.setAttribute("authTime", authResult.getAuthTime());
        
        // 设置主要角色（如果有多个角色，取第一个）
        if (authResult.getRoles() != null && !authResult.getRoles().isEmpty()) {
            request.setAttribute("role", authResult.getRoles().get(0));
        }
    }

    /**
     * 发送认证错误响应
     */
    private void sendAuthenticationError(HttpServletResponse response, AuthenticationResult authResult) 
            throws IOException {
        response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
        response.setContentType("application/json;charset=UTF-8");
        
        Map<String, Object> errorResponse = new HashMap<>();
        errorResponse.put("code", 401);
        errorResponse.put("message", authResult.getErrorMessage());
        errorResponse.put("errorCode", authResult.getErrorCode());
        errorResponse.put("timestamp", System.currentTimeMillis());
        
        response.getWriter().write(objectMapper.writeValueAsString(errorResponse));
    }

    /**
     * 发送内部错误响应
     */
    private void sendInternalError(HttpServletResponse response, String message) throws IOException {
        response.setStatus(HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
        response.setContentType("application/json;charset=UTF-8");
        
        Map<String, Object> errorResponse = new HashMap<>();
        errorResponse.put("code", 500);
        errorResponse.put("message", message);
        errorResponse.put("timestamp", System.currentTimeMillis());
        
        response.getWriter().write(objectMapper.writeValueAsString(errorResponse));
    }

    /**
     * 发送频率限制错误响应
     */
    private void sendRateLimitError(HttpServletResponse response) throws IOException {
        response.setStatus(429); // Too Many Requests
        response.setContentType("application/json;charset=UTF-8");
        
        Map<String, Object> errorResponse = new HashMap<>();
        errorResponse.put("code", 429);
        errorResponse.put("message", "请求频率过高，请稍后重试");
        errorResponse.put("timestamp", System.currentTimeMillis());
        
        response.getWriter().write(objectMapper.writeValueAsString(errorResponse));
    }

    /**
     * 检查频率限制
     */
    private boolean checkRateLimit(HttpServletRequest request) {
        // 这里可以实现频率限制逻辑
        // 例如：基于IP的频率限制、基于用户的频率限制等
        // 暂时返回true，表示通过频率检查
        return true;
    }

    /**
     * 获取Long类型的Header值
     */
    private Long getLongHeader(HttpServletRequest request, String headerName) {
        String headerValue = request.getHeader(headerName);
        if (StringUtils.hasText(headerValue)) {
            try {
                return Long.parseLong(headerValue);
            } catch (NumberFormatException e) {
                log.warn("无效的Header值: {}={}", headerName, headerValue);
            }
        }
        return null;
    }

    /**
     * 获取客户端IP地址
     */
    private String getClientIp(HttpServletRequest request) {
        String xForwardedFor = request.getHeader("X-Forwarded-For");
        if (StringUtils.hasText(xForwardedFor) && !"unknown".equalsIgnoreCase(xForwardedFor)) {
            return xForwardedFor.split(",")[0].trim();
        }
        
        String xRealIp = request.getHeader("X-Real-IP");
        if (StringUtils.hasText(xRealIp) && !"unknown".equalsIgnoreCase(xRealIp)) {
            return xRealIp;
        }
        
        return request.getRemoteAddr();
    }
}