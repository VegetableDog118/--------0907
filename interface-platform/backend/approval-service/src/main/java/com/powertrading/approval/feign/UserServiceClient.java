package com.powertrading.approval.feign;

import com.powertrading.approval.common.Result;
import com.powertrading.approval.dto.ApiResponse;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;

import java.util.List;
import java.util.Map;

/**
 * 用户服务Feign客户端
 *
 * @author PowerTrading
 * @since 2024-01-15
 */
@FeignClient(name = "user-service", url = "http://localhost:8087/user-service")
public interface UserServiceClient {

    /**
     * 根据用户ID查询用户信息
     *
     * @param userId 用户ID
     * @return 用户信息
     */
    @GetMapping("/info/{userId}")
    Result<Map<String, Object>> getUserInfo(@PathVariable("userId") String userId);

    /**
     * 更新用户API权限
     *
     * @param userId 用户ID
     * @param interfaceIds 接口ID列表
     * @return 更新结果
     */
    @PostMapping("/permissions/{userId}/interfaces")
    Result<Boolean> updateUserInterfacePermissions(
            @PathVariable("userId") String userId,
            @RequestBody List<String> interfaceIds
    );

    /**
     * 批量更新用户API权限
     *
     * @param permissionUpdates 权限更新列表
     * @return 更新结果
     */
    @PostMapping("/permissions/batch")
    Result<Boolean> batchUpdateUserPermissions(@RequestBody List<Map<String, Object>> permissionUpdates);

    /**
     * 检查用户是否存在
     *
     * @param userId 用户ID
     * @return 是否存在
     */
    @GetMapping("/api/v1/users/exists/{userId}")
    ApiResponse<Boolean> userExists(@PathVariable("userId") String userId);

    /**
     * 获取用户角色
     *
     * @param userId 用户ID
     * @return 用户角色
     */
    @GetMapping("/api/v1/users/role/{userId}")
    ApiResponse<String> getUserRole(@PathVariable("userId") String userId);
}