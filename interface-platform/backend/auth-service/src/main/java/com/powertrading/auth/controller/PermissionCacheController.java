package com.powertrading.auth.controller;

import com.powertrading.auth.service.PermissionCacheService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.*;

/**
 * 权限缓存管理控制器
 *
 * @author PowerTrading Team
 * @since 2024-01-15
 */
@Slf4j
@RestController
@RequestMapping("/api/v1/permission-cache")
@Tag(name = "权限缓存管理", description = "权限缓存的管理和操作接口")
public class PermissionCacheController {

    @Autowired
    private PermissionCacheService permissionCacheService;

    /**
     * 缓存用户权限
     */
    @PostMapping("/cache-user-permissions")
    @Operation(summary = "缓存用户权限", description = "将用户权限存储到缓存中")
    public ResponseEntity<Map<String, Object>> cacheUserPermissions(
            @Parameter(description = "用户ID") @RequestParam String userId,
            @Parameter(description = "权限列表") @RequestParam Set<String> permissions) {
        
        try {
            Set<String> cachedPermissions = permissionCacheService.cacheUserPermissions(userId, permissions);
            
            Map<String, Object> response = new HashMap<>();
            response.put("code", 200);
            response.put("message", "用户权限缓存成功");
            response.put("data", Map.of(
                    "userId", userId,
                    "permissionCount", cachedPermissions.size(),
                    "permissions", cachedPermissions
            ));
            
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            log.error("缓存用户权限失败", e);
            
            Map<String, Object> response = new HashMap<>();
            response.put("code", 500);
            response.put("message", "缓存用户权限失败: " + e.getMessage());
            
            return ResponseEntity.status(500).body(response);
        }
    }

    /**
     * 获取用户权限
     */
    @GetMapping("/user-permissions")
    @Operation(summary = "获取用户权限", description = "从缓存中获取用户权限")
    public ResponseEntity<Map<String, Object>> getUserPermissions(
            @Parameter(description = "用户ID") @RequestParam String userId) {
        
        try {
            Set<String> permissions = permissionCacheService.getUserPermissions(userId);
            
            Map<String, Object> response = new HashMap<>();
            response.put("code", 200);
            response.put("message", "获取用户权限成功");
            response.put("data", Map.of(
                    "userId", userId,
                    "permissionCount", permissions.size(),
                    "permissions", permissions
            ));
            
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            log.error("获取用户权限失败", e);
            
            Map<String, Object> response = new HashMap<>();
            response.put("code", 500);
            response.put("message", "获取用户权限失败: " + e.getMessage());
            
            return ResponseEntity.status(500).body(response);
        }
    }

    /**
     * 缓存角色权限
     */
    @PostMapping("/cache-role-permissions")
    @Operation(summary = "缓存角色权限", description = "将角色权限存储到缓存中")
    public ResponseEntity<Map<String, Object>> cacheRolePermissions(
            @Parameter(description = "角色名称") @RequestParam String role,
            @Parameter(description = "权限列表") @RequestParam Set<String> permissions) {
        
        try {
            Set<String> cachedPermissions = permissionCacheService.cacheRolePermissions(role, permissions);
            
            Map<String, Object> response = new HashMap<>();
            response.put("code", 200);
            response.put("message", "角色权限缓存成功");
            response.put("data", Map.of(
                    "role", role,
                    "permissionCount", cachedPermissions.size(),
                    "permissions", cachedPermissions
            ));
            
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            log.error("缓存角色权限失败", e);
            
            Map<String, Object> response = new HashMap<>();
            response.put("code", 500);
            response.put("message", "缓存角色权限失败: " + e.getMessage());
            
            return ResponseEntity.status(500).body(response);
        }
    }

    /**
     * 获取角色权限
     */
    @GetMapping("/role-permissions")
    @Operation(summary = "获取角色权限", description = "从缓存中获取角色权限")
    public ResponseEntity<Map<String, Object>> getRolePermissions(
            @Parameter(description = "角色名称") @RequestParam String role) {
        
        try {
            Set<String> permissions = permissionCacheService.getRolePermissions(role);
            
            Map<String, Object> response = new HashMap<>();
            response.put("code", 200);
            response.put("message", "获取角色权限成功");
            response.put("data", Map.of(
                    "role", role,
                    "permissionCount", permissions.size(),
                    "permissions", permissions
            ));
            
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            log.error("获取角色权限失败", e);
            
            Map<String, Object> response = new HashMap<>();
            response.put("code", 500);
            response.put("message", "获取角色权限失败: " + e.getMessage());
            
            return ResponseEntity.status(500).body(response);
        }
    }

    /**
     * 缓存接口权限
     */
    @PostMapping("/cache-interface-permissions")
    @Operation(summary = "缓存接口权限", description = "将接口所需权限存储到缓存中")
    public ResponseEntity<Map<String, Object>> cacheInterfacePermissions(
            @Parameter(description = "接口路径") @RequestParam String interfacePath,
            @Parameter(description = "所需权限列表") @RequestParam Set<String> requiredPermissions) {
        
        try {
            permissionCacheService.cacheInterfacePermissions(interfacePath, requiredPermissions);
            
            Map<String, Object> response = new HashMap<>();
            response.put("code", 200);
            response.put("message", "接口权限缓存成功");
            response.put("data", Map.of(
                    "interfacePath", interfacePath,
                    "permissionCount", requiredPermissions.size(),
                    "requiredPermissions", requiredPermissions
            ));
            
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            log.error("缓存接口权限失败", e);
            
            Map<String, Object> response = new HashMap<>();
            response.put("code", 500);
            response.put("message", "缓存接口权限失败: " + e.getMessage());
            
            return ResponseEntity.status(500).body(response);
        }
    }

    /**
     * 获取接口权限
     */
    @GetMapping("/interface-permissions")
    @Operation(summary = "获取接口权限", description = "从缓存中获取接口所需权限")
    public ResponseEntity<Map<String, Object>> getInterfacePermissions(
            @Parameter(description = "接口路径") @RequestParam String interfacePath) {
        
        try {
            Set<String> permissions = permissionCacheService.getInterfacePermissions(interfacePath);
            
            Map<String, Object> response = new HashMap<>();
            response.put("code", 200);
            response.put("message", "获取接口权限成功");
            response.put("data", Map.of(
                    "interfacePath", interfacePath,
                    "permissionCount", permissions.size(),
                    "requiredPermissions", permissions
            ));
            
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            log.error("获取接口权限失败", e);
            
            Map<String, Object> response = new HashMap<>();
            response.put("code", 500);
            response.put("message", "获取接口权限失败: " + e.getMessage());
            
            return ResponseEntity.status(500).body(response);
        }
    }

    /**
     * 检查用户权限
     */
    @PostMapping("/check-permission")
    @Operation(summary = "检查用户权限", description = "检查用户是否具有指定权限")
    public ResponseEntity<Map<String, Object>> checkPermission(
            @Parameter(description = "用户ID") @RequestParam String userId,
            @Parameter(description = "权限名称") @RequestParam String permission) {
        
        try {
            boolean hasPermission = permissionCacheService.hasPermission(userId, permission);
            
            Map<String, Object> response = new HashMap<>();
            response.put("code", 200);
            response.put("message", hasPermission ? "用户具有该权限" : "用户没有该权限");
            response.put("data", Map.of(
                    "userId", userId,
                    "permission", permission,
                    "hasPermission", hasPermission
            ));
            
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            log.error("检查用户权限失败", e);
            
            Map<String, Object> response = new HashMap<>();
            response.put("code", 500);
            response.put("message", "检查用户权限失败: " + e.getMessage());
            
            return ResponseEntity.status(500).body(response);
        }
    }

    /**
     * 检查接口访问权限
     */
    @PostMapping("/check-interface-access")
    @Operation(summary = "检查接口访问权限", description = "检查用户是否可以访问指定接口")
    public ResponseEntity<Map<String, Object>> checkInterfaceAccess(
            @Parameter(description = "用户ID") @RequestParam String userId,
            @Parameter(description = "接口路径") @RequestParam String interfacePath) {
        
        try {
            boolean canAccess = permissionCacheService.canAccessInterface(userId, interfacePath);
            
            Map<String, Object> response = new HashMap<>();
            response.put("code", 200);
            response.put("message", canAccess ? "用户可以访问该接口" : "用户无权限访问该接口");
            response.put("data", Map.of(
                    "userId", userId,
                    "interfacePath", interfacePath,
                    "canAccess", canAccess
            ));
            
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            log.error("检查接口访问权限失败", e);
            
            Map<String, Object> response = new HashMap<>();
            response.put("code", 500);
            response.put("message", "检查接口访问权限失败: " + e.getMessage());
            
            return ResponseEntity.status(500).body(response);
        }
    }

    /**
     * 刷新用户权限缓存
     */
    @PostMapping("/refresh-user-permissions")
    @Operation(summary = "刷新用户权限缓存", description = "清除并重新加载用户权限缓存")
    public ResponseEntity<Map<String, Object>> refreshUserPermissions(
            @Parameter(description = "用户ID") @RequestParam String userId) {
        
        try {
            permissionCacheService.refreshUserPermissions(userId);
            
            Map<String, Object> response = new HashMap<>();
            response.put("code", 200);
            response.put("message", "用户权限缓存刷新成功");
            response.put("data", Map.of("userId", userId));
            
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            log.error("刷新用户权限缓存失败", e);
            
            Map<String, Object> response = new HashMap<>();
            response.put("code", 500);
            response.put("message", "刷新用户权限缓存失败: " + e.getMessage());
            
            return ResponseEntity.status(500).body(response);
        }
    }

    /**
     * 刷新角色权限缓存
     */
    @PostMapping("/refresh-role-permissions")
    @Operation(summary = "刷新角色权限缓存", description = "清除并重新加载角色权限缓存")
    public ResponseEntity<Map<String, Object>> refreshRolePermissions(
            @Parameter(description = "角色名称") @RequestParam String role) {
        
        try {
            permissionCacheService.refreshRolePermissions(role);
            
            Map<String, Object> response = new HashMap<>();
            response.put("code", 200);
            response.put("message", "角色权限缓存刷新成功");
            response.put("data", Map.of("role", role));
            
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            log.error("刷新角色权限缓存失败", e);
            
            Map<String, Object> response = new HashMap<>();
            response.put("code", 500);
            response.put("message", "刷新角色权限缓存失败: " + e.getMessage());
            
            return ResponseEntity.status(500).body(response);
        }
    }

    /**
     * 批量刷新用户权限缓存
     */
    @PostMapping("/batch-refresh-user-permissions")
    @Operation(summary = "批量刷新用户权限缓存", description = "批量清除并重新加载多个用户的权限缓存")
    public ResponseEntity<Map<String, Object>> batchRefreshUserPermissions(
            @Parameter(description = "用户ID列表") @RequestParam List<String> userIds) {
        
        try {
            permissionCacheService.batchRefreshUserPermissions(userIds);
            
            Map<String, Object> response = new HashMap<>();
            response.put("code", 200);
            response.put("message", "批量刷新用户权限缓存成功");
            response.put("data", Map.of(
                    "userIds", userIds,
                    "userCount", userIds.size()
            ));
            
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            log.error("批量刷新用户权限缓存失败", e);
            
            Map<String, Object> response = new HashMap<>();
            response.put("code", 500);
            response.put("message", "批量刷新用户权限缓存失败: " + e.getMessage());
            
            return ResponseEntity.status(500).body(response);
        }
    }

    /**
     * 清空所有权限缓存
     */
    @PostMapping("/clear-all")
    @Operation(summary = "清空所有权限缓存", description = "清空所有用户、角色、接口的权限缓存")
    public ResponseEntity<Map<String, Object>> clearAllPermissionCache() {
        
        try {
            permissionCacheService.clearAllPermissionCache();
            
            Map<String, Object> response = new HashMap<>();
            response.put("code", 200);
            response.put("message", "所有权限缓存清空成功");
            
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            log.error("清空所有权限缓存失败", e);
            
            Map<String, Object> response = new HashMap<>();
            response.put("code", 500);
            response.put("message", "清空所有权限缓存失败: " + e.getMessage());
            
            return ResponseEntity.status(500).body(response);
        }
    }

    /**
     * 获取缓存统计信息
     */
    @GetMapping("/statistics")
    @Operation(summary = "获取缓存统计信息", description = "获取权限缓存的统计数据")
    public ResponseEntity<Map<String, Object>> getCacheStatistics() {
        
        try {
            Map<String, Object> statistics = permissionCacheService.getCacheStatistics();
            
            Map<String, Object> response = new HashMap<>();
            response.put("code", 200);
            response.put("message", "获取缓存统计信息成功");
            response.put("data", statistics);
            
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            log.error("获取缓存统计信息失败", e);
            
            Map<String, Object> response = new HashMap<>();
            response.put("code", 500);
            response.put("message", "获取缓存统计信息失败: " + e.getMessage());
            
            return ResponseEntity.status(500).body(response);
        }
    }

    /**
     * 预热权限缓存
     */
    @PostMapping("/warmup")
    @Operation(summary = "预热权限缓存", description = "预先加载指定用户和角色的权限到缓存中")
    public ResponseEntity<Map<String, Object>> warmupPermissionCache(
            @Parameter(description = "用户ID列表") @RequestParam(required = false) List<String> userIds,
            @Parameter(description = "角色列表") @RequestParam(required = false) List<String> roles) {
        
        try {
            if (userIds == null) userIds = new ArrayList<>();
            if (roles == null) roles = new ArrayList<>();
            
            permissionCacheService.warmupPermissionCache(userIds, roles);
            
            Map<String, Object> response = new HashMap<>();
            response.put("code", 200);
            response.put("message", "权限缓存预热成功");
            response.put("data", Map.of(
                    "userCount", userIds.size(),
                    "roleCount", roles.size()
            ));
            
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            log.error("预热权限缓存失败", e);
            
            Map<String, Object> response = new HashMap<>();
            response.put("code", 500);
            response.put("message", "预热权限缓存失败: " + e.getMessage());
            
            return ResponseEntity.status(500).body(response);
        }
    }

    /**
     * 健康检查
     */
    @GetMapping("/health")
    @Operation(summary = "健康检查", description = "检查权限缓存服务的健康状态")
    public ResponseEntity<Map<String, Object>> health() {
        Map<String, Object> response = new HashMap<>();
        response.put("code", 200);
        response.put("message", "权限缓存服务运行正常");
        response.put("timestamp", System.currentTimeMillis());
        
        return ResponseEntity.ok(response);
    }
}