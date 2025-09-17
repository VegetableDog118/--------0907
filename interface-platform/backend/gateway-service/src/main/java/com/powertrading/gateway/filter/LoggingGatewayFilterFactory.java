package com.powertrading.gateway.filter;

import lombok.Data;
import lombok.extern.slf4j.Slf4j;
import org.springframework.cloud.gateway.filter.GatewayFilter;
import org.springframework.cloud.gateway.filter.factory.AbstractGatewayFilterFactory;
import org.springframework.core.io.buffer.DataBuffer;
import org.springframework.core.io.buffer.DataBufferFactory;
import org.springframework.core.io.buffer.DataBufferUtils;
import org.springframework.http.server.reactive.ServerHttpRequest;
import org.springframework.http.server.reactive.ServerHttpRequestDecorator;
import org.springframework.http.server.reactive.ServerHttpResponse;
import org.springframework.http.server.reactive.ServerHttpResponseDecorator;
import org.springframework.stereotype.Component;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.nio.charset.StandardCharsets;
import java.util.concurrent.atomic.AtomicLong;

/**
 * 请求日志和性能监控过滤器
 * 记录请求响应信息和性能指标
 */
@Slf4j
@Component
public class LoggingGatewayFilterFactory extends AbstractGatewayFilterFactory<LoggingGatewayFilterFactory.Config> {

    public LoggingGatewayFilterFactory() {
        super(Config.class);
    }

    @Override
    public GatewayFilter apply(Config config) {
        return (exchange, chain) -> {
            long startTime = System.currentTimeMillis();
            ServerHttpRequest request = exchange.getRequest();
            
            // 生成请求ID
            String requestId = generateRequestId();
            
            // 记录请求信息
            logRequest(request, requestId, config);
            
            // 包装请求和响应以便记录详细信息
            ServerHttpRequest wrappedRequest = new LoggingServerHttpRequestDecorator(request, config);
            ServerHttpResponse wrappedResponse = new LoggingServerHttpResponseDecorator(
                    exchange.getResponse(), requestId, startTime, config);
            
            ServerWebExchange wrappedExchange = exchange.mutate()
                    .request(wrappedRequest)
                    .response(wrappedResponse)
                    .build();
            
            // 添加请求ID到响应头
            wrappedResponse.getHeaders().add("X-Request-Id", requestId);
            
            return chain.filter(wrappedExchange)
                    .doOnSuccess(aVoid -> {
                        long duration = System.currentTimeMillis() - startTime;
                        logResponse(wrappedResponse, requestId, duration, config);
                        logPerformanceMetrics(request, wrappedResponse, duration, config);
                    })
                    .doOnError(throwable -> {
                        long duration = System.currentTimeMillis() - startTime;
                        logError(request, requestId, duration, throwable, config);
                    });
        };
    }

    /**
     * 生成请求ID
     */
    private String generateRequestId() {
        return String.valueOf(System.currentTimeMillis()) + "-" + 
               String.valueOf(Thread.currentThread().getId());
    }

    /**
     * 记录请求信息
     */
    private void logRequest(ServerHttpRequest request, String requestId, Config config) {
        if (!config.isLogRequest()) {
            return;
        }
        
        StringBuilder logBuilder = new StringBuilder();
        logBuilder.append("\n=== 请求开始 ===")
                  .append("\n请求ID: ").append(requestId)
                  .append("\n方法: ").append(request.getMethod())
                  .append("\n路径: ").append(request.getURI())
                  .append("\n客户端IP: ").append(getClientIp(request));
        
        if (config.isLogHeaders()) {
            logBuilder.append("\n请求头:");
            request.getHeaders().forEach((name, values) -> {
                if (!isSensitiveHeader(name)) {
                    logBuilder.append("\n  ").append(name).append(": ").append(String.join(", ", values));
                }
            });
        }
        
        log.info(logBuilder.toString());
    }

    /**
     * 记录响应信息
     */
    private void logResponse(ServerHttpResponse response, String requestId, long duration, Config config) {
        if (!config.isLogResponse()) {
            return;
        }
        
        StringBuilder logBuilder = new StringBuilder();
        logBuilder.append("\n=== 请求结束 ===")
                  .append("\n请求ID: ").append(requestId)
                  .append("\n状态码: ").append(response.getStatusCode())
                  .append("\n耗时: ").append(duration).append("ms");
        
        if (config.isLogHeaders()) {
            logBuilder.append("\n响应头:");
            response.getHeaders().forEach((name, values) -> {
                logBuilder.append("\n  ").append(name).append(": ").append(String.join(", ", values));
            });
        }
        
        log.info(logBuilder.toString());
    }

    /**
     * 记录性能指标
     */
    private void logPerformanceMetrics(ServerHttpRequest request, ServerHttpResponse response, 
                                     long duration, Config config) {
        if (!config.isLogPerformance()) {
            return;
        }
        
        String path = request.getURI().getPath();
        String method = request.getMethod().toString();
        int statusCode = response.getStatusCode() != null ? response.getStatusCode().value() : 0;
        
        // 记录性能指标
        log.info("性能指标 - 路径: {}, 方法: {}, 状态码: {}, 耗时: {}ms", 
                path, method, statusCode, duration);
        
        // 慢请求告警
        if (duration > config.getSlowRequestThreshold()) {
            log.warn("慢请求告警 - 路径: {}, 方法: {}, 耗时: {}ms, 阈值: {}ms", 
                    path, method, duration, config.getSlowRequestThreshold());
        }
    }

    /**
     * 记录错误信息
     */
    private void logError(ServerHttpRequest request, String requestId, long duration, 
                         Throwable throwable, Config config) {
        log.error("请求异常 - 请求ID: {}, 路径: {}, 方法: {}, 耗时: {}ms, 异常: {}", 
                requestId, request.getURI().getPath(), request.getMethod(), 
                duration, throwable.getMessage(), throwable);
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
     * 检查是否为敏感请求头
     */
    private boolean isSensitiveHeader(String headerName) {
        String lowerName = headerName.toLowerCase();
        return lowerName.contains("authorization") || 
               lowerName.contains("cookie") || 
               lowerName.contains("token") ||
               lowerName.contains("password");
    }

    /**
     * 请求装饰器
     */
    private static class LoggingServerHttpRequestDecorator extends ServerHttpRequestDecorator {
        private final Config config;
        
        public LoggingServerHttpRequestDecorator(ServerHttpRequest delegate, Config config) {
            super(delegate);
            this.config = config;
        }
        
        @Override
        public Flux<DataBuffer> getBody() {
            if (!config.isLogRequestBody()) {
                return super.getBody();
            }
            
            return DataBufferUtils.join(super.getBody())
                    .doOnNext(dataBuffer -> {
                        byte[] content = new byte[dataBuffer.readableByteCount()];
                        dataBuffer.read(content);
                        DataBufferUtils.release(dataBuffer);
                        
                        String body = new String(content, StandardCharsets.UTF_8);
                        if (body.length() > config.getMaxBodyLogLength()) {
                            body = body.substring(0, config.getMaxBodyLogLength()) + "...";
                        }
                        log.info("请求体: {}", body);
                    })
                    .flux();
        }
    }

    /**
     * 响应装饰器
     */
    private static class LoggingServerHttpResponseDecorator extends ServerHttpResponseDecorator {
        private final String requestId;
        private final long startTime;
        private final Config config;
        
        public LoggingServerHttpResponseDecorator(ServerHttpResponse delegate, String requestId, 
                                                 long startTime, Config config) {
            super(delegate);
            this.requestId = requestId;
            this.startTime = startTime;
            this.config = config;
        }
        
        @Override
        public Mono<Void> writeWith(org.reactivestreams.Publisher<? extends DataBuffer> body) {
            if (!config.isLogResponseBody()) {
                return super.writeWith(body);
            }
            
            return super.writeWith(Flux.from(body)
                    .doOnNext(dataBuffer -> {
                        byte[] content = new byte[dataBuffer.readableByteCount()];
                        dataBuffer.read(content);
                        
                        String responseBody = new String(content, StandardCharsets.UTF_8);
                        if (responseBody.length() > config.getMaxBodyLogLength()) {
                            responseBody = responseBody.substring(0, config.getMaxBodyLogLength()) + "...";
                        }
                        log.info("响应体 - 请求ID: {}, 内容: {}", requestId, responseBody);
                    }));
        }
    }

    @Data
    public static class Config {
        private boolean logRequest = true;
        private boolean logResponse = true;
        private boolean logHeaders = true;
        private boolean logRequestBody = false;
        private boolean logResponseBody = false;
        private boolean logPerformance = true;
        private int maxBodyLogLength = 1000;
        private long slowRequestThreshold = 3000; // 慢请求阈值（毫秒）
    }
}