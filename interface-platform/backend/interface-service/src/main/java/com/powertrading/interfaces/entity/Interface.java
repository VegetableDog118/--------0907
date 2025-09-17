package com.powertrading.interfaces.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.time.LocalDateTime;
import java.util.Map;

/**
 * 接口实体类
 *
 * @author PowerTrading Team
 * @since 2024-01-15
 */
@Data
@EqualsAndHashCode(callSuper = false)
@TableName("interfaces")
public class Interface {

    /**
     * 接口ID
     */
    @TableId(value = "id", type = IdType.ASSIGN_ID)
    private String id;

    /**
     * 接口名称
     */
    @TableField("interface_name")
    private String interfaceName;

    /**
     * 接口路径
     */
    @TableField("interface_path")
    private String interfacePath;

    /**
     * 接口描述
     */
    @TableField("description")
    private String description;

    /**
     * 分类ID
     */
    @TableField("category_id")
    private String categoryId;

    /**
     * 数据源ID
     */
    @TableField("data_source_id")
    private String dataSourceId;

    /**
     * 数据表名
     */
    @TableField("table_name")
    private String tableName;

    /**
     * 请求方法
     */
    @TableField("request_method")
    private String requestMethod;

    /**
     * 接口状态：unpublished-未上架，published-已上架，offline-已下架
     */
    @TableField("status")
    private String status;

    /**
     * 接口版本
     */
    @TableField("version")
    private String version;

    /**
     * SQL模板
     */
    @TableField("sql_template")
    private String sqlTemplate;

    /**
     * 响应格式定义
     */
    @TableField("response_format")
    private String responseFormat;

    /**
     * 限流配置（每分钟请求数）
     */
    @TableField("rate_limit")
    private Integer rateLimit;

    /**
     * 超时时间（秒）
     */
    @TableField("timeout")
    private Integer timeout;

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
     * 上架时间
     */
    @TableField("publish_time")
    private LocalDateTime publishTime;

    /**
     * 下架时间
     */
    @TableField("offline_time")
    private LocalDateTime offlineTime;

    /**
     * 创建人
     */
    @TableField(value = "create_by", fill = FieldFill.INSERT)
    private String createBy;

    /**
     * 更新人
     */
    @TableField(value = "update_by", fill = FieldFill.INSERT_UPDATE)
    private String updateBy;

    /**
     * 上架人
     */
    @TableField("publish_by")
    private String publishBy;

    /**
     * 下架人
     */
    @TableField("offline_by")
    private String offlineBy;

    /**
     * 下架原因
     */
    @TableField("offline_reason")
    private String offlineReason;

    /**
     * 逻辑删除标识 - 暂时注释掉，因为数据库表中没有deleted字段
     */
    // @TableField("deleted")
    // @TableLogic
    // private Integer deleted;

    // 接口状态常量
    public static final String STATUS_UNPUBLISHED = "unpublished";
    public static final String STATUS_PUBLISHED = "published";
    public static final String STATUS_OFFLINE = "offline";

    // 请求方法常量
    public static final String METHOD_GET = "GET";
    public static final String METHOD_POST = "POST";
    public static final String METHOD_PUT = "PUT";
    public static final String METHOD_DELETE = "DELETE";
    
    // 手动添加setter方法
    public void setId(String id) {
        this.id = id;
    }
    
    public void setOfflineTime(LocalDateTime offlineTime) {
        this.offlineTime = offlineTime;
    }
    
    public void setOfflineBy(String offlineBy) {
        this.offlineBy = offlineBy;
    }
    
    public void setOfflineReason(String offlineReason) {
        this.offlineReason = offlineReason;
    }
    
    public void setUpdateBy(String updateBy) {
        this.updateBy = updateBy;
    }
    
    public void setCreateBy(String createBy) {
        this.createBy = createBy;
    }
    
    public String getResponseFormat() {
        return this.responseFormat;
    }
    
    public String getInterfaceName() {
        return this.interfaceName;
    }
    
    public String getInterfacePath() {
        return this.interfacePath;
    }
    
    public String getDataSourceId() {
        return this.dataSourceId;
    }
    
    public String getTableName() {
        return this.tableName;
    }
    
    public String getSqlTemplate() {
        return this.sqlTemplate;
    }
    
    public String getId() {
        return this.id;
    }
    
    public String getStatus() {
        return this.status;
    }
    
    public String getRequestMethod() {
        return this.requestMethod;
    }
    
    public String getDescription() {
        return this.description;
    }
    
    public Integer getRateLimit() {
        return this.rateLimit;
    }
    
    public Integer getTimeout() {
        return this.timeout;
    }
    
    public void setInterfacePath(String interfacePath) {
        this.interfacePath = interfacePath;
    }
    
    public void setDescription(String description) {
        this.description = description;
    }
    
    public String getCategoryId() {
        return this.categoryId;
    }
    
    public void setCategoryId(String categoryId) {
        this.categoryId = categoryId;
    }
    
    public void setDataSourceId(String dataSourceId) {
        this.dataSourceId = dataSourceId;
    }
    
    public void setTableName(String tableName) {
        this.tableName = tableName;
    }
    
    public void setSqlTemplate(String sqlTemplate) {
        this.sqlTemplate = sqlTemplate;
    }
    
    public void setRequestMethod(String requestMethod) {
        this.requestMethod = requestMethod;
    }
    
    public void setStatus(String status) {
        this.status = status;
    }
    
    public void setVersion(String version) {
        this.version = version;
    }
    
    public void setResponseFormat(String responseFormat) {
        this.responseFormat = responseFormat;
    }
    
    public void setRateLimit(Integer rateLimit) {
        this.rateLimit = rateLimit;
    }
    
    public void setTimeout(Integer timeout) {
        this.timeout = timeout;
    }
    
    public void setInterfaceName(String interfaceName) {
        this.interfaceName = interfaceName;
    }
    
    public void setPublishTime(LocalDateTime publishTime) {
        this.publishTime = publishTime;
    }
    
    public void setPublishBy(String publishBy) {
        this.publishBy = publishBy;
    }
}