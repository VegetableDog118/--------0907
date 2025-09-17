package com.powertrading.approval.service;

import com.powertrading.approval.entity.SubscriptionApplication;

import java.util.List;

/**
 * 申请状态管理服务接口
 *
 * @author PowerTrading
 * @since 2024-01-15
 */
public interface ApplicationStatusService {

    /**
     * 更新申请状态
     *
     * @param applicationId 申请ID
     * @param newStatus 新状态
     * @param processBy 处理人
     * @param comment 处理意见
     * @return 更新结果
     */
    boolean updateApplicationStatus(String applicationId, String newStatus, String processBy, String comment);

    /**
     * 批量更新申请状态
     *
     * @param applicationIds 申请ID列表
     * @param newStatus 新状态
     * @param processBy 处理人
     * @param comment 处理意见
     * @return 更新结果
     */
    boolean batchUpdateApplicationStatus(List<String> applicationIds, String newStatus, String processBy, String comment);

    /**
     * 验证状态转换是否合法
     *
     * @param currentStatus 当前状态
     * @param targetStatus 目标状态
     * @return 是否合法
     */
    boolean isValidStatusTransition(String currentStatus, String targetStatus);

    /**
     * 获取申请状态历史
     *
     * @param applicationId 申请ID
     * @return 状态历史
     */
    List<String> getStatusHistory(String applicationId);

    /**
     * 自动更新用户权限
     *
     * @param applicationId 申请ID
     * @return 更新结果
     */
    boolean autoUpdateUserPermissions(String applicationId);

    /**
     * 批量自动更新用户权限
     *
     * @param applicationIds 申请ID列表
     * @return 更新结果
     */
    boolean batchAutoUpdateUserPermissions(List<String> applicationIds);

    /**
     * 检查申请是否过期
     *
     * @param applicationId 申请ID
     * @return 是否过期
     */
    boolean isApplicationExpired(String applicationId);

    /**
     * 处理过期申请
     *
     * @return 处理数量
     */
    int processExpiredApplications();

    /**
     * 获取状态统计
     *
     * @return 状态统计
     */
    java.util.Map<String, Long> getStatusStatistics();

    /**
     * 重置申请状态（管理员功能）
     *
     * @param applicationId 申请ID
     * @param adminUserId 管理员用户ID
     * @param reason 重置原因
     * @return 重置结果
     */
    boolean resetApplicationStatus(String applicationId, String adminUserId, String reason);
}