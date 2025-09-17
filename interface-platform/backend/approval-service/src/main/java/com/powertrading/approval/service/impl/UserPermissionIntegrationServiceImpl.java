package com.powertrading.approval.service.impl;

import com.powertrading.approval.common.Result;
import com.powertrading.approval.feign.UserServiceClient;
import com.powertrading.approval.service.UserPermissionIntegrationService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.retry.annotation.Backoff;
import org.springframework.retry.annotation.Retryable;
import org.springframework.stereotype.Service;

import java.util.*;
import java.util.stream.Collectors;

/**
 * 用户权限集成服务实现类
 *
 * @author PowerTrading
 * @since 2024-01-15
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class UserPermissionIntegrationServiceImpl implements UserPermissionIntegrationService {

    private final UserServiceClient userServiceClient;

    @Override
    @Retryable(value = Exception.class, maxAttempts = 3, backoff = @Backoff(delay = 1000))
    public boolean syncUserInterfacePermissions(String userId, List<String> interfaceIds) {
        log.info("同步用户接口权限，用户ID：{}，接口数量：{}", userId, interfaceIds.size());

        try {
            // 验证用户是否存在
            if (!validateUserExists(userId)) {
                log.warn("用户不存在：{}", userId);
                return false;
            }

            // 调用用户服务更新权限
            Result<Boolean> result = userServiceClient.updateUserInterfacePermissions(userId, interfaceIds);
            
            if (result.isSuccess() && Boolean.TRUE.equals(result.getData())) {
                log.info("用户权限同步成功，用户ID：{}，接口数量：{}", userId, interfaceIds.size());
                return true;
            } else {
                log.warn("用户权限同步失败，用户ID：{}，错误信息：{}", userId, result.getMessage());
                return false;
            }
        } catch (Exception e) {
            log.error("同步用户权限异常，用户ID：{}，错误：{}", userId, e.getMessage(), e);
            throw e; // 重新抛出异常以触发重试
        }
    }

    @Override
    public boolean batchSyncUserPermissions(Map<String, List<String>> userPermissions) {
        log.info("批量同步用户权限，用户数量：{}", userPermissions.size());

        try {
            // 构建批量更新请求
            List<Map<String, Object>> permissionUpdates = new ArrayList<>();
            
            for (Map.Entry<String, List<String>> entry : userPermissions.entrySet()) {
                String userId = entry.getKey();
                List<String> interfaceIds = entry.getValue();
                
                Map<String, Object> update = new HashMap<>();
                update.put("userId", userId);
                update.put("interfaceIds", interfaceIds);
                update.put("action", "grant");
                
                permissionUpdates.add(update);
            }

            // 调用用户服务批量更新权限
            Result<Boolean> result = userServiceClient.batchUpdateUserPermissions(permissionUpdates);
            
            if (result.isSuccess() && Boolean.TRUE.equals(result.getData())) {
                log.info("批量用户权限同步成功，用户数量：{}", userPermissions.size());
                return true;
            } else {
                log.warn("批量用户权限同步失败，错误信息：{}", result.getMessage());
                return false;
            }
        } catch (Exception e) {
            log.error("批量同步用户权限异常：{}", e.getMessage(), e);
            return false;
        }
    }

    @Override
    public boolean revokeUserInterfacePermissions(String userId, List<String> interfaceIds) {
        log.info("撤销用户接口权限，用户ID：{}，接口数量：{}", userId, interfaceIds.size());

        try {
            // 这里可以调用用户服务的权限撤销接口
            // 由于当前用户服务接口设计，我们通过传递空列表来表示撤销
            Result<Boolean> result = userServiceClient.updateUserInterfacePermissions(userId, Collections.emptyList());
            
            if (result.isSuccess() && Boolean.TRUE.equals(result.getData())) {
                log.info("用户权限撤销成功，用户ID：{}", userId);
                return true;
            } else {
                log.warn("用户权限撤销失败，用户ID：{}，错误信息：{}", userId, result.getMessage());
                return false;
            }
        } catch (Exception e) {
            log.error("撤销用户权限异常，用户ID：{}，错误：{}", userId, e.getMessage(), e);
            return false;
        }
    }

    @Override
    public boolean batchRevokeUserPermissions(Map<String, List<String>> userPermissions) {
        log.info("批量撤销用户权限，用户数量：{}", userPermissions.size());

        try {
            // 构建批量撤销请求
            List<Map<String, Object>> permissionUpdates = new ArrayList<>();
            
            for (Map.Entry<String, List<String>> entry : userPermissions.entrySet()) {
                String userId = entry.getKey();
                List<String> interfaceIds = entry.getValue();
                
                Map<String, Object> update = new HashMap<>();
                update.put("userId", userId);
                update.put("interfaceIds", interfaceIds);
                update.put("action", "revoke");
                
                permissionUpdates.add(update);
            }

            // 调用用户服务批量撤销权限
            Result<Boolean> result = userServiceClient.batchUpdateUserPermissions(permissionUpdates);
            
            if (result.isSuccess() && Boolean.TRUE.equals(result.getData())) {
                log.info("批量用户权限撤销成功，用户数量：{}", userPermissions.size());
                return true;
            } else {
                log.warn("批量用户权限撤销失败，错误信息：{}", result.getMessage());
                return false;
            }
        } catch (Exception e) {
            log.error("批量撤销用户权限异常：{}", e.getMessage(), e);
            return false;
        }
    }

    @Override
    public boolean validateUserExists(String userId) {
        try {
            com.powertrading.approval.dto.ApiResponse<Boolean> result = userServiceClient.userExists(userId);
            return result.isSuccess() && Boolean.TRUE.equals(result.getData());
        } catch (Exception e) {
            log.warn("验证用户存在性失败，用户ID：{}，错误：{}", userId, e.getMessage());
            return false;
        }
    }

    @Override
    public List<String> batchValidateUsersExist(List<String> userIds) {
        log.debug("批量验证用户存在性，用户数量：{}", userIds.size());

        return userIds.stream()
                .filter(this::validateUserExists)
                .collect(Collectors.toList());
    }

    @Override
    public List<String> getUserCurrentPermissions(String userId) {
        try {
            Result<Map<String, Object>> result = userServiceClient.getUserInfo(userId);
            if (result.isSuccess() && result.getData() != null) {
                Map<String, Object> userInfo = result.getData();
                @SuppressWarnings("unchecked")
                List<String> permissions = (List<String>) userInfo.get("interfacePermissions");
                return permissions != null ? permissions : Collections.emptyList();
            }
        } catch (Exception e) {
            log.warn("获取用户当前权限失败，用户ID：{}，错误：{}", userId, e.getMessage());
        }
        return Collections.emptyList();
    }

    @Override
    public boolean checkUserRolePermission(String userId, String requiredRole) {
        try {
            com.powertrading.approval.dto.ApiResponse<String> result = userServiceClient.getUserRole(userId);
            if (result.isSuccess()) {
                String userRole = result.getData();
                return requiredRole.equals(userRole) || "admin".equals(userRole);
            }
        } catch (Exception e) {
            log.warn("检查用户角色权限失败，用户ID：{}，错误：{}", userId, e.getMessage());
        }
        return false;
    }

    @Override
    public boolean syncWithRetry(String userId, List<String> interfaceIds, int maxRetries) {
        log.info("带重试的权限同步，用户ID：{}，最大重试次数：{}", userId, maxRetries);

        for (int attempt = 1; attempt <= maxRetries; attempt++) {
            try {
                if (syncUserInterfacePermissions(userId, interfaceIds)) {
                    log.info("权限同步成功，用户ID：{}，尝试次数：{}", userId, attempt);
                    return true;
                }
            } catch (Exception e) {
                log.warn("权限同步失败，用户ID：{}，尝试次数：{}，错误：{}", userId, attempt, e.getMessage());
                
                if (attempt < maxRetries) {
                    try {
                        // 指数退避
                        Thread.sleep(1000L * attempt);
                    } catch (InterruptedException ie) {
                        Thread.currentThread().interrupt();
                        break;
                    }
                }
            }
        }

        log.error("权限同步最终失败，用户ID：{}，已尝试{}次", userId, maxRetries);
        return false;
    }

    @Override
    public Map<String, Object> checkSyncStatus(String userId, List<String> interfaceIds) {
        Map<String, Object> status = new HashMap<>();
        status.put("userId", userId);
        status.put("requestedInterfaces", interfaceIds);
        
        try {
            // 获取用户当前权限
            List<String> currentPermissions = getUserCurrentPermissions(userId);
            status.put("currentPermissions", currentPermissions);
            
            // 检查同步状态
            boolean allSynced = currentPermissions.containsAll(interfaceIds);
            status.put("synced", allSynced);
            
            // 找出未同步的接口
            List<String> notSynced = interfaceIds.stream()
                    .filter(id -> !currentPermissions.contains(id))
                    .collect(Collectors.toList());
            status.put("notSyncedInterfaces", notSynced);
            
            status.put("checkTime", System.currentTimeMillis());
            
        } catch (Exception e) {
            log.error("检查同步状态失败，用户ID：{}，错误：{}", userId, e.getMessage());
            status.put("error", e.getMessage());
            status.put("synced", false);
        }
        
        return status;
    }
}