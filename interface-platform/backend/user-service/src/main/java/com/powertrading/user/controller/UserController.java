package com.powertrading.user.controller;

import com.powertrading.user.common.ApiResponse;
import com.powertrading.user.dto.*;
import com.powertrading.user.service.UserService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import javax.servlet.http.HttpServletRequest;
import javax.validation.Valid;
import javax.validation.constraints.NotBlank;
import java.util.List;

/**
 * 用户管理控制器
 *
 * @author PowerTrading Team
 * @since 2024-01-15
 */
@RestController
@RequestMapping("/api/v1/users")
@Tag(name = "用户管理", description = "用户注册、登录、信息管理等接口")
@Validated
public class UserController {

    private static final Logger logger = LoggerFactory.getLogger(UserController.class);

    @Autowired
    private UserService userService;

    /**
     * 用户注册
     */
    @PostMapping("/register")
    @Operation(summary = "用户注册", description = "企业用户注册，需要提供企业信息和联系人信息")
    public ApiResponse<UserRegisterResponse> register(
            @Valid @RequestBody UserRegisterRequest request) {
        try {
            UserRegisterResponse response = userService.register(request);
            return ApiResponse.success("注册成功，等待审核", response);
        } catch (Exception e) {
            logger.error("用户注册失败: {}", e.getMessage(), e);
            return ApiResponse.error("REGISTER_FAILED", e.getMessage());
        }
    }

    /**
     * 用户登录
     */
    @PostMapping("/login")
    @Operation(summary = "用户登录", description = "支持用户名、手机号、邮箱登录")
    public ApiResponse<UserLoginResponse> login(
            @Valid @RequestBody UserLoginRequest request,
            HttpServletRequest httpRequest) {
        try {
            UserLoginResponse response = userService.login(request);
            return ApiResponse.success("登录成功", response);
        } catch (Exception e) {
            logger.error("用户登录失败: {}", e.getMessage(), e);
            return ApiResponse.error("LOGIN_FAILED", e.getMessage());
        }
    }

    /**
     * 用户登出
     */
    @PostMapping("/logout")
    @Operation(summary = "用户登出", description = "用户登出，使Token失效")
    @PreAuthorize("hasAuthority('user:read:self')")
    public ApiResponse<Void> logout(HttpServletRequest request) {
        try {
            String token = extractToken(request);
            if (token != null) {
                userService.logout(token);
            }
            return ApiResponse.<Void>success("登出成功", null);
        } catch (Exception e) {
            logger.error("用户登出失败: {}", e.getMessage(), e);
            return ApiResponse.error("LOGOUT_FAILED", e.getMessage());
        }
    }

    /**
     * 获取当前用户信息
     */
    @GetMapping("/profile")
    @Operation(summary = "获取当前用户信息", description = "获取当前登录用户的详细信息")
    @PreAuthorize("hasAuthority('user:read:self')")
    public ApiResponse<UserInfoResponse> getProfile(
            @Parameter(hidden = true) @RequestAttribute("userId") String userId) {
        try {
            UserInfoResponse response = userService.getUserInfo(userId);
            return ApiResponse.success(response);
        } catch (Exception e) {
            logger.error("获取用户信息失败: {}", e.getMessage(), e);
            return ApiResponse.error("GET_USER_INFO_FAILED", e.getMessage());
        }
    }

    /**
     * 更新用户信息
     */
    @PutMapping("/profile")
    @Operation(summary = "更新用户信息", description = "更新当前用户的基本信息")
    @PreAuthorize("hasAuthority('user:write:self')")
    public ApiResponse<Void> updateProfile(
            @Parameter(hidden = true) @RequestAttribute("userId") String userId,
            @Valid @RequestBody UserUpdateRequest request) {
        try {
            userService.updateUserInfo(userId, request);
            return ApiResponse.<Void>success("用户信息更新成功", null);
        } catch (Exception e) {
            logger.error("更新用户信息失败: {}", e.getMessage(), e);
            return ApiResponse.error("UPDATE_USER_INFO_FAILED", e.getMessage());
        }
    }

    /**
     * 修改密码
     */
    @PutMapping("/password")
    @Operation(summary = "修改密码", description = "修改当前用户的登录密码")
    @PreAuthorize("hasAuthority('user:write:self')")
    public ApiResponse<Void> changePassword(
            @Parameter(hidden = true) @RequestAttribute("userId") String userId,
            @Valid @RequestBody ChangePasswordRequest request) {
        try {
            userService.changePassword(userId, request);
            return ApiResponse.<Void>success("密码修改成功", null);
        } catch (Exception e) {
            logger.error("修改密码失败: {}", e.getMessage(), e);
            return ApiResponse.error("CHANGE_PASSWORD_FAILED", e.getMessage());
        }
    }

    /**
     * 获取API密钥
     */
    @GetMapping("/api-key")
    @Operation(summary = "获取API密钥", description = "获取当前用户的API密钥信息")
    @PreAuthorize("hasAuthority('user:read:self')")
    public ApiResponse<ApiKeyResponse> getApiKey(
            @Parameter(hidden = true) @RequestAttribute("userId") String userId) {
        try {
            ApiKeyResponse response = userService.getApiKey(userId);
            return ApiResponse.success(response);
        } catch (Exception e) {
            logger.error("获取API密钥失败: {}", e.getMessage(), e);
            return ApiResponse.error("GET_API_KEY_FAILED", e.getMessage());
        }
    }

    /**
     * 重置API密钥
     */
    @PostMapping("/api-key/reset")
    @Operation(summary = "重置API密钥", description = "重新生成用户的API密钥")
    @PreAuthorize("hasAuthority('user:write:self')")
    public ApiResponse<ApiKeyResponse> resetApiKey(
            @Parameter(hidden = true) @RequestAttribute("userId") String userId) {
        try {
            ApiKeyResponse response = userService.resetApiKey(userId);
            return ApiResponse.success("API密钥重置成功", response);
        } catch (Exception e) {
            logger.error("重置API密钥失败: {}", e.getMessage(), e);
            return ApiResponse.error("RESET_API_KEY_FAILED", e.getMessage());
        }
    }

    /**
     * 获取用户权限列表
     */
    @GetMapping("/permissions")
    @Operation(summary = "获取用户权限", description = "获取当前用户的权限列表")
    @PreAuthorize("hasAuthority('user:read:self')")
    public ApiResponse<List<String>> getUserPermissions(
            @Parameter(hidden = true) @RequestAttribute("role") String role) {
        try {
            List<String> permissions = userService.getUserPermissions(role);
            return ApiResponse.success(permissions);
        } catch (Exception e) {
            logger.error("获取用户权限失败: {}", e.getMessage(), e);
            return ApiResponse.error("GET_PERMISSIONS_FAILED", e.getMessage());
        }
    }

    /**
     * Token验证
     */
    @PostMapping("/validate-token")
    @Operation(summary = "Token验证", description = "验证JWT Token的有效性")
    public ApiResponse<TokenInfo> validateToken(
            @RequestParam @NotBlank(message = "Token不能为空") String token) {
        try {
            TokenInfo tokenInfo = userService.validateToken(token);
            return ApiResponse.success(tokenInfo);
        } catch (Exception e) {
            logger.error("Token验证失败: {}", e.getMessage(), e);
            return ApiResponse.error("TOKEN_INVALID", e.getMessage());
        }
    }

    // ========== 管理员接口 ==========

    /**
     * 获取用户列表（管理员）
     */
    @GetMapping("/list")
    @Operation(summary = "获取用户列表", description = "管理员获取用户列表，支持分页和筛选")
    @PreAuthorize("hasAuthority('user:read')")
    public ApiResponse<PageResponse<UserInfoResponse>> getUserList(
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "10") int size,
            @RequestParam(required = false) String status,
            @RequestParam(required = false) String keyword) {
        try {
            PageResponse<UserInfoResponse> response = userService.getUserList(page, size, status, keyword);
            return ApiResponse.success(response);
        } catch (Exception e) {
            logger.error("获取用户列表失败: {}", e.getMessage(), e);
            return ApiResponse.error("GET_USER_LIST_FAILED", e.getMessage());
        }
    }

    /**
     * 获取指定用户信息（管理员）
     */
    @GetMapping("/{userId}")
    @Operation(summary = "获取指定用户信息", description = "管理员获取指定用户的详细信息")
    @PreAuthorize("hasAuthority('user:read')")
    public ApiResponse<UserInfoResponse> getUserById(
            @PathVariable @NotBlank(message = "用户ID不能为空") String userId) {
        try {
            UserInfoResponse response = userService.getUserInfo(userId);
            return ApiResponse.success(response);
        } catch (Exception e) {
            logger.error("获取用户信息失败: {}", e.getMessage(), e);
            return ApiResponse.error("GET_USER_INFO_FAILED", e.getMessage());
        }
    }

    /**
     * 审核通过用户（管理员）
     */
    @PostMapping("/{userId}/approve")
    @Operation(summary = "审核通过用户", description = "管理员审核通过用户注册申请")
    @PreAuthorize("hasAuthority('user:write')")
    public ApiResponse<Void> approveUser(
            @PathVariable @NotBlank(message = "用户ID不能为空") String userId,
            @Parameter(hidden = true) @RequestAttribute("userId") String approvedBy) {
        try {
            userService.approveUser(userId, approvedBy);
            return ApiResponse.<Void>success("用户审核通过", null);
        } catch (Exception e) {
            logger.error("用户审核失败: {}", e.getMessage(), e);
            return ApiResponse.error("APPROVE_USER_FAILED", e.getMessage());
        }
    }

    /**
     * 审核拒绝用户（管理员）
     */
    @PostMapping("/{userId}/reject")
    @Operation(summary = "审核拒绝用户", description = "管理员拒绝用户注册申请")
    @PreAuthorize("hasAuthority('user:write')")
    public ApiResponse<Void> rejectUser(
            @PathVariable @NotBlank(message = "用户ID不能为空") String userId,
            @RequestParam @NotBlank(message = "拒绝原因不能为空") String reason,
            @Parameter(hidden = true) @RequestAttribute("userId") String rejectedBy) {
        try {
            userService.rejectUser(userId, rejectedBy, reason);
            return ApiResponse.<Void>success("用户审核拒绝", null);
        } catch (Exception e) {
            logger.error("用户审核拒绝失败: {}", e.getMessage(), e);
            return ApiResponse.error("REJECT_USER_FAILED", e.getMessage());
        }
    }

    /**
     * 锁定用户（管理员）
     */
    @PostMapping("/{userId}/lock")
    @Operation(summary = "锁定用户", description = "管理员锁定用户账户")
    @PreAuthorize("hasAuthority('user:write')")
    public ApiResponse<Void> lockUser(
            @PathVariable @NotBlank(message = "用户ID不能为空") String userId,
            @RequestParam @NotBlank(message = "锁定原因不能为空") String reason,
            @Parameter(hidden = true) @RequestAttribute("userId") String lockedBy) {
        try {
            userService.lockUser(userId, lockedBy, reason);
            return ApiResponse.<Void>success("用户锁定成功", null);
        } catch (Exception e) {
            logger.error("用户锁定失败: {}", e.getMessage(), e);
            return ApiResponse.error("LOCK_USER_FAILED", e.getMessage());
        }
    }

    /**
     * 解锁用户（管理员）
     */
    @PostMapping("/{userId}/unlock")
    @Operation(summary = "解锁用户", description = "管理员解锁用户账户")
    @PreAuthorize("hasAuthority('user:write')")
    public ApiResponse<Void> unlockUser(
            @PathVariable @NotBlank(message = "用户ID不能为空") String userId,
            @Parameter(hidden = true) @RequestAttribute("userId") String unlockedBy) {
        try {
            userService.unlockUser(userId, unlockedBy);
            return ApiResponse.<Void>success("用户解锁成功", null);
        } catch (Exception e) {
            logger.error("用户解锁失败: {}", e.getMessage(), e);
            return ApiResponse.error("UNLOCK_USER_FAILED", e.getMessage());
        }
    }

    /**
     * 检查用户是否存在（内部服务调用）
     */
    @GetMapping("/exists/{userId}")
    @Operation(summary = "检查用户是否存在", description = "内部服务调用，检查指定用户是否存在")
    @PreAuthorize("permitAll()")
    public ApiResponse<Boolean> userExists(@PathVariable String userId) {
        try {
            boolean exists = userService.userExists(userId);
            return ApiResponse.success(exists);
        } catch (Exception e) {
            logger.error("检查用户是否存在失败: {}", e.getMessage(), e);
            return ApiResponse.error("CHECK_USER_EXISTS_FAILED", e.getMessage());
        }
    }

    /**
     * 获取用户角色（内部服务调用）
     */
    @GetMapping("/role/{userId}")
    @Operation(summary = "获取用户角色", description = "内部服务调用，获取指定用户的角色")
    @PreAuthorize("permitAll()")
    public ApiResponse<String> getUserRole(@PathVariable String userId) {
        try {
            UserInfoResponse userInfo = userService.getUserInfo(userId);
            return ApiResponse.success(userInfo.getRole());
        } catch (Exception e) {
            logger.error("获取用户角色失败: {}", e.getMessage(), e);
            return ApiResponse.error("GET_USER_ROLE_FAILED", e.getMessage());
        }
    }

    /**
     * 从请求头中提取Token
     */
    private String extractToken(HttpServletRequest request) {
        String bearerToken = request.getHeader("Authorization");
        if (bearerToken != null && bearerToken.startsWith("Bearer ")) {
            return bearerToken.substring(7);
        }
        return null;
    }
}