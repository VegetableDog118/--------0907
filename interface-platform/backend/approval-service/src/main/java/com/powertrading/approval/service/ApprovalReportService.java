package com.powertrading.approval.service;

import java.util.List;
import java.util.Map;

/**
 * 审批统计和报表服务接口
 *
 * @author PowerTrading
 * @since 2024-01-15
 */
public interface ApprovalReportService {

    /**
     * 获取审批概览统计
     *
     * @return 概览统计数据
     */
    Map<String, Object> getApprovalOverview();

    /**
     * 获取审批趋势统计
     *
     * @param days 统计天数
     * @return 趋势统计数据
     */
    Map<String, Object> getApprovalTrend(Integer days);

    /**
     * 获取审批人员工作量报表
     *
     * @param startTime 开始时间
     * @param endTime 结束时间
     * @return 工作量报表
     */
    List<Map<String, Object>> getProcessorWorkloadReport(String startTime, String endTime);

    /**
     * 获取用户申请统计报表
     *
     * @param startTime 开始时间
     * @param endTime 结束时间
     * @return 用户申请统计
     */
    List<Map<String, Object>> getUserApplicationReport(String startTime, String endTime);

    /**
     * 获取接口订阅统计报表
     *
     * @param startTime 开始时间
     * @param endTime 结束时间
     * @return 接口订阅统计
     */
    List<Map<String, Object>> getInterfaceSubscriptionReport(String startTime, String endTime);

    /**
     * 获取审批效率分析
     *
     * @param startTime 开始时间
     * @param endTime 结束时间
     * @return 效率分析数据
     */
    Map<String, Object> getApprovalEfficiencyAnalysis(String startTime, String endTime);

    /**
     * 获取月度审批报表
     *
     * @param year 年份
     * @param month 月份
     * @return 月度报表
     */
    Map<String, Object> getMonthlyApprovalReport(Integer year, Integer month);

    /**
     * 获取季度审批报表
     *
     * @param year 年份
     * @param quarter 季度（1-4）
     * @return 季度报表
     */
    Map<String, Object> getQuarterlyApprovalReport(Integer year, Integer quarter);

    /**
     * 获取年度审批报表
     *
     * @param year 年份
     * @return 年度报表
     */
    Map<String, Object> getYearlyApprovalReport(Integer year);

    /**
     * 获取审批时长分析
     *
     * @param startTime 开始时间
     * @param endTime 结束时间
     * @return 时长分析数据
     */
    Map<String, Object> getApprovalDurationAnalysis(String startTime, String endTime);

    /**
     * 获取拒绝原因分析
     *
     * @param startTime 开始时间
     * @param endTime 结束时间
     * @return 拒绝原因分析
     */
    List<Map<String, Object>> getRejectionReasonAnalysis(String startTime, String endTime);

    /**
     * 获取热门接口统计
     *
     * @param startTime 开始时间
     * @param endTime 结束时间
     * @param limit 限制数量
     * @return 热门接口列表
     */
    List<Map<String, Object>> getPopularInterfacesReport(String startTime, String endTime, Integer limit);

    /**
     * 生成审批报表文件
     *
     * @param reportType 报表类型
     * @param parameters 报表参数
     * @param format 文件格式（excel/pdf/csv）
     * @return 文件路径
     */
    String generateReportFile(String reportType, Map<String, Object> parameters, String format);

    /**
     * 获取实时审批数据
     *
     * @return 实时数据
     */
    Map<String, Object> getRealTimeApprovalData();

    /**
     * 获取审批预警信息
     *
     * @return 预警信息列表
     */
    List<Map<String, Object>> getApprovalAlerts();

    /**
     * 获取自定义报表数据
     *
     * @param query 自定义查询条件
     * @return 报表数据
     */
    List<Map<String, Object>> getCustomReportData(Map<String, Object> query);
}