package com.powertrading.user.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.powertrading.user.entity.Permission;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

import java.util.List;

/**
 * 权限Mapper接口
 *
 * @author PowerTrading Team
 * @since 2024-01-15
 */
@Mapper
public interface PermissionMapper extends BaseMapper<Permission> {

    /**
     * 根据权限编码查询权限
     *
     * @param permissionCode 权限编码
     * @return 权限信息
     */
    @Select("SELECT * FROM permissions WHERE permission_code = #{permissionCode} AND deleted = 0")
    Permission findByPermissionCode(@Param("permissionCode") String permissionCode);

    /**
     * 根据角色ID查询权限列表
     *
     * @param roleId 角色ID
     * @return 权限列表
     */
    @Select("SELECT p.* FROM permissions p " +
            "INNER JOIN role_permissions rp ON p.permission_id = rp.permission_id " +
            "WHERE rp.role_id = #{roleId} AND p.deleted = 0 AND p.status = 'ACTIVE' " +
            "ORDER BY p.sort_order, p.permission_name")
    List<Permission> findByRoleId(@Param("roleId") String roleId);

    /**
     * 根据用户ID查询权限列表
     *
     * @param userId 用户ID
     * @return 权限列表
     */
    @Select("SELECT DISTINCT p.* FROM permissions p " +
            "INNER JOIN role_permissions rp ON p.permission_id = rp.permission_id " +
            "INNER JOIN users u ON u.role = (SELECT role_code FROM roles WHERE role_id = rp.role_id) " +
            "WHERE u.user_id = #{userId} AND p.deleted = 0 AND p.status = 'ACTIVE' " +
            "ORDER BY p.sort_order, p.permission_name")
    List<Permission> findByUserId(@Param("userId") String userId);

    /**
     * 根据父权限ID查询子权限列表
     *
     * @param parentId 父权限ID
     * @return 权限列表
     */
    @Select("SELECT * FROM permissions WHERE parent_id = #{parentId} AND deleted = 0 AND status = 'ACTIVE' ORDER BY sort_order, permission_name")
    List<Permission> findByParentId(@Param("parentId") String parentId);

    /**
     * 查询根权限列表（父权限ID为空）
     *
     * @return 权限列表
     */
    @Select("SELECT * FROM permissions WHERE parent_id IS NULL AND deleted = 0 AND status = 'ACTIVE' ORDER BY sort_order, permission_name")
    List<Permission> findRootPermissions();

    /**
     * 根据资源类型查询权限列表
     *
     * @param resourceType 资源类型
     * @return 权限列表
     */
    @Select("SELECT * FROM permissions WHERE resource_type = #{resourceType} AND deleted = 0 AND status = 'ACTIVE' ORDER BY sort_order, permission_name")
    List<Permission> findByResourceType(@Param("resourceType") String resourceType);

    /**
     * 根据状态查询权限列表
     *
     * @param status 状态
     * @return 权限列表
     */
    @Select("SELECT * FROM permissions WHERE status = #{status} AND deleted = 0 ORDER BY sort_order, permission_name")
    List<Permission> findByStatus(@Param("status") String status);

    /**
     * 查询所有有效权限
     *
     * @return 权限列表
     */
    @Select("SELECT * FROM permissions WHERE deleted = 0 ORDER BY sort_order, permission_name")
    List<Permission> findAllActive();

    /**
     * 根据权限名称模糊查询
     *
     * @param permissionName 权限名称
     * @return 权限列表
     */
    @Select("SELECT * FROM permissions WHERE permission_name LIKE CONCAT('%', #{permissionName}, '%') AND deleted = 0 ORDER BY sort_order, permission_name")
    List<Permission> findByPermissionNameLike(@Param("permissionName") String permissionName);

    /**
     * 检查权限编码是否存在
     *
     * @param permissionCode 权限编码
     * @param excludeId 排除的权限ID
     * @return 存在数量
     */
    @Select("SELECT COUNT(*) FROM permissions WHERE permission_code = #{permissionCode} AND permission_id != #{excludeId} AND deleted = 0")
    int countByPermissionCodeExcludeId(@Param("permissionCode") String permissionCode, @Param("excludeId") String excludeId);

    /**
     * 检查权限名称是否存在
     *
     * @param permissionName 权限名称
     * @param excludeId 排除的权限ID
     * @return 存在数量
     */
    @Select("SELECT COUNT(*) FROM permissions WHERE permission_name = #{permissionName} AND permission_id != #{excludeId} AND deleted = 0")
    int countByPermissionNameExcludeId(@Param("permissionName") String permissionName, @Param("excludeId") String excludeId);

    /**
     * 根据角色编码查询权限编码列表
     *
     * @param roleCode 角色编码
     * @return 权限编码列表
     */
    @Select("SELECT DISTINCT p.permission_code FROM permissions p " +
            "INNER JOIN role_permissions rp ON p.permission_id = rp.permission_id " +
            "INNER JOIN roles r ON r.role_id = rp.role_id " +
            "WHERE r.role_code = #{roleCode} AND p.deleted = 0 AND p.status = 'ACTIVE' AND r.deleted = 0 AND r.status = 'ACTIVE'")
    List<String> findPermissionCodesByRoleCode(@Param("roleCode") String roleCode);
}