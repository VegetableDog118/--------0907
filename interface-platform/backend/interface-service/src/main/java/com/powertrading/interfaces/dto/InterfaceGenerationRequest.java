package com.powertrading.interfaces.dto;

import lombok.Data;
import javax.validation.Valid;
import javax.validation.constraints.NotBlank;
import javax.validation.constraints.NotNull;
import java.util.List;

/**
 * 接口生成请求DTO
 *
 * @author PowerTrading Team
 * @since 2024-01-15
 */
@Data
public class InterfaceGenerationRequest {
    
    /**
     * MySQL表名
     */
    @NotBlank(message = "表名不能为空")
    private String tableName;
    
    /**
     * 接口配置
     */
    @Valid
    @NotNull(message = "接口配置不能为空")
    private InterfaceConfig interfaceConfig;
    
    /**
     * 参数列表
     */
    private List<ParameterDefinition> parameters;
    
    // 手动添加getter方法
    public String getTableName() {
        return tableName;
    }
    
    public InterfaceConfig getInterfaceConfig() {
        return interfaceConfig;
    }
    
    public List<ParameterDefinition> getParameters() {
        return parameters;
    }
    
    /**
     * 接口配置
     */
    @Data
    public static class InterfaceConfig {
        
        /**
         * 接口名称
         */
        @NotBlank(message = "接口名称不能为空")
        private String name;
        
        /**
         * 接口描述
         */
        private String description;
        
        /**
         * 业务分类
         */
        @NotBlank(message = "业务分类不能为空")
        private String businessType;
        
        /**
         * 请求方法
         */
        private String method = "POST";
        
        /**
         * 限流配置
         */
        private Integer rateLimit = 1000;
        
        /**
         * 超时时间
         */
        private Integer timeout = 30;
        
        // 手动添加getter方法
        public String getName() {
            return name;
        }
        
        public String getDescription() {
            return description;
        }
        
        public String getBusinessType() {
            return businessType;
        }
        
        public String getMethod() {
            return method;
        }
        
        public Integer getRateLimit() {
            return rateLimit;
        }
        
        public Integer getTimeout() {
            return timeout;
        }
    }
    
    /**
     * 参数定义
     */
    @Data
    public static class ParameterDefinition {
        
        /**
         * 参数名
         */
        private String name;
        
        /**
         * 参数类型
         */
        private String type;
        
        /**
         * 参数位置
         */
        private String location;
        
        /**
         * 是否必需
         */
        private Boolean required;
        
        /**
         * 参数描述
         */
        private String description;
        
        /**
         * 默认值
         */
        private String defaultValue;
        
        /**
         * 是否为标准参数
         */
        private Boolean isStandard;
        
        // 手动添加getter方法
        public String getName() {
            return name;
        }
        
        public String getType() {
            return type;
        }
        
        public String getLocation() {
            return location;
        }
        
        public Boolean getRequired() {
            return required;
        }
        
        public String getDescription() {
            return description;
        }
        
        public String getDefaultValue() {
            return defaultValue;
        }
        
        public Boolean getIsStandard() {
            return isStandard;
        }
    }
    
    /**
     * 接口配置（兼容性别名）
     */
    @Data
    public static class InterfaceConfiguration {
        
        /**
         * 接口名称
         */
        @NotBlank(message = "接口名称不能为空")
        private String interfaceName;
        
        /**
         * 接口路径
         */
        private String interfacePath;
        
        /**
         * 接口描述
         */
        private String description;
        
        /**
         * 业务分类ID
         */
        @NotBlank(message = "业务分类不能为空")
        private String categoryId;
        
        /**
         * 请求方法
         */
        private String requestMethod = "POST";
        
        /**
         * 限流配置
         */
        private Integer rateLimit = 1000;
        
        /**
         * 超时时间
         */
        private Integer timeout = 30;
        
        // 手动添加getter方法
        public String getInterfaceName() {
            return interfaceName;
        }
        
        public String getInterfacePath() {
            return interfacePath;
        }
        
        public String getDescription() {
            return description;
        }
        
        public String getCategoryId() {
            return categoryId;
        }
        
        public String getRequestMethod() {
            return requestMethod;
        }
        
        public Integer getRateLimit() {
            return rateLimit;
        }
        
        public Integer getTimeout() {
            return timeout;
        }
    }
    
    /**
     * 参数配置（兼容性别名）
     */
    @Data
    public static class ParameterConfiguration {
        
        /**
         * 参数名
         */
        private String paramName;
        
        /**
         * 参数类型
         */
        private String paramType;
        
        /**
         * 参数位置
         */
        private String paramLocation;
        
        /**
         * 是否必需
         */
        private Boolean required;
        
        /**
         * 参数描述
         */
        private String description;
        
        /**
         * 默认值
         */
        private String defaultValue;
        
        // 手动添加getter方法
        public String getParamName() {
            return paramName;
        }
        
        public String getParamType() {
            return paramType;
        }
        
        public String getParamLocation() {
            return paramLocation;
        }
        
        public Boolean getRequired() {
            return required;
        }
        
        public String getDescription() {
            return description;
        }
        
        public String getDefaultValue() {
            return defaultValue;
        }
        
        public String getExample() {
            return defaultValue; // 使用defaultValue作为example
        }
        
        public Integer getSortOrder() {
            return 1; // 默认排序
        }
        
        // 手动添加setter方法
        public void setParamName(String paramName) {
            this.paramName = paramName;
        }
        
        public void setParamType(String paramType) {
            this.paramType = paramType;
        }
        
        public void setParamLocation(String paramLocation) {
            this.paramLocation = paramLocation;
        }
        
        public void setRequired(Boolean required) {
            this.required = required;
        }
        
        public void setDescription(String description) {
            this.description = description;
        }
        
        public void setDefaultValue(String defaultValue) {
            this.defaultValue = defaultValue;
        }
    }
    
    // 为InterfaceGenerationRequest添加getConfig方法
    public InterfaceGenerationRequest.InterfaceConfiguration getConfig() {
        // 将InterfaceConfig转换为InterfaceConfiguration
        InterfaceConfiguration config = new InterfaceConfiguration();
        if (this.interfaceConfig != null) {
            config.interfaceName = this.interfaceConfig.getName();
            config.description = this.interfaceConfig.getDescription();
            config.categoryId = this.interfaceConfig.getBusinessType();
            config.requestMethod = this.interfaceConfig.getMethod();
            config.rateLimit = this.interfaceConfig.getRateLimit();
            config.timeout = this.interfaceConfig.getTimeout();
        }
        return config;
    }
}