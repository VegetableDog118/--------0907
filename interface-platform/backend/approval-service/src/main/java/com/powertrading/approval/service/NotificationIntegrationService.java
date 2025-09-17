package com.powertrading.approval.service;

import java.util.List;
import java.util.Map;

/**
 * 通知集成服务接口
 *
 * @author PowerTrading
 * @since 2024-01-15
 */
public interface NotificationIntegrationService {

    /**
     * 发送申请提交通知
     *
     * @param userId 用户ID
     * @param applicationId 申请ID
     * @param interfaceCount 接口数量
     * @return 发送结果
     */
    boolean sendApplicationSubmittedNotification(String userId, String applicationId, Integer interfaceCount);

    /**
     * 发送审批结果通知
     *
     * @param userId 用户ID
     * @param applicationId 申请ID
     * @param action 审批动作（approved/rejected）
     * @param processBy 处理人
     * @param comment 处理意见
     * @return 发送结果
     */
    boolean sendApprovalResultNotification(String userId, String applicationId, String action, String processBy, String comment);

    /**
     * 批量发送审批结果通知
     *
     * @param notifications 通知列表
     * @return 发送结果
     */
    boolean batchSendApprovalNotifications(List<Map<String, Object>> notifications);

    /**
     * 发送审批提醒通知（给审批人员）
     *
     * @param processBy 审批人员
     * @param pendingCount 待审批数量
     * @return 发送结果
     */
    boolean sendApprovalReminderNotification(String processBy, Integer pendingCount);

    /**
     * 发送申请过期通知
     *
     * @param userId 用户ID
     * @param applicationId 申请ID
     * @param expiredDays 过期天数
     * @return 发送结果
     */
    boolean sendApplicationExpiredNotification(String userId, String applicationId, Integer expiredDays);

    /**
     * 发送权限更新通知
     *
     * @param userId 用户ID
     * @param interfaceIds 接口ID列表
     * @param action 动作（grant/revoke）
     * @return 发送结果
     */
    boolean sendPermissionUpdateNotification(String userId, List<String> interfaceIds, String action);

    /**
     * 发送系统通知
     *
     * @param title 通知标题
     * @param content 通知内容
     * @param targetUsers 目标用户列表
     * @return 发送结果
     */
    boolean sendSystemNotification(String title, String content, List<String> targetUsers);

    /**
     * 发送邮件通知
     *
     * @param userId 用户ID
     * @param subject 邮件主题
     * @param content 邮件内容
     * @param templateCode 模板编码
     * @param templateData 模板数据
     * @return 发送结果
     */
    boolean sendEmailNotification(String userId, String subject, String content, String templateCode, Map<String, Object> templateData);

    /**
     * 发送短信通知
     *
     * @param userId 用户ID
     * @param content 短信内容
     * @param templateCode 模板编码
     * @param templateData 模板数据
     * @return 发送结果
     */
    boolean sendSmsNotification(String userId, String content, String templateCode, Map<String, Object> templateData);

    /**
     * 检查通知发送状态
     *
     * @param notificationId 通知ID
     * @return 发送状态
     */
    Map<String, Object> checkNotificationStatus(String notificationId);

    /**
     * 重试失败的通知
     *
     * @param notificationId 通知ID
     * @return 重试结果
     */
    boolean retryFailedNotification(String notificationId);

    /**
     * 获取用户通知偏好
     *
     * @param userId 用户ID
     * @return 通知偏好
     */
    Map<String, Object> getUserNotificationPreferences(String userId);

    /**
     * 更新用户通知偏好
     *
     * @param userId 用户ID
     * @param preferences 通知偏好
     * @return 更新结果
     */
    boolean updateUserNotificationPreferences(String userId, Map<String, Object> preferences);
}