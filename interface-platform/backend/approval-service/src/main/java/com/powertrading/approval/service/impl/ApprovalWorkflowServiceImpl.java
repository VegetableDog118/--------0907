package com.powertrading.approval.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.powertrading.approval.common.Result;
import com.powertrading.approval.dto.ProcessApplicationRequest;
import com.powertrading.approval.entity.SubscriptionApplication;
import com.powertrading.approval.entity.UserInterfaceSubscription;
import com.powertrading.approval.feign.NotificationServiceClient;
import com.powertrading.approval.feign.UserServiceClient;
import com.powertrading.approval.mapper.SubscriptionApplicationMapper;
import com.powertrading.approval.mapper.UserInterfaceSubscriptionMapper;
import com.powertrading.approval.service.ApprovalWorkflowService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * 审批工作流服务实现类
 *
 * @author PowerTrading
 * @since 2024-01-15
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class ApprovalWorkflowServiceImpl implements ApprovalWorkflowService {

    private final SubscriptionApplicationMapper applicationMapper;
    private final UserInterfaceSubscriptionMapper subscriptionMapper;
    private final UserServiceClient userServiceClient;
    private final NotificationServiceClient notificationServiceClient;

    @Override
    @Transactional(rollbackFor = Exception.class)
    public boolean executeFirstLevelApproval(String processBy, ProcessApplicationRequest request) {
        log.info("执行一级审批流程，处理人：{}，申请数量：{}，操作：{}", processBy, request.getApplicationIds().size(), request.getAction());

        // 1. 验证审批权限
        if (!validateApprovalPermission(processBy)) {
            throw new RuntimeException("无审批权限");
        }

        // 2. 检查申请状态
        for (String applicationId : request.getApplicationIds()) {
            if (!canApprove(applicationId)) {
                throw new RuntimeException("申请ID：" + applicationId + " 不能审批");
            }
        }

        // 3. 更新申请状态
        LocalDateTime processTime = LocalDateTime.now();
        int updateCount = applicationMapper.batchUpdateStatus(
                request.getApplicationIds(),
                request.getAction(),
                processBy,
                request.getComment(),
                processTime
        );

        if (updateCount != request.getApplicationIds().size()) {
            throw new RuntimeException("部分申请更新失败");
        }

        // 4. 如果是通过申请，创建订阅记录并更新用户权限
        if ("approved".equals(request.getAction())) {
            processApprovedApplications(request.getApplicationIds());
        }

        // 5. 发送审批通知
        sendApprovalNotifications(request.getApplicationIds(), request.getAction(), processBy);

        log.info("一级审批流程执行完成，处理了{}个申请", updateCount);
        return true;
    }

    @Override
    public boolean validateApprovalPermission(String userId) {
        try {
            com.powertrading.approval.dto.ApiResponse<String> roleResult = userServiceClient.getUserRole(userId);
            if (!roleResult.isSuccess()) {
                log.warn("获取用户角色失败：{}", roleResult.getMessage());
                return false;
            }

            // 只有结算部角色可以进行审批
            String role = roleResult.getData();
            boolean hasPermission = "settlement".equals(role) || "admin".equals(role) || "ADMIN".equals(role);
            
            log.debug("用户{}的角色为{}，审批权限：{}", userId, role, hasPermission);
            return hasPermission;
        } catch (Exception e) {
            log.error("验证审批权限失败：{}", e.getMessage(), e);
            return false;
        }
    }

    @Override
    public boolean canApprove(String applicationId) {
        SubscriptionApplication application = applicationMapper.selectById(applicationId);
        if (application == null) {
            log.warn("申请不存在：{}", applicationId);
            return false;
        }

        // 只有待审批状态的申请可以审批
        boolean canApprove = SubscriptionApplication.Status.PENDING.getCode().equals(application.getStatus());
        log.debug("申请{}状态为{}，可否审批：{}", applicationId, application.getStatus(), canApprove);
        return canApprove;
    }

    @Override
    public List<SubscriptionApplication> getPendingApplications(Integer pageNum, Integer pageSize) {
        Page<SubscriptionApplication> page = new Page<>(pageNum, pageSize);
        
        LambdaQueryWrapper<SubscriptionApplication> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(SubscriptionApplication::getStatus, SubscriptionApplication.Status.PENDING.getCode())
                .orderByAsc(SubscriptionApplication::getSubmitTime);
        
        Page<SubscriptionApplication> result = applicationMapper.selectPage(page, wrapper);
        return result.getRecords();
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public boolean autoApprove(String applicationId) {
        log.info("执行自动审批：{}", applicationId);

        SubscriptionApplication application = applicationMapper.selectById(applicationId);
        if (application == null || !canApprove(applicationId)) {
            return false;
        }

        // 自动审批规则：接口数量少于等于5个且预计调用次数少于1000次
        boolean autoApproveCondition = application.getInterfaceIds().size() <= 5 
                && (application.getEstimatedCalls() == null || application.getEstimatedCalls() <= 1000);

        if (autoApproveCondition) {
            // 自动通过
            application.setStatus(SubscriptionApplication.Status.APPROVED.getCode());
            application.setProcessBy("system");
            application.setProcessComment("系统自动审批通过");
            application.setProcessTime(LocalDateTime.now());
            application.setUpdateTime(LocalDateTime.now());
            
            int result = applicationMapper.updateById(application);
            if (result > 0) {
                // 创建订阅记录
                processApprovedApplications(List.of(applicationId));
                log.info("申请{}自动审批通过", applicationId);
                return true;
            }
        }

        return false;
    }

    @Override
    public String getApprovalStatus(String applicationId) {
        SubscriptionApplication application = applicationMapper.selectById(applicationId);
        return application != null ? application.getStatus() : null;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public boolean withdrawApproval(String processBy, String applicationId, String reason) {
        log.info("撤回审批，处理人：{}，申请ID：{}，原因：{}", processBy, applicationId, reason);

        SubscriptionApplication application = applicationMapper.selectById(applicationId);
        if (application == null) {
            throw new RuntimeException("申请不存在");
        }

        // 只有已通过的申请可以撤回
        if (!SubscriptionApplication.Status.APPROVED.getCode().equals(application.getStatus())) {
            throw new RuntimeException("只能撤回已通过的申请");
        }

        // 验证撤回权限（只有原审批人或管理员可以撤回）
        if (!processBy.equals(application.getProcessBy()) && !validateApprovalPermission(processBy)) {
            throw new RuntimeException("无权限撤回此申请");
        }

        // 更新申请状态为待审批
        application.setStatus(SubscriptionApplication.Status.PENDING.getCode());
        application.setProcessBy(null);
        application.setProcessComment("审批已撤回：" + reason);
        application.setProcessTime(null);
        application.setUpdateTime(LocalDateTime.now());
        
        int result = applicationMapper.updateById(application);
        if (result > 0) {
            // 删除相关订阅记录
            removeSubscriptions(applicationId);
            log.info("成功撤回审批：{}", applicationId);
            return true;
        }

        return false;
    }

    /**
     * 处理已通过的申请
     */
    private void processApprovedApplications(List<String> applicationIds) {
        for (String applicationId : applicationIds) {
            SubscriptionApplication application = applicationMapper.selectById(applicationId);
            if (application == null) {
                continue;
            }

            // 创建订阅记录
            createSubscriptionRecords(application);
            
            // 更新用户权限
            updateUserPermissions(application);
        }
    }

    /**
     * 创建订阅记录
     */
    private void createSubscriptionRecords(SubscriptionApplication application) {
        for (String interfaceId : application.getInterfaceIds()) {
            // 检查是否已存在订阅
            UserInterfaceSubscription existing = subscriptionMapper.selectByUserAndInterface(
                    application.getUserId(), interfaceId
            );
            
            if (existing != null) {
                // 更新现有订阅状态
                existing.setStatus(UserInterfaceSubscription.Status.ACTIVE.getCode());
                existing.setSubscribeTime(LocalDateTime.now());
                existing.setUpdateTime(LocalDateTime.now());
                subscriptionMapper.updateById(existing);
            } else {
                // 创建新订阅
                UserInterfaceSubscription subscription = new UserInterfaceSubscription();
                subscription.setId(cn.hutool.core.util.IdUtil.getSnowflakeNextIdStr());
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
     * 更新用户权限
     */
    private void updateUserPermissions(SubscriptionApplication application) {
        try {
            userServiceClient.updateUserInterfacePermissions(
                    application.getUserId(),
                    application.getInterfaceIds()
            );
            log.debug("更新用户{}的接口权限成功", application.getUserId());
        } catch (Exception e) {
            log.warn("更新用户权限失败：{}", e.getMessage());
        }
    }

    /**
     * 发送审批通知
     */
    private void sendApprovalNotifications(List<String> applicationIds, String action, String processBy) {
        try {
            for (String applicationId : applicationIds) {
                SubscriptionApplication application = applicationMapper.selectById(applicationId);
                if (application != null) {
                    Map<String, Object> notificationRequest = new HashMap<>();
                    notificationRequest.put("userId", application.getUserId());
                    notificationRequest.put("applicationId", applicationId);
                    notificationRequest.put("action", action);
                    notificationRequest.put("processBy", processBy);
                    notificationRequest.put("type", "approval_result");
                    notificationRequest.put("interfaceCount", application.getInterfaceIds().size());
                    
                    notificationServiceClient.sendApprovalNotification(notificationRequest);
                }
            }
        } catch (Exception e) {
            log.warn("发送审批通知失败：{}", e.getMessage());
        }
    }

    /**
     * 移除订阅记录
     */
    private void removeSubscriptions(String applicationId) {
        List<UserInterfaceSubscription> subscriptions = subscriptionMapper.selectByApplicationId(applicationId);
        for (UserInterfaceSubscription subscription : subscriptions) {
            subscription.setStatus(UserInterfaceSubscription.Status.CANCELLED.getCode());
            subscription.setCancelTime(LocalDateTime.now());
            subscription.setUpdateTime(LocalDateTime.now());
            subscriptionMapper.updateById(subscription);
        }
    }
}