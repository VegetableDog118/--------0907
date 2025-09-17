package com.powertrading.approval.service.impl;

import cn.hutool.core.util.IdUtil;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.powertrading.approval.dto.ProcessApplicationRequest;
import com.powertrading.approval.entity.SubscriptionApplication;
import com.powertrading.approval.mapper.SubscriptionApplicationMapper;
import com.powertrading.approval.service.ApprovalWorkflowService;
import com.powertrading.approval.service.BatchApprovalService;
import com.powertrading.approval.service.UserPermissionIntegrationService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;

/**
 * 批量审批服务实现类
 *
 * @author PowerTrading
 * @since 2024-01-15
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class BatchApprovalServiceImpl implements BatchApprovalService {

    private final SubscriptionApplicationMapper applicationMapper;
    private final ApprovalWorkflowService approvalWorkflowService;
    private final UserPermissionIntegrationService userPermissionIntegrationService;
    
    // 异步任务状态缓存
    private final Map<String, Map<String, Object>> asyncTaskCache = new ConcurrentHashMap<>();

    @Override
    @Transactional(rollbackFor = Exception.class)
    public Map<String, Object> batchApproveApplications(String processBy, List<String> applicationIds, String action, String comment) {
        log.info("批量审批申请，处理人：{}，申请数量：{}，操作：{}", processBy, applicationIds.size(), action);

        Map<String, Object> result = new HashMap<>();
        result.put("processBy", processBy);
        result.put("action", action);
        result.put("totalCount", applicationIds.size());
        result.put("processTime", LocalDateTime.now());

        try {
            // 验证审批权限
            if (!approvalWorkflowService.validateApprovalPermission(processBy)) {
                result.put("success", false);
                result.put("message", "无审批权限");
                return result;
            }

            // 筛选可以审批的申请
            List<String> validApplicationIds = applicationIds.stream()
                    .filter(approvalWorkflowService::canApprove)
                    .collect(Collectors.toList());

            if (validApplicationIds.isEmpty()) {
                result.put("success", false);
                result.put("message", "没有可审批的申请");
                result.put("validCount", 0);
                return result;
            }

            // 执行批量审批
            ProcessApplicationRequest request = new ProcessApplicationRequest();
            request.setApplicationIds(validApplicationIds);
            request.setAction(action);
            request.setComment(comment);

            boolean success = approvalWorkflowService.executeFirstLevelApproval(processBy, request);
            
            result.put("success", success);
            result.put("validCount", validApplicationIds.size());
            result.put("invalidCount", applicationIds.size() - validApplicationIds.size());
            result.put("processedIds", validApplicationIds);
            
            if (success) {
                result.put("message", "批量审批成功");
                log.info("批量审批成功，处理人：{}，成功数量：{}", processBy, validApplicationIds.size());
            } else {
                result.put("message", "批量审批失败");
                log.warn("批量审批失败，处理人：{}", processBy);
            }

        } catch (Exception e) {
            log.error("批量审批异常：{}", e.getMessage(), e);
            result.put("success", false);
            result.put("message", "批量审批异常：" + e.getMessage());
        }

        return result;
    }

    @Override
    public Map<String, Object> smartBatchApproval(String processBy, Map<String, Object> criteria) {
        log.info("智能批量审批，处理人：{}，条件：{}", processBy, criteria);

        // 根据条件查询符合的申请
        List<String> applicationIds = getBatchApprovalPreview(criteria);
        
        if (applicationIds.isEmpty()) {
            Map<String, Object> result = new HashMap<>();
            result.put("success", false);
            result.put("message", "没有符合条件的申请");
            return result;
        }

        // 智能判断审批动作
        String action = determineSmartAction(applicationIds);
        String comment = "智能批量审批 - " + criteria.toString();

        return batchApproveApplications(processBy, applicationIds, action, comment);
    }

    @Override
    public Map<String, Object> batchApprovalByCondition(
            String processBy,
            String userId,
            Integer maxInterfaceCount,
            Integer maxEstimatedCalls,
            String action,
            String comment) {
        
        log.info("按条件批量审批，处理人：{}，用户ID：{}，最大接口数：{}，最大调用数：{}", 
                processBy, userId, maxInterfaceCount, maxEstimatedCalls);

        // 构建查询条件
        LambdaQueryWrapper<SubscriptionApplication> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(SubscriptionApplication::getStatus, "pending");
        
        if (userId != null && !userId.isEmpty()) {
            wrapper.eq(SubscriptionApplication::getUserId, userId);
        }
        
        List<SubscriptionApplication> applications = applicationMapper.selectList(wrapper);
        
        // 根据条件筛选
        List<String> filteredIds = applications.stream()
                .filter(app -> {
                    boolean matchInterface = maxInterfaceCount == null || 
                            app.getInterfaceIds().size() <= maxInterfaceCount;
                    boolean matchCalls = maxEstimatedCalls == null || 
                            app.getEstimatedCalls() == null || 
                            app.getEstimatedCalls() <= maxEstimatedCalls;
                    return matchInterface && matchCalls;
                })
                .map(SubscriptionApplication::getId)
                .collect(Collectors.toList());

        return batchApproveApplications(processBy, filteredIds, action, comment);
    }

    @Override
    public List<String> getBatchApprovalPreview(Map<String, Object> criteria) {
        log.debug("获取批量审批预览，条件：{}", criteria);

        LambdaQueryWrapper<SubscriptionApplication> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(SubscriptionApplication::getStatus, "pending");

        // 应用筛选条件
        if (criteria.containsKey("userId")) {
            wrapper.eq(SubscriptionApplication::getUserId, criteria.get("userId"));
        }
        
        if (criteria.containsKey("startTime")) {
            LocalDateTime startTime = LocalDateTime.parse(
                    criteria.get("startTime").toString(),
                    DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss")
            );
            wrapper.ge(SubscriptionApplication::getSubmitTime, startTime);
        }
        
        if (criteria.containsKey("endTime")) {
            LocalDateTime endTime = LocalDateTime.parse(
                    criteria.get("endTime").toString(),
                    DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss")
            );
            wrapper.le(SubscriptionApplication::getSubmitTime, endTime);
        }

        List<SubscriptionApplication> applications = applicationMapper.selectList(wrapper);
        
        // 进一步筛选
        return applications.stream()
                .filter(app -> matchesCriteria(app, criteria))
                .map(SubscriptionApplication::getId)
                .collect(Collectors.toList());
    }

    @Override
    @Async
    public String asyncBatchApproval(String processBy, ProcessApplicationRequest request) {
        String taskId = IdUtil.getSnowflakeNextIdStr();
        log.info("异步批量审批开始，任务ID：{}，处理人：{}", taskId, processBy);

        // 初始化任务状态
        Map<String, Object> taskStatus = new HashMap<>();
        taskStatus.put("taskId", taskId);
        taskStatus.put("status", "RUNNING");
        taskStatus.put("processBy", processBy);
        taskStatus.put("startTime", LocalDateTime.now());
        taskStatus.put("totalCount", request.getApplicationIds().size());
        taskStatus.put("processedCount", 0);
        
        asyncTaskCache.put(taskId, taskStatus);

        // 异步执行批量审批
        CompletableFuture.runAsync(() -> {
            try {
                Map<String, Object> result = batchApproveApplications(
                        processBy,
                        request.getApplicationIds(),
                        request.getAction(),
                        request.getComment()
                );
                
                // 更新任务状态
                taskStatus.put("status", "COMPLETED");
                taskStatus.put("endTime", LocalDateTime.now());
                taskStatus.put("result", result);
                taskStatus.put("success", result.get("success"));
                taskStatus.put("processedCount", result.get("validCount"));
                
            } catch (Exception e) {
                log.error("异步批量审批失败，任务ID：{}，错误：{}", taskId, e.getMessage(), e);
                taskStatus.put("status", "FAILED");
                taskStatus.put("endTime", LocalDateTime.now());
                taskStatus.put("error", e.getMessage());
            }
        });

        return taskId;
    }

    @Override
    public Map<String, Object> getAsyncBatchApprovalStatus(String taskId) {
        Map<String, Object> status = asyncTaskCache.get(taskId);
        if (status == null) {
            Map<String, Object> notFound = new HashMap<>();
            notFound.put("taskId", taskId);
            notFound.put("status", "NOT_FOUND");
            notFound.put("message", "任务不存在或已过期");
            return notFound;
        }
        return new HashMap<>(status);
    }

    @Override
    public Map<String, Object> getBatchApprovalStatistics(String processBy, String startTime, String endTime) {
        log.info("获取批量审批统计，处理人：{}，时间范围：{} - {}", processBy, startTime, endTime);

        LocalDateTime start = null;
        LocalDateTime end = null;
        
        if (startTime != null && !startTime.isEmpty()) {
            start = LocalDateTime.parse(startTime, DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss"));
        }
        if (endTime != null && !endTime.isEmpty()) {
            end = LocalDateTime.parse(endTime, DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss"));
        }

        LambdaQueryWrapper<SubscriptionApplication> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(SubscriptionApplication::getProcessBy, processBy)
                .in(SubscriptionApplication::getStatus, "approved", "rejected");
        
        if (start != null) {
            wrapper.ge(SubscriptionApplication::getProcessTime, start);
        }
        if (end != null) {
            wrapper.le(SubscriptionApplication::getProcessTime, end);
        }

        List<SubscriptionApplication> applications = applicationMapper.selectList(wrapper);
        
        Map<String, Object> statistics = new HashMap<>();
        statistics.put("processBy", processBy);
        statistics.put("totalProcessed", applications.size());
        statistics.put("approved", applications.stream().filter(app -> "approved".equals(app.getStatus())).count());
        statistics.put("rejected", applications.stream().filter(app -> "rejected".equals(app.getStatus())).count());
        statistics.put("startTime", startTime);
        statistics.put("endTime", endTime);
        
        return statistics;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public Map<String, Object> revokeBatchApproval(String processBy, List<String> applicationIds, String reason) {
        log.info("撤销批量审批，处理人：{}，申请数量：{}", processBy, applicationIds.size());

        Map<String, Object> result = new HashMap<>();
        result.put("processBy", processBy);
        result.put("totalCount", applicationIds.size());
        result.put("revokeTime", LocalDateTime.now());

        try {
            int successCount = 0;
            List<String> failedIds = new ArrayList<>();
            
            for (String applicationId : applicationIds) {
                try {
                    if (approvalWorkflowService.withdrawApproval(processBy, applicationId, reason)) {
                        successCount++;
                    } else {
                        failedIds.add(applicationId);
                    }
                } catch (Exception e) {
                    log.warn("撤销申请失败，申请ID：{}，错误：{}", applicationId, e.getMessage());
                    failedIds.add(applicationId);
                }
            }
            
            result.put("success", successCount > 0);
            result.put("successCount", successCount);
            result.put("failedCount", failedIds.size());
            result.put("failedIds", failedIds);
            result.put("message", String.format("撤销完成，成功：%d，失败：%d", successCount, failedIds.size()));
            
        } catch (Exception e) {
            log.error("批量撤销审批异常：{}", e.getMessage(), e);
            result.put("success", false);
            result.put("message", "批量撤销异常：" + e.getMessage());
        }

        return result;
    }

    @Override
    public String exportBatchApprovalResult(String taskId) {
        log.info("导出批量审批结果，任务ID：{}", taskId);
        
        Map<String, Object> taskStatus = asyncTaskCache.get(taskId);
        if (taskStatus == null) {
            throw new RuntimeException("任务不存在或已过期");
        }
        
        // 这里可以实现导出逻辑，生成Excel或CSV文件
        // 简化实现，返回文件路径
        String filePath = "/tmp/batch_approval_result_" + taskId + ".csv";
        
        // TODO: 实际的文件生成逻辑
        
        return filePath;
    }

    /**
     * 智能判断审批动作
     */
    private String determineSmartAction(List<String> applicationIds) {
        // 简单的智能规则：如果申请的接口数量都不超过3个，自动通过
        List<SubscriptionApplication> applications = applicationMapper.selectBatchIds(applicationIds);
        
        boolean allSimple = applications.stream()
                .allMatch(app -> app.getInterfaceIds().size() <= 3 && 
                         (app.getEstimatedCalls() == null || app.getEstimatedCalls() <= 500));
        
        return allSimple ? "approved" : "rejected";
    }

    /**
     * 检查申请是否匹配条件
     */
    private boolean matchesCriteria(SubscriptionApplication application, Map<String, Object> criteria) {
        if (criteria.containsKey("maxInterfaceCount")) {
            Integer maxCount = (Integer) criteria.get("maxInterfaceCount");
            if (application.getInterfaceIds().size() > maxCount) {
                return false;
            }
        }
        
        if (criteria.containsKey("maxEstimatedCalls")) {
            Integer maxCalls = (Integer) criteria.get("maxEstimatedCalls");
            if (application.getEstimatedCalls() != null && application.getEstimatedCalls() > maxCalls) {
                return false;
            }
        }
        
        return true;
    }
}