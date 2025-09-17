package com.powertrading.gateway.service;

import com.alibaba.nacos.api.config.ConfigService;
import com.alibaba.nacos.api.config.listener.Listener;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.Data;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.CommandLineRunner;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.cloud.gateway.event.RefreshRoutesEvent;
import org.springframework.cloud.gateway.filter.FilterDefinition;
import org.springframework.cloud.gateway.handler.predicate.PredicateDefinition;
import org.springframework.cloud.gateway.route.RouteDefinition;
import org.springframework.cloud.gateway.route.RouteDefinitionRepository;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.net.URI;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.Executor;

/**
 * 路由热更新服务
 * 支持从Nacos配置中心动态更新路由配置
 */
@Slf4j
@Service
@ConditionalOnProperty(name = "spring.cloud.nacos.config.enabled", havingValue = "true", matchIfMissing = false)
public class RouteRefreshService implements CommandLineRunner, RouteDefinitionRepository {

    @Autowired
    private ConfigService configService;
    
    @Autowired
    private ApplicationEventPublisher publisher;
    
    @Value("${spring.cloud.nacos.config.group:DEFAULT_GROUP}")
    private String configGroup;
    
    @Value("${gateway.route.config.data-id:gateway-routes}")
    private String routeConfigDataId;
    
    private final ObjectMapper objectMapper = new ObjectMapper();
    private final Map<String, RouteDefinition> routeDefinitionMap = new ConcurrentHashMap<>();

    @Override
    public void run(String... args) throws Exception {
        // 启动时加载路由配置
        loadRouteConfig();
        
        // 监听配置变化
        listenConfigChange();
    }

    /**
     * 加载路由配置
     */
    private void loadRouteConfig() {
        try {
            String configContent = configService.getConfig(routeConfigDataId, configGroup, 5000);
            if (configContent != null && !configContent.trim().isEmpty()) {
                updateRouteDefinitions(configContent);
                log.info("成功加载路由配置，共{}条路由", routeDefinitionMap.size());
            } else {
                log.info("未找到路由配置，使用默认配置");
            }
        } catch (Exception e) {
            log.error("加载路由配置失败", e);
        }
    }

    /**
     * 监听配置变化
     */
    private void listenConfigChange() {
        try {
            configService.addListener(routeConfigDataId, configGroup, new Listener() {
                @Override
                public Executor getExecutor() {
                    return null;
                }

                @Override
                public void receiveConfigInfo(String configInfo) {
                    log.info("检测到路由配置变化，开始更新路由");
                    try {
                        updateRouteDefinitions(configInfo);
                        // 发布路由刷新事件
                        publisher.publishEvent(new RefreshRoutesEvent(this));
                        log.info("路由配置更新成功，共{}条路由", routeDefinitionMap.size());
                    } catch (Exception e) {
                        log.error("更新路由配置失败", e);
                    }
                }
            });
            log.info("开始监听路由配置变化: dataId={}, group={}", routeConfigDataId, configGroup);
        } catch (Exception e) {
            log.error("监听路由配置变化失败", e);
        }
    }

    /**
     * 更新路由定义
     */
    private void updateRouteDefinitions(String configContent) throws Exception {
        List<RouteConfig> routeConfigs = objectMapper.readValue(
                configContent, new TypeReference<List<RouteConfig>>() {});
        
        // 清空现有路由
        routeDefinitionMap.clear();
        
        // 添加新路由
        for (RouteConfig routeConfig : routeConfigs) {
            RouteDefinition routeDefinition = convertToRouteDefinition(routeConfig);
            routeDefinitionMap.put(routeDefinition.getId(), routeDefinition);
        }
    }

    /**
     * 转换路由配置为路由定义
     */
    private RouteDefinition convertToRouteDefinition(RouteConfig routeConfig) {
        RouteDefinition routeDefinition = new RouteDefinition();
        routeDefinition.setId(routeConfig.getId());
        routeDefinition.setUri(URI.create(routeConfig.getUri()));
        routeDefinition.setOrder(routeConfig.getOrder());
        
        // 设置断言
        List<PredicateDefinition> predicates = new ArrayList<>();
        for (PredicateConfig predicateConfig : routeConfig.getPredicates()) {
            PredicateDefinition predicate = new PredicateDefinition();
            predicate.setName(predicateConfig.getName());
            predicate.setArgs(predicateConfig.getArgs());
            predicates.add(predicate);
        }
        routeDefinition.setPredicates(predicates);
        
        // 设置过滤器
        if (routeConfig.getFilters() != null) {
            List<FilterDefinition> filters = new ArrayList<>();
            for (FilterConfig filterConfig : routeConfig.getFilters()) {
                FilterDefinition filter = new FilterDefinition();
                filter.setName(filterConfig.getName());
                filter.setArgs(filterConfig.getArgs());
                filters.add(filter);
            }
            routeDefinition.setFilters(filters);
        }
        
        // 设置元数据
        if (routeConfig.getMetadata() != null) {
            routeDefinition.setMetadata(routeConfig.getMetadata());
        }
        
        return routeDefinition;
    }

    @Override
    public Flux<RouteDefinition> getRouteDefinitions() {
        return Flux.fromIterable(routeDefinitionMap.values());
    }

    @Override
    public Mono<Void> save(Mono<RouteDefinition> route) {
        return route.doOnNext(routeDefinition -> {
            routeDefinitionMap.put(routeDefinition.getId(), routeDefinition);
            log.info("保存路由定义: {}", routeDefinition.getId());
        }).then();
    }

    @Override
    public Mono<Void> delete(Mono<String> routeId) {
        return routeId.doOnNext(id -> {
            routeDefinitionMap.remove(id);
            log.info("删除路由定义: {}", id);
        }).then();
    }

    /**
     * 发布路由配置到Nacos
     */
    public boolean publishRouteConfig(List<RouteConfig> routeConfigs) {
        try {
            String configContent = objectMapper.writeValueAsString(routeConfigs);
            return configService.publishConfig(routeConfigDataId, configGroup, configContent);
        } catch (Exception e) {
            log.error("发布路由配置失败", e);
            return false;
        }
    }

    /**
     * 获取当前路由配置
     */
    public List<RouteConfig> getCurrentRouteConfigs() {
        List<RouteConfig> routeConfigs = new ArrayList<>();
        for (RouteDefinition routeDefinition : routeDefinitionMap.values()) {
            RouteConfig routeConfig = convertToRouteConfig(routeDefinition);
            routeConfigs.add(routeConfig);
        }
        return routeConfigs;
    }

    /**
     * 转换路由定义为路由配置
     */
    private RouteConfig convertToRouteConfig(RouteDefinition routeDefinition) {
        RouteConfig routeConfig = new RouteConfig();
        routeConfig.setId(routeDefinition.getId());
        routeConfig.setUri(routeDefinition.getUri().toString());
        routeConfig.setOrder(routeDefinition.getOrder());
        
        // 转换断言
        List<PredicateConfig> predicates = new ArrayList<>();
        for (PredicateDefinition predicate : routeDefinition.getPredicates()) {
            PredicateConfig predicateConfig = new PredicateConfig();
            predicateConfig.setName(predicate.getName());
            predicateConfig.setArgs(predicate.getArgs());
            predicates.add(predicateConfig);
        }
        routeConfig.setPredicates(predicates);
        
        // 转换过滤器
        if (routeDefinition.getFilters() != null) {
            List<FilterConfig> filters = new ArrayList<>();
            for (FilterDefinition filter : routeDefinition.getFilters()) {
                FilterConfig filterConfig = new FilterConfig();
                filterConfig.setName(filter.getName());
                filterConfig.setArgs(filter.getArgs());
                filters.add(filterConfig);
            }
            routeConfig.setFilters(filters);
        }
        
        // 转换元数据
        routeConfig.setMetadata(routeDefinition.getMetadata());
        
        return routeConfig;
    }

    // 配置类定义
    @Data
    public static class RouteConfig {
        private String id;
        private String uri;
        private int order = 0;
        private List<PredicateConfig> predicates;
        private List<FilterConfig> filters;
        private Map<String, Object> metadata;
    }

    @Data
    public static class PredicateConfig {
        private String name;
        private Map<String, String> args;
    }

    @Data
    public static class FilterConfig {
        private String name;
        private Map<String, String> args;
    }
}