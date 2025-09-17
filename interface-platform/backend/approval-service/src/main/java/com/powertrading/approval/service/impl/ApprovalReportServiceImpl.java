package com.powertrading.approval.service.impl;

import cn.hutool.core.date.DateUtil;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.powertrading.approval.entity.SubscriptionApplication;
import com.powertrading.approval.entity.UserInterfaceSubscription;
import com.powertrading.approval.mapper.SubscriptionApplicationMapper;
import com.powertrading.approval.mapper.UserInterfaceSubscriptionMapper;
import com.powertrading.approval.service.ApprovalReportService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.stream.Collectors;

/**
 * 审批统计和报表服务实现类
 *
 * @author PowerTrading
 * @since 2024-01-15
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class ApprovalReportServiceImpl implements ApprovalReportService {

    private final SubscriptionApplicationMapper applicationMapper;
    private final UserInterfaceSubscriptionMapper subscriptionMapper;

    @Override
    public Map<String, Object> getApprovalOverview() {
        log.info("获取审批概览统计");

        Map<String, Object> overview = new HashMap<>();
        
        // 获取所有申请
        List<SubscriptionApplication> allApplications = applicationMapper.selectList(null);
        
        // 基础统计
        long totalApplications = allApplications.size();
        long pendingApplications = allApplications.stream().filter(app -> "pending".equals(app.getStatus())).count();
        long approvedApplications = allApplications.stream().filter(app -> "approved".equals(app.getStatus())).count();
        long rejectedApplications = allApplications.stream().filter(app -> "rejected".equals(app.getStatus())).count();
        
        overview.put("totalApplications", totalApplications);
        overview.put("pendingApplications", pendingApplications);
        overview.put("approvedApplications", approvedApplications);
        overview.put("rejectedApplications", rejectedApplications);
        overview.put("approvalRate", totalApplications > 0 ? (double) approvedApplications / totalApplications : 0.0);
        
        // 今日统计
        LocalDateTime todayStart = LocalDateTime.now().withHour(0).withMinute(0).withSecond(0).withNano(0);
        long todaySubmitted = allApplications.stream()
                .filter(app -> app.getSubmitTime().isAfter(todayStart))
                .count();
        long todayProcessed = allApplications.stream()
                .filter(app -> app.getProcessTime() != null && app.getProcessTime().isAfter(todayStart))
                .count();
        
        overview.put("todaySubmitted", todaySubmitted);
        overview.put("todayProcessed", todayProcessed);
        
        // 活跃用户数
        Set<String> activeUsers = allApplications.stream()
                .filter(app -> app.getSubmitTime().isAfter(LocalDateTime.now().minusDays(30)))
                .map(SubscriptionApplication::getUserId)
                .collect(Collectors.toSet());
        overview.put("activeUsers", activeUsers.size());
        
        // 活跃订阅数
        List<UserInterfaceSubscription> activeSubscriptions = subscriptionMapper.selectList(
                new LambdaQueryWrapper<UserInterfaceSubscription>()
                        .eq(UserInterfaceSubscription::getStatus, "active")
        );
        overview.put("activeSubscriptions", activeSubscriptions.size());
        
        overview.put("updateTime", LocalDateTime.now());
        
        return overview;
    }

    @Override
    public Map<String, Object> getApprovalTrend(Integer days) {
        log.info("获取审批趋势统计，天数：{}", days);

        LocalDateTime endTime = LocalDateTime.now();
        LocalDateTime startTime = endTime.minusDays(days);
        
        LambdaQueryWrapper<SubscriptionApplication> wrapper = new LambdaQueryWrapper<>();
        wrapper.ge(SubscriptionApplication::getSubmitTime, startTime)
                .le(SubscriptionApplication::getSubmitTime, endTime);
        
        List<SubscriptionApplication> applications = applicationMapper.selectList(wrapper);
        
        Map<String, Object> trend = new HashMap<>();
        
        // 按日期分组统计
        Map<String, Long> dailySubmissions = new HashMap<>();
        Map<String, Long> dailyApprovals = new HashMap<>();
        Map<String, Long> dailyRejections = new HashMap<>();
        
        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd");
        
        for (SubscriptionApplication app : applications) {
            String submitDate = app.getSubmitTime().format(formatter);
            dailySubmissions.put(submitDate, dailySubmissions.getOrDefault(submitDate, 0L) + 1);
            
            if (app.getProcessTime() != null) {
                String processDate = app.getProcessTime().format(formatter);
                if ("approved".equals(app.getStatus())) {
                    dailyApprovals.put(processDate, dailyApprovals.getOrDefault(processDate, 0L) + 1);
                } else if ("rejected".equals(app.getStatus())) {
                    dailyRejections.put(processDate, dailyRejections.getOrDefault(processDate, 0L) + 1);
                }
            }
        }
        
        trend.put("dailySubmissions", dailySubmissions);
        trend.put("dailyApprovals", dailyApprovals);
        trend.put("dailyRejections", dailyRejections);
        trend.put("startTime", startTime);
        trend.put("endTime", endTime);
        trend.put("days", days);
        
        return trend;
    }

    @Override
    public List<Map<String, Object>> getProcessorWorkloadReport(String startTime, String endTime) {
        log.info("获取审批人员工作量报表，时间范围：{} - {}", startTime, endTime);

        LocalDateTime start = LocalDateTime.parse(startTime, DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss"));
        LocalDateTime end = LocalDateTime.parse(endTime, DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss"));
        
        LambdaQueryWrapper<SubscriptionApplication> wrapper = new LambdaQueryWrapper<>();
        wrapper.isNotNull(SubscriptionApplication::getProcessBy)
                .ge(SubscriptionApplication::getProcessTime, start)
                .le(SubscriptionApplication::getProcessTime, end);
        
        List<SubscriptionApplication> applications = applicationMapper.selectList(wrapper);
        
        // 按处理人分组统计
        Map<String, Map<String, Object>> workloadMap = new HashMap<>();
        
        for (SubscriptionApplication app : applications) {
            String processBy = app.getProcessBy();
            workloadMap.computeIfAbsent(processBy, k -> {
                Map<String, Object> stats = new HashMap<>();
                stats.put("processBy", k);
                stats.put("totalProcessed", 0L);
                stats.put("approved", 0L);
                stats.put("rejected", 0L);
                stats.put("avgProcessingTime", 0.0);
                stats.put("processingTimes", new ArrayList<Long>());
                return stats;
            });
            
            Map<String, Object> stats = workloadMap.get(processBy);
            stats.put("totalProcessed", (Long) stats.get("totalProcessed") + 1);
            
            if ("approved".equals(app.getStatus())) {
                stats.put("approved", (Long) stats.get("approved") + 1);
            } else if ("rejected".equals(app.getStatus())) {
                stats.put("rejected", (Long) stats.get("rejected") + 1);
            }
            
            // 计算处理时长
            if (app.getProcessTime() != null && app.getSubmitTime() != null) {
                long processingTime = Duration.between(app.getSubmitTime(), app.getProcessTime()).toHours();
                @SuppressWarnings("unchecked")
                List<Long> times = (List<Long>) stats.get("processingTimes");
                times.add(processingTime);
            }
        }
        
        // 计算平均处理时间
        for (Map<String, Object> stats : workloadMap.values()) {
            @SuppressWarnings("unchecked")
            List<Long> times = (List<Long>) stats.get("processingTimes");
            if (!times.isEmpty()) {
                double avgTime = times.stream().mapToLong(Long::longValue).average().orElse(0.0);
                stats.put("avgProcessingTime", avgTime);
            }
            stats.remove("processingTimes"); // 移除临时数据
        }
        
        return workloadMap.values().stream()
                .sorted((a, b) -> Long.compare(
                        (Long) b.get("totalProcessed"),
                        (Long) a.get("totalProcessed")
                ))
                .collect(Collectors.toList());
    }

    @Override
    public List<Map<String, Object>> getUserApplicationReport(String startTime, String endTime) {
        log.info("获取用户申请统计报表，时间范围：{} - {}", startTime, endTime);

        LocalDateTime start = LocalDateTime.parse(startTime, DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss"));
        LocalDateTime end = LocalDateTime.parse(endTime, DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss"));
        
        LambdaQueryWrapper<SubscriptionApplication> wrapper = new LambdaQueryWrapper<>();
        wrapper.ge(SubscriptionApplication::getSubmitTime, start)
                .le(SubscriptionApplication::getSubmitTime, end);
        
        List<SubscriptionApplication> applications = applicationMapper.selectList(wrapper);
        
        // 按用户分组统计
        Map<String, Map<String, Object>> userStatsMap = new HashMap<>();
        
        for (SubscriptionApplication app : applications) {
            String userId = app.getUserId();
            userStatsMap.computeIfAbsent(userId, k -> {
                Map<String, Object> stats = new HashMap<>();
                stats.put("userId", k);
                stats.put("totalApplications", 0L);
                stats.put("approved", 0L);
                stats.put("rejected", 0L);
                stats.put("pending", 0L);
                stats.put("totalInterfaces", 0L);
                return stats;
            });
            
            Map<String, Object> stats = userStatsMap.get(userId);
            stats.put("totalApplications", (Long) stats.get("totalApplications") + 1);
            stats.put("totalInterfaces", (Long) stats.get("totalInterfaces") + app.getInterfaceIds().size());
            
            switch (app.getStatus()) {
                case "approved":
                    stats.put("approved", (Long) stats.get("approved") + 1);
                    break;
                case "rejected":
                    stats.put("rejected", (Long) stats.get("rejected") + 1);
                    break;
                case "pending":
                    stats.put("pending", (Long) stats.get("pending") + 1);
                    break;
            }
        }
        
        // 计算成功率
        for (Map<String, Object> stats : userStatsMap.values()) {
            long total = (Long) stats.get("totalApplications");
            long approved = (Long) stats.get("approved");
            stats.put("successRate", total > 0 ? (double) approved / total : 0.0);
        }
        
        return userStatsMap.values().stream()
                .sorted((a, b) -> Long.compare(
                        (Long) b.get("totalApplications"),
                        (Long) a.get("totalApplications")
                ))
                .collect(Collectors.toList());
    }

    @Override
    public List<Map<String, Object>> getInterfaceSubscriptionReport(String startTime, String endTime) {
        log.info("获取接口订阅统计报表，时间范围：{} - {}", startTime, endTime);

        LocalDateTime start = LocalDateTime.parse(startTime, DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss"));
        LocalDateTime end = LocalDateTime.parse(endTime, DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss"));
        
        LambdaQueryWrapper<UserInterfaceSubscription> wrapper = new LambdaQueryWrapper<>();
        wrapper.ge(UserInterfaceSubscription::getSubscribeTime, start)
                .le(UserInterfaceSubscription::getSubscribeTime, end);
        
        List<UserInterfaceSubscription> subscriptions = subscriptionMapper.selectList(wrapper);
        
        // 按接口分组统计
        Map<String, Map<String, Object>> interfaceStatsMap = new HashMap<>();
        
        for (UserInterfaceSubscription subscription : subscriptions) {
            String interfaceId = subscription.getInterfaceId();
            interfaceStatsMap.computeIfAbsent(interfaceId, k -> {
                Map<String, Object> stats = new HashMap<>();
                stats.put("interfaceId", k);
                stats.put("totalSubscriptions", 0L);
                stats.put("activeSubscriptions", 0L);
                stats.put("totalCalls", 0L);
                return stats;
            });
            
            Map<String, Object> stats = interfaceStatsMap.get(interfaceId);
            stats.put("totalSubscriptions", (Long) stats.get("totalSubscriptions") + 1);
            
            if ("active".equals(subscription.getStatus())) {
                stats.put("activeSubscriptions", (Long) stats.get("activeSubscriptions") + 1);
            }
            
            if (subscription.getCallCount() != null) {
                stats.put("totalCalls", (Long) stats.get("totalCalls") + subscription.getCallCount());
            }
        }
        
        return interfaceStatsMap.values().stream()
                .sorted((a, b) -> Long.compare(
                        (Long) b.get("totalSubscriptions"),
                        (Long) a.get("totalSubscriptions")
                ))
                .collect(Collectors.toList());
    }

    @Override
    public Map<String, Object> getApprovalEfficiencyAnalysis(String startTime, String endTime) {
        log.info("获取审批效率分析，时间范围：{} - {}", startTime, endTime);

        LocalDateTime start = LocalDateTime.parse(startTime, DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss"));
        LocalDateTime end = LocalDateTime.parse(endTime, DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss"));
        
        LambdaQueryWrapper<SubscriptionApplication> wrapper = new LambdaQueryWrapper<>();
        wrapper.isNotNull(SubscriptionApplication::getProcessTime)
                .ge(SubscriptionApplication::getProcessTime, start)
                .le(SubscriptionApplication::getProcessTime, end);
        
        List<SubscriptionApplication> applications = applicationMapper.selectList(wrapper);
        
        Map<String, Object> analysis = new HashMap<>();
        
        if (applications.isEmpty()) {
            analysis.put("totalProcessed", 0);
            analysis.put("avgProcessingTime", 0.0);
            analysis.put("minProcessingTime", 0.0);
            analysis.put("maxProcessingTime", 0.0);
            return analysis;
        }
        
        // 计算处理时长
        List<Long> processingTimes = applications.stream()
                .filter(app -> app.getProcessTime() != null && app.getSubmitTime() != null)
                .map(app -> Duration.between(app.getSubmitTime(), app.getProcessTime()).toHours())
                .collect(Collectors.toList());
        
        if (!processingTimes.isEmpty()) {
            double avgTime = processingTimes.stream().mapToLong(Long::longValue).average().orElse(0.0);
            long minTime = processingTimes.stream().mapToLong(Long::longValue).min().orElse(0L);
            long maxTime = processingTimes.stream().mapToLong(Long::longValue).max().orElse(0L);
            
            analysis.put("avgProcessingTime", avgTime);
            analysis.put("minProcessingTime", (double) minTime);
            analysis.put("maxProcessingTime", (double) maxTime);
        }
        
        analysis.put("totalProcessed", applications.size());
        analysis.put("approvedCount", applications.stream().filter(app -> "approved".equals(app.getStatus())).count());
        analysis.put("rejectedCount", applications.stream().filter(app -> "rejected".equals(app.getStatus())).count());
        
        // 效率等级分布
        Map<String, Long> efficiencyDistribution = new HashMap<>();
        for (Long time : processingTimes) {
            String level;
            if (time <= 24) {
                level = "高效（≤24小时）";
            } else if (time <= 72) {
                level = "正常（24-72小时）";
            } else {
                level = "较慢（>72小时）";
            }
            efficiencyDistribution.put(level, efficiencyDistribution.getOrDefault(level, 0L) + 1);
        }
        analysis.put("efficiencyDistribution", efficiencyDistribution);
        
        return analysis;
    }

    @Override
    public Map<String, Object> getMonthlyApprovalReport(Integer year, Integer month) {
        log.info("获取月度审批报表，年份：{}，月份：{}", year, month);

        LocalDateTime startTime = LocalDateTime.of(year, month, 1, 0, 0, 0);
        LocalDateTime endTime = startTime.plusMonths(1).minusSeconds(1);
        
        return getApprovalEfficiencyAnalysis(
                startTime.format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss")),
                endTime.format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss"))
        );
    }

    @Override
    public Map<String, Object> getQuarterlyApprovalReport(Integer year, Integer quarter) {
        log.info("获取季度审批报表，年份：{}，季度：{}", year, quarter);

        int startMonth = (quarter - 1) * 3 + 1;
        LocalDateTime startTime = LocalDateTime.of(year, startMonth, 1, 0, 0, 0);
        LocalDateTime endTime = startTime.plusMonths(3).minusSeconds(1);
        
        return getApprovalEfficiencyAnalysis(
                startTime.format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss")),
                endTime.format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss"))
        );
    }

    @Override
    public Map<String, Object> getYearlyApprovalReport(Integer year) {
        log.info("获取年度审批报表，年份：{}", year);

        LocalDateTime startTime = LocalDateTime.of(year, 1, 1, 0, 0, 0);
        LocalDateTime endTime = LocalDateTime.of(year, 12, 31, 23, 59, 59);
        
        return getApprovalEfficiencyAnalysis(
                startTime.format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss")),
                endTime.format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss"))
        );
    }

    @Override
    public Map<String, Object> getApprovalDurationAnalysis(String startTime, String endTime) {
        log.info("获取审批时长分析，时间范围：{} - {}", startTime, endTime);
        
        return getApprovalEfficiencyAnalysis(startTime, endTime);
    }

    @Override
    public List<Map<String, Object>> getRejectionReasonAnalysis(String startTime, String endTime) {
        log.info("获取拒绝原因分析，时间范围：{} - {}", startTime, endTime);

        LocalDateTime start = LocalDateTime.parse(startTime, DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss"));
        LocalDateTime end = LocalDateTime.parse(endTime, DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss"));
        
        LambdaQueryWrapper<SubscriptionApplication> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(SubscriptionApplication::getStatus, "rejected")
                .ge(SubscriptionApplication::getProcessTime, start)
                .le(SubscriptionApplication::getProcessTime, end)
                .isNotNull(SubscriptionApplication::getProcessComment);
        
        List<SubscriptionApplication> rejectedApplications = applicationMapper.selectList(wrapper);
        
        // 分析拒绝原因
        Map<String, Long> reasonCount = new HashMap<>();
        for (SubscriptionApplication app : rejectedApplications) {
            String comment = app.getProcessComment();
            if (comment != null && !comment.isEmpty()) {
                // 简单的关键词分析
                if (comment.contains("权限") || comment.contains("授权")) {
                    reasonCount.put("权限不足", reasonCount.getOrDefault("权限不足", 0L) + 1);
                } else if (comment.contains("资料") || comment.contains("信息")) {
                    reasonCount.put("资料不全", reasonCount.getOrDefault("资料不全", 0L) + 1);
                } else if (comment.contains("业务") || comment.contains("场景")) {
                    reasonCount.put("业务场景不符", reasonCount.getOrDefault("业务场景不符", 0L) + 1);
                } else {
                    reasonCount.put("其他原因", reasonCount.getOrDefault("其他原因", 0L) + 1);
                }
            }
        }
        
        return reasonCount.entrySet().stream()
                .map(entry -> {
                    Map<String, Object> item = new HashMap<>();
                    item.put("reason", entry.getKey());
                    item.put("count", entry.getValue());
                    item.put("percentage", rejectedApplications.size() > 0 ? 
                            (double) entry.getValue() / rejectedApplications.size() * 100 : 0.0);
                    return item;
                })
                .sorted((a, b) -> Long.compare((Long) b.get("count"), (Long) a.get("count")))
                .collect(Collectors.toList());
    }

    @Override
    public List<Map<String, Object>> getPopularInterfacesReport(String startTime, String endTime, Integer limit) {
        log.info("获取热门接口统计，时间范围：{} - {}，限制：{}", startTime, endTime, limit);
        
        List<Map<String, Object>> interfaceReport = getInterfaceSubscriptionReport(startTime, endTime);
        
        return interfaceReport.stream()
                .limit(limit)
                .collect(Collectors.toList());
    }

    @Override
    public String generateReportFile(String reportType, Map<String, Object> parameters, String format) {
        log.info("生成审批报表文件，类型：{}，格式：{}", reportType, format);
        
        // 简化实现，返回文件路径
        String fileName = String.format("%s_report_%s.%s", reportType, System.currentTimeMillis(), format);
        String filePath = "/tmp/" + fileName;
        
        // TODO: 实际的文件生成逻辑
        log.info("报表文件生成：{}", filePath);
        
        return filePath;
    }

    @Override
    public Map<String, Object> getRealTimeApprovalData() {
        log.info("获取实时审批数据");
        
        return getApprovalOverview();
    }

    @Override
    public List<Map<String, Object>> getApprovalAlerts() {
        log.info("获取审批预警信息");
        
        List<Map<String, Object>> alerts = new ArrayList<>();
        
        // 检查长时间未处理的申请
        LocalDateTime alertTime = LocalDateTime.now().minusDays(3);
        LambdaQueryWrapper<SubscriptionApplication> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(SubscriptionApplication::getStatus, "pending")
                .lt(SubscriptionApplication::getSubmitTime, alertTime);
        
        List<SubscriptionApplication> overdueApplications = applicationMapper.selectList(wrapper);
        
        if (!overdueApplications.isEmpty()) {
            Map<String, Object> alert = new HashMap<>();
            alert.put("type", "overdue_applications");
            alert.put("level", "warning");
            alert.put("title", "申请处理超时预警");
            alert.put("message", String.format("有%d个申请超过3天未处理", overdueApplications.size()));
            alert.put("count", overdueApplications.size());
            alert.put("createTime", LocalDateTime.now());
            alerts.add(alert);
        }
        
        // 检查今日待处理申请数量
        LocalDateTime todayStart = LocalDateTime.now().withHour(0).withMinute(0).withSecond(0).withNano(0);
        long todayPending = applicationMapper.selectCount(
                new LambdaQueryWrapper<SubscriptionApplication>()
                        .eq(SubscriptionApplication::getStatus, "pending")
                        .ge(SubscriptionApplication::getSubmitTime, todayStart)
        );
        
        if (todayPending > 10) {
            Map<String, Object> alert = new HashMap<>();
            alert.put("type", "high_pending_count");
            alert.put("level", "info");
            alert.put("title", "今日待处理申请较多");
            alert.put("message", String.format("今日有%d个申请待处理", todayPending));
            alert.put("count", todayPending);
            alert.put("createTime", LocalDateTime.now());
            alerts.add(alert);
        }
        
        return alerts;
    }

    @Override
    public List<Map<String, Object>> getCustomReportData(Map<String, Object> query) {
        log.info("获取自定义报表数据，查询条件：{}", query);
        
        // 简化实现，根据查询条件返回相应数据
        String reportType = (String) query.get("reportType");
        
        switch (reportType) {
            case "user":
                return getUserApplicationReport(
                        (String) query.get("startTime"),
                        (String) query.get("endTime")
                );
            case "processor":
                return getProcessorWorkloadReport(
                        (String) query.get("startTime"),
                        (String) query.get("endTime")
                );
            case "interface":
                return getInterfaceSubscriptionReport(
                        (String) query.get("startTime"),
                        (String) query.get("endTime")
                );
            default:
                return Collections.emptyList();
        }
    }
}