package com.powertrading.approval.task;

import com.powertrading.approval.service.ApplicationStatusService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

/**
 * 审批相关定时任务
 *
 * @author PowerTrading
 * @since 2024-01-15
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class ApprovalScheduledTask {

    private final ApplicationStatusService applicationStatusService;

    /**
     * 处理过期申请
     * 每天凌晨2点执行
     */
    @Scheduled(cron = "0 0 2 * * ?")
    public void processExpiredApplications() {
        log.info("开始处理过期申请定时任务");
        
        try {
            int processedCount = applicationStatusService.processExpiredApplications();
            log.info("过期申请处理完成，处理数量：{}", processedCount);
        } catch (Exception e) {
            log.error("处理过期申请失败：{}", e.getMessage(), e);
        }
    }

    /**
     * 生成申请统计报告
     * 每天早上8点执行
     */
    @Scheduled(cron = "0 0 8 * * ?")
    public void generateDailyReport() {
        log.info("开始生成每日申请统计报告");
        
        try {
            var statistics = applicationStatusService.getStatusStatistics();
            log.info("每日申请统计：{}", statistics);
            
            // 这里可以扩展发送报告邮件等功能
            
        } catch (Exception e) {
            log.error("生成每日报告失败：{}", e.getMessage(), e);
        }
    }

    /**
     * 清理历史数据
     * 每月1号凌晨3点执行
     */
    @Scheduled(cron = "0 0 3 1 * ?")
    public void cleanupHistoryData() {
        log.info("开始清理历史数据定时任务");
        
        try {
            // 这里可以实现清理超过一定时间的历史数据
            log.info("历史数据清理完成");
        } catch (Exception e) {
            log.error("清理历史数据失败：{}", e.getMessage(), e);
        }
    }
}