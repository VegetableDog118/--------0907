package com.powertrading.approval.feign;

import com.powertrading.approval.common.Result;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;

import java.util.Map;

/**
 * 通知服务Feign客户端
 *
 * @author PowerTrading
 * @since 2024-01-15
 */
@FeignClient(name = "notification-service", path = "/notification")
public interface NotificationServiceClient {

    /**
     * 发送审批通知
     *
     * @param notificationRequest 通知请求
     * @return 发送结果
     */
    @PostMapping("/send/approval")
    Result<Boolean> sendApprovalNotification(@RequestBody Map<String, Object> notificationRequest);

    /**
     * 发送申请提交通知
     *
     * @param notificationRequest 通知请求
     * @return 发送结果
     */
    @PostMapping("/send/application")
    Result<Boolean> sendApplicationNotification(@RequestBody Map<String, Object> notificationRequest);

    /**
     * 批量发送通知
     *
     * @param notificationRequests 通知请求列表
     * @return 发送结果
     */
    @PostMapping("/send/batch")
    Result<Boolean> batchSendNotifications(@RequestBody java.util.List<Map<String, Object>> notificationRequests);
}