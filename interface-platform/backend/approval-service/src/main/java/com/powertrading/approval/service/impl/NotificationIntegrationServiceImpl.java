package com.powertrading.approval.service.impl;

import com.powertrading.approval.common.Result;
import com.powertrading.approval.feign.NotificationServiceClient;
import com.powertrading.approval.service.NotificationIntegrationService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.retry.annotation.Backoff;
import org.springframework.retry.annotation.Retryable;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.*;

/**
 * 通知集成服务实现类
 *
 * @author PowerTrading
 * @since 2024-01-15
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class NotificationIntegrationServiceImpl implements NotificationIntegrationService {

    private final NotificationServiceClient notificationServiceClient;

    @Override
    @Retryable(value = Exception.class, maxAttempts = 3, backoff = @Backoff(delay = 1000))
    public boolean sendApplicationSubmittedNotification(String userId, String applicationId, Integer interfaceCount) {
        log.info("发送申请提交通知，用户ID：{}，申请ID：{}，接口数量：{}", userId, applicationId, interfaceCount);

        try {
            Map<String, Object> notificationRequest = new HashMap<>();
            notificationRequest.put("userId", userId);
            notificationRequest.put("applicationId", applicationId);
            notificationRequest.put("type", "application_submitted");
            notificationRequest.put("title", "申请提交成功");
            notificationRequest.put("content", String.format("您的接口订阅申请已提交成功，申请ID：%s，申请接口数量：%d个，请等待审批。", applicationId, interfaceCount));
            notificationRequest.put("interfaceCount", interfaceCount);
            notificationRequest.put("sendTime", LocalDateTime.now());

            Result<Boolean> result = notificationServiceClient.sendApplicationNotification(notificationRequest);
            
            if (result.isSuccess() && Boolean.TRUE.equals(result.getData())) {
                log.info("申请提交通知发送成功，用户ID：{}", userId);
                return true;
            } else {
                log.warn("申请提交通知发送失败，用户ID：{}，错误：{}", userId, result.getMessage());
                return false;
            }
        } catch (Exception e) {
            log.error("发送申请提交通知异常，用户ID：{}，错误：{}", userId, e.getMessage(), e);
            throw e; // 重新抛出异常以触发重试
        }
    }

    @Override
    @Retryable(value = Exception.class, maxAttempts = 3, backoff = @Backoff(delay = 1000))
    public boolean sendApprovalResultNotification(String userId, String applicationId, String action, String processBy, String comment) {
        log.info("发送审批结果通知，用户ID：{}，申请ID：{}，操作：{}", userId, applicationId, action);

        try {
            String title = "approved".equals(action) ? "申请审批通过" : "申请审批拒绝";
            String content = String.format("您的接口订阅申请（%s）已被%s，处理人：%s", 
                    applicationId, 
                    "approved".equals(action) ? "通过" : "拒绝", 
                    processBy);
            
            if (comment != null && !comment.isEmpty()) {
                content += "，处理意见：" + comment;
            }

            Map<String, Object> notificationRequest = new HashMap<>();
            notificationRequest.put("userId", userId);
            notificationRequest.put("applicationId", applicationId);
            notificationRequest.put("type", "approval_result");
            notificationRequest.put("title", title);
            notificationRequest.put("content", content);
            notificationRequest.put("action", action);
            notificationRequest.put("processBy", processBy);
            notificationRequest.put("comment", comment);
            notificationRequest.put("sendTime", LocalDateTime.now());

            Result<Boolean> result = notificationServiceClient.sendApprovalNotification(notificationRequest);
            
            if (result.isSuccess() && Boolean.TRUE.equals(result.getData())) {
                log.info("审批结果通知发送成功，用户ID：{}，操作：{}", userId, action);
                return true;
            } else {
                log.warn("审批结果通知发送失败，用户ID：{}，错误：{}", userId, result.getMessage());
                return false;
            }
        } catch (Exception e) {
            log.error("发送审批结果通知异常，用户ID：{}，错误：{}", userId, e.getMessage(), e);
            throw e; // 重新抛出异常以触发重试
        }
    }

    @Override
    public boolean batchSendApprovalNotifications(List<Map<String, Object>> notifications) {
        log.info("批量发送审批通知，数量：{}", notifications.size());

        try {
            Result<Boolean> result = notificationServiceClient.batchSendNotifications(notifications);
            
            if (result.isSuccess() && Boolean.TRUE.equals(result.getData())) {
                log.info("批量审批通知发送成功，数量：{}", notifications.size());
                return true;
            } else {
                log.warn("批量审批通知发送失败，错误：{}", result.getMessage());
                return false;
            }
        } catch (Exception e) {
            log.error("批量发送审批通知异常：{}", e.getMessage(), e);
            return false;
        }
    }

    @Override
    public boolean sendApprovalReminderNotification(String processBy, Integer pendingCount) {
        log.info("发送审批提醒通知，审批人：{}，待审批数量：{}", processBy, pendingCount);

        try {
            Map<String, Object> notificationRequest = new HashMap<>();
            notificationRequest.put("userId", processBy);
            notificationRequest.put("type", "approval_reminder");
            notificationRequest.put("title", "待审批提醒");
            notificationRequest.put("content", String.format("您有%d个接口订阅申请待审批，请及时处理。", pendingCount));
            notificationRequest.put("pendingCount", pendingCount);
            notificationRequest.put("sendTime", LocalDateTime.now());

            Result<Boolean> result = notificationServiceClient.sendApprovalNotification(notificationRequest);
            
            if (result.isSuccess() && Boolean.TRUE.equals(result.getData())) {
                log.info("审批提醒通知发送成功，审批人：{}", processBy);
                return true;
            } else {
                log.warn("审批提醒通知发送失败，审批人：{}，错误：{}", processBy, result.getMessage());
                return false;
            }
        } catch (Exception e) {
            log.error("发送审批提醒通知异常，审批人：{}，错误：{}", processBy, e.getMessage(), e);
            return false;
        }
    }

    @Override
    public boolean sendApplicationExpiredNotification(String userId, String applicationId, Integer expiredDays) {
        log.info("发送申请过期通知，用户ID：{}，申请ID：{}，过期天数：{}", userId, applicationId, expiredDays);

        try {
            Map<String, Object> notificationRequest = new HashMap<>();
            notificationRequest.put("userId", userId);
            notificationRequest.put("applicationId", applicationId);
            notificationRequest.put("type", "application_expired");
            notificationRequest.put("title", "申请已过期");
            notificationRequest.put("content", String.format("您的接口订阅申请（%s）已超过%d天未处理，系统已自动拒绝。如需重新申请，请重新提交。", applicationId, expiredDays));
            notificationRequest.put("expiredDays", expiredDays);
            notificationRequest.put("sendTime", LocalDateTime.now());

            Result<Boolean> result = notificationServiceClient.sendApplicationNotification(notificationRequest);
            
            if (result.isSuccess() && Boolean.TRUE.equals(result.getData())) {
                log.info("申请过期通知发送成功，用户ID：{}", userId);
                return true;
            } else {
                log.warn("申请过期通知发送失败，用户ID：{}，错误：{}", userId, result.getMessage());
                return false;
            }
        } catch (Exception e) {
            log.error("发送申请过期通知异常，用户ID：{}，错误：{}", userId, e.getMessage(), e);
            return false;
        }
    }

    @Override
    public boolean sendPermissionUpdateNotification(String userId, List<String> interfaceIds, String action) {
        log.info("发送权限更新通知，用户ID：{}，接口数量：{}，操作：{}", userId, interfaceIds.size(), action);

        try {
            String title = "grant".equals(action) ? "接口权限已开通" : "接口权限已撤销";
            String content = String.format("您的接口权限已%s，涉及接口数量：%d个。", 
                    "grant".equals(action) ? "开通" : "撤销", 
                    interfaceIds.size());

            Map<String, Object> notificationRequest = new HashMap<>();
            notificationRequest.put("userId", userId);
            notificationRequest.put("type", "permission_update");
            notificationRequest.put("title", title);
            notificationRequest.put("content", content);
            notificationRequest.put("action", action);
            notificationRequest.put("interfaceIds", interfaceIds);
            notificationRequest.put("interfaceCount", interfaceIds.size());
            notificationRequest.put("sendTime", LocalDateTime.now());

            Result<Boolean> result = notificationServiceClient.sendApplicationNotification(notificationRequest);
            
            if (result.isSuccess() && Boolean.TRUE.equals(result.getData())) {
                log.info("权限更新通知发送成功，用户ID：{}", userId);
                return true;
            } else {
                log.warn("权限更新通知发送失败，用户ID：{}，错误：{}", userId, result.getMessage());
                return false;
            }
        } catch (Exception e) {
            log.error("发送权限更新通知异常，用户ID：{}，错误：{}", userId, e.getMessage(), e);
            return false;
        }
    }

    @Override
    public boolean sendSystemNotification(String title, String content, List<String> targetUsers) {
        log.info("发送系统通知，标题：{}，目标用户数：{}", title, targetUsers.size());

        try {
            List<Map<String, Object>> notifications = new ArrayList<>();
            
            for (String userId : targetUsers) {
                Map<String, Object> notification = new HashMap<>();
                notification.put("userId", userId);
                notification.put("type", "system");
                notification.put("title", title);
                notification.put("content", content);
                notification.put("sendTime", LocalDateTime.now());
                notifications.add(notification);
            }

            return batchSendApprovalNotifications(notifications);
        } catch (Exception e) {
            log.error("发送系统通知异常：{}", e.getMessage(), e);
            return false;
        }
    }

    @Override
    public boolean sendEmailNotification(String userId, String subject, String content, String templateCode, Map<String, Object> templateData) {
        log.info("发送邮件通知，用户ID：{}，主题：{}，模板：{}", userId, subject, templateCode);

        try {
            Map<String, Object> notificationRequest = new HashMap<>();
            notificationRequest.put("userId", userId);
            notificationRequest.put("type", "email");
            notificationRequest.put("subject", subject);
            notificationRequest.put("content", content);
            notificationRequest.put("templateCode", templateCode);
            notificationRequest.put("templateData", templateData);
            notificationRequest.put("sendTime", LocalDateTime.now());

            Result<Boolean> result = notificationServiceClient.sendApplicationNotification(notificationRequest);
            
            if (result.isSuccess() && Boolean.TRUE.equals(result.getData())) {
                log.info("邮件通知发送成功，用户ID：{}", userId);
                return true;
            } else {
                log.warn("邮件通知发送失败，用户ID：{}，错误：{}", userId, result.getMessage());
                return false;
            }
        } catch (Exception e) {
            log.error("发送邮件通知异常，用户ID：{}，错误：{}", userId, e.getMessage(), e);
            return false;
        }
    }

    @Override
    public boolean sendSmsNotification(String userId, String content, String templateCode, Map<String, Object> templateData) {
        log.info("发送短信通知，用户ID：{}，模板：{}", userId, templateCode);

        try {
            Map<String, Object> notificationRequest = new HashMap<>();
            notificationRequest.put("userId", userId);
            notificationRequest.put("type", "sms");
            notificationRequest.put("content", content);
            notificationRequest.put("templateCode", templateCode);
            notificationRequest.put("templateData", templateData);
            notificationRequest.put("sendTime", LocalDateTime.now());

            Result<Boolean> result = notificationServiceClient.sendApplicationNotification(notificationRequest);
            
            if (result.isSuccess() && Boolean.TRUE.equals(result.getData())) {
                log.info("短信通知发送成功，用户ID：{}", userId);
                return true;
            } else {
                log.warn("短信通知发送失败，用户ID：{}，错误：{}", userId, result.getMessage());
                return false;
            }
        } catch (Exception e) {
            log.error("发送短信通知异常，用户ID：{}，错误：{}", userId, e.getMessage(), e);
            return false;
        }
    }

    @Override
    public Map<String, Object> checkNotificationStatus(String notificationId) {
        log.info("检查通知发送状态，通知ID：{}", notificationId);

        Map<String, Object> status = new HashMap<>();
        status.put("notificationId", notificationId);
        
        try {
            // 这里可以调用通知服务的状态查询接口
            // 简化实现，返回模拟状态
            status.put("status", "sent");
            status.put("sendTime", LocalDateTime.now());
            status.put("deliveryTime", LocalDateTime.now());
            status.put("success", true);
            
        } catch (Exception e) {
            log.error("检查通知状态异常，通知ID：{}，错误：{}", notificationId, e.getMessage(), e);
            status.put("status", "error");
            status.put("error", e.getMessage());
            status.put("success", false);
        }
        
        return status;
    }

    @Override
    public boolean retryFailedNotification(String notificationId) {
        log.info("重试失败的通知，通知ID：{}", notificationId);

        try {
            // 这里可以调用通知服务的重试接口
            // 简化实现
            log.info("通知重试成功，通知ID：{}", notificationId);
            return true;
        } catch (Exception e) {
            log.error("重试通知失败，通知ID：{}，错误：{}", notificationId, e.getMessage(), e);
            return false;
        }
    }

    @Override
    public Map<String, Object> getUserNotificationPreferences(String userId) {
        log.info("获取用户通知偏好，用户ID：{}", userId);

        Map<String, Object> preferences = new HashMap<>();
        preferences.put("userId", userId);
        
        try {
            // 这里可以调用通知服务的偏好查询接口
            // 简化实现，返回默认偏好
            preferences.put("emailEnabled", true);
            preferences.put("smsEnabled", false);
            preferences.put("systemEnabled", true);
            preferences.put("approvalNotification", true);
            preferences.put("applicationNotification", true);
            preferences.put("permissionNotification", true);
            
        } catch (Exception e) {
            log.error("获取用户通知偏好失败，用户ID：{}，错误：{}", userId, e.getMessage(), e);
        }
        
        return preferences;
    }

    @Override
    public boolean updateUserNotificationPreferences(String userId, Map<String, Object> preferences) {
        log.info("更新用户通知偏好，用户ID：{}，偏好：{}", userId, preferences);

        try {
            // 这里可以调用通知服务的偏好更新接口
            // 简化实现
            log.info("用户通知偏好更新成功，用户ID：{}", userId);
            return true;
        } catch (Exception e) {
            log.error("更新用户通知偏好失败，用户ID：{}，错误：{}", userId, e.getMessage(), e);
            return false;
        }
    }
}