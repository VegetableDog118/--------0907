package com.powertrading.gateway.filter;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.Data;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.cloud.gateway.filter.GatewayFilter;
import org.springframework.cloud.gateway.filter.factory.AbstractGatewayFilterFactory;
import org.springframework.core.io.buffer.DataBuffer;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.server.reactive.ServerHttpRequest;
import org.springframework.http.server.reactive.ServerHttpResponse;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.client.WebClient;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.nio.charset.StandardCharsets;
import java.util.Arrays;
import java.util.List;
import java.util.Map;

/**
 * 认证鉴权过滤器工厂
 * 负责Token验证和权限校验
 */
@Slf4j
@Component
public class AuthGatewayFilterFactory extends AbstractGatewayFilterFactory<AuthGatewayFilterFactory.Config> {

    @Autowired
    private WebClient.Builder webClientBuilder;
    
    private final ObjectMapper objectMapper = new ObjectMapper();
    
    // 不需要认证的路径
    private static final List<String> SKIP_AUTH_PATHS = Arrays.asList(
            "/api/v1/auth/login",
            "/api/v1/auth/register",
            "/api/v1/auth/refresh",
            "/gateway/routes",
            "/actuator",
            "/swagger",
            "/v3/api-docs"
    );

    public AuthGatewayFilterFactory() {
        super(Config.class);
    }

    @Override
    public GatewayFilter apply(Config config) {
        return (exchange, chain) -> {
            ServerHttpRequest request = exchange.getRequest();
            String path = request.getURI().getPath();
            
            // 检查是否需要跳过认证
            if (shouldSkipAuth(path)) {
                log.debug("跳过认证检查: {}", path);
                return chain.filter(exchange);
            }
            
            // 获取Token
            String token = extractToken(request);
            if (token == null || token.isEmpty()) {
                log.warn("请求缺少Token: {}", path);
                return handleUnauthorized(exchange, "缺少认证Token");
            }
            
            // 验证Token
            return validateToken(token)
                    .flatMap(authResult -> {
                        if (authResult.isValid()) {
                            // Token有效，添加用户信息到请求头
                            ServerHttpRequest mutatedRequest = request.mutate()
                                    .header("X-User-Id", authResult.getUserId())
                                    .header("X-User-Role", authResult.getRole())
                                    .header("X-User-Permissions", String.join(",", authResult.getPermissions()))
                                    .build();
                            
                            ServerWebExchange mutatedExchange = exchange.mutate()
                                    .request(mutatedRequest)
                                    .build();
                            
                            log.debug("认证成功: userId={}, role={}, path={}", 
                                    authResult.getUserId(), authResult.getRole(), path);
                            
                            return chain.filter(mutatedExchange);
                        } else {
                            log.warn("Token验证失败: {}, path={}", authResult.getMessage(), path);
                            return handleUnauthorized(exchange, authResult.getMessage());
                        }
                    })
                    .onErrorResume(throwable -> {
                        log.error("认证过程异常: path={}, error={}", path, throwable.getMessage(), throwable);
                        return handleUnauthorized(exchange, "认证服务异常");
                    });
        };
    }

    /**
     * 检查是否需要跳过认证
     */
    private boolean shouldSkipAuth(String path) {
        return SKIP_AUTH_PATHS.stream().anyMatch(path::startsWith);
    }

    /**
     * 从请求中提取Token
     */
    private String extractToken(ServerHttpRequest request) {
        String authHeader = request.getHeaders().getFirst(HttpHeaders.AUTHORIZATION);
        if (authHeader != null && authHeader.startsWith("Bearer ")) {
            return authHeader.substring(7);
        }
        return null;
    }

    /**
     * 验证Token
     */
    private Mono<AuthResult> validateToken(String token) {
        return webClientBuilder.build()
                .post()
                .uri("lb://auth-service/api/v1/auth/validate")
                .header(HttpHeaders.AUTHORIZATION, "Bearer " + token)
                .retrieve()
                .bodyToMono(Map.class)
                .map(response -> {
                    Integer code = (Integer) response.get("code");
                    if (code != null && code == 200) {
                        Map<String, Object> data = (Map<String, Object>) response.get("data");
                        AuthResult result = new AuthResult();
                        result.setValid(true);
                        result.setUserId((String) data.get("userId"));
                        result.setRole((String) data.get("role"));
                        result.setPermissions((List<String>) data.get("permissions"));
                        return result;
                    } else {
                        AuthResult result = new AuthResult();
                        result.setValid(false);
                        result.setMessage((String) response.get("message"));
                        return result;
                    }
                })
                .onErrorReturn(new AuthResult(false, "Token验证失败"));
    }

    /**
     * 处理未授权请求
     */
    private Mono<Void> handleUnauthorized(ServerWebExchange exchange, String message) {
        ServerHttpResponse response = exchange.getResponse();
        response.setStatusCode(HttpStatus.UNAUTHORIZED);
        response.getHeaders().add(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON_VALUE);
        
        Map<String, Object> result = Map.of(
                "code", 401,
                "message", message,
                "timestamp", System.currentTimeMillis()
        );
        
        try {
            String body = objectMapper.writeValueAsString(result);
            DataBuffer buffer = response.bufferFactory().wrap(body.getBytes(StandardCharsets.UTF_8));
            return response.writeWith(Flux.just(buffer));
        } catch (JsonProcessingException e) {
            log.error("序列化响应失败", e);
            return response.setComplete();
        }
    }

    @Data
    public static class Config {
        private boolean enabled = true;
        private String authServiceUrl = "lb://auth-service";
    }

    @Data
    public static class AuthResult {
        private boolean valid;
        private String userId;
        private String role;
        private List<String> permissions;
        private String message;
        
        public AuthResult() {}
        
        public AuthResult(boolean valid, String message) {
            this.valid = valid;
            this.message = message;
        }
    }
}