package com.powertrading.notification.entity;

import com.baomidou.mybatisplus.annotation.*;
import com.fasterxml.jackson.annotation.JsonFormat;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.experimental.Accessors;

import javax.validation.constraints.NotBlank;
import javax.validation.constraints.NotNull;
import java.io.Serializable;
import java.time.LocalDateTime;

/**
 * 通知实体类
 *
 * @author PowerTrading
 * @since 2024-01-15
 */
@Data
@EqualsAndHashCode(callSuper = false)
@Accessors(chain = true)
@TableName("notifications")
@Schema(description = "通知")
public class Notification implements Serializable {

    private static final long serialVersionUID = 1L;

    @Schema(description = "通知ID")
    @TableId(value = "id", type = IdType.ASSIGN_ID)
    private String id;

    @Schema(description = "接收用户ID")
    @TableField("user_id")
    @NotBlank(message = "接收用户ID不能为空")
    private String userId;

    @Schema(description = "通知标题")
    @TableField("title")
    @NotBlank(message = "通知标题不能为空")
    private String title;

    @Schema(description = "通知内容")
    @TableField("content")
    @NotBlank(message = "通知内容不能为空")
    private String content;

    @Schema(description = "通知类型：system-系统通知，approval-审批通知，subscription-订阅通知，api-API通知")
    @TableField("type")
    @NotBlank(message = "通知类型不能为空")
    private String type;

    @Schema(description = "阅读状态：unread-未读，read-已读")
    @TableField("status")
    private String status;

    @Schema(description = "发送时间")
    @TableField("send_time")
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime sendTime;

    @Schema(description = "阅读时间")
    @TableField("read_time")
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime readTime;

    @Schema(description = "创建时间")
    @TableField(value = "create_time", fill = FieldFill.INSERT)
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime createTime;

    /**
     * 通知类型枚举
     */
    public enum Type {
        SYSTEM("system", "系统通知"),
        APPROVAL("approval", "审批通知"),
        SUBSCRIPTION("subscription", "订阅通知"),
        API("api", "API通知");

        private final String code;
        private final String desc;

        Type(String code, String desc) {
            this.code = code;
            this.desc = desc;
        }

        public String getCode() {
            return code;
        }

        public String getDesc() {
            return desc;
        }
    }

    /**
     * 阅读状态枚举
     */
    public enum Status {
        UNREAD("unread", "未读"),
        READ("read", "已读");

        private final String code;
        private final String desc;

        Status(String code, String desc) {
            this.code = code;
            this.desc = desc;
        }

        public String getCode() {
            return code;
        }

        public String getDesc() {
            return desc;
        }
    }
}