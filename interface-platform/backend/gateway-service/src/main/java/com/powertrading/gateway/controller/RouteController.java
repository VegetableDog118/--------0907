package com.powertrading.gateway.controller;

import com.powertrading.gateway.service.DynamicRouteService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

/**
 * 路由管理控制器
 * 提供动态路由管理的REST API
 */
@Slf4j
@RestController
@RequestMapping("/gateway/routes")
@Tag(name = "路由管理", description = "动态路由管理接口")
public class RouteController {

    @Autowired
    private DynamicRouteService dynamicRouteService;

    /**
     * 添加路由
     */
    @PostMapping("/add")
    @Operation(summary = "添加路由", description = "动态添加新的路由规则")
    public Map<String, Object> addRoute(@RequestBody Map<String, Object> routeInfo) {
        try {
            String routeId = (String) routeInfo.get("routeId");
            String uri = (String) routeInfo.get("uri");
            String path = (String) routeInfo.get("path");
            String method = (String) routeInfo.get("method");
            Integer stripPrefix = (Integer) routeInfo.get("stripPrefix");
            
            String result = dynamicRouteService.addRoute(routeId, uri, path, method, stripPrefix);
            
            if ("success".equals(result)) {
                return Map.of("code", 200, "message", "路由添加成功", "data", routeId);
            } else {
                return Map.of("code", 500, "message", "路由添加失败: " + result);
            }
            
        } catch (Exception e) {
            log.error("添加路由异常", e);
            return Map.of("code", 500, "message", "添加路由异常: " + e.getMessage());
        }
    }

    /**
     * 删除路由
     */
    @DeleteMapping("/delete/{routeId}")
    @Operation(summary = "删除路由", description = "删除指定的路由规则")
    public Map<String, Object> deleteRoute(@PathVariable String routeId) {
        try {
            String result = dynamicRouteService.deleteRoute(routeId);
            
            if ("success".equals(result)) {
                return Map.of("code", 200, "message", "路由删除成功");
            } else {
                return Map.of("code", 500, "message", "路由删除失败: " + result);
            }
            
        } catch (Exception e) {
            log.error("删除路由异常", e);
            return Map.of("code", 500, "message", "删除路由异常: " + e.getMessage());
        }
    }

    /**
     * 更新路由
     */
    @PutMapping("/update")
    @Operation(summary = "更新路由", description = "更新现有的路由规则")
    public Map<String, Object> updateRoute(@RequestBody Map<String, Object> routeInfo) {
        try {
            String routeId = (String) routeInfo.get("routeId");
            String uri = (String) routeInfo.get("uri");
            String path = (String) routeInfo.get("path");
            String method = (String) routeInfo.get("method");
            Integer stripPrefix = (Integer) routeInfo.get("stripPrefix");
            
            String result = dynamicRouteService.updateRoute(routeId, uri, path, method, stripPrefix);
            
            if ("success".equals(result)) {
                return Map.of("code", 200, "message", "路由更新成功");
            } else {
                return Map.of("code", 500, "message", "路由更新失败: " + result);
            }
            
        } catch (Exception e) {
            log.error("更新路由异常", e);
            return Map.of("code", 500, "message", "更新路由异常: " + e.getMessage());
        }
    }

    /**
     * 刷新路由
     */
    @PostMapping("/refresh")
    @Operation(summary = "刷新路由", description = "手动刷新所有路由配置")
    public Map<String, Object> refreshRoutes() {
        try {
            dynamicRouteService.refreshRoutes();
            return Map.of("code", 200, "message", "路由刷新成功");
            
        } catch (Exception e) {
            log.error("刷新路由异常", e);
            return Map.of("code", 500, "message", "刷新路由异常: " + e.getMessage());
        }
    }

    /**
     * 为接口添加路由
     */
    @PostMapping("/interface")
    @Operation(summary = "添加接口路由", description = "为新上架的接口添加路由")
    public Map<String, Object> addInterfaceRoute(@RequestBody Map<String, Object> interfaceInfo) {
        try {
            String interfaceId = (String) interfaceInfo.get("interfaceId");
            String path = (String) interfaceInfo.get("path");
            String method = (String) interfaceInfo.get("method");
            String targetService = (String) interfaceInfo.get("targetService");
            
            String result = dynamicRouteService.addInterfaceRoute(interfaceId, path, method, targetService);
            
            if ("success".equals(result)) {
                return Map.of("code", 200, "message", "接口路由添加成功", "data", "interface-" + interfaceId);
            } else {
                return Map.of("code", 500, "message", "接口路由添加失败: " + result);
            }
            
        } catch (Exception e) {
            log.error("添加接口路由异常", e);
            return Map.of("code", 500, "message", "添加接口路由异常: " + e.getMessage());
        }
    }

    /**
     * 删除接口路由
     */
    @DeleteMapping("/interface/{interfaceId}")
    @Operation(summary = "删除接口路由", description = "删除指定接口的路由")
    public Map<String, Object> deleteInterfaceRoute(@PathVariable String interfaceId) {
        try {
            String result = dynamicRouteService.deleteInterfaceRoute(interfaceId);
            
            if ("success".equals(result)) {
                return Map.of("code", 200, "message", "接口路由删除成功");
            } else {
                return Map.of("code", 500, "message", "接口路由删除失败: " + result);
            }
            
        } catch (Exception e) {
            log.error("删除接口路由异常", e);
            return Map.of("code", 500, "message", "删除接口路由异常: " + e.getMessage());
        }
    }
}