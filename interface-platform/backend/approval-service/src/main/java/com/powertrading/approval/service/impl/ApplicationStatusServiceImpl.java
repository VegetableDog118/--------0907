package com.powertrading.approval.service.impl;

import cn.hutool.core.util.IdUtil;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.powertrading.approval.entity.SubscriptionApplication;
import com.powertrading.approval.entity.UserInterfaceSubscription;
import com.powertrading.approval.feign.UserServiceClient;
import com.powertrading.approval.mapper.SubscriptionApplicationMapper;
import com.powertrading.approval.mapper.UserInterfaceSubscriptionMapper;
import com.powertrading.approval.service.ApplicationStatusService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.*;
import java.util.stream.Collectors;

/**
 * 申请状态管理服务实现类
 *
 * @author PowerTrading
 * @since 2024-01-15
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class ApplicationStatusServiceImpl implements ApplicationStatusService {

    private final SubscriptionApplicationMapper applicationMapper;
    private final UserInterfaceSubscriptionMapper subscriptionMapper;
    private final UserServiceClient userServiceClient;

    // 状态转换规则
    private static final Map<String, Set<String>> STATUS_TRANSITIONS = new HashMap<>();
    
    static {
        // pending -> approved, rejected
        STATUS_TRANSITIONS.put("pending", Set.of("approved", "rejected"));
        // approved -> rejected (撤回)
        STATUS_TRANSITIONS.put("approved", Set.of("rejected"));
        // rejected -> pending (重新提交)
        STATUS_TRANSITIONS.put("rejected", Set.of("pending"));
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public boolean updateApplicationStatus(String applicationId, String newStatus, String processBy, String comment) {
        log.info("更新申请状态，申请ID：{}，新状态：{}，处理人：{}", applicationId, newStatus, processBy);

        SubscriptionApplication application = applicationMapper.selectById(applicationId);
        if (application == null) {
            throw new RuntimeException("申请不存在");
        }

        // 验证状态转换是否合法
        if (!isValidStatusTransition(application.getStatus(), newStatus)) {
            throw new RuntimeException(String.format("不允许从状态%s转换到%s", application.getStatus(), newStatus));
        }

        // 更新申请状态
        application.setStatus(newStatus);
        application.setProcessBy(processBy);
        application.setProcessComment(comment);
        application.setProcessTime(LocalDateTime.now());
        application.setUpdateTime(LocalDateTime.now());

        int result = applicationMapper.updateById(application);
        if (result > 0) {
            // 如果状态变为已通过，自动更新用户权限
            if ("approved".equals(newStatus)) {
                autoUpdateUserPermissions(applicationId);
            }
            // 如果状态变为已拒绝且之前是已通过，需要撤销权限
            else if ("rejected".equals(newStatus) && "approved".equals(application.getStatus())) {
                revokeUserPermissions(applicationId);
            }
            
            log.info("申请状态更新成功：{}", applicationId);
            return true;
        }

        return false;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public boolean batchUpdateApplicationStatus(List<String> applicationIds, String newStatus, String processBy, String comment) {
        log.info("批量更新申请状态，数量：{}，新状态：{}，处理人：{}", applicationIds.size(), newStatus, processBy);

        LocalDateTime processTime = LocalDateTime.now();
        int updateCount = applicationMapper.batchUpdateStatus(applicationIds, newStatus, processBy, comment, processTime);
        
        if (updateCount > 0) {
            // 如果状态变为已通过，批量自动更新用户权限
            if ("approved".equals(newStatus)) {
                batchAutoUpdateUserPermissions(applicationIds);
            }
            
            log.info("批量更新申请状态成功，更新数量：{}", updateCount);
            return true;
        }

        return false;
    }

    @Override
    public boolean isValidStatusTransition(String currentStatus, String targetStatus) {
        if (currentStatus == null || targetStatus == null) {
            return false;
        }
        
        Set<String> allowedTransitions = STATUS_TRANSITIONS.get(currentStatus);
        return allowedTransitions != null && allowedTransitions.contains(targetStatus);
    }

    @Override
    public List<String> getStatusHistory(String applicationId) {
        // 这里简化实现，实际项目中可能需要单独的状态历史表
        SubscriptionApplication application = applicationMapper.selectById(applicationId);
        if (application == null) {
            return Collections.emptyList();
        }
        
        List<String> history = new ArrayList<>();
        history.add("pending - " + application.getSubmitTime());
        
        if (application.getProcessTime() != null) {
            history.add(application.getStatus() + " - " + application.getProcessTime());
        }
        
        return history;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public boolean autoUpdateUserPermissions(String applicationId) {
        log.info("自动更新用户权限，申请ID：{}", applicationId);

        SubscriptionApplication application = applicationMapper.selectById(applicationId);
        if (application == null || !"approved".equals(application.getStatus())) {
            return false;
        }

        try {
            // 创建或更新订阅记录
            createOrUpdateSubscriptions(application);
            
            // 调用用户服务更新权限
            userServiceClient.updateUserInterfacePermissions(
                    application.getUserId(),
                    application.getInterfaceIds()
            );
            
            log.info("用户权限更新成功，用户ID：{}，接口数量：{}", 
                    application.getUserId(), application.getInterfaceIds().size());
            return true;
        } catch (Exception e) {
            log.error("自动更新用户权限失败：{}", e.getMessage(), e);
            return false;
        }
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public boolean batchAutoUpdateUserPermissions(List<String> applicationIds) {
        log.info("批量自动更新用户权限，申请数量：{}", applicationIds.size());

        int successCount = 0;
        for (String applicationId : applicationIds) {
            if (autoUpdateUserPermissions(applicationId)) {
                successCount++;
            }
        }

        log.info("批量更新用户权限完成，成功：{}/{}", successCount, applicationIds.size());
        return successCount == applicationIds.size();
    }

    @Override
    public boolean isApplicationExpired(String applicationId) {
        SubscriptionApplication application = applicationMapper.selectById(applicationId);
        if (application == null) {
            return false;
        }

        // 如果申请超过30天未处理，视为过期
        if ("pending".equals(application.getStatus())) {
            LocalDateTime expireTime = application.getSubmitTime().plusDays(30);
            return LocalDateTime.now().isAfter(expireTime);
        }

        return false;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public int processExpiredApplications() {
        log.info("处理过期申请");

        LambdaQueryWrapper<SubscriptionApplication> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(SubscriptionApplication::getStatus, "pending")
                .lt(SubscriptionApplication::getSubmitTime, LocalDateTime.now().minusDays(30));

        List<SubscriptionApplication> expiredApplications = applicationMapper.selectList(wrapper);
        
        if (expiredApplications.isEmpty()) {
            return 0;
        }

        List<String> expiredIds = expiredApplications.stream()
                .map(SubscriptionApplication::getId)
                .collect(Collectors.toList());

        // 将过期申请状态更新为已拒绝
        int updateCount = applicationMapper.batchUpdateStatus(
                expiredIds,
                "rejected",
                "system",
                "申请超时自动拒绝",
                LocalDateTime.now()
        );

        log.info("处理过期申请完成，处理数量：{}", updateCount);
        return updateCount;
    }

    @Override
    public Map<String, Long> getStatusStatistics() {
        LambdaQueryWrapper<SubscriptionApplication> wrapper = new LambdaQueryWrapper<>();
        List<SubscriptionApplication> applications = applicationMapper.selectList(wrapper);

        return applications.stream()
                .collect(Collectors.groupingBy(
                        SubscriptionApplication::getStatus,
                        Collectors.counting()
                ));
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public boolean resetApplicationStatus(String applicationId, String adminUserId, String reason) {
        log.info("重置申请状态，申请ID：{}，管理员：{}，原因：{}", applicationId, adminUserId, reason);

        // 验证管理员权限
        try {
            var roleResult = userServiceClient.getUserRole(adminUserId);
            if (!roleResult.isSuccess() || !"admin".equals(roleResult.getData())) {
                throw new RuntimeException("无管理员权限");
            }
        } catch (Exception e) {
            throw new RuntimeException("验证管理员权限失败：" + e.getMessage());
        }

        SubscriptionApplication application = applicationMapper.selectById(applicationId);
        if (application == null) {
            throw new RuntimeException("申请不存在");
        }

        // 重置为待审批状态
        application.setStatus("pending");
        application.setProcessBy(null);
        application.setProcessComment("管理员重置：" + reason);
        application.setProcessTime(null);
        application.setUpdateTime(LocalDateTime.now());

        int result = applicationMapper.updateById(application);
        if (result > 0) {
            // 如果之前是已通过状态，需要撤销相关权限
            revokeUserPermissions(applicationId);
            log.info("申请状态重置成功：{}", applicationId);
            return true;
        }

        return false;
    }

    /**
     * 创建或更新订阅记录
     */
    private void createOrUpdateSubscriptions(SubscriptionApplication application) {
        for (String interfaceId : application.getInterfaceIds()) {
            UserInterfaceSubscription existing = subscriptionMapper.selectByUserAndInterface(
                    application.getUserId(), interfaceId
            );
            
            if (existing != null) {
                // 更新现有订阅
                existing.setStatus(UserInterfaceSubscription.Status.ACTIVE.getCode());
                existing.setSubscribeTime(LocalDateTime.now());
                existing.setUpdateTime(LocalDateTime.now());
                subscriptionMapper.updateById(existing);
            } else {
                // 创建新订阅
                UserInterfaceSubscription subscription = new UserInterfaceSubscription();
                subscription.setId(IdUtil.getSnowflakeNextIdStr());
                subscription.setUserId(application.getUserId());
                subscription.setInterfaceId(interfaceId);
                subscription.setApplicationId(application.getId());
                subscription.setStatus(UserInterfaceSubscription.Status.ACTIVE.getCode());
                subscription.setSubscribeTime(LocalDateTime.now());
                subscription.setCallCount(0);
                subscription.setCreateTime(LocalDateTime.now());
                subscription.setUpdateTime(LocalDateTime.now());
                
                subscriptionMapper.insert(subscription);
            }
        }
    }

    /**
     * 撤销用户权限
     */
    private void revokeUserPermissions(String applicationId) {
        try {
            List<UserInterfaceSubscription> subscriptions = subscriptionMapper.selectByApplicationId(applicationId);
            for (UserInterfaceSubscription subscription : subscriptions) {
                subscription.setStatus(UserInterfaceSubscription.Status.CANCELLED.getCode());
                subscription.setCancelTime(LocalDateTime.now());
                subscription.setUpdateTime(LocalDateTime.now());
                subscriptionMapper.updateById(subscription);
            }
            log.info("撤销用户权限成功，申请ID：{}", applicationId);
        } catch (Exception e) {
            log.warn("撤销用户权限失败：{}", e.getMessage());
        }
    }
}