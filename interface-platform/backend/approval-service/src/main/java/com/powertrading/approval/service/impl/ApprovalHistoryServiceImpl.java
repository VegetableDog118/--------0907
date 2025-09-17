package com.powertrading.approval.service.impl;

import cn.hutool.core.util.StrUtil;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.powertrading.approval.entity.SubscriptionApplication;
import com.powertrading.approval.mapper.SubscriptionApplicationMapper;
import com.powertrading.approval.service.ApprovalHistoryService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.stream.Collectors;

/**
 * 审批历史服务实现类
 *
 * @author PowerTrading
 * @since 2024-01-15
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class ApprovalHistoryServiceImpl implements ApprovalHistoryService {

    private final SubscriptionApplicationMapper applicationMapper;

    @Override
    public IPage<SubscriptionApplication> getApprovalHistory(
            Integer pageNum,
            Integer pageSize,
            String userId,
            String processBy,
            String status,
            String startTime,
            String endTime) {
        
        log.info("查询审批历史，页码：{}，每页：{}，用户：{}，处理人：{}，状态：{}", 
                pageNum, pageSize, userId, processBy, status);

        Page<SubscriptionApplication> page = new Page<>(pageNum, pageSize);
        
        LocalDateTime start = null;
        LocalDateTime end = null;
        
        if (StrUtil.isNotBlank(startTime)) {
            start = LocalDateTime.parse(startTime, DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss"));
        }
        if (StrUtil.isNotBlank(endTime)) {
            end = LocalDateTime.parse(endTime, DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss"));
        }
        
        return applicationMapper.selectApplicationPage(page, userId, status, start, end);
    }

    @Override
    public IPage<SubscriptionApplication> getUserApprovalHistory(String userId, Integer pageNum, Integer pageSize) {
        log.info("查询用户审批历史，用户ID：{}，页码：{}，每页：{}", userId, pageNum, pageSize);

        Page<SubscriptionApplication> page = new Page<>(pageNum, pageSize);
        
        LambdaQueryWrapper<SubscriptionApplication> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(SubscriptionApplication::getUserId, userId)
                .orderByDesc(SubscriptionApplication::getSubmitTime);
        
        return applicationMapper.selectPage(page, wrapper);
    }

    @Override
    public IPage<SubscriptionApplication> getProcessorHistory(String processBy, Integer pageNum, Integer pageSize) {
        log.info("查询审批人处理历史，处理人：{}，页码：{}，每页：{}", processBy, pageNum, pageSize);

        Page<SubscriptionApplication> page = new Page<>(pageNum, pageSize);
        
        LambdaQueryWrapper<SubscriptionApplication> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(SubscriptionApplication::getProcessBy, processBy)
                .in(SubscriptionApplication::getStatus, "approved", "rejected")
                .orderByDesc(SubscriptionApplication::getProcessTime);
        
        return applicationMapper.selectPage(page, wrapper);
    }

    @Override
    public List<Map<String, Object>> getApplicationDetailHistory(String applicationId) {
        log.info("获取申请详细历史，申请ID：{}", applicationId);

        SubscriptionApplication application = applicationMapper.selectById(applicationId);
        if (application == null) {
            return Collections.emptyList();
        }

        List<Map<String, Object>> history = new ArrayList<>();
        
        // 提交记录
        Map<String, Object> submitRecord = new HashMap<>();
        submitRecord.put("action", "submit");
        submitRecord.put("actionName", "提交申请");
        submitRecord.put("operator", application.getUserId());
        submitRecord.put("operatorName", "申请用户");
        submitRecord.put("time", application.getSubmitTime());
        submitRecord.put("comment", application.getReason());
        submitRecord.put("status", "pending");
        history.add(submitRecord);
        
        // 处理记录
        if (application.getProcessTime() != null) {
            Map<String, Object> processRecord = new HashMap<>();
            processRecord.put("action", application.getStatus());
            processRecord.put("actionName", "approved".equals(application.getStatus()) ? "审批通过" : "审批拒绝");
            processRecord.put("operator", application.getProcessBy());
            processRecord.put("operatorName", "审批人员");
            processRecord.put("time", application.getProcessTime());
            processRecord.put("comment", application.getProcessComment());
            processRecord.put("status", application.getStatus());
            history.add(processRecord);
        }
        
        // 按时间排序
        history.sort((a, b) -> {
            LocalDateTime timeA = (LocalDateTime) a.get("time");
            LocalDateTime timeB = (LocalDateTime) b.get("time");
            return timeA.compareTo(timeB);
        });
        
        return history;
    }

    @Override
    public String exportApprovalHistory(Map<String, Object> criteria) {
        log.info("导出审批历史，条件：{}", criteria);
        
        // 查询数据
        LambdaQueryWrapper<SubscriptionApplication> wrapper = buildQueryWrapper(criteria);
        List<SubscriptionApplication> applications = applicationMapper.selectList(wrapper);
        
        // 生成导出文件
        String fileName = "approval_history_" + System.currentTimeMillis() + ".csv";
        String filePath = "/tmp/" + fileName;
        
        // TODO: 实际的CSV文件生成逻辑
        log.info("导出文件生成：{}，记录数：{}", filePath, applications.size());
        
        return filePath;
    }

    @Override
    public Map<String, Object> getApprovalHistoryStatistics(String startTime, String endTime) {
        log.info("获取审批历史统计，时间范围：{} - {}", startTime, endTime);

        LocalDateTime start = null;
        LocalDateTime end = null;
        
        if (StrUtil.isNotBlank(startTime)) {
            start = LocalDateTime.parse(startTime, DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss"));
        }
        if (StrUtil.isNotBlank(endTime)) {
            end = LocalDateTime.parse(endTime, DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss"));
        }
        
        return applicationMapper.selectApplicationStatistics(null, start, end);
    }

    @Override
    public List<Map<String, Object>> getApprovalCountByPeriod(String startTime, String endTime, String groupBy) {
        log.info("按时间段统计审批数量，时间范围：{} - {}，分组：{}", startTime, endTime, groupBy);

        LocalDateTime start = LocalDateTime.parse(startTime, DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss"));
        LocalDateTime end = LocalDateTime.parse(endTime, DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss"));
        
        LambdaQueryWrapper<SubscriptionApplication> wrapper = new LambdaQueryWrapper<>();
        wrapper.ge(SubscriptionApplication::getSubmitTime, start)
                .le(SubscriptionApplication::getSubmitTime, end)
                .orderByAsc(SubscriptionApplication::getSubmitTime);
        
        List<SubscriptionApplication> applications = applicationMapper.selectList(wrapper);
        
        // 按时间分组统计
        Map<String, Long> countMap = new HashMap<>();
        DateTimeFormatter formatter;
        
        switch (groupBy.toLowerCase()) {
            case "day":
                formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd");
                break;
            case "week":
                formatter = DateTimeFormatter.ofPattern("yyyy-'W'ww");
                break;
            case "month":
                formatter = DateTimeFormatter.ofPattern("yyyy-MM");
                break;
            default:
                formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd");
        }
        
        for (SubscriptionApplication app : applications) {
            String period = app.getSubmitTime().format(formatter);
            countMap.put(period, countMap.getOrDefault(period, 0L) + 1);
        }
        
        return countMap.entrySet().stream()
                .map(entry -> {
                    Map<String, Object> item = new HashMap<>();
                    item.put("period", entry.getKey());
                    item.put("count", entry.getValue());
                    return item;
                })
                .sorted((a, b) -> ((String) a.get("period")).compareTo((String) b.get("period")))
                .collect(Collectors.toList());
    }

    @Override
    public List<Map<String, Object>> getProcessorWorkloadStatistics(String startTime, String endTime) {
        log.info("获取审批人员工作量统计，时间范围：{} - {}", startTime, endTime);

        LocalDateTime start = LocalDateTime.parse(startTime, DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss"));
        LocalDateTime end = LocalDateTime.parse(endTime, DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss"));
        
        LambdaQueryWrapper<SubscriptionApplication> wrapper = new LambdaQueryWrapper<>();
        wrapper.isNotNull(SubscriptionApplication::getProcessBy)
                .ge(SubscriptionApplication::getProcessTime, start)
                .le(SubscriptionApplication::getProcessTime, end);
        
        List<SubscriptionApplication> applications = applicationMapper.selectList(wrapper);
        
        // 按处理人分组统计
        Map<String, Map<String, Long>> workloadMap = new HashMap<>();
        
        for (SubscriptionApplication app : applications) {
            String processBy = app.getProcessBy();
            workloadMap.computeIfAbsent(processBy, k -> new HashMap<>());
            
            Map<String, Long> stats = workloadMap.get(processBy);
            stats.put("total", stats.getOrDefault("total", 0L) + 1);
            stats.put(app.getStatus(), stats.getOrDefault(app.getStatus(), 0L) + 1);
        }
        
        return workloadMap.entrySet().stream()
                .map(entry -> {
                    Map<String, Object> item = new HashMap<>();
                    item.put("processBy", entry.getKey());
                    item.putAll(entry.getValue());
                    return item;
                })
                .sorted((a, b) -> Long.compare(
                        (Long) b.getOrDefault("total", 0L),
                        (Long) a.getOrDefault("total", 0L)
                ))
                .collect(Collectors.toList());
    }

    @Override
    public List<Map<String, Object>> getRecentApprovalActivities(Integer limit) {
        log.info("查询最近的审批活动，限制数量：{}", limit);

        Page<SubscriptionApplication> page = new Page<>(1, limit);
        
        LambdaQueryWrapper<SubscriptionApplication> wrapper = new LambdaQueryWrapper<>();
        wrapper.isNotNull(SubscriptionApplication::getProcessTime)
                .orderByDesc(SubscriptionApplication::getProcessTime);
        
        IPage<SubscriptionApplication> result = applicationMapper.selectPage(page, wrapper);
        
        return result.getRecords().stream()
                .map(app -> {
                    Map<String, Object> activity = new HashMap<>();
                    activity.put("applicationId", app.getId());
                    activity.put("userId", app.getUserId());
                    activity.put("processBy", app.getProcessBy());
                    activity.put("action", app.getStatus());
                    activity.put("actionName", "approved".equals(app.getStatus()) ? "审批通过" : "审批拒绝");
                    activity.put("processTime", app.getProcessTime());
                    activity.put("interfaceCount", app.getInterfaceIds().size());
                    activity.put("comment", app.getProcessComment());
                    return activity;
                })
                .collect(Collectors.toList());
    }

    @Override
    public IPage<SubscriptionApplication> searchApprovalHistory(String keyword, Integer pageNum, Integer pageSize) {
        log.info("搜索审批历史，关键词：{}，页码：{}，每页：{}", keyword, pageNum, pageSize);

        Page<SubscriptionApplication> page = new Page<>(pageNum, pageSize);
        
        LambdaQueryWrapper<SubscriptionApplication> wrapper = new LambdaQueryWrapper<>();
        if (StrUtil.isNotBlank(keyword)) {
            wrapper.and(w -> w
                    .like(SubscriptionApplication::getReason, keyword)
                    .or().like(SubscriptionApplication::getBusinessScenario, keyword)
                    .or().like(SubscriptionApplication::getProcessComment, keyword)
                    .or().eq(SubscriptionApplication::getUserId, keyword)
                    .or().eq(SubscriptionApplication::getProcessBy, keyword)
            );
        }
        wrapper.orderByDesc(SubscriptionApplication::getSubmitTime);
        
        return applicationMapper.selectPage(page, wrapper);
    }

    @Override
    public Map<String, Object> getApprovalTrendAnalysis(Integer days) {
        log.info("获取审批趋势分析，分析天数：{}", days);

        LocalDateTime endTime = LocalDateTime.now();
        LocalDateTime startTime = endTime.minusDays(days);
        
        LambdaQueryWrapper<SubscriptionApplication> wrapper = new LambdaQueryWrapper<>();
        wrapper.ge(SubscriptionApplication::getSubmitTime, startTime)
                .le(SubscriptionApplication::getSubmitTime, endTime);
        
        List<SubscriptionApplication> applications = applicationMapper.selectList(wrapper);
        
        Map<String, Object> trend = new HashMap<>();
        
        // 总体统计
        long totalSubmitted = applications.size();
        long totalApproved = applications.stream().filter(app -> "approved".equals(app.getStatus())).count();
        long totalRejected = applications.stream().filter(app -> "rejected".equals(app.getStatus())).count();
        long totalPending = applications.stream().filter(app -> "pending".equals(app.getStatus())).count();
        
        trend.put("totalSubmitted", totalSubmitted);
        trend.put("totalApproved", totalApproved);
        trend.put("totalRejected", totalRejected);
        trend.put("totalPending", totalPending);
        trend.put("approvalRate", totalSubmitted > 0 ? (double) totalApproved / totalSubmitted : 0.0);
        
        // 每日趋势
        Map<String, Long> dailySubmissions = new HashMap<>();
        Map<String, Long> dailyApprovals = new HashMap<>();
        
        DateTimeFormatter dayFormatter = DateTimeFormatter.ofPattern("yyyy-MM-dd");
        
        for (SubscriptionApplication app : applications) {
            String day = app.getSubmitTime().format(dayFormatter);
            dailySubmissions.put(day, dailySubmissions.getOrDefault(day, 0L) + 1);
            
            if ("approved".equals(app.getStatus()) && app.getProcessTime() != null) {
                String processDay = app.getProcessTime().format(dayFormatter);
                dailyApprovals.put(processDay, dailyApprovals.getOrDefault(processDay, 0L) + 1);
            }
        }
        
        trend.put("dailySubmissions", dailySubmissions);
        trend.put("dailyApprovals", dailyApprovals);
        trend.put("analysisStartTime", startTime);
        trend.put("analysisEndTime", endTime);
        trend.put("analysisDays", days);
        
        return trend;
    }

    /**
     * 构建查询条件
     */
    private LambdaQueryWrapper<SubscriptionApplication> buildQueryWrapper(Map<String, Object> criteria) {
        LambdaQueryWrapper<SubscriptionApplication> wrapper = new LambdaQueryWrapper<>();
        
        if (criteria.containsKey("userId")) {
            wrapper.eq(SubscriptionApplication::getUserId, criteria.get("userId"));
        }
        
        if (criteria.containsKey("processBy")) {
            wrapper.eq(SubscriptionApplication::getProcessBy, criteria.get("processBy"));
        }
        
        if (criteria.containsKey("status")) {
            wrapper.eq(SubscriptionApplication::getStatus, criteria.get("status"));
        }
        
        if (criteria.containsKey("startTime")) {
            LocalDateTime start = LocalDateTime.parse(
                    criteria.get("startTime").toString(),
                    DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss")
            );
            wrapper.ge(SubscriptionApplication::getSubmitTime, start);
        }
        
        if (criteria.containsKey("endTime")) {
            LocalDateTime end = LocalDateTime.parse(
                    criteria.get("endTime").toString(),
                    DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss")
            );
            wrapper.le(SubscriptionApplication::getSubmitTime, end);
        }
        
        wrapper.orderByDesc(SubscriptionApplication::getSubmitTime);
        
        return wrapper;
    }
}