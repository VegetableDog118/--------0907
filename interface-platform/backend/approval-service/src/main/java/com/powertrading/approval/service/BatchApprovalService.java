package com.powertrading.approval.service;

import com.powertrading.approval.dto.ProcessApplicationRequest;

import java.util.List;
import java.util.Map;

/**
 * 批量审批服务接口
 *
 * @author PowerTrading
 * @since 2024-01-15
 */
public interface BatchApprovalService {

    /**
     * 批量审批申请
     *
     * @param processBy 处理人
     * @param applicationIds 申请ID列表
     * @param action 审批动作（approved/rejected）
     * @param comment 处理意见
     * @return 批量处理结果
     */
    Map<String, Object> batchApproveApplications(String processBy, List<String> applicationIds, String action, String comment);

    /**
     * 智能批量审批（基于规则）
     *
     * @param processBy 处理人
     * @param criteria 审批条件
     * @return 批量处理结果
     */
    Map<String, Object> smartBatchApproval(String processBy, Map<String, Object> criteria);

    /**
     * 按条件批量审批
     *
     * @param processBy 处理人
     * @param userId 用户ID（可选）
     * @param maxInterfaceCount 最大接口数量
     * @param maxEstimatedCalls 最大预计调用次数
     * @param action 审批动作
     * @param comment 处理意见
     * @return 批量处理结果
     */
    Map<String, Object> batchApprovalByCondition(
            String processBy,
            String userId,
            Integer maxInterfaceCount,
            Integer maxEstimatedCalls,
            String action,
            String comment
    );

    /**
     * 获取批量审批预览
     *
     * @param criteria 筛选条件
     * @return 符合条件的申请列表
     */
    List<String> getBatchApprovalPreview(Map<String, Object> criteria);

    /**
     * 异步批量审批
     *
     * @param processBy 处理人
     * @param request 批量处理请求
     * @return 任务ID
     */
    String asyncBatchApproval(String processBy, ProcessApplicationRequest request);

    /**
     * 查询异步批量审批状态
     *
     * @param taskId 任务ID
     * @return 任务状态
     */
    Map<String, Object> getAsyncBatchApprovalStatus(String taskId);

    /**
     * 批量审批统计
     *
     * @param processBy 处理人
     * @param startTime 开始时间
     * @param endTime 结束时间
     * @return 统计结果
     */
    Map<String, Object> getBatchApprovalStatistics(String processBy, String startTime, String endTime);

    /**
     * 撤销批量审批
     *
     * @param processBy 处理人
     * @param applicationIds 申请ID列表
     * @param reason 撤销原因
     * @return 撤销结果
     */
    Map<String, Object> revokeBatchApproval(String processBy, List<String> applicationIds, String reason);

    /**
     * 导出批量审批结果
     *
     * @param taskId 任务ID
     * @return 导出文件路径
     */
    String exportBatchApprovalResult(String taskId);
}