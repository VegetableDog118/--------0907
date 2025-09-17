package com.powertrading.auth.service;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

/**
 * 权限缓存管理服务
 *
 * @author PowerTrading Team
 * @since 2024-01-15
 */
@Slf4j
@Service
public class PermissionCacheService {

    @Autowired
    private RedisTemplate<String, Object> redisTemplate;

    @Value("${auth.permission.cache-ttl:3600}")
    private Long permissionCacheTtl; // 权限缓存TTL，默认1小时

    @Value("${auth.permission.refresh-threshold:300}")
    private Long refreshThreshold; // 刷新阈值，默认5分钟

    private static final String USER_PERMISSIONS_PREFIX = "auth:permissions:user:";
    private static final String ROLE_PERMISSIONS_PREFIX = "auth:permissions:role:";
    private static final String PERMISSION_CACHE_INFO_PREFIX = "auth:cache:info:";
    private static final String INTERFACE_PERMISSIONS_PREFIX = "auth:permissions:interface:";
    private static final DateTimeFormatter DATETIME_FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");

    /**
     * 缓存用户权限
     */
    @Cacheable(value = "userPermissions", key = "#userId")
    public Set<String> cacheUserPermissions(String userId, Set<String> permissions) {
        try {
            String cacheKey = USER_PERMISSIONS_PREFIX + userId;
            
            // 存储权限集合
            redisTemplate.opsForSet().getOperations().delete(cacheKey);
            if (permissions != null && !permissions.isEmpty()) {
                redisTemplate.opsForSet().add(cacheKey, permissions.toArray());
            }
            redisTemplate.expire(cacheKey, permissionCacheTtl, TimeUnit.SECONDS);

            // 存储缓存信息
            cacheCacheInfo("user", userId, permissions.size());

            log.info("用户权限已缓存: userId={}, permissionCount={}", userId, permissions.size());
            return permissions;
        } catch (Exception e) {
            log.error("缓存用户权限失败: userId={}", userId, e);
            return permissions;
        }
    }

    /**
     * 获取用户权限（从缓存）
     */
    public Set<String> getUserPermissions(String userId) {
        try {
            String cacheKey = USER_PERMISSIONS_PREFIX + userId;
            Set<Object> cachedPermissions = redisTemplate.opsForSet().members(cacheKey);
            
            if (cachedPermissions != null && !cachedPermissions.isEmpty()) {
                Set<String> permissions = cachedPermissions.stream()
                        .map(Object::toString)
                        .collect(Collectors.toSet());
                
                // 检查是否需要刷新缓存
                if (shouldRefreshCache(cacheKey)) {
                    log.info("用户权限缓存即将过期，建议刷新: userId={}", userId);
                }
                
                return permissions;
            }
            
            return new HashSet<>();
        } catch (Exception e) {
            log.error("获取用户权限缓存失败: userId={}", userId, e);
            return new HashSet<>();
        }
    }

    /**
     * 缓存角色权限
     */
    public Set<String> cacheRolePermissions(String role, Set<String> permissions) {
        try {
            String cacheKey = ROLE_PERMISSIONS_PREFIX + role;
            
            // 存储权限集合
            redisTemplate.opsForSet().getOperations().delete(cacheKey);
            if (permissions != null && !permissions.isEmpty()) {
                redisTemplate.opsForSet().add(cacheKey, permissions.toArray());
            }
            redisTemplate.expire(cacheKey, permissionCacheTtl, TimeUnit.SECONDS);

            // 存储缓存信息
            cacheCacheInfo("role", role, permissions.size());

            log.info("角色权限已缓存: role={}, permissionCount={}", role, permissions.size());
            return permissions;
        } catch (Exception e) {
            log.error("缓存角色权限失败: role={}", role, e);
            return permissions;
        }
    }

    /**
     * 获取角色权限（从缓存）
     */
    public Set<String> getRolePermissions(String role) {
        try {
            String cacheKey = ROLE_PERMISSIONS_PREFIX + role;
            Set<Object> cachedPermissions = redisTemplate.opsForSet().members(cacheKey);
            
            if (cachedPermissions != null && !cachedPermissions.isEmpty()) {
                return cachedPermissions.stream()
                        .map(Object::toString)
                        .collect(Collectors.toSet());
            }
            
            return new HashSet<>();
        } catch (Exception e) {
            log.error("获取角色权限缓存失败: role={}", role, e);
            return new HashSet<>();
        }
    }

    /**
     * 缓存接口权限映射
     */
    public void cacheInterfacePermissions(String interfacePath, Set<String> requiredPermissions) {
        try {
            String cacheKey = INTERFACE_PERMISSIONS_PREFIX + interfacePath;
            
            // 存储接口所需权限
            redisTemplate.opsForSet().getOperations().delete(cacheKey);
            if (requiredPermissions != null && !requiredPermissions.isEmpty()) {
                redisTemplate.opsForSet().add(cacheKey, requiredPermissions.toArray());
            }
            redisTemplate.expire(cacheKey, permissionCacheTtl * 2, TimeUnit.SECONDS); // 接口权限缓存时间更长

            log.info("接口权限已缓存: interface={}, permissionCount={}", 
                    interfacePath, requiredPermissions.size());
        } catch (Exception e) {
            log.error("缓存接口权限失败: interface={}", interfacePath, e);
        }
    }

    /**
     * 获取接口所需权限
     */
    public Set<String> getInterfacePermissions(String interfacePath) {
        try {
            String cacheKey = INTERFACE_PERMISSIONS_PREFIX + interfacePath;
            Set<Object> cachedPermissions = redisTemplate.opsForSet().members(cacheKey);
            
            if (cachedPermissions != null && !cachedPermissions.isEmpty()) {
                return cachedPermissions.stream()
                        .map(Object::toString)
                        .collect(Collectors.toSet());
            }
            
            return new HashSet<>();
        } catch (Exception e) {
            log.error("获取接口权限缓存失败: interface={}", interfacePath, e);
            return new HashSet<>();
        }
    }

    /**
     * 检查用户是否有指定权限（使用缓存）
     */
    public boolean hasPermission(String userId, String permission) {
        try {
            Set<String> userPermissions = getUserPermissions(userId);
            return userPermissions.contains(permission) || userPermissions.contains("*");
        } catch (Exception e) {
            log.error("检查用户权限失败: userId={}, permission={}", userId, permission, e);
            return false;
        }
    }

    /**
     * 检查用户是否有任一权限
     */
    public boolean hasAnyPermission(String userId, Set<String> permissions) {
        try {
            Set<String> userPermissions = getUserPermissions(userId);
            
            // 检查是否有超级权限
            if (userPermissions.contains("*")) {
                return true;
            }
            
            // 检查是否有任一指定权限
            return permissions.stream().anyMatch(userPermissions::contains);
        } catch (Exception e) {
            log.error("检查用户任一权限失败: userId={}, permissions={}", userId, permissions, e);
            return false;
        }
    }

    /**
     * 检查用户是否有所有权限
     */
    public boolean hasAllPermissions(String userId, Set<String> permissions) {
        try {
            Set<String> userPermissions = getUserPermissions(userId);
            
            // 检查是否有超级权限
            if (userPermissions.contains("*")) {
                return true;
            }
            
            // 检查是否有所有指定权限
            return userPermissions.containsAll(permissions);
        } catch (Exception e) {
            log.error("检查用户所有权限失败: userId={}, permissions={}", userId, permissions, e);
            return false;
        }
    }

    /**
     * 检查用户是否可以访问接口
     */
    public boolean canAccessInterface(String userId, String interfacePath) {
        try {
            Set<String> requiredPermissions = getInterfacePermissions(interfacePath);
            
            // 如果接口没有权限要求，允许访问
            if (requiredPermissions.isEmpty()) {
                return true;
            }
            
            // 检查用户是否有任一所需权限
            return hasAnyPermission(userId, requiredPermissions);
        } catch (Exception e) {
            log.error("检查接口访问权限失败: userId={}, interface={}", userId, interfacePath, e);
            return false;
        }
    }

    /**
     * 刷新用户权限缓存
     */
    @CacheEvict(value = "userPermissions", key = "#userId")
    public void refreshUserPermissions(String userId) {
        try {
            String cacheKey = USER_PERMISSIONS_PREFIX + userId;
            redisTemplate.delete(cacheKey);
            
            // 删除缓存信息
            String infoKey = PERMISSION_CACHE_INFO_PREFIX + "user:" + userId;
            redisTemplate.delete(infoKey);
            
            log.info("用户权限缓存已刷新: userId={}", userId);
        } catch (Exception e) {
            log.error("刷新用户权限缓存失败: userId={}", userId, e);
        }
    }

    /**
     * 刷新角色权限缓存
     */
    public void refreshRolePermissions(String role) {
        try {
            String cacheKey = ROLE_PERMISSIONS_PREFIX + role;
            redisTemplate.delete(cacheKey);
            
            // 删除缓存信息
            String infoKey = PERMISSION_CACHE_INFO_PREFIX + "role:" + role;
            redisTemplate.delete(infoKey);
            
            log.info("角色权限缓存已刷新: role={}", role);
        } catch (Exception e) {
            log.error("刷新角色权限缓存失败: role={}", role, e);
        }
    }

    /**
     * 批量刷新用户权限缓存
     */
    public void batchRefreshUserPermissions(List<String> userIds) {
        try {
            for (String userId : userIds) {
                refreshUserPermissions(userId);
            }
            log.info("批量刷新用户权限缓存完成: userCount={}", userIds.size());
        } catch (Exception e) {
            log.error("批量刷新用户权限缓存失败", e);
        }
    }

    /**
     * 清空所有权限缓存
     */
    public void clearAllPermissionCache() {
        try {
            // 清空用户权限缓存
            Set<String> userKeys = redisTemplate.keys(USER_PERMISSIONS_PREFIX + "*");
            if (userKeys != null && !userKeys.isEmpty()) {
                redisTemplate.delete(userKeys);
            }
            
            // 清空角色权限缓存
            Set<String> roleKeys = redisTemplate.keys(ROLE_PERMISSIONS_PREFIX + "*");
            if (roleKeys != null && !roleKeys.isEmpty()) {
                redisTemplate.delete(roleKeys);
            }
            
            // 清空接口权限缓存
            Set<String> interfaceKeys = redisTemplate.keys(INTERFACE_PERMISSIONS_PREFIX + "*");
            if (interfaceKeys != null && !interfaceKeys.isEmpty()) {
                redisTemplate.delete(interfaceKeys);
            }
            
            // 清空缓存信息
            Set<String> infoKeys = redisTemplate.keys(PERMISSION_CACHE_INFO_PREFIX + "*");
            if (infoKeys != null && !infoKeys.isEmpty()) {
                redisTemplate.delete(infoKeys);
            }
            
            log.info("所有权限缓存已清空");
        } catch (Exception e) {
            log.error("清空权限缓存失败", e);
        }
    }

    /**
     * 获取权限缓存统计信息
     */
    public Map<String, Object> getCacheStatistics() {
        try {
            Map<String, Object> stats = new HashMap<>();
            
            // 统计用户权限缓存
            Set<String> userKeys = redisTemplate.keys(USER_PERMISSIONS_PREFIX + "*");
            stats.put("cachedUsers", userKeys != null ? userKeys.size() : 0);
            
            // 统计角色权限缓存
            Set<String> roleKeys = redisTemplate.keys(ROLE_PERMISSIONS_PREFIX + "*");
            stats.put("cachedRoles", roleKeys != null ? roleKeys.size() : 0);
            
            // 统计接口权限缓存
            Set<String> interfaceKeys = redisTemplate.keys(INTERFACE_PERMISSIONS_PREFIX + "*");
            stats.put("cachedInterfaces", interfaceKeys != null ? interfaceKeys.size() : 0);
            
            // 计算缓存命中率（这里简化处理）
            stats.put("hitRate", 0.95); // 实际应该通过统计计算
            
            stats.put("cacheConfig", Map.of(
                    "ttl", permissionCacheTtl,
                    "refreshThreshold", refreshThreshold
            ));
            
            return stats;
        } catch (Exception e) {
            log.error("获取权限缓存统计信息失败", e);
            return new HashMap<>();
        }
    }

    /**
     * 预热权限缓存
     */
    public void warmupPermissionCache(List<String> userIds, List<String> roles) {
        try {
            log.info("开始预热权限缓存: users={}, roles={}", userIds.size(), roles.size());
            
            // 这里应该从数据库加载权限数据并缓存
            // 由于没有数据库连接，这里只是示例
            for (String userId : userIds) {
                Set<String> permissions = loadUserPermissionsFromDatabase(userId);
                cacheUserPermissions(userId, permissions);
            }
            
            for (String role : roles) {
                Set<String> permissions = loadRolePermissionsFromDatabase(role);
                cacheRolePermissions(role, permissions);
            }
            
            log.info("权限缓存预热完成");
        } catch (Exception e) {
            log.error("预热权限缓存失败", e);
        }
    }

    /**
     * 定时刷新即将过期的缓存
     */
    @Scheduled(fixedRate = 300000) // 每5分钟执行一次
    public void refreshExpiringCache() {
        try {
            // 检查用户权限缓存
            Set<String> userKeys = redisTemplate.keys(USER_PERMISSIONS_PREFIX + "*");
            if (userKeys != null) {
                for (String key : userKeys) {
                    if (shouldRefreshCache(key)) {
                        String userId = key.substring(USER_PERMISSIONS_PREFIX.length());
                        // 这里应该重新加载权限并缓存
                        log.info("刷新即将过期的用户权限缓存: userId={}", userId);
                    }
                }
            }
            
            // 检查角色权限缓存
            Set<String> roleKeys = redisTemplate.keys(ROLE_PERMISSIONS_PREFIX + "*");
            if (roleKeys != null) {
                for (String key : roleKeys) {
                    if (shouldRefreshCache(key)) {
                        String role = key.substring(ROLE_PERMISSIONS_PREFIX.length());
                        // 这里应该重新加载权限并缓存
                        log.info("刷新即将过期的角色权限缓存: role={}", role);
                    }
                }
            }
        } catch (Exception e) {
            log.error("定时刷新缓存失败", e);
        }
    }

    /**
     * 检查缓存是否需要刷新
     */
    private boolean shouldRefreshCache(String cacheKey) {
        try {
            Long ttl = redisTemplate.getExpire(cacheKey, TimeUnit.SECONDS);
            return ttl != null && ttl > 0 && ttl < refreshThreshold;
        } catch (Exception e) {
            log.error("检查缓存刷新状态失败: key={}", cacheKey, e);
            return false;
        }
    }

    /**
     * 缓存缓存信息
     */
    private void cacheCacheInfo(String type, String id, int permissionCount) {
        try {
            String infoKey = PERMISSION_CACHE_INFO_PREFIX + type + ":" + id;
            
            Map<String, Object> info = new HashMap<>();
            info.put("type", type);
            info.put("id", id);
            info.put("permissionCount", permissionCount);
            info.put("cacheTime", System.currentTimeMillis());
            info.put("cacheDateTime", LocalDateTime.now().format(DATETIME_FORMATTER));
            
            redisTemplate.opsForHash().putAll(infoKey, info);
            redisTemplate.expire(infoKey, permissionCacheTtl, TimeUnit.SECONDS);
        } catch (Exception e) {
            log.error("缓存缓存信息失败", e);
        }
    }

    /**
     * 从数据库加载用户权限（示例方法）
     */
    private Set<String> loadUserPermissionsFromDatabase(String userId) {
        // 这里应该从数据库加载用户权限
        // 暂时返回示例权限
        return Set.of("interface:read", "interface:write", "user:read");
    }

    /**
     * 从数据库加载角色权限（示例方法）
     */
    private Set<String> loadRolePermissionsFromDatabase(String role) {
        // 这里应该从数据库加载角色权限
        // 暂时返回示例权限
        switch (role) {
            case "admin":
                return Set.of("*"); // 超级权限
            case "user":
                return Set.of("interface:read", "user:read");
            default:
                return new HashSet<>();
        }
    }
}