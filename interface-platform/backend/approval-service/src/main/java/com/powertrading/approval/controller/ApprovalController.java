package com.powertrading.approval.controller;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.powertrading.approval.common.PageResult;
import com.powertrading.approval.common.Result;
import com.powertrading.approval.dto.ApplicationQueryRequest;
import com.powertrading.approval.dto.ProcessApplicationRequest;
import com.powertrading.approval.dto.SubmitApplicationRequest;
import com.powertrading.approval.entity.SubscriptionApplication;
import com.powertrading.approval.entity.UserInterfaceSubscription;
import com.powertrading.approval.feign.NotificationServiceClient;
import com.powertrading.approval.feign.UserServiceClient;
import com.powertrading.approval.service.ApprovalService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import javax.validation.Valid;
import javax.validation.constraints.NotBlank;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * 审批管理控制器
 *
 * @author PowerTrading
 * @since 2024-01-15
 */
@Slf4j
@RestController
@RequestMapping("/api/approval")
@RequiredArgsConstructor
@Validated
@Tag(name = "审批管理", description = "订阅申请审批相关接口")
public class ApprovalController {

    private final ApprovalService approvalService;
    private final UserServiceClient userServiceClient;
    private final NotificationServiceClient notificationServiceClient;

    @Operation(summary = "提交订阅申请", description = "用户提交接口订阅申请")
    @PostMapping("/applications")
    public Result<String> submitApplication(
            @Parameter(description = "用户ID", required = true) @RequestHeader("X-User-Id") String userId,
            @Valid @RequestBody SubmitApplicationRequest request
    ) {
        try {
            // 验证用户是否存在
            com.powertrading.approval.dto.ApiResponse<Boolean> userExistsResult = userServiceClient.userExists(userId);
            if (!userExistsResult.isSuccess() || !Boolean.TRUE.equals(userExistsResult.getData())) {
                return Result.badRequest("用户不存在");
            }

            String applicationId = approvalService.submitApplication(userId, request);

            // 发送申请提交通知
            try {
                Map<String, Object> notificationRequest = new HashMap<>();
                notificationRequest.put("userId", userId);
                notificationRequest.put("applicationId", applicationId);
                notificationRequest.put("type", "application_submitted");
                notificationRequest.put("interfaceCount", request.getInterfaceIds().size());
                notificationServiceClient.sendApplicationNotification(notificationRequest);
            } catch (Exception e) {
                log.warn("发送申请提交通知失败：{}", e.getMessage());
            }

            return Result.success("申请提交成功", applicationId);
        } catch (Exception e) {
            log.error("提交申请失败：{}", e.getMessage(), e);
            return Result.error(e.getMessage());
        }
    }

    @Operation(summary = "批量提交订阅申请", description = "用户批量提交接口订阅申请")
    @PostMapping("/applications/batch")
    public Result<List<String>> batchSubmitApplications(
            @Parameter(description = "用户ID", required = true) @RequestHeader("X-User-Id") String userId,
            @Valid @RequestBody List<SubmitApplicationRequest> requests
    ) {
        try {
            // 验证用户是否存在
            com.powertrading.approval.dto.ApiResponse<Boolean> userExistsResult = userServiceClient.userExists(userId);
            if (!userExistsResult.isSuccess() || !Boolean.TRUE.equals(userExistsResult.getData())) {
                return Result.badRequest("用户不存在");
            }

            List<String> applicationIds = approvalService.batchSubmitApplications(userId, requests);
            return Result.success("批量申请提交成功", applicationIds);
        } catch (Exception e) {
            log.error("批量提交申请失败：{}", e.getMessage(), e);
            return Result.error(e.getMessage());
        }
    }

    @Operation(summary = "处理申请", description = "审批人员处理订阅申请（通过/拒绝）")
    @PostMapping("/applications/process")
    public Result<Boolean> processApplication(
            @Parameter(description = "处理人ID", required = true) @RequestHeader("X-User-Id") String processBy,
            @Valid @RequestBody ProcessApplicationRequest request
    ) {
        try {
            // 验证处理人权限
            com.powertrading.approval.dto.ApiResponse<String> roleResult = userServiceClient.getUserRole(processBy);
            if (!roleResult.isSuccess()) {
                return Result.forbidden("无权限进行审批操作");
            }
            String role = roleResult.getData();
            if (!"settlement".equals(role) && !"admin".equals(role) && !"ADMIN".equals(role)) {
                return Result.forbidden("无权限进行审批操作");
            }

            boolean success = approvalService.processApplication(processBy, request);
            
            if (success) {
                // 如果是通过申请，更新用户权限
                if ("approved".equals(request.getAction())) {
                    updateUserPermissions(request.getApplicationIds());
                }

                // 发送审批通知
                sendApprovalNotifications(request.getApplicationIds(), request.getAction(), processBy);
            }

            return Result.success("处理成功", success);
        } catch (Exception e) {
            log.error("处理申请失败：{}", e.getMessage(), e);
            return Result.error(e.getMessage());
        }
    }

    @Operation(summary = "批量处理申请", description = "批量处理订阅申请")
    @PostMapping("/applications/process/batch")
    public Result<Boolean> batchProcessApplications(
            @Parameter(description = "处理人ID", required = true) @RequestHeader("X-User-Id") String processBy,
            @Valid @RequestBody List<ProcessApplicationRequest> requests
    ) {
        try {
            // 验证处理人权限
            com.powertrading.approval.dto.ApiResponse<String> roleResult = userServiceClient.getUserRole(processBy);
            if (!roleResult.isSuccess()) {
                return Result.forbidden("无权限进行审批操作");
            }
            String role = roleResult.getData();
            if (!"settlement".equals(role) && !"admin".equals(role) && !"ADMIN".equals(role)) {
                return Result.forbidden("无权限进行审批操作");
            }

            boolean success = approvalService.batchProcessApplications(processBy, requests);
            return Result.success("批量处理成功", success);
        } catch (Exception e) {
            log.error("批量处理申请失败：{}", e.getMessage(), e);
            return Result.error(e.getMessage());
        }
    }

    @Operation(summary = "查询申请列表", description = "分页查询订阅申请列表")
    @GetMapping("/applications")
    public Result<PageResult<SubscriptionApplication>> getApplications(
            @Valid ApplicationQueryRequest request
    ) {
        try {
            IPage<SubscriptionApplication> page = approvalService.getApplicationPage(request);
            return Result.success(PageResult.of(page));
        } catch (Exception e) {
            log.error("查询申请列表失败：{}", e.getMessage(), e);
            return Result.error(e.getMessage());
        }
    }

    @Operation(summary = "查询申请详情", description = "根据ID查询申请详情")
    @GetMapping("/applications/{applicationId}")
    public Result<SubscriptionApplication> getApplicationById(
            @Parameter(description = "申请ID", required = true) @PathVariable String applicationId
    ) {
        try {
            SubscriptionApplication application = approvalService.getApplicationById(applicationId);
            if (application == null) {
                return Result.notFound("申请不存在");
            }
            return Result.success(application);
        } catch (Exception e) {
            log.error("查询申请详情失败：{}", e.getMessage(), e);
            return Result.error(e.getMessage());
        }
    }

    @Operation(summary = "查询用户申请历史", description = "查询用户的申请历史记录")
    @GetMapping("/applications/history")
    public Result<PageResult<SubscriptionApplication>> getUserApplicationHistory(
            @Parameter(description = "用户ID", required = true) @RequestHeader("X-User-Id") String userId,
            @Parameter(description = "页码") @RequestParam(defaultValue = "1") Integer pageNum,
            @Parameter(description = "每页大小") @RequestParam(defaultValue = "10") Integer pageSize
    ) {
        try {
            IPage<SubscriptionApplication> page = approvalService.getUserApplicationHistory(userId, pageNum, pageSize);
            return Result.success(PageResult.of(page));
        } catch (Exception e) {
            log.error("查询用户申请历史失败：{}", e.getMessage(), e);
            return Result.error(e.getMessage());
        }
    }

    @Operation(summary = "查询用户订阅列表", description = "查询用户的接口订阅列表")
    @GetMapping("/subscriptions")
    public Result<PageResult<UserInterfaceSubscription>> getUserSubscriptions(
            @Parameter(description = "用户ID", required = true) @RequestHeader("X-User-Id") String userId,
            @Parameter(description = "页码") @RequestParam(defaultValue = "1") Integer pageNum,
            @Parameter(description = "每页大小") @RequestParam(defaultValue = "10") Integer pageSize
    ) {
        try {
            IPage<UserInterfaceSubscription> page = approvalService.getUserSubscriptions(userId, pageNum, pageSize);
            return Result.success(PageResult.of(page));
        } catch (Exception e) {
            log.error("查询用户订阅列表失败：{}", e.getMessage(), e);
            return Result.error(e.getMessage());
        }
    }

    @Operation(summary = "取消申请", description = "用户取消待审批的申请")
    @DeleteMapping("/applications/{applicationId}")
    public Result<Boolean> cancelApplication(
            @Parameter(description = "用户ID", required = true) @RequestHeader("X-User-Id") String userId,
            @Parameter(description = "申请ID", required = true) @PathVariable String applicationId
    ) {
        try {
            boolean success = approvalService.cancelApplication(userId, applicationId);
            return Result.success("取消成功", success);
        } catch (Exception e) {
            log.error("取消申请失败：{}", e.getMessage(), e);
            return Result.error(e.getMessage());
        }
    }

    @Operation(summary = "检查接口权限", description = "检查用户是否有权限访问指定接口")
    @GetMapping("/permissions/{interfaceId}")
    public Result<Boolean> checkInterfacePermission(
            @Parameter(description = "用户ID", required = true) @RequestHeader("X-User-Id") String userId,
            @Parameter(description = "接口ID", required = true) @PathVariable String interfaceId
    ) {
        try {
            boolean hasPermission = approvalService.hasInterfacePermission(userId, interfaceId);
            return Result.success(hasPermission);
        } catch (Exception e) {
            log.error("检查接口权限失败：{}", e.getMessage(), e);
            return Result.error(e.getMessage());
        }
    }

    @Operation(summary = "获取申请统计", description = "获取申请统计信息")
    @GetMapping("/statistics")
    public Result<Map<String, Object>> getApplicationStatistics(
            @Parameter(description = "开始时间") @RequestParam(required = false) String startTime,
            @Parameter(description = "结束时间") @RequestParam(required = false) String endTime
    ) {
        try {
            Map<String, Object> statistics = approvalService.getApplicationStatistics(startTime, endTime);
            return Result.success(statistics);
        } catch (Exception e) {
            log.error("获取申请统计失败：{}", e.getMessage(), e);
            return Result.error(e.getMessage());
        }
    }

    /**
     * 更新用户权限
     */
    private void updateUserPermissions(List<String> applicationIds) {
        try {
            for (String applicationId : applicationIds) {
                SubscriptionApplication application = approvalService.getApplicationById(applicationId);
                if (application != null) {
                    userServiceClient.updateUserInterfacePermissions(
                            application.getUserId(),
                            application.getInterfaceIds()
                    );
                }
            }
        } catch (Exception e) {
            log.warn("更新用户权限失败：{}", e.getMessage());
        }
    }

    /**
     * 发送审批通知
     */
    private void sendApprovalNotifications(List<String> applicationIds, String action, String processBy) {
        try {
            for (String applicationId : applicationIds) {
                SubscriptionApplication application = approvalService.getApplicationById(applicationId);
                if (application != null) {
                    Map<String, Object> notificationRequest = new HashMap<>();
                    notificationRequest.put("userId", application.getUserId());
                    notificationRequest.put("applicationId", applicationId);
                    notificationRequest.put("action", action);
                    notificationRequest.put("processBy", processBy);
                    notificationRequest.put("type", "approval_result");
                    notificationServiceClient.sendApprovalNotification(notificationRequest);
                }
            }
        } catch (Exception e) {
            log.warn("发送审批通知失败：{}", e.getMessage());
        }
    }
}