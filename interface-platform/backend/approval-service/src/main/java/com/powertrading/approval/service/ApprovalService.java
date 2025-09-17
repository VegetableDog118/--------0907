package com.powertrading.approval.service;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.powertrading.approval.dto.ApplicationQueryRequest;
import com.powertrading.approval.dto.ProcessApplicationRequest;
import com.powertrading.approval.dto.SubmitApplicationRequest;
import com.powertrading.approval.entity.SubscriptionApplication;
import com.powertrading.approval.entity.UserInterfaceSubscription;

import java.util.List;
import java.util.Map;

/**
 * 审批服务接口
 *
 * @author PowerTrading
 * @since 2024-01-15
 */
public interface ApprovalService {

    /**
     * 提交订阅申请
     *
     * @param userId 用户ID
     * @param request 申请请求
     * @return 申请ID
     */
    String submitApplication(String userId, SubmitApplicationRequest request);

    /**
     * 批量提交订阅申请
     *
     * @param userId 用户ID
     * @param requests 申请请求列表
     * @return 申请ID列表
     */
    List<String> batchSubmitApplications(String userId, List<SubmitApplicationRequest> requests);

    /**
     * 处理申请（审批）
     *
     * @param processBy 处理人
     * @param request 处理请求
     * @return 处理结果
     */
    boolean processApplication(String processBy, ProcessApplicationRequest request);

    /**
     * 批量处理申请
     *
     * @param processBy 处理人
     * @param requests 处理请求列表
     * @return 处理结果
     */
    boolean batchProcessApplications(String processBy, List<ProcessApplicationRequest> requests);

    /**
     * 分页查询申请列表
     *
     * @param request 查询请求
     * @return 分页结果
     */
    IPage<SubscriptionApplication> getApplicationPage(ApplicationQueryRequest request);

    /**
     * 根据ID查询申请详情
     *
     * @param applicationId 申请ID
     * @return 申请详情
     */
    SubscriptionApplication getApplicationById(String applicationId);

    /**
     * 查询用户的申请历史
     *
     * @param userId 用户ID
     * @param pageNum 页码
     * @param pageSize 每页大小
     * @return 分页结果
     */
    IPage<SubscriptionApplication> getUserApplicationHistory(String userId, Integer pageNum, Integer pageSize);

    /**
     * 查询用户的订阅列表
     *
     * @param userId 用户ID
     * @param pageNum 页码
     * @param pageSize 每页大小
     * @return 分页结果
     */
    IPage<UserInterfaceSubscription> getUserSubscriptions(String userId, Integer pageNum, Integer pageSize);

    /**
     * 获取申请统计信息
     *
     * @param startTime 开始时间（可选）
     * @param endTime 结束时间（可选）
     * @return 统计信息
     */
    Map<String, Object> getApplicationStatistics(String startTime, String endTime);

    /**
     * 取消申请
     *
     * @param userId 用户ID
     * @param applicationId 申请ID
     * @return 取消结果
     */
    boolean cancelApplication(String userId, String applicationId);

    /**
     * 检查用户是否有权限访问接口
     *
     * @param userId 用户ID
     * @param interfaceId 接口ID
     * @return 是否有权限
     */
    boolean hasInterfacePermission(String userId, String interfaceId);

    /**
     * 更新接口调用次数
     *
     * @param userId 用户ID
     * @param interfaceId 接口ID
     * @param callCount 调用次数增量
     * @return 更新结果
     */
    boolean updateCallCount(String userId, String interfaceId, Integer callCount);
}