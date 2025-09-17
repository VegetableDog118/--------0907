package com.powertrading.gateway.service;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.cloud.gateway.event.RefreshRoutesEvent;
import org.springframework.cloud.gateway.filter.FilterDefinition;
import org.springframework.cloud.gateway.handler.predicate.PredicateDefinition;
import org.springframework.cloud.gateway.route.RouteDefinition;
import org.springframework.cloud.gateway.route.RouteDefinitionWriter;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Mono;

import java.net.URI;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;

/**
 * 动态路由服务
 * 支持运行时动态添加、修改、删除路由
 */
@Slf4j
@Service
public class DynamicRouteService {

    @Autowired
    private RouteDefinitionWriter routeDefinitionWriter;
    
    @Autowired
    private ApplicationEventPublisher publisher;

    /**
     * 添加路由
     * @param routeId 路由ID
     * @param uri 目标URI
     * @param path 路径匹配规则
     * @param method HTTP方法
     * @param stripPrefix 是否去除路径前缀
     * @return 操作结果
     */
    public String addRoute(String routeId, String uri, String path, String method, Integer stripPrefix) {
        try {
            RouteDefinition definition = new RouteDefinition();
            definition.setId(routeId);
            definition.setUri(URI.create(uri));
            
            // 设置断言
            PredicateDefinition pathPredicate = new PredicateDefinition();
            pathPredicate.setName("Path");
            pathPredicate.addArg("pattern", path);
            
            if (method != null && !method.isEmpty()) {
                PredicateDefinition methodPredicate = new PredicateDefinition();
                methodPredicate.setName("Method");
                methodPredicate.addArg("methods", method);
                definition.setPredicates(Arrays.asList(pathPredicate, methodPredicate));
            } else {
                definition.setPredicates(Arrays.asList(pathPredicate));
            }
            
            // 设置过滤器
            if (stripPrefix != null && stripPrefix > 0) {
                FilterDefinition stripPrefixFilter = new FilterDefinition();
                stripPrefixFilter.setName("StripPrefix");
                stripPrefixFilter.addArg("parts", String.valueOf(stripPrefix));
                definition.setFilters(Arrays.asList(stripPrefixFilter));
            }
            
            // 添加元数据
            Map<String, Object> metadata = new HashMap<>();
            metadata.put("created_time", System.currentTimeMillis());
            metadata.put("type", "dynamic");
            definition.setMetadata(metadata);
            
            routeDefinitionWriter.save(Mono.just(definition)).subscribe();
            
            // 发布路由刷新事件
            publisher.publishEvent(new RefreshRoutesEvent(this));
            
            log.info("动态添加路由成功: routeId={}, uri={}, path={}", routeId, uri, path);
            return "success";
            
        } catch (Exception e) {
            log.error("动态添加路由失败: routeId={}, error={}", routeId, e.getMessage(), e);
            return "failed: " + e.getMessage();
        }
    }

    /**
     * 删除路由
     * @param routeId 路由ID
     * @return 操作结果
     */
    public String deleteRoute(String routeId) {
        try {
            routeDefinitionWriter.delete(Mono.just(routeId)).subscribe();
            
            // 发布路由刷新事件
            publisher.publishEvent(new RefreshRoutesEvent(this));
            
            log.info("动态删除路由成功: routeId={}", routeId);
            return "success";
            
        } catch (Exception e) {
            log.error("动态删除路由失败: routeId={}, error={}", routeId, e.getMessage(), e);
            return "failed: " + e.getMessage();
        }
    }

    /**
     * 更新路由
     * @param routeId 路由ID
     * @param uri 目标URI
     * @param path 路径匹配规则
     * @param method HTTP方法
     * @param stripPrefix 是否去除路径前缀
     * @return 操作结果
     */
    public String updateRoute(String routeId, String uri, String path, String method, Integer stripPrefix) {
        try {
            // 先删除原路由
            deleteRoute(routeId);
            
            // 再添加新路由
            return addRoute(routeId, uri, path, method, stripPrefix);
            
        } catch (Exception e) {
            log.error("动态更新路由失败: routeId={}, error={}", routeId, e.getMessage(), e);
            return "failed: " + e.getMessage();
        }
    }

    /**
     * 刷新路由
     */
    public void refreshRoutes() {
        publisher.publishEvent(new RefreshRoutesEvent(this));
        log.info("手动刷新路由配置");
    }

    /**
     * 为接口服务添加动态路由
     * @param interfaceId 接口ID
     * @param path 接口路径
     * @param method HTTP方法
     * @param targetService 目标服务
     * @return 操作结果
     */
    public String addInterfaceRoute(String interfaceId, String path, String method, String targetService) {
        String routeId = "interface-" + interfaceId;
        String uri = "lb://" + targetService;
        
        return addRoute(routeId, uri, path, method, null);
    }

    /**
     * 删除接口路由
     * @param interfaceId 接口ID
     * @return 操作结果
     */
    public String deleteInterfaceRoute(String interfaceId) {
        String routeId = "interface-" + interfaceId;
        return deleteRoute(routeId);
    }
}