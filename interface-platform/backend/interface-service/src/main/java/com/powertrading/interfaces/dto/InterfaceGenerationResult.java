package com.powertrading.interfaces.dto;

import lombok.Builder;
import lombok.Data;

/**
 * 接口生成结果DTO
 *
 * @author PowerTrading Team
 * @since 2024-01-15
 */
@Data
@Builder
public class InterfaceGenerationResult {
    
    public InterfaceGenerationResult() {
        // 默认构造函数
    }
    
    /**
     * 接口ID
     */
    private String interfaceId;
    
    /**
     * 接口路径
     */
    private String interfacePath;
    
    /**
     * 接口状态
     */
    private String status;
    
    /**
     * 生成消息
     */
    private String message;
    
    /**
     * 生成时间戳
     */
    private Long timestamp;
    
    /**
     * 是否成功
     */
    private Boolean success;
    
    // 手动添加getter方法以确保兼容性
    public Boolean getSuccess() {
        return success;
    }
    
    public String getMessage() {
        return message;
    }
    
    // 手动添加builder方法
    public static InterfaceGenerationResultBuilder builder() {
        return new InterfaceGenerationResultBuilder();
    }
    
    public static class InterfaceGenerationResultBuilder {
        private String interfaceId;
        private String interfacePath;
        private String status;
        private String message;
        private Long timestamp;
        private Boolean success;
        
        public InterfaceGenerationResultBuilder interfaceId(String interfaceId) {
            this.interfaceId = interfaceId;
            return this;
        }
        
        public InterfaceGenerationResultBuilder interfacePath(String interfacePath) {
            this.interfacePath = interfacePath;
            return this;
        }
        
        public InterfaceGenerationResultBuilder status(String status) {
            this.status = status;
            return this;
        }
        
        public InterfaceGenerationResultBuilder message(String message) {
            this.message = message;
            return this;
        }
        
        public InterfaceGenerationResultBuilder timestamp(Long timestamp) {
            this.timestamp = timestamp;
            return this;
        }
        
        public InterfaceGenerationResultBuilder success(Boolean success) {
            this.success = success;
            return this;
        }
        
        public InterfaceGenerationResult build() {
            InterfaceGenerationResult result = new InterfaceGenerationResult();
            result.interfaceId = this.interfaceId;
            result.interfacePath = this.interfacePath;
            result.status = this.status;
            result.message = this.message;
            result.timestamp = this.timestamp;
            result.success = this.success;
            return result;
        }
    }
}