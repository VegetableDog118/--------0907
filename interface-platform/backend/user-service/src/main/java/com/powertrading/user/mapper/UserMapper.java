package com.powertrading.user.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.powertrading.user.entity.User;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;
import org.apache.ibatis.annotations.Update;

import java.time.LocalDateTime;
import java.util.List;

/**
 * 用户Mapper接口
 *
 * @author PowerTrading Team
 * @since 2024-01-15
 */
@Mapper
public interface UserMapper extends BaseMapper<User> {

    /**
     * 根据手机号查询用户
     *
     * @param phone 手机号
     * @return 用户信息
     */
    @Select("SELECT * FROM users WHERE phone = #{phone} AND deleted = 0")
    User findByPhone(@Param("phone") String phone);

    /**
     * 根据邮箱查询用户
     *
     * @param email 邮箱
     * @return 用户信息
     */
    @Select("SELECT * FROM users WHERE email = #{email} AND deleted = 0")
    User findByEmail(@Param("email") String email);

    /**
     * 根据企业名称查询用户
     *
     * @param companyName 企业名称
     * @return 用户信息
     */
    @Select("SELECT * FROM users WHERE company_name = #{companyName} AND deleted = 0")
    User findByCompanyName(@Param("companyName") String companyName);

    /**
     * 根据统一社会信用代码查询用户
     *
     * @param creditCode 统一社会信用代码
     * @return 用户信息
     */
    @Select("SELECT * FROM users WHERE credit_code = #{creditCode} AND deleted = 0")
    User findByCreditCode(@Param("creditCode") String creditCode);

    /**
     * 根据AppId查询用户
     *
     * @param appId AppId
     * @return 用户信息
     */
    @Select("SELECT * FROM users WHERE app_id = #{appId} AND deleted = 0")
    User findByAppId(@Param("appId") String appId);

    /**
     * 更新登录失败次数
     *
     * @param userId 用户ID
     * @param attempts 失败次数
     * @return 更新行数
     */
    @Update("UPDATE users SET failed_login_attempts = #{attempts} WHERE id = #{userId}")
    int updateFailedLoginAttempts(@Param("userId") String userId, @Param("attempts") int attempts);

    /**
     * 锁定用户账号
     *
     * @param userId 用户ID
     * @param lockedUntil 锁定到期时间
     * @return 更新行数
     */
    @Update("UPDATE users SET locked_until = #{lockedUntil} WHERE id = #{userId}")
    int lockUser(@Param("userId") String userId, @Param("lockedUntil") LocalDateTime lockedUntil);

    /**
     * 解锁用户账号
     *
     * @param userId 用户ID
     * @return 更新行数
     */
    @Update("UPDATE users SET locked_until = NULL, failed_login_attempts = 0 WHERE id = #{userId}")
    int unlockUser(@Param("userId") String userId);

    /**
     * 更新最后登录信息
     *
     * @param userId 用户ID
     * @param loginTime 登录时间
     * @param loginIp 登录IP
     * @return 更新行数
     */
    @Update("UPDATE users SET last_login_time = #{loginTime}, last_login_ip = #{loginIp} WHERE id = #{userId}")
    int updateLastLoginInfo(@Param("userId") String userId, 
                           @Param("loginTime") LocalDateTime loginTime, 
                           @Param("loginIp") String loginIp);

    /**
     * 根据角色查询用户列表
     *
     * @param role 角色
     * @return 用户列表
     */
    @Select("SELECT * FROM users WHERE role = #{role} AND deleted = 0 ORDER BY created_at DESC")
    List<User> findByRole(@Param("role") String role);

    /**
     * 根据状态查询用户列表
     *
     * @param status 状态
     * @return 用户列表
     */
    @Select("SELECT * FROM users WHERE status = #{status} AND deleted = 0 ORDER BY created_at DESC")
    List<User> findByStatus(@Param("status") String status);

    /**
     * 更新用户状态
     *
     * @param userId 用户ID
     * @param status 状态
     * @return 更新行数
     */
    @Update("UPDATE users SET status = #{status} WHERE id = #{userId}")
    int updateStatus(@Param("userId") String userId, @Param("status") String status);

    /**
     * 更新用户密码
     *
     * @param userId 用户ID
     * @param password 新密码
     * @return 更新行数
     */
    @Update("UPDATE users SET password = #{password} WHERE id = #{userId}")
    int updatePassword(@Param("userId") String userId, @Param("password") String password);

    /**
     * 更新API密钥
     *
     * @param userId 用户ID
     * @param appId AppId
     * @param appSecret AppSecret
     * @return 更新行数
     */
    @Update("UPDATE users SET app_id = #{appId}, app_secret = #{appSecret} WHERE id = #{userId}")
    int updateApiKey(@Param("userId") String userId, 
                     @Param("appId") String appId, 
                     @Param("appSecret") String appSecret);
}