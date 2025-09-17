package com.powertrading.auth.config;

import com.powertrading.auth.filter.GatewayAuthFilter;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.web.servlet.FilterRegistrationBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.Ordered;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;
import org.springframework.web.servlet.config.annotation.InterceptorRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

import java.util.Arrays;

/**
 * 网关集成配置
 *
 * @author PowerTrading Team
 * @since 2024-01-15
 */
@Slf4j
@Configuration
public class GatewayIntegrationConfig implements WebMvcConfigurer {

    @Autowired
    private GatewayAuthFilter gatewayAuthFilter;

    /**
     * 注册网关认证过滤器
     */
    @Bean
    public FilterRegistrationBean<GatewayAuthFilter> gatewayAuthFilterRegistration() {
        FilterRegistrationBean<GatewayAuthFilter> registration = new FilterRegistrationBean<>();
        registration.setFilter(gatewayAuthFilter);
        registration.addUrlPatterns("/api/*"); // 只对API路径进行认证
        registration.setName("gatewayAuthFilter");
        registration.setOrder(Ordered.HIGHEST_PRECEDENCE + 1); // 设置较高优先级
        
        log.info("网关认证过滤器注册完成");
        return registration;
    }

    /**
     * CORS配置
     */
    @Bean
    public CorsConfigurationSource corsConfigurationSource() {
        CorsConfiguration configuration = new CorsConfiguration();
        
        // 允许的源
        configuration.setAllowedOriginPatterns(Arrays.asList("*"));
        
        // 允许的HTTP方法
        configuration.setAllowedMethods(Arrays.asList(
                "GET", "POST", "PUT", "DELETE", "OPTIONS", "HEAD", "PATCH"
        ));
        
        // 允许的请求头
        configuration.setAllowedHeaders(Arrays.asList(
                "Authorization",
                "Content-Type",
                "X-App-Id",
                "X-Api-Key",
                "X-Signature",
                "X-Timestamp",
                "X-Nonce",
                "X-Forwarded-For",
                "X-Real-IP",
                "User-Agent",
                "Accept",
                "Origin",
                "Access-Control-Request-Method",
                "Access-Control-Request-Headers"
        ));
        
        // 暴露的响应头
        configuration.setExposedHeaders(Arrays.asList(
                "Authorization",
                "X-Total-Count",
                "X-Page-Number",
                "X-Page-Size"
        ));
        
        // 允许携带凭证
        configuration.setAllowCredentials(true);
        
        // 预检请求缓存时间
        configuration.setMaxAge(3600L);
        
        UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
        source.registerCorsConfiguration("/api/**", configuration);
        
        log.info("CORS配置完成");
        return source;
    }

    /**
     * 添加拦截器
     */
    @Override
    public void addInterceptors(InterceptorRegistry registry) {
        // 可以在这里添加其他拦截器
        log.info("拦截器配置完成");
    }

    /**
     * 网关认证配置属性
     */
    @Bean
    public GatewayAuthProperties gatewayAuthProperties() {
        return new GatewayAuthProperties();
    }

    /**
     * 网关认证配置属性类
     */
    public static class GatewayAuthProperties {
        
        private boolean enabled = true;
        private String[] excludedPaths = {
                "/api/v1/auth/health",
                "/api/v1/multi-auth/health",
                "/api/v1/security/health",
                "/api/v1/permission-cache/health",
                "/swagger-ui/**",
                "/api-docs/**",
                "/actuator/**",
                "/favicon.ico"
        };
        private String[] authPaths = {
                "/api/v1/auth/token/generate",
                "/api/v1/auth/token/refresh",
                "/api/v1/apikey/generate"
        };
        private boolean rateLimitEnabled = true;
        private int rateLimitPerMinute = 60;
        private boolean ipWhitelistEnabled = false;
        private String[] ipWhitelist = {};
        
        // Getters and Setters
        public boolean isEnabled() {
            return enabled;
        }
        
        public void setEnabled(boolean enabled) {
            this.enabled = enabled;
        }
        
        public String[] getExcludedPaths() {
            return excludedPaths;
        }
        
        public void setExcludedPaths(String[] excludedPaths) {
            this.excludedPaths = excludedPaths;
        }
        
        public String[] getAuthPaths() {
            return authPaths;
        }
        
        public void setAuthPaths(String[] authPaths) {
            this.authPaths = authPaths;
        }
        
        public boolean isRateLimitEnabled() {
            return rateLimitEnabled;
        }
        
        public void setRateLimitEnabled(boolean rateLimitEnabled) {
            this.rateLimitEnabled = rateLimitEnabled;
        }
        
        public int getRateLimitPerMinute() {
            return rateLimitPerMinute;
        }
        
        public void setRateLimitPerMinute(int rateLimitPerMinute) {
            this.rateLimitPerMinute = rateLimitPerMinute;
        }
        
        public boolean isIpWhitelistEnabled() {
            return ipWhitelistEnabled;
        }
        
        public void setIpWhitelistEnabled(boolean ipWhitelistEnabled) {
            this.ipWhitelistEnabled = ipWhitelistEnabled;
        }
        
        public String[] getIpWhitelist() {
            return ipWhitelist;
        }
        
        public void setIpWhitelist(String[] ipWhitelist) {
            this.ipWhitelist = ipWhitelist;
        }
    }
}