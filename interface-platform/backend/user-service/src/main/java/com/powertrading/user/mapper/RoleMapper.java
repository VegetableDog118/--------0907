package com.powertrading.user.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.powertrading.user.entity.Role;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

import java.util.List;

/**
 * 角色Mapper接口
 *
 * @author PowerTrading Team
 * @since 2024-01-15
 */
@Mapper
public interface RoleMapper extends BaseMapper<Role> {

    /**
     * 根据角色编码查询角色
     *
     * @param roleCode 角色编码
     * @return 角色信息
     */
    @Select("SELECT * FROM roles WHERE role_code = #{roleCode} AND deleted = 0")
    Role findByRoleCode(@Param("roleCode") String roleCode);

    /**
     * 根据状态查询角色列表
     *
     * @param status 状态
     * @return 角色列表
     */
    @Select("SELECT * FROM roles WHERE status = #{status} AND deleted = 0 ORDER BY role_name")
    List<Role> findByStatus(@Param("status") String status);

    /**
     * 查询所有有效角色
     *
     * @return 角色列表
     */
    @Select("SELECT * FROM roles WHERE deleted = 0 ORDER BY role_name")
    List<Role> findAllActive();

    /**
     * 根据角色名称模糊查询
     *
     * @param roleName 角色名称
     * @return 角色列表
     */
    @Select("SELECT * FROM roles WHERE role_name LIKE CONCAT('%', #{roleName}, '%') AND deleted = 0 ORDER BY role_name")
    List<Role> findByRoleNameLike(@Param("roleName") String roleName);

    /**
     * 检查角色编码是否存在
     *
     * @param roleCode 角色编码
     * @param excludeId 排除的角色ID
     * @return 存在数量
     */
    @Select("SELECT COUNT(*) FROM roles WHERE role_code = #{roleCode} AND role_id != #{excludeId} AND deleted = 0")
    int countByRoleCodeExcludeId(@Param("roleCode") String roleCode, @Param("excludeId") String excludeId);

    /**
     * 检查角色名称是否存在
     *
     * @param roleName 角色名称
     * @param excludeId 排除的角色ID
     * @return 存在数量
     */
    @Select("SELECT COUNT(*) FROM roles WHERE role_name = #{roleName} AND role_id != #{excludeId} AND deleted = 0")
    int countByRoleNameExcludeId(@Param("roleName") String roleName, @Param("excludeId") String excludeId);
}