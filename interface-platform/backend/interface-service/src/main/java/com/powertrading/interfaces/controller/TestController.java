package com.powertrading.interfaces.controller;

import com.powertrading.interfaces.common.ApiResponse;
import com.powertrading.interfaces.entity.Interface;
import com.powertrading.interfaces.mapper.InterfaceMapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;

/**
 * 临时测试控制器 - 用于测试下架功能
 */
@RestController
@RequestMapping("/api/test")
public class TestController {

    @Autowired
    private InterfaceMapper interfaceMapper;

    /**
     * 测试下架功能
     */
    @PostMapping("/offline/{interfaceId}")
    public ApiResponse<String> testOffline(@PathVariable String interfaceId, @RequestParam String reason) {
        try {
            // 直接使用MyBatis Plus的updateById方法
            Interface updateInterface = new Interface();
            updateInterface.setId(interfaceId);
            updateInterface.setStatus(Interface.STATUS_OFFLINE);
            updateInterface.setOfflineTime(LocalDateTime.now());
            updateInterface.setOfflineBy("test-admin");
            updateInterface.setOfflineReason(reason);
            updateInterface.setUpdateBy("test-admin");
            
            int result = interfaceMapper.updateById(updateInterface);
            
            if (result > 0) {
                return ApiResponse.success("下架成功");
            } else {
                return ApiResponse.error("下架失败：接口不存在");
            }
        } catch (Exception e) {
            return ApiResponse.error("下架失败: " + e.getMessage());
        }
    }
    
    /**
     * 测试上架功能
     */
    @PostMapping("/publish/{interfaceId}")
    public ApiResponse<String> testPublish(@PathVariable String interfaceId) {
        try {
            Interface updateInterface = new Interface();
            updateInterface.setId(interfaceId);
            updateInterface.setStatus(Interface.STATUS_PUBLISHED);
            updateInterface.setPublishTime(LocalDateTime.now());
            updateInterface.setPublishBy("test-admin");
            updateInterface.setUpdateBy("test-admin");
            // 清空下架相关字段
            updateInterface.setOfflineTime(null);
            updateInterface.setOfflineBy(null);
            updateInterface.setOfflineReason(null);
            
            int result = interfaceMapper.updateById(updateInterface);
            
            if (result > 0) {
                return ApiResponse.success("上架成功");
            } else {
                return ApiResponse.error("上架失败：接口不存在");
            }
        } catch (Exception e) {
            return ApiResponse.error("上架失败: " + e.getMessage());
        }
    }
}