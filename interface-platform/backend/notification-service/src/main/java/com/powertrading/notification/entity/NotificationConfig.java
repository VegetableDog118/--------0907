package com.powertrading.notification.entity;

import com.baomidou.mybatisplus.annotation.*;
import com.fasterxml.jackson.annotation.JsonFormat;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.experimental.Accessors;

import javax.validation.constraints.NotBlank;
import java.io.Serializable;
import java.time.LocalDateTime;

/**
 * 通知配置实体类
 *
 * @author PowerTrading
 * @since 2024-01-15
 */
@Data
@EqualsAndHashCode(callSuper = false)
@Accessors(chain = true)
@TableName("notification_configs")
@Schema(description = "通知配置")
public class NotificationConfig implements Serializable {

    private static final long serialVersionUID = 1L;

    @Schema(description = "配置ID")
    @TableId(value = "id", type = IdType.ASSIGN_ID)
    private String id;

    @Schema(description = "用户ID")
    @TableField("user_id")
    @NotBlank(message = "用户ID不能为空")
    private String userId;

    @Schema(description = "通知类型")
    @TableField("notification_type")
    @NotBlank(message = "通知类型不能为空")
    private String notificationType;

    @Schema(description = "邮件通知开关：1-开启，0-关闭")
    @TableField("email_enabled")
    private Integer emailEnabled;

    @Schema(description = "站内消息开关：1-开启，0-关闭")
    @TableField("message_enabled")
    private Integer messageEnabled;

    @Schema(description = "短信通知开关：1-开启，0-关闭")
    @TableField("sms_enabled")
    private Integer smsEnabled;

    @Schema(description = "免打扰时间开始")
    @TableField("quiet_start_time")
    private String quietStartTime;

    @Schema(description = "免打扰时间结束")
    @TableField("quiet_end_time")
    private String quietEndTime;

    @Schema(description = "创建时间")
    @TableField(value = "create_time", fill = FieldFill.INSERT)
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime createTime;

    @Schema(description = "更新时间")
    @TableField(value = "update_time", fill = FieldFill.INSERT_UPDATE)
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime updateTime;

    /**
     * 开关状态枚举
     */
    public enum Switch {
        DISABLED(0, "关闭"),
        ENABLED(1, "开启");

        private final Integer code;
        private final String desc;

        Switch(Integer code, String desc) {
            this.code = code;
            this.desc = desc;
        }

        public Integer getCode() {
            return code;
        }

        public String getDesc() {
            return desc;
        }
    }
}