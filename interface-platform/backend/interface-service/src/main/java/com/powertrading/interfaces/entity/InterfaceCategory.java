package com.powertrading.interfaces.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.time.LocalDateTime;

/**
 * 接口分类实体类
 *
 * @author PowerTrading Team
 * @since 2024-01-15
 */
@Data
@EqualsAndHashCode(callSuper = false)
@TableName("interface_categories")
public class InterfaceCategory {

    /**
     * 分类ID
     */
    @TableId(value = "id", type = IdType.ASSIGN_ID)
    private String id;

    /**
     * 分类编码
     */
    @TableField("category_code")
    private String categoryCode;

    /**
     * 分类名称
     */
    @TableField("category_name")
    private String categoryName;

    /**
     * 分类描述
     */
    @TableField("description")
    private String description;

    /**
     * 分类颜色
     */
    @TableField("color")
    private String color;

    /**
     * 排序
     */
    @TableField("sort_order")
    private Integer sortOrder;

    /**
     * 状态：1-启用，0-禁用
     */
    @TableField("status")
    private Integer status;

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

    /**
     * 创建人
     */
    @TableField("create_by")
    private String createBy;

    /**
     * 更新人
     */
    @TableField("update_by")
    private String updateBy;

    /**
     * 接口数量（非数据库字段）
     */
    @TableField(exist = false)
    private Integer interfaceCount;

    // 状态常量
    public static final Integer STATUS_ENABLED = 1;
    public static final Integer STATUS_DISABLED = 0;

    // 预定义分类常量
    public static final String CATEGORY_DAY_AHEAD_SPOT = "day_ahead_spot";
    public static final String CATEGORY_FORECAST = "forecast";
    public static final String CATEGORY_ANCILLARY_SERVICE = "ancillary_service";
    public static final String CATEGORY_GRID_OPERATION = "grid_operation";
    public static final String CATEGORY_BASIC_DATA = "basic_data";
    public static final String CATEGORY_BUSINESS_DATA = "business_data";
    public static final String CATEGORY_STATISTICAL_DATA = "statistical_data";
    
    // 手动添加setter方法以解决编译问题
    public void setId(String id) {
        this.id = id;
    }
    
    public void setCategoryCode(String categoryCode) {
        this.categoryCode = categoryCode;
    }
    
    public void setCategoryName(String categoryName) {
        this.categoryName = categoryName;
    }
    
    public void setDescription(String description) {
        this.description = description;
    }
    
    public void setStatus(Integer status) {
        this.status = status;
    }
    
    public void setSortOrder(Integer sortOrder) {
        this.sortOrder = sortOrder;
    }
    
    public void setCreateBy(String createBy) {
        this.createBy = createBy;
    }
    
    public void setUpdateBy(String updateBy) {
        this.updateBy = updateBy;
    }
    
    public void setColor(String color) {
        this.color = color;
    }
    
    public void setColorCode(String colorCode) {
        this.color = colorCode;
    }
    
    // 手动添加getter方法
    public String getCategoryCode() {
        return this.categoryCode;
    }
    
    public String getCategoryName() {
        return this.categoryName;
    }
    
    public String getColor() {
        return this.color;
    }
    
    public String getColorCode() {
        return this.color;
    }
    
    public void setInterfaceCount(Integer interfaceCount) {
        this.interfaceCount = interfaceCount;
    }
    
    public Integer getInterfaceCount() {
        return this.interfaceCount;
    }
}