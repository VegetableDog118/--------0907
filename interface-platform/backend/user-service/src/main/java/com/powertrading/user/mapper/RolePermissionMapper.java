package com.powertrading.user.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.powertrading.user.entity.RolePermission;
import org.apache.ibatis.annotations.Delete;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

import java.util.List;

/**
 * 角色权限关联Mapper接口
 *
 * @author PowerTrading Team
 * @since 2024-01-15
 */
@Mapper
public interface RolePermissionMapper extends BaseMapper<RolePermission> {

    /**
     * 根据角色ID查询权限ID列表
     *
     * @param roleId 角色ID
     * @return 权限ID列表
     */
    @Select("SELECT permission_id FROM role_permissions WHERE role_id = #{roleId}")
    List<String> findPermissionIdsByRoleId(@Param("roleId") String roleId);

    /**
     * 根据权限ID查询角色ID列表
     *
     * @param permissionId 权限ID
     * @return 角色ID列表
     */
    @Select("SELECT role_id FROM role_permissions WHERE permission_id = #{permissionId}")
    List<String> findRoleIdsByPermissionId(@Param("permissionId") String permissionId);

    /**
     * 检查角色权限关联是否存在
     *
     * @param roleId 角色ID
     * @param permissionId 权限ID
     * @return 存在数量
     */
    @Select("SELECT COUNT(*) FROM role_permissions WHERE role_id = #{roleId} AND permission_id = #{permissionId}")
    int countByRoleIdAndPermissionId(@Param("roleId") String roleId, @Param("permissionId") String permissionId);

    /**
     * 根据角色ID删除所有权限关联
     *
     * @param roleId 角色ID
     * @return 删除数量
     */
    @Delete("DELETE FROM role_permissions WHERE role_id = #{roleId}")
    int deleteByRoleId(@Param("roleId") String roleId);

    /**
     * 根据权限ID删除所有角色关联
     *
     * @param permissionId 权限ID
     * @return 删除数量
     */
    @Delete("DELETE FROM role_permissions WHERE permission_id = #{permissionId}")
    int deleteByPermissionId(@Param("permissionId") String permissionId);

    /**
     * 删除指定的角色权限关联
     *
     * @param roleId 角色ID
     * @param permissionId 权限ID
     * @return 删除数量
     */
    @Delete("DELETE FROM role_permissions WHERE role_id = #{roleId} AND permission_id = #{permissionId}")
    int deleteByRoleIdAndPermissionId(@Param("roleId") String roleId, @Param("permissionId") String permissionId);

    /**
     * 批量插入角色权限关联
     *
     * @param rolePermissions 角色权限关联列表
     * @return 插入数量
     */
    int batchInsert(@Param("rolePermissions") List<RolePermission> rolePermissions);

    /**
     * 根据角色ID查询角色权限关联列表
     *
     * @param roleId 角色ID
     * @return 角色权限关联列表
     */
    @Select("SELECT * FROM role_permissions WHERE role_id = #{roleId} ORDER BY create_time")
    List<RolePermission> findByRoleId(@Param("roleId") String roleId);

    /**
     * 根据权限ID查询角色权限关联列表
     *
     * @param permissionId 权限ID
     * @return 角色权限关联列表
     */
    @Select("SELECT * FROM role_permissions WHERE permission_id = #{permissionId} ORDER BY create_time")
    List<RolePermission> findByPermissionId(@Param("permissionId") String permissionId);
}