package com.powertrading.gateway.exception;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.web.reactive.error.ErrorWebExceptionHandler;
import org.springframework.core.annotation.Order;
import org.springframework.core.io.buffer.DataBuffer;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.server.reactive.ServerHttpResponse;
import org.springframework.stereotype.Component;
import org.springframework.web.server.ResponseStatusException;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.nio.charset.StandardCharsets;
import java.util.Map;

/**
 * 全局异常处理器
 * 统一处理网关层面的异常
 */
@Slf4j
@Order(-1)
@Component
public class GlobalExceptionHandler implements ErrorWebExceptionHandler {

    private final ObjectMapper objectMapper = new ObjectMapper();

    @Override
    public Mono<Void> handle(ServerWebExchange exchange, Throwable ex) {
        ServerHttpResponse response = exchange.getResponse();
        
        if (response.isCommitted()) {
            return Mono.error(ex);
        }
        
        // 设置响应头
        response.getHeaders().add("Content-Type", MediaType.APPLICATION_JSON_VALUE);
        
        // 根据异常类型设置状态码和错误信息
        ErrorResponse errorResponse = buildErrorResponse(ex);
        response.setStatusCode(HttpStatus.valueOf(errorResponse.getCode()));
        
        // 记录异常日志
        logException(exchange, ex, errorResponse);
        
        try {
            String body = objectMapper.writeValueAsString(errorResponse);
            DataBuffer buffer = response.bufferFactory().wrap(body.getBytes(StandardCharsets.UTF_8));
            return response.writeWith(Flux.just(buffer));
        } catch (JsonProcessingException e) {
            log.error("序列化异常响应失败", e);
            return response.setComplete();
        }
    }

    /**
     * 构建错误响应
     */
    private ErrorResponse buildErrorResponse(Throwable ex) {
        ErrorResponse errorResponse = new ErrorResponse();
        errorResponse.setTimestamp(System.currentTimeMillis());
        
        if (ex instanceof ResponseStatusException) {
            ResponseStatusException rse = (ResponseStatusException) ex;
            errorResponse.setCode(rse.getStatus().value());
            errorResponse.setMessage(rse.getReason() != null ? rse.getReason() : rse.getStatus().getReasonPhrase());
            errorResponse.setError(rse.getStatus().getReasonPhrase());
        } else if (ex instanceof org.springframework.cloud.gateway.support.NotFoundException) {
            errorResponse.setCode(HttpStatus.NOT_FOUND.value());
            errorResponse.setMessage("请求的服务不存在");
            errorResponse.setError("Service Not Found");
        } else if (ex instanceof org.springframework.web.server.ServerWebInputException) {
            errorResponse.setCode(HttpStatus.BAD_REQUEST.value());
            errorResponse.setMessage("请求参数错误");
            errorResponse.setError("Bad Request");
        } else if (ex instanceof java.net.ConnectException) {
            errorResponse.setCode(HttpStatus.SERVICE_UNAVAILABLE.value());
            errorResponse.setMessage("服务暂时不可用，请稍后重试");
            errorResponse.setError("Service Unavailable");
        } else if (ex instanceof java.util.concurrent.TimeoutException) {
            errorResponse.setCode(HttpStatus.GATEWAY_TIMEOUT.value());
            errorResponse.setMessage("请求超时，请稍后重试");
            errorResponse.setError("Gateway Timeout");
        } else if (ex instanceof io.netty.channel.ConnectTimeoutException) {
            errorResponse.setCode(HttpStatus.GATEWAY_TIMEOUT.value());
            errorResponse.setMessage("连接超时，请检查网络或稍后重试");
            errorResponse.setError("Connection Timeout");
        } else {
            errorResponse.setCode(HttpStatus.INTERNAL_SERVER_ERROR.value());
            errorResponse.setMessage("系统内部错误，请联系管理员");
            errorResponse.setError("Internal Server Error");
        }
        
        return errorResponse;
    }

    /**
     * 记录异常日志
     */
    private void logException(ServerWebExchange exchange, Throwable ex, ErrorResponse errorResponse) {
        String path = exchange.getRequest().getURI().getPath();
        String method = exchange.getRequest().getMethod().toString();
        String clientIp = getClientIp(exchange);
        
        if (errorResponse.getCode() >= 500) {
            // 服务器错误，记录ERROR级别日志
            log.error("网关异常 - 路径: {}, 方法: {}, 客户端IP: {}, 状态码: {}, 异常: {}", 
                    path, method, clientIp, errorResponse.getCode(), ex.getMessage(), ex);
        } else if (errorResponse.getCode() >= 400) {
            // 客户端错误，记录WARN级别日志
            log.warn("客户端错误 - 路径: {}, 方法: {}, 客户端IP: {}, 状态码: {}, 消息: {}", 
                    path, method, clientIp, errorResponse.getCode(), errorResponse.getMessage());
        } else {
            // 其他情况，记录INFO级别日志
            log.info("请求处理 - 路径: {}, 方法: {}, 客户端IP: {}, 状态码: {}, 消息: {}", 
                    path, method, clientIp, errorResponse.getCode(), errorResponse.getMessage());
        }
    }

    /**
     * 获取客户端IP
     */
    private String getClientIp(ServerWebExchange exchange) {
        String xForwardedFor = exchange.getRequest().getHeaders().getFirst("X-Forwarded-For");
        if (xForwardedFor != null && !xForwardedFor.isEmpty()) {
            return xForwardedFor.split(",")[0].trim();
        }
        
        String xRealIp = exchange.getRequest().getHeaders().getFirst("X-Real-IP");
        if (xRealIp != null && !xRealIp.isEmpty()) {
            return xRealIp;
        }
        
        return exchange.getRequest().getRemoteAddress() != null ? 
                exchange.getRequest().getRemoteAddress().getAddress().getHostAddress() : "unknown";
    }

    /**
     * 错误响应实体
     */
    public static class ErrorResponse {
        private int code;
        private String message;
        private String error;
        private long timestamp;
        private String path;
        
        // Getters and Setters
        public int getCode() {
            return code;
        }
        
        public void setCode(int code) {
            this.code = code;
        }
        
        public String getMessage() {
            return message;
        }
        
        public void setMessage(String message) {
            this.message = message;
        }
        
        public String getError() {
            return error;
        }
        
        public void setError(String error) {
            this.error = error;
        }
        
        public long getTimestamp() {
            return timestamp;
        }
        
        public void setTimestamp(long timestamp) {
            this.timestamp = timestamp;
        }
        
        public String getPath() {
            return path;
        }
        
        public void setPath(String path) {
            this.path = path;
        }
    }
}