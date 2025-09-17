package com.powertrading.user.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.powertrading.user.dto.PageResponse;
import com.powertrading.user.entity.Permission;
import com.powertrading.user.entity.Role;
import com.powertrading.user.entity.RolePermission;
import com.powertrading.user.entity.User;
import com.powertrading.user.mapper.PermissionMapper;
import com.powertrading.user.mapper.RoleMapper;
import com.powertrading.user.mapper.RolePermissionMapper;
import com.powertrading.user.mapper.UserMapper;
import com.powertrading.user.service.PermissionService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.time.LocalDateTime;
import java.util.*;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

/**
 * 权限管理服务实现类
 *
 * @author PowerTrading Team
 * @since 2024-01-15
 */
@Service
@Transactional
public class PermissionServiceImpl implements PermissionService {

    private static final Logger logger = LoggerFactory.getLogger(PermissionServiceImpl.class);

    @Autowired
    private RoleMapper roleMapper;

    @Autowired
    private PermissionMapper permissionMapper;

    @Autowired
    private RolePermissionMapper rolePermissionMapper;

    @Autowired
    private UserMapper userMapper;

    @Autowired
    private RedisTemplate<String, Object> redisTemplate;

    private static final String REDIS_ROLE_PREFIX = "role:";
    private static final String REDIS_PERMISSION_PREFIX = "permission:";
    private static final String REDIS_USER_PERMISSION_PREFIX = "user_permission:";
    private static final int CACHE_EXPIRE_HOURS = 2;

    // ========== 角色管理 ==========

    @Override
    public String createRole(Role role) {
        logger.info("创建角色: {}", role.getRoleName());

        // 检查角色编码是否已存在
        if (roleMapper.findByRoleCode(role.getRoleCode()) != null) {
            throw new RuntimeException("角色编码已存在: " + role.getRoleCode());
        }

        // 检查角色名称是否已存在
        if (roleMapper.countByRoleNameExcludeId(role.getRoleName(), "") > 0) {
            throw new RuntimeException("角色名称已存在: " + role.getRoleName());
        }

        role.setRoleId(UUID.randomUUID().toString().replace("-", ""));
        role.setCreateTime(LocalDateTime.now());
        role.setUpdateTime(LocalDateTime.now());
        
        if (!StringUtils.hasText(role.getStatus())) {
            role.setStatus("ACTIVE");
        }

        roleMapper.insert(role);
        
        // 清除相关缓存
        clearRoleCache();
        
        logger.info("角色创建成功: {}", role.getRoleId());
        return role.getRoleId();
    }

    @Override
    public void updateRole(String roleId, Role role) {
        logger.info("更新角色: {}", roleId);

        Role existingRole = roleMapper.selectById(roleId);
        if (existingRole == null) {
            throw new RuntimeException("角色不存在: " + roleId);
        }

        // 检查角色编码是否已被其他角色使用
        if (StringUtils.hasText(role.getRoleCode()) && 
            roleMapper.countByRoleCodeExcludeId(role.getRoleCode(), roleId) > 0) {
            throw new RuntimeException("角色编码已存在: " + role.getRoleCode());
        }

        // 检查角色名称是否已被其他角色使用
        if (StringUtils.hasText(role.getRoleName()) && 
            roleMapper.countByRoleNameExcludeId(role.getRoleName(), roleId) > 0) {
            throw new RuntimeException("角色名称已存在: " + role.getRoleName());
        }

        // 更新角色信息
        existingRole.setRoleName(role.getRoleName());
        existingRole.setRoleCode(role.getRoleCode());
        existingRole.setDescription(role.getDescription());
        existingRole.setStatus(role.getStatus());
        existingRole.setUpdateTime(LocalDateTime.now());

        roleMapper.updateById(existingRole);
        
        // 清除相关缓存
        clearRoleCache();
        clearUserPermissionCache();
        
        logger.info("角色更新成功: {}", roleId);
    }

    @Override
    public void deleteRole(String roleId) {
        logger.info("删除角色: {}", roleId);

        Role role = roleMapper.selectById(roleId);
        if (role == null) {
            throw new RuntimeException("角色不存在: " + roleId);
        }

        // 检查是否有用户使用该角色
        QueryWrapper<User> userQuery = new QueryWrapper<>();
        userQuery.eq("role", role.getRoleCode());
        long userCount = userMapper.selectCount(userQuery);
        if (userCount > 0) {
            throw new RuntimeException("该角色正在被用户使用，无法删除");
        }

        // 删除角色权限关联
        rolePermissionMapper.deleteByRoleId(roleId);
        
        // 删除角色
        roleMapper.deleteById(roleId);
        
        // 清除相关缓存
        clearRoleCache();
        clearUserPermissionCache();
        
        logger.info("角色删除成功: {}", roleId);
    }

    @Override
    public Role getRoleById(String roleId) {
        String cacheKey = REDIS_ROLE_PREFIX + "id:" + roleId;
        Role role = (Role) redisTemplate.opsForValue().get(cacheKey);
        
        if (role == null) {
            role = roleMapper.selectById(roleId);
            if (role != null) {
                redisTemplate.opsForValue().set(cacheKey, role, CACHE_EXPIRE_HOURS, TimeUnit.HOURS);
            }
        }
        
        return role;
    }

    @Override
    public Role getRoleByCode(String roleCode) {
        String cacheKey = REDIS_ROLE_PREFIX + "code:" + roleCode;
        Role role = (Role) redisTemplate.opsForValue().get(cacheKey);
        
        if (role == null) {
            role = roleMapper.findByRoleCode(roleCode);
            if (role != null) {
                redisTemplate.opsForValue().set(cacheKey, role, CACHE_EXPIRE_HOURS, TimeUnit.HOURS);
            }
        }
        
        return role;
    }

    @Override
    public PageResponse<Role> getRoleList(int page, int size, String status, String keyword) {
        Page<Role> pageParam = new Page<>(page, size);
        QueryWrapper<Role> queryWrapper = new QueryWrapper<>();
        
        if (StringUtils.hasText(status)) {
            queryWrapper.eq("status", status);
        }
        
        if (StringUtils.hasText(keyword)) {
            queryWrapper.and(wrapper -> wrapper
                .like("role_name", keyword)
                .or().like("role_code", keyword)
                .or().like("description", keyword)
            );
        }
        
        queryWrapper.orderByDesc("create_time");
        
        IPage<Role> rolePage = roleMapper.selectPage(pageParam, queryWrapper);
        
        return new PageResponse<>(rolePage.getRecords(), rolePage.getTotal(), 
            rolePage.getCurrent(), rolePage.getSize());
    }

    @Override
    public List<Role> getAllActiveRoles() {
        String cacheKey = REDIS_ROLE_PREFIX + "active:all";
        @SuppressWarnings("unchecked")
        List<Role> roles = (List<Role>) redisTemplate.opsForValue().get(cacheKey);
        
        if (roles == null) {
            roles = roleMapper.findByStatus("ACTIVE");
            redisTemplate.opsForValue().set(cacheKey, roles, CACHE_EXPIRE_HOURS, TimeUnit.HOURS);
        }
        
        return roles;
    }

    // ========== 权限管理 ==========

    @Override
    public String createPermission(Permission permission) {
        logger.info("创建权限: {}", permission.getPermissionName());

        // 检查权限编码是否已存在
        if (permissionMapper.findByPermissionCode(permission.getPermissionCode()) != null) {
            throw new RuntimeException("权限编码已存在: " + permission.getPermissionCode());
        }

        // 检查权限名称是否已存在
        if (permissionMapper.countByPermissionNameExcludeId(permission.getPermissionName(), "") > 0) {
            throw new RuntimeException("权限名称已存在: " + permission.getPermissionName());
        }

        permission.setPermissionId(UUID.randomUUID().toString().replace("-", ""));
        permission.setCreateTime(LocalDateTime.now());
        permission.setUpdateTime(LocalDateTime.now());
        
        if (!StringUtils.hasText(permission.getStatus())) {
            permission.setStatus("ACTIVE");
        }
        
        if (permission.getSortOrder() == null) {
            permission.setSortOrder(0);
        }

        permissionMapper.insert(permission);
        
        // 清除相关缓存
        clearPermissionCache();
        clearUserPermissionCache();
        
        logger.info("权限创建成功: {}", permission.getPermissionId());
        return permission.getPermissionId();
    }

    @Override
    public void updatePermission(String permissionId, Permission permission) {
        logger.info("更新权限: {}", permissionId);

        Permission existingPermission = permissionMapper.selectById(permissionId);
        if (existingPermission == null) {
            throw new RuntimeException("权限不存在: " + permissionId);
        }

        // 检查权限编码是否已被其他权限使用
        if (StringUtils.hasText(permission.getPermissionCode()) && 
            permissionMapper.countByPermissionCodeExcludeId(permission.getPermissionCode(), permissionId) > 0) {
            throw new RuntimeException("权限编码已存在: " + permission.getPermissionCode());
        }

        // 检查权限名称是否已被其他权限使用
        if (StringUtils.hasText(permission.getPermissionName()) && 
            permissionMapper.countByPermissionNameExcludeId(permission.getPermissionName(), permissionId) > 0) {
            throw new RuntimeException("权限名称已存在: " + permission.getPermissionName());
        }

        // 更新权限信息
        existingPermission.setPermissionName(permission.getPermissionName());
        existingPermission.setPermissionCode(permission.getPermissionCode());
        existingPermission.setResourceType(permission.getResourceType());
        existingPermission.setResourcePath(permission.getResourcePath());
        existingPermission.setParentId(permission.getParentId());
        existingPermission.setSortOrder(permission.getSortOrder());
        existingPermission.setDescription(permission.getDescription());
        existingPermission.setStatus(permission.getStatus());
        existingPermission.setUpdateTime(LocalDateTime.now());

        permissionMapper.updateById(existingPermission);
        
        // 清除相关缓存
        clearPermissionCache();
        clearUserPermissionCache();
        
        logger.info("权限更新成功: {}", permissionId);
    }

    @Override
    public void deletePermission(String permissionId) {
        logger.info("删除权限: {}", permissionId);

        Permission permission = permissionMapper.selectById(permissionId);
        if (permission == null) {
            throw new RuntimeException("权限不存在: " + permissionId);
        }

        // 检查是否有子权限
        List<Permission> children = permissionMapper.findByParentId(permissionId);
        if (!children.isEmpty()) {
            throw new RuntimeException("该权限存在子权限，无法删除");
        }

        // 删除角色权限关联
        rolePermissionMapper.deleteByPermissionId(permissionId);
        
        // 删除权限
        permissionMapper.deleteById(permissionId);
        
        // 清除相关缓存
        clearPermissionCache();
        clearUserPermissionCache();
        
        logger.info("权限删除成功: {}", permissionId);
    }

    @Override
    public Permission getPermissionById(String permissionId) {
        String cacheKey = REDIS_PERMISSION_PREFIX + "id:" + permissionId;
        Permission permission = (Permission) redisTemplate.opsForValue().get(cacheKey);
        
        if (permission == null) {
            permission = permissionMapper.selectById(permissionId);
            if (permission != null) {
                redisTemplate.opsForValue().set(cacheKey, permission, CACHE_EXPIRE_HOURS, TimeUnit.HOURS);
            }
        }
        
        return permission;
    }

    @Override
    public Permission getPermissionByCode(String permissionCode) {
        String cacheKey = REDIS_PERMISSION_PREFIX + "code:" + permissionCode;
        Permission permission = (Permission) redisTemplate.opsForValue().get(cacheKey);
        
        if (permission == null) {
            permission = permissionMapper.findByPermissionCode(permissionCode);
            if (permission != null) {
                redisTemplate.opsForValue().set(cacheKey, permission, CACHE_EXPIRE_HOURS, TimeUnit.HOURS);
            }
        }
        
        return permission;
    }

    @Override
    public PageResponse<Permission> getPermissionList(int page, int size, String resourceType, String status, String keyword) {
        Page<Permission> pageParam = new Page<>(page, size);
        QueryWrapper<Permission> queryWrapper = new QueryWrapper<>();
        
        if (StringUtils.hasText(resourceType)) {
            queryWrapper.eq("resource_type", resourceType);
        }
        
        if (StringUtils.hasText(status)) {
            queryWrapper.eq("status", status);
        }
        
        if (StringUtils.hasText(keyword)) {
            queryWrapper.and(wrapper -> wrapper
                .like("permission_name", keyword)
                .or().like("permission_code", keyword)
                .or().like("description", keyword)
            );
        }
        
        queryWrapper.orderByAsc("sort_order").orderByAsc("permission_name");
        
        IPage<Permission> permissionPage = permissionMapper.selectPage(pageParam, queryWrapper);
        
        return new PageResponse<>(permissionPage.getRecords(), permissionPage.getTotal(), 
            permissionPage.getCurrent(), permissionPage.getSize());
    }

    @Override
    public List<Permission> getPermissionTree() {
        String cacheKey = REDIS_PERMISSION_PREFIX + "tree:all";
        @SuppressWarnings("unchecked")
        List<Permission> tree = (List<Permission>) redisTemplate.opsForValue().get(cacheKey);
        
        if (tree == null) {
            List<Permission> allPermissions = permissionMapper.findAllActive();
            tree = buildPermissionTree(allPermissions, null);
            redisTemplate.opsForValue().set(cacheKey, tree, CACHE_EXPIRE_HOURS, TimeUnit.HOURS);
        }
        
        return tree;
    }

    @Override
    public List<Permission> getPermissionsByParentId(String parentId) {
        return permissionMapper.findByParentId(parentId);
    }

    @Override
    public List<Permission> getAllActivePermissions() {
        String cacheKey = REDIS_PERMISSION_PREFIX + "active:all";
        @SuppressWarnings("unchecked")
        List<Permission> permissions = (List<Permission>) redisTemplate.opsForValue().get(cacheKey);
        
        if (permissions == null) {
            permissions = permissionMapper.findByStatus("ACTIVE");
            redisTemplate.opsForValue().set(cacheKey, permissions, CACHE_EXPIRE_HOURS, TimeUnit.HOURS);
        }
        
        return permissions;
    }

    // ========== 角色权限关联管理 ==========

    @Override
    public void assignPermissionsToRole(String roleId, List<String> permissionIds) {
        logger.info("为角色分配权限: {} -> {}", roleId, permissionIds);

        Role role = roleMapper.selectById(roleId);
        if (role == null) {
            throw new RuntimeException("角色不存在: " + roleId);
        }

        // 删除现有权限关联
        rolePermissionMapper.deleteByRoleId(roleId);
        
        // 添加新的权限关联
        if (permissionIds != null && !permissionIds.isEmpty()) {
            List<RolePermission> rolePermissions = permissionIds.stream()
                .map(permissionId -> new RolePermission(roleId, permissionId))
                .collect(Collectors.toList());
            
            rolePermissionMapper.batchInsert(rolePermissions);
        }
        
        // 清除相关缓存
        clearUserPermissionCache();
        
        logger.info("角色权限分配成功: {}", roleId);
    }

    @Override
    public void removePermissionsFromRole(String roleId, List<String> permissionIds) {
        logger.info("移除角色权限: {} -> {}", roleId, permissionIds);

        if (permissionIds != null && !permissionIds.isEmpty()) {
            for (String permissionId : permissionIds) {
                rolePermissionMapper.deleteByRoleIdAndPermissionId(roleId, permissionId);
            }
        }
        
        // 清除相关缓存
        clearUserPermissionCache();
        
        logger.info("角色权限移除成功: {}", roleId);
    }

    @Override
    public List<Permission> getRolePermissions(String roleId) {
        String cacheKey = REDIS_PERMISSION_PREFIX + "role:" + roleId;
        @SuppressWarnings("unchecked")
        List<Permission> permissions = (List<Permission>) redisTemplate.opsForValue().get(cacheKey);
        
        if (permissions == null) {
            permissions = permissionMapper.findByRoleId(roleId);
            redisTemplate.opsForValue().set(cacheKey, permissions, CACHE_EXPIRE_HOURS, TimeUnit.HOURS);
        }
        
        return permissions;
    }

    @Override
    public List<String> getRolePermissionIds(String roleId) {
        return rolePermissionMapper.findPermissionIdsByRoleId(roleId);
    }

    @Override
    public List<String> getRolePermissionCodes(String roleCode) {
        String cacheKey = REDIS_PERMISSION_PREFIX + "codes:role:" + roleCode;
        @SuppressWarnings("unchecked")
        List<String> permissionCodes = (List<String>) redisTemplate.opsForValue().get(cacheKey);
        
        if (permissionCodes == null) {
            permissionCodes = permissionMapper.findPermissionCodesByRoleCode(roleCode);
            redisTemplate.opsForValue().set(cacheKey, permissionCodes, CACHE_EXPIRE_HOURS, TimeUnit.HOURS);
        }
        
        return permissionCodes;
    }

    @Override
    public List<Permission> getUserPermissions(String userId) {
        String cacheKey = REDIS_USER_PERMISSION_PREFIX + userId;
        @SuppressWarnings("unchecked")
        List<Permission> permissions = (List<Permission>) redisTemplate.opsForValue().get(cacheKey);
        
        if (permissions == null) {
            permissions = permissionMapper.findByUserId(userId);
            redisTemplate.opsForValue().set(cacheKey, permissions, CACHE_EXPIRE_HOURS, TimeUnit.HOURS);
        }
        
        return permissions;
    }

    @Override
    public List<String> getUserPermissionCodes(String userId) {
        List<Permission> permissions = getUserPermissions(userId);
        return permissions.stream()
            .map(Permission::getPermissionCode)
            .collect(Collectors.toList());
    }

    @Override
    public boolean hasPermission(String userId, String permissionCode) {
        List<String> userPermissions = getUserPermissionCodes(userId);
        return userPermissions.contains(permissionCode);
    }

    @Override
    public boolean hasAnyPermission(String userId, List<String> permissionCodes) {
        List<String> userPermissions = getUserPermissionCodes(userId);
        return permissionCodes.stream().anyMatch(userPermissions::contains);
    }

    @Override
    public boolean hasAllPermissions(String userId, List<String> permissionCodes) {
        List<String> userPermissions = getUserPermissionCodes(userId);
        return userPermissions.containsAll(permissionCodes);
    }

    // ========== 权限验证 ==========

    @Override
    public boolean checkResourcePermission(String userId, String resourcePath, String httpMethod) {
        List<Permission> userPermissions = getUserPermissions(userId);
        
        return userPermissions.stream()
            .filter(p -> "API".equals(p.getResourceType()))
            .anyMatch(p -> matchResourcePath(p.getResourcePath(), resourcePath));
    }

    @Override
    public List<Permission> getUserMenuPermissions(String userId) {
        List<Permission> userPermissions = getUserPermissions(userId);
        return userPermissions.stream()
            .filter(p -> "MENU".equals(p.getResourceType()))
            .collect(Collectors.toList());
    }

    @Override
    public List<Permission> getUserApiPermissions(String userId) {
        List<Permission> userPermissions = getUserPermissions(userId);
        return userPermissions.stream()
            .filter(p -> "API".equals(p.getResourceType()))
            .collect(Collectors.toList());
    }

    // ========== 私有辅助方法 ==========

    private List<Permission> buildPermissionTree(List<Permission> permissions, String parentId) {
        return permissions.stream()
            .filter(p -> Objects.equals(p.getParentId(), parentId))
            .map(p -> {
                List<Permission> children = buildPermissionTree(permissions, p.getPermissionId());
                // 这里可以设置children属性，如果Permission实体有children字段的话
                return p;
            })
            .collect(Collectors.toList());
    }

    private boolean matchResourcePath(String permissionPath, String requestPath) {
        if (permissionPath == null || requestPath == null) {
            return false;
        }
        
        // 简单的路径匹配，支持通配符 **
        if (permissionPath.endsWith("/**")) {
            String basePath = permissionPath.substring(0, permissionPath.length() - 3);
            return requestPath.startsWith(basePath);
        }
        
        return permissionPath.equals(requestPath);
    }

    private void clearRoleCache() {
        Set<String> keys = redisTemplate.keys(REDIS_ROLE_PREFIX + "*");
        if (keys != null && !keys.isEmpty()) {
            redisTemplate.delete(keys);
        }
    }

    private void clearPermissionCache() {
        Set<String> keys = redisTemplate.keys(REDIS_PERMISSION_PREFIX + "*");
        if (keys != null && !keys.isEmpty()) {
            redisTemplate.delete(keys);
        }
    }

    private void clearUserPermissionCache() {
        Set<String> keys = redisTemplate.keys(REDIS_USER_PERMISSION_PREFIX + "*");
        if (keys != null && !keys.isEmpty()) {
            redisTemplate.delete(keys);
        }
    }
}