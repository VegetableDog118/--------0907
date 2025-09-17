package com.powertrading.approval.service.impl;

import cn.hutool.core.util.IdUtil;
import cn.hutool.core.util.StrUtil;
import com.alibaba.fastjson2.JSON;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.powertrading.approval.dto.ApplicationQueryRequest;
import com.powertrading.approval.dto.ProcessApplicationRequest;
import com.powertrading.approval.dto.SubmitApplicationRequest;
import com.powertrading.approval.entity.SubscriptionApplication;
import com.powertrading.approval.entity.UserInterfaceSubscription;
import com.powertrading.approval.mapper.SubscriptionApplicationMapper;
import com.powertrading.approval.mapper.UserInterfaceSubscriptionMapper;
import com.powertrading.approval.service.ApprovalService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * 审批服务实现类
 *
 * @author PowerTrading
 * @since 2024-01-15
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class ApprovalServiceImpl implements ApprovalService {

    private final SubscriptionApplicationMapper applicationMapper;
    private final UserInterfaceSubscriptionMapper subscriptionMapper;

    @Override
    @Transactional(rollbackFor = Exception.class)
    public String submitApplication(String userId, SubmitApplicationRequest request) {
        log.info("用户{}提交订阅申请，接口数量：{}", userId, request.getInterfaceIds().size());

        // 检查是否已存在相同的申请
        String interfaceIdsJson = JSON.toJSONString(request.getInterfaceIds());
        List<SubscriptionApplication> existingApplications = applicationMapper.selectExistingApplications(userId, interfaceIdsJson);
        
        if (!existingApplications.isEmpty()) {
            throw new RuntimeException("存在重复的申请，请勿重复提交");
        }

        // 创建申请记录
        SubscriptionApplication application = new SubscriptionApplication();
        application.setId(IdUtil.getSnowflakeNextIdStr());
        application.setUserId(userId);
        application.setInterfaceIds(request.getInterfaceIds());
        application.setReason(request.getReason());
        application.setBusinessScenario(request.getBusinessScenario());
        application.setEstimatedCalls(request.getEstimatedCalls());
        application.setStatus(SubscriptionApplication.Status.PENDING.getCode());
        application.setSubmitTime(LocalDateTime.now());
        application.setCreateTime(LocalDateTime.now());
        application.setUpdateTime(LocalDateTime.now());

        int result = applicationMapper.insert(application);
        if (result <= 0) {
            throw new RuntimeException("提交申请失败");
        }

        log.info("用户{}成功提交申请，申请ID：{}", userId, application.getId());
        return application.getId();
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public List<String> batchSubmitApplications(String userId, List<SubmitApplicationRequest> requests) {
        log.info("用户{}批量提交订阅申请，申请数量：{}", userId, requests.size());

        List<String> applicationIds = new ArrayList<>();
        for (SubmitApplicationRequest request : requests) {
            String applicationId = submitApplication(userId, request);
            applicationIds.add(applicationId);
        }

        return applicationIds;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public boolean processApplication(String processBy, ProcessApplicationRequest request) {
        log.info("处理人{}处理申请，申请数量：{}，操作：{}", processBy, request.getApplicationIds().size(), request.getAction());

        LocalDateTime processTime = LocalDateTime.now();
        String status = request.getAction();
        String comment = request.getComment();

        // 批量更新申请状态
        int updateCount = applicationMapper.batchUpdateStatus(
                request.getApplicationIds(), status, processBy, comment, processTime
        );

        if (updateCount <= 0) {
            throw new RuntimeException("处理申请失败");
        }

        // 如果是通过申请，需要创建订阅记录
        if ("approved".equals(status)) {
            createSubscriptions(request.getApplicationIds());
        }

        log.info("成功处理{}个申请", updateCount);
        return true;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public boolean batchProcessApplications(String processBy, List<ProcessApplicationRequest> requests) {
        log.info("处理人{}批量处理申请，批次数量：{}", processBy, requests.size());

        for (ProcessApplicationRequest request : requests) {
            processApplication(processBy, request);
        }

        return true;
    }

    @Override
    public IPage<SubscriptionApplication> getApplicationPage(ApplicationQueryRequest request) {
        Page<SubscriptionApplication> page = new Page<>(request.getPageNum(), request.getPageSize());
        
        return applicationMapper.selectApplicationPage(
                page,
                request.getUserId(),
                request.getStatus(),
                request.getStartTime(),
                request.getEndTime()
        );
    }

    @Override
    public SubscriptionApplication getApplicationById(String applicationId) {
        return applicationMapper.selectById(applicationId);
    }

    @Override
    public IPage<SubscriptionApplication> getUserApplicationHistory(String userId, Integer pageNum, Integer pageSize) {
        Page<SubscriptionApplication> page = new Page<>(pageNum, pageSize);
        
        LambdaQueryWrapper<SubscriptionApplication> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(SubscriptionApplication::getUserId, userId)
                .orderByDesc(SubscriptionApplication::getSubmitTime);
        
        return applicationMapper.selectPage(page, wrapper);
    }

    @Override
    public IPage<UserInterfaceSubscription> getUserSubscriptions(String userId, Integer pageNum, Integer pageSize) {
        Page<UserInterfaceSubscription> page = new Page<>(pageNum, pageSize);
        
        return subscriptionMapper.selectSubscriptionPage(page, userId, null, "active");
    }

    @Override
    public Map<String, Object> getApplicationStatistics(String startTime, String endTime) {
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
    @Transactional(rollbackFor = Exception.class)
    public boolean cancelApplication(String userId, String applicationId) {
        log.info("用户{}取消申请：{}", userId, applicationId);

        SubscriptionApplication application = applicationMapper.selectById(applicationId);
        if (application == null) {
            throw new RuntimeException("申请不存在");
        }

        if (!userId.equals(application.getUserId())) {
            throw new RuntimeException("无权限取消此申请");
        }

        if (!SubscriptionApplication.Status.PENDING.getCode().equals(application.getStatus())) {
            throw new RuntimeException("只能取消待审批的申请");
        }

        // 删除申请记录
        int result = applicationMapper.deleteById(applicationId);
        return result > 0;
    }

    @Override
    public boolean hasInterfacePermission(String userId, String interfaceId) {
        UserInterfaceSubscription subscription = subscriptionMapper.selectByUserAndInterface(userId, interfaceId);
        return subscription != null && "active".equals(subscription.getStatus());
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public boolean updateCallCount(String userId, String interfaceId, Integer callCount) {
        int result = subscriptionMapper.updateCallCount(userId, interfaceId, callCount, LocalDateTime.now());
        return result > 0;
    }

    /**
     * 创建订阅记录
     */
    private void createSubscriptions(List<String> applicationIds) {
        log.info("为{}个申请创建订阅记录", applicationIds.size());

        List<UserInterfaceSubscription> subscriptions = new ArrayList<>();
        
        for (String applicationId : applicationIds) {
            SubscriptionApplication application = applicationMapper.selectById(applicationId);
            if (application == null) {
                continue;
            }

            for (String interfaceId : application.getInterfaceIds()) {
                // 检查是否已存在订阅
                UserInterfaceSubscription existing = subscriptionMapper.selectByUserAndInterface(
                        application.getUserId(), interfaceId
                );
                
                if (existing != null) {
                    // 更新现有订阅状态
                    existing.setStatus(UserInterfaceSubscription.Status.ACTIVE.getCode());
                    existing.setSubscribeTime(LocalDateTime.now());
                    existing.setUpdateTime(LocalDateTime.now());
                    subscriptionMapper.updateById(existing);
                } else {
                    // 创建新订阅
                    UserInterfaceSubscription subscription = new UserInterfaceSubscription();
                    subscription.setId(IdUtil.getSnowflakeNextIdStr());
                    subscription.setUserId(application.getUserId());
                    subscription.setInterfaceId(interfaceId);
                    subscription.setApplicationId(applicationId);
                    subscription.setStatus(UserInterfaceSubscription.Status.ACTIVE.getCode());
                    subscription.setSubscribeTime(LocalDateTime.now());
                    subscription.setCallCount(0);
                    subscription.setCreateTime(LocalDateTime.now());
                    subscription.setUpdateTime(LocalDateTime.now());
                    
                    subscriptions.add(subscription);
                }
            }
        }

        if (!subscriptions.isEmpty()) {
            subscriptionMapper.batchInsert(subscriptions);
            log.info("成功创建{}个订阅记录", subscriptions.size());
        }
    }
}