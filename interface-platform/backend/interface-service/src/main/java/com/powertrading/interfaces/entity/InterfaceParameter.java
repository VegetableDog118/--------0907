package com.powertrading.interfaces.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.time.LocalDateTime;

/**
 * 接口参数实体类
 *
 * @author PowerTrading Team
 * @since 2024-01-15
 */
@Data
@EqualsAndHashCode(callSuper = false)
@TableName("interface_parameters")
public class InterfaceParameter {

    /**
     * 参数ID
     */
    @TableId(value = "id", type = IdType.ASSIGN_ID)
    private String id;

    /**
     * 接口ID
     */
    @TableField("interface_id")
    private String interfaceId;

    /**
     * 参数名称
     */
    @TableField("param_name")
    private String paramName;

    /**
     * 参数类型
     */
    @TableField("param_type")
    private String paramType;

    /**
     * 参数位置
     */
    @TableField("param_location")
    private String paramLocation;

    /**
     * 参数描述
     */
    @TableField("description")
    private String description;

    /**
     * 是否必需
     */
    @TableField("required")
    private Boolean required;

    /**
     * 默认值
     */
    @TableField("default_value")
    private String defaultValue;

    /**
     * 校验规则
     */
    @TableField("validation_rule")
    private String validationRule;

    /**
     * 示例值
     */
    @TableField("example")
    private String example;

    /**
     * 排序
     */
    @TableField("sort_order")
    private Integer sortOrder;

    /**
     * 创建时间
     */
    @TableField(value = "create_time", fill = FieldFill.INSERT)
    private LocalDateTime createTime;

    /**
     * 更新时间
     */
    @TableField(value = "update_time", fill = FieldFill.INSERT_UPDATE)
    private LocalDateTime updateTime;

    // 参数类型常量
    public static final String TYPE_STRING = "string";
    public static final String TYPE_INTEGER = "integer";
    public static final String TYPE_NUMBER = "number";
    public static final String TYPE_BOOLEAN = "boolean";
    public static final String TYPE_DATE = "date";
    public static final String TYPE_DATETIME = "datetime";

    // 参数位置常量
    public static final String LOCATION_QUERY = "query";
    public static final String LOCATION_BODY = "body";
    public static final String LOCATION_HEADER = "header";
    public static final String LOCATION_PATH = "path";
    
    // 手动添加缺失的setter方法
    public void setId(String id) {
        this.id = id;
    }
    
    public void setInterfaceId(String interfaceId) {
        this.interfaceId = interfaceId;
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
    
    // 手动添加缺失的getter方法
    public String getParamLocation() {
        return this.paramLocation;
    }
    
    public String getDescription() {
        return this.description;
    }
    
    public Boolean getRequired() {
        return this.required;
    }
    
    public String getDefaultValue() {
        return this.defaultValue;
    }
    
    public String getValidationRule() {
        return this.validationRule;
    }
    
    public String getExample() {
        return this.example;
    }
    
    public Integer getSortOrder() {
        return this.sortOrder;
    }
    
    public String getId() {
        return this.id;
    }
    
    public String getParamName() {
        return this.paramName;
    }
    
    public String getParamType() {
        return this.paramType;
    }
}