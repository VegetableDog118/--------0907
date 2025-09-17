package com.powertrading.gateway.filter;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.Data;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.cloud.gateway.filter.GatewayFilter;
import org.springframework.cloud.gateway.filter.factory.AbstractGatewayFilterFactory;
import org.springframework.core.io.buffer.DataBuffer;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.core.script.DefaultRedisScript;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.server.reactive.ServerHttpRequest;
import org.springframework.http.server.reactive.ServerHttpResponse;
import org.springframework.stereotype.Component;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.Arrays;
import java.util.List;
import java.util.Map;

/**
 * 限流过滤器工厂
 * 基于Redis实现分布式限流
 */
@Slf4j
@Component
public class RateLimitGatewayFilterFactory extends AbstractGatewayFilterFactory<RateLimitGatewayFilterFactory.Config> {

    @Autowired
    private RedisTemplate<String, String> redisTemplate;
    
    private final ObjectMapper objectMapper = new ObjectMapper();
    
    // Lua脚本实现滑动窗口限流
    private static final String RATE_LIMIT_SCRIPT = 
            "local key = KEYS[1]\n" +
            "local window = tonumber(ARGV[1])\n" +
            "local limit = tonumber(ARGV[2])\n" +
            "local current_time = tonumber(ARGV[3])\n" +
            "\n" +
            "-- 清理过期的记录\n" +
            "redis.call('ZREMRANGEBYSCORE', key, 0, current_time - window * 1000)\n" +
            "\n" +
            "-- 获取当前窗口内的请求数\n" +
            "local current_requests = redis.call('ZCARD', key)\n" +
            "\n" +
            "if current_requests < limit then\n" +
            "    -- 添加当前请求\n" +
            "    redis.call('ZADD', key, current_time, current_time)\n" +
            "    redis.call('EXPIRE', key, window)\n" +
            "    return {1, limit - current_requests - 1}\n" +
            "else\n" +
            "    return {0, 0}\n" +
            "end";

    public RateLimitGatewayFilterFactory() {
        super(Config.class);
    }

    @Override
    public GatewayFilter apply(Config config) {
        return (exchange, chain) -> {
            ServerHttpRequest request = exchange.getRequest();
            
            // 生成限流key
            String rateLimitKey = generateRateLimitKey(request, config);
            
            // 执行限流检查
            return checkRateLimit(rateLimitKey, config)
                    .flatMap(result -> {
                        if (result.isAllowed()) {
                            // 添加限流信息到响应头
                            ServerHttpResponse response = exchange.getResponse();
                            response.getHeaders().add("X-RateLimit-Limit", String.valueOf(config.getLimit()));
                            response.getHeaders().add("X-RateLimit-Remaining", String.valueOf(result.getRemaining()));
                            response.getHeaders().add("X-RateLimit-Window", String.valueOf(config.getWindow()));
                            
                            log.debug("限流检查通过: key={}, remaining={}", rateLimitKey, result.getRemaining());
                            return chain.filter(exchange);
                        } else {
                            log.warn("请求被限流: key={}, limit={}, window={}s", 
                                    rateLimitKey, config.getLimit(), config.getWindow());
                            return handleRateLimited(exchange, config);
                        }
                    })
                    .onErrorResume(throwable -> {
                        log.error("限流检查异常: key={}, error={}", rateLimitKey, throwable.getMessage(), throwable);
                        // 限流异常时允许请求通过，避免影响业务
                        return chain.filter(exchange);
                    });
        };
    }

    /**
     * 生成限流key
     */
    private String generateRateLimitKey(ServerHttpRequest request, Config config) {
        String keyPrefix = "rate_limit:" + config.getKeyPrefix() + ":";
        
        switch (config.getKeyType()) {
            case "ip":
                String clientIp = getClientIp(request);
                return keyPrefix + "ip:" + clientIp;
            case "user":
                String userId = request.getHeaders().getFirst("X-User-Id");
                return keyPrefix + "user:" + (userId != null ? userId : "anonymous");
            case "api":
                String path = request.getURI().getPath();
                return keyPrefix + "api:" + path;
            case "user_api":
                String userIdForApi = request.getHeaders().getFirst("X-User-Id");
                String apiPath = request.getURI().getPath();
                return keyPrefix + "user_api:" + (userIdForApi != null ? userIdForApi : "anonymous") + ":" + apiPath;
            default:
                return keyPrefix + "global";
        }
    }

    /**
     * 获取客户端IP
     */
    private String getClientIp(ServerHttpRequest request) {
        String xForwardedFor = request.getHeaders().getFirst("X-Forwarded-For");
        if (xForwardedFor != null && !xForwardedFor.isEmpty()) {
            return xForwardedFor.split(",")[0].trim();
        }
        
        String xRealIp = request.getHeaders().getFirst("X-Real-IP");
        if (xRealIp != null && !xRealIp.isEmpty()) {
            return xRealIp;
        }
        
        return request.getRemoteAddress() != null ? 
                request.getRemoteAddress().getAddress().getHostAddress() : "unknown";
    }

    /**
     * 执行限流检查
     */
    private Mono<RateLimitResult> checkRateLimit(String key, Config config) {
        return Mono.fromCallable(() -> {
            DefaultRedisScript<List> script = new DefaultRedisScript<>();
            script.setScriptText(RATE_LIMIT_SCRIPT);
            script.setResultType(List.class);
            
            List<String> keys = Arrays.asList(key);
            List<String> args = Arrays.asList(
                    String.valueOf(config.getWindow()),
                    String.valueOf(config.getLimit()),
                    String.valueOf(System.currentTimeMillis())
            );
            
            List<Long> result = redisTemplate.execute(script, keys, args.toArray());
            
            if (result != null && result.size() >= 2) {
                boolean allowed = result.get(0) == 1;
                long remaining = result.get(1);
                return new RateLimitResult(allowed, remaining);
            } else {
                // Redis异常时允许请求通过
                return new RateLimitResult(true, config.getLimit());
            }
        });
    }

    /**
     * 处理被限流的请求
     */
    private Mono<Void> handleRateLimited(ServerWebExchange exchange, Config config) {
        ServerHttpResponse response = exchange.getResponse();
        response.setStatusCode(HttpStatus.TOO_MANY_REQUESTS);
        response.getHeaders().add(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON_VALUE);
        response.getHeaders().add("X-RateLimit-Limit", String.valueOf(config.getLimit()));
        response.getHeaders().add("X-RateLimit-Remaining", "0");
        response.getHeaders().add("X-RateLimit-Window", String.valueOf(config.getWindow()));
        response.getHeaders().add("Retry-After", String.valueOf(config.getWindow()));
        
        Map<String, Object> result = Map.of(
                "code", 429,
                "message", "请求过于频繁，请稍后再试",
                "limit", config.getLimit(),
                "window", config.getWindow(),
                "timestamp", System.currentTimeMillis()
        );
        
        try {
            String body = objectMapper.writeValueAsString(result);
            DataBuffer buffer = response.bufferFactory().wrap(body.getBytes(StandardCharsets.UTF_8));
            return response.writeWith(Flux.just(buffer));
        } catch (JsonProcessingException e) {
            log.error("序列化限流响应失败", e);
            return response.setComplete();
        }
    }

    @Data
    public static class Config {
        private int limit = 100; // 限制次数
        private int window = 60; // 时间窗口（秒）
        private String keyType = "ip"; // 限流key类型：ip, user, api, user_api, global
        private String keyPrefix = "default"; // key前缀
    }

    @Data
    public static class RateLimitResult {
        private boolean allowed;
        private long remaining;
        
        public RateLimitResult(boolean allowed, long remaining) {
            this.allowed = allowed;
            this.remaining = remaining;
        }
    }
}