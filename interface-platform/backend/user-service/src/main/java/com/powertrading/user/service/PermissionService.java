package com.powertrading.user.service;

import com.powertrading.user.dto.PageResponse;
import com.powertrading.user.entity.Permission;
import com.powertrading.user.entity.Role;

import java.util.List;

/**
 * 权限管理服务接口
 *
 * @author PowerTrading Team
 * @since 2024-01-15
 */
public interface PermissionService {

    // ========== 角色管理 ==========

    /**
     * 创建角色
     *
     * @param role 角色信息
     * @return 角色ID
     */
    String createRole(Role role);

    /**
     * 更新角色
     *
     * @param roleId 角色ID
     * @param role 角色信息
     */
    void updateRole(String roleId, Role role);

    /**
     * 删除角色
     *
     * @param roleId 角色ID
     */
    void deleteRole(String roleId);

    /**
     * 根据ID获取角色
     *
     * @param roleId 角色ID
     * @return 角色信息
     */
    Role getRoleById(String roleId);

    /**
     * 根据角色编码获取角色
     *
     * @param roleCode 角色编码
     * @return 角色信息
     */
    Role getRoleByCode(String roleCode);

    /**
     * 获取角色列表
     *
     * @param page 页码
     * @param size 每页大小
     * @param status 状态筛选
     * @param keyword 关键词搜索
     * @return 角色分页列表
     */
    PageResponse<Role> getRoleList(int page, int size, String status, String keyword);

    /**
     * 获取所有有效角色
     *
     * @return 角色列表
     */
    List<Role> getAllActiveRoles();

    // ========== 权限管理 ==========

    /**
     * 创建权限
     *
     * @param permission 权限信息
     * @return 权限ID
     */
    String createPermission(Permission permission);

    /**
     * 更新权限
     *
     * @param permissionId 权限ID
     * @param permission 权限信息
     */
    void updatePermission(String permissionId, Permission permission);

    /**
     * 删除权限
     *
     * @param permissionId 权限ID
     */
    void deletePermission(String permissionId);

    /**
     * 根据ID获取权限
     *
     * @param permissionId 权限ID
     * @return 权限信息
     */
    Permission getPermissionById(String permissionId);

    /**
     * 根据权限编码获取权限
     *
     * @param permissionCode 权限编码
     * @return 权限信息
     */
    Permission getPermissionByCode(String permissionCode);

    /**
     * 获取权限列表
     *
     * @param page 页码
     * @param size 每页大小
     * @param resourceType 资源类型筛选
     * @param status 状态筛选
     * @param keyword 关键词搜索
     * @return 权限分页列表
     */
    PageResponse<Permission> getPermissionList(int page, int size, String resourceType, String status, String keyword);

    /**
     * 获取权限树结构
     *
     * @return 权限树列表
     */
    List<Permission> getPermissionTree();

    /**
     * 根据父权限ID获取子权限列表
     *
     * @param parentId 父权限ID
     * @return 子权限列表
     */
    List<Permission> getPermissionsByParentId(String parentId);

    /**
     * 获取所有有效权限
     *
     * @return 权限列表
     */
    List<Permission> getAllActivePermissions();

    // ========== 角色权限关联管理 ==========

    /**
     * 为角色分配权限
     *
     * @param roleId 角色ID
     * @param permissionIds 权限ID列表
     */
    void assignPermissionsToRole(String roleId, List<String> permissionIds);

    /**
     * 移除角色的权限
     *
     * @param roleId 角色ID
     * @param permissionIds 权限ID列表
     */
    void removePermissionsFromRole(String roleId, List<String> permissionIds);

    /**
     * 获取角色的权限列表
     *
     * @param roleId 角色ID
     * @return 权限列表
     */
    List<Permission> getRolePermissions(String roleId);

    /**
     * 获取角色的权限ID列表
     *
     * @param roleId 角色ID
     * @return 权限ID列表
     */
    List<String> getRolePermissionIds(String roleId);

    /**
     * 获取角色的权限编码列表
     *
     * @param roleCode 角色编码
     * @return 权限编码列表
     */
    List<String> getRolePermissionCodes(String roleCode);

    /**
     * 获取用户的权限列表
     *
     * @param userId 用户ID
     * @return 权限列表
     */
    List<Permission> getUserPermissions(String userId);

    /**
     * 获取用户的权限编码列表
     *
     * @param userId 用户ID
     * @return 权限编码列表
     */
    List<String> getUserPermissionCodes(String userId);

    /**
     * 检查用户是否拥有指定权限
     *
     * @param userId 用户ID
     * @param permissionCode 权限编码
     * @return 是否拥有权限
     */
    boolean hasPermission(String userId, String permissionCode);

    /**
     * 检查用户是否拥有任一权限
     *
     * @param userId 用户ID
     * @param permissionCodes 权限编码列表
     * @return 是否拥有任一权限
     */
    boolean hasAnyPermission(String userId, List<String> permissionCodes);

    /**
     * 检查用户是否拥有所有权限
     *
     * @param userId 用户ID
     * @param permissionCodes 权限编码列表
     * @return 是否拥有所有权限
     */
    boolean hasAllPermissions(String userId, List<String> permissionCodes);

    // ========== 权限验证 ==========

    /**
     * 验证资源访问权限
     *
     * @param userId 用户ID
     * @param resourcePath 资源路径
     * @param httpMethod HTTP方法
     * @return 是否有权限访问
     */
    boolean checkResourcePermission(String userId, String resourcePath, String httpMethod);

    /**
     * 获取用户可访问的菜单权限
     *
     * @param userId 用户ID
     * @return 菜单权限列表
     */
    List<Permission> getUserMenuPermissions(String userId);

    /**
     * 获取用户可访问的API权限
     *
     * @param userId 用户ID
     * @return API权限列表
     */
    List<Permission> getUserApiPermissions(String userId);
}