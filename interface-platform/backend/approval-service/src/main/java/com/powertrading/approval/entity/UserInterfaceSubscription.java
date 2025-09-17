package com.powertrading.approval.entity;

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
 * 用户接口订阅实体类
 *
 * @author PowerTrading
 * @since 2024-01-15
 */
@Data
@EqualsAndHashCode(callSuper = false)
@Accessors(chain = true)
@TableName("user_interface_subscriptions")
@Schema(description = "用户接口订阅")
public class UserInterfaceSubscription implements Serializable {

    private static final long serialVersionUID = 1L;

    @Schema(description = "ID")
    @TableId(value = "id", type = IdType.ASSIGN_ID)
    private String id;

    @Schema(description = "用户ID")
    @TableField("user_id")
    @NotBlank(message = "用户ID不能为空")
    private String userId;

    @Schema(description = "接口ID")
    @TableField("interface_id")
    @NotBlank(message = "接口ID不能为空")
    private String interfaceId;

    @Schema(description = "申请ID")
    @TableField("application_id")
    @NotBlank(message = "申请ID不能为空")
    private String applicationId;

    @Schema(description = "订阅状态：active-活跃，inactive-非活跃，expired-已过期，cancelled-已取消")
    @TableField("status")
    private String status;

    @Schema(description = "订阅时间")
    @TableField("subscribe_time")
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime subscribeTime;

    @Schema(description = "过期时间")
    @TableField("expire_time")
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime expireTime;

    @Schema(description = "取消时间")
    @TableField("cancel_time")
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime cancelTime;

    @Schema(description = "调用次数")
    @TableField("call_count")
    private Integer callCount;

    @Schema(description = "最后调用时间")
    @TableField("last_call_time")
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime lastCallTime;

    @Schema(description = "创建时间")
    @TableField(value = "create_time", fill = FieldFill.INSERT)
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime createTime;

    @Schema(description = "更新时间")
    @TableField(value = "update_time", fill = FieldFill.INSERT_UPDATE)
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime updateTime;

    /**
     * 订阅状态枚举
     */
    public enum Status {
        ACTIVE("active", "活跃"),
        INACTIVE("inactive", "非活跃"),
        EXPIRED("expired", "已过期"),
        CANCELLED("cancelled", "已取消");

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