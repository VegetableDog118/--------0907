package com.powertrading.gateway.config;

import org.springframework.cloud.gateway.route.RouteLocator;
import org.springframework.cloud.gateway.route.builder.RouteLocatorBuilder;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.reactive.CorsWebFilter;
import org.springframework.web.cors.reactive.UrlBasedCorsConfigurationSource;
import org.springframework.web.util.pattern.PathPatternParser;

/**
 * 网关配置类
 * 负责配置路由规则、CORS、负载均衡等
 */
@Configuration
public class GatewayConfig {

    /**
     * 自定义路由配置
     * 支持动态路由和负载均衡
     */
    @Bean
    public RouteLocator customRouteLocator(RouteLocatorBuilder builder) {
        return builder.routes()
                // 用户服务路由
                .route("user-service", r -> r.path("/api/v1/users/**")
                        .uri("lb://user-service"))
                
                // 认证服务路由
                .route("auth-service", r -> r.path("/api/v1/auth/**")
                        .uri("lb://auth-service"))
                
                // 接口管理服务路由
                .route("interface-service", r -> r.path("/api/v1/interfaces/**")
                        .uri("lb://interface-service"))
                
                // 数据源服务路由
                .route("datasource-service", r -> r.path("/api/v1/datasources/**")
                        .uri("lb://datasource-service"))
                
                // 审批服务路由
                .route("approval-service", r -> r.path("/api/v1/applications/**")
                        .uri("lb://approval-service"))
                
                // 通知服务路由
                .route("notification-service", r -> r.path("/api/v1/notifications/**")
                        .uri("lb://notification-service"))
                
                // 动态接口路由 - 支持运行时生成的接口
                .route("dynamic-api", r -> r.path("/api/dynamic/**")
                        .uri("lb://interface-service"))
                
                .build();
    }

    /**
     * CORS配置
     * 支持跨域请求
     */
    @Bean
    public CorsWebFilter corsWebFilter() {
        CorsConfiguration corsConfig = new CorsConfiguration();
        corsConfig.setAllowCredentials(true);
        corsConfig.addAllowedOriginPattern("*");
        corsConfig.addAllowedHeader("*");
        corsConfig.addAllowedMethod("*");
        corsConfig.setMaxAge(3600L);
        
        UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource(new PathPatternParser());
        source.registerCorsConfiguration("/**", corsConfig);
        
        return new CorsWebFilter(source);
    }
}