package com.powertrading.user.controller;

import com.powertrading.user.common.ApiResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.HashMap;
import java.util.Map;

/**
 * 系统管理控制器
 *
 * @author PowerTrading Team
 * @since 2024-01-15
 */
@RestController
@RequestMapping("/api/v1/system")
@Tag(name = "系统管理", description = "系统统计和管理相关接口")
public class SystemController {

    private static final Logger logger = LoggerFactory.getLogger(SystemController.class);

    /**
     * 获取系统统计数据
     */
    @GetMapping("/stats")
    @Operation(summary = "获取系统统计数据", description = "获取系统用户、角色、配置等统计信息")
    @PreAuthorize("permitAll()")
    public ApiResponse<Map<String, Object>> getSystemStats() {
        try {
            Map<String, Object> stats = new HashMap<>();
            
            // 模拟统计数据，实际应该从数据库查询
            stats.put("userCount", 156);
            stats.put("roleCount", 4);
            stats.put("configCount", 23);
            stats.put("datasourceCount", 8);
            stats.put("systemStatus", "正常");
            stats.put("todayLogCount", 89);
            
            logger.info("获取系统统计数据成功");
            return ApiResponse.success(stats);
        } catch (Exception e) {
            logger.error("获取系统统计数据失败: {}", e.getMessage(), e);
            return ApiResponse.error("获取系统统计数据失败: " + e.getMessage());
        }
    }
}