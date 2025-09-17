package com.powertrading.approval.service;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.powertrading.approval.entity.SubscriptionApplication;

import java.util.List;
import java.util.Map;

/**
 * 审批历史服务接口
 *
 * @author PowerTrading
 * @since 2024-01-15
 */
public interface ApprovalHistoryService {

    /**
     * 分页查询审批历史
     *
     * @param pageNum 页码
     * @param pageSize 每页大小
     * @param userId 用户ID（可选）
     * @param processBy 处理人（可选）
     * @param status 状态（可选）
     * @param startTime 开始时间（可选）
     * @param endTime 结束时间（可选）
     * @return 分页结果
     */
    IPage<SubscriptionApplication> getApprovalHistory(
            Integer pageNum,
            Integer pageSize,
            String userId,
            String processBy,
            String status,
            String startTime,
            String endTime
    );

    /**
     * 查询用户的审批历史
     *
     * @param userId 用户ID
     * @param pageNum 页码
     * @param pageSize 每页大小
     * @return 分页结果
     */
    IPage<SubscriptionApplication> getUserApprovalHistory(String userId, Integer pageNum, Integer pageSize);

    /**
     * 查询审批人的处理历史
     *
     * @param processBy 处理人
     * @param pageNum 页码
     * @param pageSize 每页大小
     * @return 分页结果
     */
    IPage<SubscriptionApplication> getProcessorHistory(String processBy, Integer pageNum, Integer pageSize);

    /**
     * 获取申请的详细历史记录
     *
     * @param applicationId 申请ID
     * @return 历史记录列表
     */
    List<Map<String, Object>> getApplicationDetailHistory(String applicationId);

    /**
     * 导出审批历史
     *
     * @param criteria 导出条件
     * @return 导出文件路径
     */
    String exportApprovalHistory(Map<String, Object> criteria);

    /**
     * 获取审批历史统计
     *
     * @param startTime 开始时间
     * @param endTime 结束时间
     * @return 统计结果
     */
    Map<String, Object> getApprovalHistoryStatistics(String startTime, String endTime);

    /**
     * 按时间段统计审批数量
     *
     * @param startTime 开始时间
     * @param endTime 结束时间
     * @param groupBy 分组方式（day/week/month）
     * @return 统计结果
     */
    List<Map<String, Object>> getApprovalCountByPeriod(String startTime, String endTime, String groupBy);

    /**
     * 获取审批人员工作量统计
     *
     * @param startTime 开始时间
     * @param endTime 结束时间
     * @return 统计结果
     */
    List<Map<String, Object>> getProcessorWorkloadStatistics(String startTime, String endTime);

    /**
     * 查询最近的审批活动
     *
     * @param limit 限制数量
     * @return 最近活动列表
     */
    List<Map<String, Object>> getRecentApprovalActivities(Integer limit);

    /**
     * 搜索审批历史
     *
     * @param keyword 关键词
     * @param pageNum 页码
     * @param pageSize 每页大小
     * @return 搜索结果
     */
    IPage<SubscriptionApplication> searchApprovalHistory(String keyword, Integer pageNum, Integer pageSize);

    /**
     * 获取审批趋势分析
     *
     * @param days 分析天数
     * @return 趋势数据
     */
    Map<String, Object> getApprovalTrendAnalysis(Integer days);
}