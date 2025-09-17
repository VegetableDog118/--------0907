package com.powertrading.auth.controller;

import com.powertrading.auth.service.SecurityPolicyService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import javax.servlet.http.HttpServletRequest;
import java.util.HashMap;
import java.util.Map;

/**
 * 安全策略管理控制器
 *
 * @author PowerTrading Team
 * @since 2024-01-15
 */
@Slf4j
@RestController
@RequestMapping("/api/v1/security")
@Tag(name = "安全策略管理", description = "安全策略配置、账户锁定、IP黑名单等安全管理接口")
public class SecurityPolicyController {

    @Autowired
    private SecurityPolicyService securityPolicyService;

    /**
     * 锁定账户
     */
    @PostMapping("/lock-account")
    @Operation(summary = "锁定账户", description = "手动锁定指定用户账户")
    public ResponseEntity<Map<String, Object>> lockAccount(
            @Parameter(description = "用户ID") @RequestParam String userId,
            @Parameter(description = "锁定原因") @RequestParam String reason,
            HttpServletRequest httpRequest) {
        
        try {
            String clientIp = getClientIp(httpRequest);
            securityPolicyService.lockAccount(userId, reason, clientIp);
            
            Map<String, Object> response = new HashMap<>();
            response.put("code", 200);
            response.put("message", "账户锁定成功");
            response.put("data", Map.of("userId", userId, "reason", reason));
            
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            log.error("锁定账户失败", e);
            
            Map<String, Object> response = new HashMap<>();
            response.put("code", 500);
            response.put("message", "锁定账户失败: " + e.getMessage());
            
            return ResponseEntity.status(500).body(response);
        }
    }

    /**
     * 解锁账户
     */
    @PostMapping("/unlock-account")
    @Operation(summary = "解锁账户", description = "手动解锁指定用户账户")
    public ResponseEntity<Map<String, Object>> unlockAccount(
            @Parameter(description = "用户ID") @RequestParam String userId) {
        
        try {
            securityPolicyService.unlockAccount(userId);
            
            Map<String, Object> response = new HashMap<>();
            response.put("code", 200);
            response.put("message", "账户解锁成功");
            response.put("data", Map.of("userId", userId));
            
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            log.error("解锁账户失败", e);
            
            Map<String, Object> response = new HashMap<>();
            response.put("code", 500);
            response.put("message", "解锁账户失败: " + e.getMessage());
            
            return ResponseEntity.status(500).body(response);
        }
    }

    /**
     * 检查账户锁定状态
     */
    @GetMapping("/account-status")
    @Operation(summary = "检查账户状态", description = "检查指定用户账户的锁定状态")
    public ResponseEntity<Map<String, Object>> getAccountStatus(
            @Parameter(description = "用户ID") @RequestParam String userId) {
        
        try {
            Map<String, Object> lockoutInfo = securityPolicyService.getAccountLockoutInfo(userId);
            
            Map<String, Object> response = new HashMap<>();
            response.put("code", 200);
            response.put("message", "获取账户状态成功");
            response.put("data", lockoutInfo);
            
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            log.error("获取账户状态失败", e);
            
            Map<String, Object> response = new HashMap<>();
            response.put("code", 500);
            response.put("message", "获取账户状态失败: " + e.getMessage());
            
            return ResponseEntity.status(500).body(response);
        }
    }

    /**
     * 添加IP到黑名单
     */
    @PostMapping("/blacklist-ip")
    @Operation(summary = "添加IP黑名单", description = "将指定IP地址添加到黑名单")
    public ResponseEntity<Map<String, Object>> addIpToBlacklist(
            @Parameter(description = "IP地址") @RequestParam String ip,
            @Parameter(description = "封禁原因") @RequestParam String reason,
            @Parameter(description = "封禁时长(秒)") @RequestParam(defaultValue = "3600") long duration) {
        
        try {
            securityPolicyService.addIpToBlacklist(ip, reason, duration);
            
            Map<String, Object> response = new HashMap<>();
            response.put("code", 200);
            response.put("message", "IP已添加到黑名单");
            response.put("data", Map.of("ip", ip, "reason", reason, "duration", duration));
            
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            log.error("添加IP到黑名单失败", e);
            
            Map<String, Object> response = new HashMap<>();
            response.put("code", 500);
            response.put("message", "添加IP到黑名单失败: " + e.getMessage());
            
            return ResponseEntity.status(500).body(response);
        }
    }

    /**
     * 从黑名单移除IP
     */
    @PostMapping("/remove-ip-blacklist")
    @Operation(summary = "移除IP黑名单", description = "从黑名单中移除指定IP地址")
    public ResponseEntity<Map<String, Object>> removeIpFromBlacklist(
            @Parameter(description = "IP地址") @RequestParam String ip) {
        
        try {
            securityPolicyService.removeIpFromBlacklist(ip);
            
            Map<String, Object> response = new HashMap<>();
            response.put("code", 200);
            response.put("message", "IP已从黑名单移除");
            response.put("data", Map.of("ip", ip));
            
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            log.error("从黑名单移除IP失败", e);
            
            Map<String, Object> response = new HashMap<>();
            response.put("code", 500);
            response.put("message", "从黑名单移除IP失败: " + e.getMessage());
            
            return ResponseEntity.status(500).body(response);
        }
    }

    /**
     * 检查IP黑名单状态
     */
    @GetMapping("/ip-status")
    @Operation(summary = "检查IP状态", description = "检查指定IP是否在黑名单中")
    public ResponseEntity<Map<String, Object>> getIpStatus(
            @Parameter(description = "IP地址") @RequestParam String ip) {
        
        try {
            boolean isBlacklisted = securityPolicyService.isIpBlacklisted(ip);
            
            Map<String, Object> data = new HashMap<>();
            data.put("ip", ip);
            data.put("blacklisted", isBlacklisted);
            
            Map<String, Object> response = new HashMap<>();
            response.put("code", 200);
            response.put("message", "获取IP状态成功");
            response.put("data", data);
            
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            log.error("获取IP状态失败", e);
            
            Map<String, Object> response = new HashMap<>();
            response.put("code", 500);
            response.put("message", "获取IP状态失败: " + e.getMessage());
            
            return ResponseEntity.status(500).body(response);
        }
    }

    /**
     * 记录可疑活动
     */
    @PostMapping("/record-suspicious")
    @Operation(summary = "记录可疑活动", description = "手动记录可疑活动")
    public ResponseEntity<Map<String, Object>> recordSuspiciousActivity(
            @Parameter(description = "用户ID") @RequestParam String userId,
            @Parameter(description = "活动类型") @RequestParam String activityType,
            @Parameter(description = "活动描述") @RequestParam String description,
            HttpServletRequest httpRequest) {
        
        try {
            String clientIp = getClientIp(httpRequest);
            securityPolicyService.recordSuspiciousActivity(userId, clientIp, activityType, description);
            
            Map<String, Object> response = new HashMap<>();
            response.put("code", 200);
            response.put("message", "可疑活动记录成功");
            response.put("data", Map.of(
                    "userId", userId,
                    "activityType", activityType,
                    "description", description
            ));
            
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            log.error("记录可疑活动失败", e);
            
            Map<String, Object> response = new HashMap<>();
            response.put("code", 500);
            response.put("message", "记录可疑活动失败: " + e.getMessage());
            
            return ResponseEntity.status(500).body(response);
        }
    }

    /**
     * 检查密码强度
     */
    @PostMapping("/check-password-strength")
    @Operation(summary = "检查密码强度", description = "检查密码是否符合安全要求")
    public ResponseEntity<Map<String, Object>> checkPasswordStrength(
            @Parameter(description = "密码") @RequestParam String password) {
        
        try {
            boolean isStrong = securityPolicyService.isPasswordStrong(password);
            
            Map<String, Object> data = new HashMap<>();
            data.put("strong", isStrong);
            data.put("requirements", Map.of(
                    "minLength", 8,
                    "requireUppercase", true,
                    "requireLowercase", true,
                    "requireDigit", true,
                    "requireSpecialChar", true
            ));
            
            Map<String, Object> response = new HashMap<>();
            response.put("code", 200);
            response.put("message", isStrong ? "密码强度符合要求" : "密码强度不足");
            response.put("data", data);
            
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            log.error("检查密码强度失败", e);
            
            Map<String, Object> response = new HashMap<>();
            response.put("code", 500);
            response.put("message", "检查密码强度失败: " + e.getMessage());
            
            return ResponseEntity.status(500).body(response);
        }
    }

    /**
     * 获取安全策略配置
     */
    @GetMapping("/policy-config")
    @Operation(summary = "获取安全策略配置", description = "获取当前的安全策略配置")
    public ResponseEntity<Map<String, Object>> getPolicyConfig() {
        
        try {
            Map<String, Object> config = securityPolicyService.getSecurityPolicyConfig();
            
            Map<String, Object> response = new HashMap<>();
            response.put("code", 200);
            response.put("message", "获取安全策略配置成功");
            response.put("data", config);
            
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            log.error("获取安全策略配置失败", e);
            
            Map<String, Object> response = new HashMap<>();
            response.put("code", 500);
            response.put("message", "获取安全策略配置失败: " + e.getMessage());
            
            return ResponseEntity.status(500).body(response);
        }
    }

    /**
     * 更新安全策略配置
     */
    @PostMapping("/policy-config")
    @Operation(summary = "更新安全策略配置", description = "更新安全策略配置")
    public ResponseEntity<Map<String, Object>> updatePolicyConfig(
            @RequestBody Map<String, Object> config) {
        
        try {
            securityPolicyService.updateSecurityPolicyConfig(config);
            
            Map<String, Object> response = new HashMap<>();
            response.put("code", 200);
            response.put("message", "安全策略配置更新成功");
            response.put("data", config);
            
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            log.error("更新安全策略配置失败", e);
            
            Map<String, Object> response = new HashMap<>();
            response.put("code", 500);
            response.put("message", "更新安全策略配置失败: " + e.getMessage());
            
            return ResponseEntity.status(500).body(response);
        }
    }

    /**
     * 获取安全统计信息
     */
    @GetMapping("/statistics")
    @Operation(summary = "获取安全统计信息", description = "获取安全相关的统计数据")
    public ResponseEntity<Map<String, Object>> getSecurityStatistics() {
        
        try {
            Map<String, Object> statistics = securityPolicyService.getSecurityStatistics();
            
            Map<String, Object> response = new HashMap<>();
            response.put("code", 200);
            response.put("message", "获取安全统计信息成功");
            response.put("data", statistics);
            
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            log.error("获取安全统计信息失败", e);
            
            Map<String, Object> response = new HashMap<>();
            response.put("code", 500);
            response.put("message", "获取安全统计信息失败: " + e.getMessage());
            
            return ResponseEntity.status(500).body(response);
        }
    }

    /**
     * 生成安全报告
     */
    @GetMapping("/security-report")
    @Operation(summary = "生成安全报告", description = "生成详细的安全状况报告")
    public ResponseEntity<Map<String, Object>> generateSecurityReport() {
        
        try {
            Map<String, Object> report = securityPolicyService.generateSecurityReport();
            
            Map<String, Object> response = new HashMap<>();
            response.put("code", 200);
            response.put("message", "安全报告生成成功");
            response.put("data", report);
            
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            log.error("生成安全报告失败", e);
            
            Map<String, Object> response = new HashMap<>();
            response.put("code", 500);
            response.put("message", "生成安全报告失败: " + e.getMessage());
            
            return ResponseEntity.status(500).body(response);
        }
    }

    /**
     * 清除登录失败记录
     */
    @PostMapping("/clear-login-failures")
    @Operation(summary = "清除登录失败记录", description = "清除指定用户的登录失败记录")
    public ResponseEntity<Map<String, Object>> clearLoginFailures(
            @Parameter(description = "用户ID") @RequestParam String userId) {
        
        try {
            securityPolicyService.clearLoginFailures(userId);
            
            Map<String, Object> response = new HashMap<>();
            response.put("code", 200);
            response.put("message", "登录失败记录清除成功");
            response.put("data", Map.of("userId", userId));
            
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            log.error("清除登录失败记录失败", e);
            
            Map<String, Object> response = new HashMap<>();
            response.put("code", 500);
            response.put("message", "清除登录失败记录失败: " + e.getMessage());
            
            return ResponseEntity.status(500).body(response);
        }
    }

    /**
     * 健康检查
     */
    @GetMapping("/health")
    @Operation(summary = "健康检查", description = "检查安全策略服务的健康状态")
    public ResponseEntity<Map<String, Object>> health() {
        Map<String, Object> response = new HashMap<>();
        response.put("code", 200);
        response.put("message", "安全策略服务运行正常");
        response.put("timestamp", System.currentTimeMillis());
        
        return ResponseEntity.ok(response);
    }

    /**
     * 获取客户端IP地址
     */
    private String getClientIp(HttpServletRequest request) {
        String xForwardedFor = request.getHeader("X-Forwarded-For");
        if (xForwardedFor != null && !xForwardedFor.isEmpty() && !"unknown".equalsIgnoreCase(xForwardedFor)) {
            return xForwardedFor.split(",")[0].trim();
        }
        
        String xRealIp = request.getHeader("X-Real-IP");
        if (xRealIp != null && !xRealIp.isEmpty() && !"unknown".equalsIgnoreCase(xRealIp)) {
            return xRealIp;
        }
        
        return request.getRemoteAddr();
    }
}