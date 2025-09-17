package com.powertrading.user.service;

import com.powertrading.user.dto.*;
import com.powertrading.user.entity.User;

import java.util.List;

/**
 * 用户服务接口
 *
 * @author PowerTrading Team
 * @since 2024-01-15
 */
public interface UserService {

    /**
     * 用户注册
     *
     * @param request 注册请求
     * @return 注册结果
     */
    UserRegisterResponse register(UserRegisterRequest request);

    /**
     * 用户登录
     *
     * @param request 登录请求
     * @return 登录结果
     */
    UserLoginResponse login(UserLoginRequest request);

    /**
     * 根据ID获取用户信息
     *
     * @param userId 用户ID
     * @return 用户信息
     */
    UserInfoResponse getUserInfo(String userId);

    /**
     * 根据AppId获取用户信息
     *
     * @param appId AppId
     * @return 用户信息
     */
    UserInfoResponse getUserByAppId(String appId);

    /**
     * 更新用户信息
     *
     * @param userId 用户ID
     * @param request 更新请求
     */
    void updateUserInfo(String userId, UserUpdateRequest request);

    /**
     * 修改密码
     *
     * @param userId 用户ID
     * @param request 修改密码请求
     */
    void changePassword(String userId, ChangePasswordRequest request);

    /**
     * 重置API密钥
     *
     * @param userId 用户ID
     * @return 新的API密钥信息
     */
    ApiKeyResponse resetApiKey(String userId);

    /**
     * 获取API密钥
     *
     * @param userId 用户ID
     * @return API密钥信息
     */
    ApiKeyResponse getApiKey(String userId);

    /**
     * 用户登出
     *
     * @param token JWT Token
     */
    void logout(String token);

    /**
     * 获取用户权限列表
     *
     * @param userId 用户ID
     * @return 权限列表
     */
    List<String> getUserPermissions(String userId);

    /**
     * 检查用户权限
     *
     * @param userId 用户ID
     * @param permission 权限
     * @return 是否有权限
     */
    boolean hasPermission(String userId, String permission);

    /**
     * 审核通过用户
     *
     * @param userId 用户ID
     * @param approvedBy 审核人
     */
    void approveUser(String userId, String approvedBy);

    /**
     * 审核拒绝用户
     *
     * @param userId 用户ID
     * @param rejectedBy 审核人
     * @param reason 拒绝原因
     */
    void rejectUser(String userId, String rejectedBy, String reason);

    /**
     * 锁定用户
     *
     * @param userId 用户ID
     * @param lockedBy 锁定人
     * @param reason 锁定原因
     */
    void lockUser(String userId, String lockedBy, String reason);

    /**
     * 解锁用户
     *
     * @param userId 用户ID
     * @param unlockedBy 解锁人
     */
    void unlockUser(String userId, String unlockedBy);

    /**
     * 获取用户列表
     *
     * @param page 页码
     * @param size 页大小
     * @param status 状态过滤
     * @param keyword 关键词
     * @return 用户列表
     */
    PageResponse<UserInfoResponse> getUserList(int page, int size, String status, String keyword);

    /**
     * 验证用户凭据
     *
     * @param account 账号
     * @param password 密码
     * @return 用户信息
     */
    User validateCredentials(String account, String password);

    /**
     * 检查账号是否被锁定
     *
     * @param user 用户信息
     * @return 是否被锁定
     */
    boolean isAccountLocked(User user);

    /**
     * 处理登录失败
     *
     * @param user 用户信息
     */
    void handleLoginFailure(User user);

    /**
     * 处理登录成功
     *
     * @param user 用户信息
     * @param clientIp 客户端IP
     */
    void handleLoginSuccess(User user, String clientIp);

    /**
     * 生成JWT Token
     *
     * @param user 用户信息
     * @return Token信息
     */
    TokenInfo generateToken(User user);

    /**
     * 验证JWT Token
     *
     * @param token Token
     * @return Token信息
     */
    TokenInfo validateToken(String token);

    /**
     * 刷新Token
     *
     * @param refreshToken 刷新Token
     * @return Token信息
     */
    TokenInfo refreshToken(String refreshToken);

    /**
     * 检查用户是否存在
     *
     * @param userId 用户ID
     * @return 是否存在
     */
    boolean userExists(String userId);
}