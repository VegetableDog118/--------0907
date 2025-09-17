package com.powertrading.interfaces.vo;

import lombok.Data;

import java.time.LocalDateTime;
import java.util.List;

/**
 * 接口响应VO
 *
 * @author PowerTrading Team
 * @since 2024-01-15
 */
@Data
public class InterfaceVO {

    /**
     * 接口ID
     */
    private String id;

    /**
     * 接口名称
     */
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
     * 分类ID
     */
    private String categoryId;

    /**
     * 分类名称
     */
    private String categoryName;

    /**
     * 分类信息
     */
    private CategoryInfo category;

    /**
     * 数据源信息
     */
    private DataSourceInfo dataSource;

    /**
     * 数据表名
     */
    private String tableName;

    /**
     * 请求方法
     */
    private String requestMethod;

    /**
     * 接口状态
     */
    private String status;

    /**
     * 状态显示名称
     */
    private String statusDisplay;

    /**
     * 接口版本
     */
    private String version;

    /**
     * 限流配置（每分钟请求数）
     */
    private Integer rateLimit;

    /**
     * 超时时间（秒）
     */
    private Integer timeout;

    /**
     * 参数列表
     */
    private List<ParameterInfo> parameters;

    /**
     * 创建时间
     */
    private LocalDateTime createTime;

    /**
     * 更新时间
     */
    private LocalDateTime updateTime;

    /**
     * 上架时间
     */
    private LocalDateTime publishTime;

    /**
     * 下架时间
     */
    private LocalDateTime offlineTime;

    /**
     * 创建人
     */
    private String createBy;

    /**
     * 创建人姓名
     */
    private String createByName;

    /**
     * 创建人姓名（别名）
     */
    private String creatorName;

    /**
     * 更新人
     */
    private String updateBy;

    /**
     * 上架人
     */
    private String publishBy;

    /**
     * 下架人
     */
    private String offlineBy;

    /**
     * 分类信息
     */
    @Data
    public static class CategoryInfo {
        private String id;
        private String categoryCode;
        private String categoryName;
        private String description;
        private String color;
    }

    /**
     * 数据源信息
     */
    @Data
    public static class DataSourceInfo {
        private String id;
        private String sourceName;
        private String sourceType;
        private String host;
        private Integer port;
        private String databaseName;
    }

    /**
     * 参数信息
     */
    @Data
    public static class ParameterInfo {
        private String id;
        private String paramName;
        private String paramType;
        private String paramLocation;
        private String description;
        private Boolean required;
        private String defaultValue;
        private String validationRule;
        private String example;
        private Integer sortOrder;
        
        // 手动添加setter方法
        public void setId(String id) {
            this.id = id;
        }
        
        public void setParamName(String paramName) {
            this.paramName = paramName;
        }
        
        public void setParamType(String paramType) {
            this.paramType = paramType;
        }
        
        public void setParamLocation(String paramLocation) {
            this.paramLocation = paramLocation;
        }
        
        public void setDescription(String description) {
            this.description = description;
        }
        
        public void setRequired(Boolean required) {
            this.required = required;
        }
        
        public void setDefaultValue(String defaultValue) {
            this.defaultValue = defaultValue;
        }
        
        public void setValidationRule(String validationRule) {
            this.validationRule = validationRule;
        }
        
        public void setExample(String example) {
            this.example = example;
        }
        
        public void setSortOrder(Integer sortOrder) {
            this.sortOrder = sortOrder;
        }
        
        public Integer getSortOrder() {
            return this.sortOrder;
        }
    }
    
    // 手动添加getter和setter方法
    public String getStatus() {
        return this.status;
    }
    
    public String getCategoryId() {
        return this.categoryId;
    }
    
    public void setCategoryId(String categoryId) {
        this.categoryId = categoryId;
    }
    
    public String getCategoryName() {
        return this.categoryName;
    }
    
    public void setCategoryName(String categoryName) {
        this.categoryName = categoryName;
    }
    
    public String getCreatorName() {
        return this.creatorName;
    }
    
    public void setCreatorName(String creatorName) {
        this.creatorName = creatorName;
    }
    
    public void setStatusDisplay(String statusDisplay) {
        this.statusDisplay = statusDisplay;
    }
    
    public void setParameters(List<ParameterInfo> parameters) {
        this.parameters = parameters;
    }
}