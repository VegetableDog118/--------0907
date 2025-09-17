package com.powertrading.approval.service;

import com.powertrading.approval.dto.ProcessApplicationRequest;
import com.powertrading.approval.entity.SubscriptionApplication;

import java.util.List;

/**
 * 审批工作流服务接口
 *
 * @author PowerTrading
 * @since 2024-01-15
 */
public interface ApprovalWorkflowService {

    /**
     * 执行一级审批流程
     *
     * @param processBy 处理人
     * @param request 处理请求
     * @return 处理结果
     */
    boolean executeFirstLevelApproval(String processBy, ProcessApplicationRequest request);

    /**
     * 验证审批权限
     *
     * @param userId 用户ID
     * @return 是否有审批权限
     */
    boolean validateApprovalPermission(String userId);

    /**
     * 检查申请是否可以审批
     *
     * @param applicationId 申请ID
     * @return 是否可以审批
     */
    boolean canApprove(String applicationId);

    /**
     * 获取待审批申请列表
     *
     * @param pageNum 页码
     * @param pageSize 每页大小
     * @return 待审批申请列表
     */
    List<SubscriptionApplication> getPendingApplications(Integer pageNum, Integer pageSize);

    /**
     * 自动审批（基于规则）
     *
     * @param applicationId 申请ID
     * @return 自动审批结果
     */
    boolean autoApprove(String applicationId);

    /**
     * 审批流程状态检查
     *
     * @param applicationId 申请ID
     * @return 流程状态
     */
    String getApprovalStatus(String applicationId);

    /**
     * 撤回审批
     *
     * @param processBy 处理人
     * @param applicationId 申请ID
     * @param reason 撤回原因
     * @return 撤回结果
     */
    boolean withdrawApproval(String processBy, String applicationId, String reason);
}