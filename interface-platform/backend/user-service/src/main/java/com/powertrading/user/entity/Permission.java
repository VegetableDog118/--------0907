package com.powertrading.user.entity;

import com.baomidou.mybatisplus.annotation.*;
import io.swagger.v3.oas.annotations.media.Schema;

import javax.validation.constraints.NotBlank;
import javax.validation.constraints.Size;
import java.time.LocalDateTime;

/**
 * 权限实体类
 *
 * @author PowerTrading Team
 * @since 2024-01-15
 */
@TableName("permissions")
@Schema(description = "权限实体")
public class Permission {

    @TableId(value = "permission_id", type = IdType.ASSIGN_ID)
    @Schema(description = "权限ID")
    private String permissionId;

    @TableField("permission_name")
    @NotBlank(message = "权限名称不能为空")
    @Size(max = 50, message = "权限名称长度不能超过50字符")
    @Schema(description = "权限名称", example = "用户管理")
    private String permissionName;

    @TableField("permission_code")
    @NotBlank(message = "权限编码不能为空")
    @Size(max = 100, message = "权限编码长度不能超过100字符")
    @Schema(description = "权限编码", example = "user:read")
    private String permissionCode;

    @TableField("resource_type")
    @NotBlank(message = "资源类型不能为空")
    @Schema(description = "资源类型", example = "API")
    private String resourceType;

    @TableField("resource_path")
    @Size(max = 200, message = "资源路径长度不能超过200字符")
    @Schema(description = "资源路径", example = "/api/v1/users/**")
    private String resourcePath;

    @TableField("parent_id")
    @Schema(description = "父权限ID")
    private String parentId;

    @TableField("sort_order")
    @Schema(description = "排序", example = "1")
    private Integer sortOrder;

    @TableField("description")
    @Size(max = 200, message = "权限描述长度不能超过200字符")
    @Schema(description = "权限描述", example = "查看用户信息")
    private String description;

    @TableField("status")
    @Schema(description = "状态", example = "ACTIVE")
    private String status;

    @TableField(value = "create_time", fill = FieldFill.INSERT)
    @Schema(description = "创建时间")
    private LocalDateTime createTime;

    @TableField(value = "update_time", fill = FieldFill.INSERT_UPDATE)
    @Schema(description = "更新时间")
    private LocalDateTime updateTime;

    @TableLogic
    @TableField("deleted")
    @Schema(description = "逻辑删除标志")
    private Integer deleted;

    // Constructors
    public Permission() {}

    public Permission(String permissionName, String permissionCode, String resourceType) {
        this.permissionName = permissionName;
        this.permissionCode = permissionCode;
        this.resourceType = resourceType;
        this.status = "ACTIVE";
        this.sortOrder = 0;
    }

    // Getters and Setters
    public String getPermissionId() {
        return permissionId;
    }

    public void setPermissionId(String permissionId) {
        this.permissionId = permissionId;
    }

    public String getPermissionName() {
        return permissionName;
    }

    public void setPermissionName(String permissionName) {
        this.permissionName = permissionName;
    }

    public String getPermissionCode() {
        return permissionCode;
    }

    public void setPermissionCode(String permissionCode) {
        this.permissionCode = permissionCode;
    }

    public String getResourceType() {
        return resourceType;
    }

    public void setResourceType(String resourceType) {
        this.resourceType = resourceType;
    }

    public String getResourcePath() {
        return resourcePath;
    }

    public void setResourcePath(String resourcePath) {
        this.resourcePath = resourcePath;
    }

    public String getParentId() {
        return parentId;
    }

    public void setParentId(String parentId) {
        this.parentId = parentId;
    }

    public Integer getSortOrder() {
        return sortOrder;
    }

    public void setSortOrder(Integer sortOrder) {
        this.sortOrder = sortOrder;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    public LocalDateTime getCreateTime() {
        return createTime;
    }

    public void setCreateTime(LocalDateTime createTime) {
        this.createTime = createTime;
    }

    public LocalDateTime getUpdateTime() {
        return updateTime;
    }

    public void setUpdateTime(LocalDateTime updateTime) {
        this.updateTime = updateTime;
    }

    public Integer getDeleted() {
        return deleted;
    }

    public void setDeleted(Integer deleted) {
        this.deleted = deleted;
    }

    @Override
    public String toString() {
        return "Permission{" +
                "permissionId='" + permissionId + '\'' +
                ", permissionName='" + permissionName + '\'' +
                ", permissionCode='" + permissionCode + '\'' +
                ", resourceType='" + resourceType + '\'' +
                ", resourcePath='" + resourcePath + '\'' +
                ", parentId='" + parentId + '\'' +
                ", sortOrder=" + sortOrder +
                ", description='" + description + '\'' +
                ", status='" + status + '\'' +
                ", createTime=" + createTime +
                ", updateTime=" + updateTime +
                ", deleted=" + deleted +
                '}';
    }
}