package com.powertrading.approval.service;

import java.util.List;
import java.util.Map;

/**
 * 用户权限集成服务接口
 *
 * @author PowerTrading
 * @since 2024-01-15
 */
public interface UserPermissionIntegrationService {

    /**
     * 同步用户接口权限
     *
     * @param userId 用户ID
     * @param interfaceIds 接口ID列表
     * @return 同步结果
     */
    boolean syncUserInterfacePermissions(String userId, List<String> interfaceIds);

    /**
     * 批量同步用户权限
     *
     * @param userPermissions 用户权限映射（用户ID -> 接口ID列表）
     * @return 同步结果
     */
    boolean batchSyncUserPermissions(Map<String, List<String>> userPermissions);

    /**
     * 撤销用户接口权限
     *
     * @param userId 用户ID
     * @param interfaceIds 接口ID列表
     * @return 撤销结果
     */
    boolean revokeUserInterfacePermissions(String userId, List<String> interfaceIds);

    /**
     * 批量撤销用户权限
     *
     * @param userPermissions 用户权限映射（用户ID -> 接口ID列表）
     * @return 撤销结果
     */
    boolean batchRevokeUserPermissions(Map<String, List<String>> userPermissions);

    /**
     * 验证用户是否存在
     *
     * @param userId 用户ID
     * @return 是否存在
     */
    boolean validateUserExists(String userId);

    /**
     * 批量验证用户是否存在
     *
     * @param userIds 用户ID列表
     * @return 存在的用户ID列表
     */
    List<String> batchValidateUsersExist(List<String> userIds);

    /**
     * 获取用户当前权限
     *
     * @param userId 用户ID
     * @return 用户权限列表
     */
    List<String> getUserCurrentPermissions(String userId);

    /**
     * 检查用户角色权限
     *
     * @param userId 用户ID
     * @param requiredRole 需要的角色
     * @return 是否有权限
     */
    boolean checkUserRolePermission(String userId, String requiredRole);

    /**
     * 同步权限失败重试
     *
     * @param userId 用户ID
     * @param interfaceIds 接口ID列表
     * @param maxRetries 最大重试次数
     * @return 同步结果
     */
    boolean syncWithRetry(String userId, List<String> interfaceIds, int maxRetries);

    /**
     * 权限同步状态检查
     *
     * @param userId 用户ID
     * @param interfaceIds 接口ID列表
     * @return 同步状态
     */
    Map<String, Object> checkSyncStatus(String userId, List<String> interfaceIds);
}