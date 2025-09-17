package com.powertrading.approval.entity;

import com.baomidou.mybatisplus.annotation.*;
import com.fasterxml.jackson.annotation.JsonFormat;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.experimental.Accessors;

import javax.validation.constraints.NotBlank;
import javax.validation.constraints.NotEmpty;
import javax.validation.constraints.NotNull;
import java.io.Serializable;
import java.time.LocalDateTime;
import java.util.List;

/**
 * 订阅申请实体类
 *
 * @author PowerTrading
 * @since 2024-01-15
 */
@Data
@EqualsAndHashCode(callSuper = false)
@Accessors(chain = true)
@TableName("subscription_applications")
@Schema(description = "订阅申请")
public class SubscriptionApplication implements Serializable {

    private static final long serialVersionUID = 1L;

    @Schema(description = "申请ID")
    @TableId(value = "id", type = IdType.ASSIGN_ID)
    private String id;

    @Schema(description = "申请用户ID")
    @TableField("user_id")
    @NotBlank(message = "申请用户ID不能为空")
    private String userId;

    @Schema(description = "申请接口ID列表")
    @TableField(value = "interface_ids", typeHandler = com.baomidou.mybatisplus.extension.handlers.JacksonTypeHandler.class)
    @NotEmpty(message = "申请接口ID列表不能为空")
    private List<String> interfaceIds;

    @Schema(description = "申请理由")
    @TableField("reason")
    @NotBlank(message = "申请理由不能为空")
    private String reason;

    @Schema(description = "业务场景描述")
    @TableField("business_scenario")
    private String businessScenario;

    @Schema(description = "预计每日调用次数")
    @TableField("estimated_calls")
    private Integer estimatedCalls;

    @Schema(description = "申请状态：pending-待审批，approved-已通过，rejected-已拒绝")
    @TableField("status")
    private String status;

    @Schema(description = "提交时间")
    @TableField("submit_time")
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime submitTime;

    @Schema(description = "处理时间")
    @TableField("process_time")
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime processTime;

    @Schema(description = "处理人")
    @TableField("process_by")
    private String processBy;

    @Schema(description = "处理意见")
    @TableField("process_comment")
    private String processComment;

    @Schema(description = "创建时间")
    @TableField(value = "create_time", fill = FieldFill.INSERT)
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime createTime;

    @Schema(description = "更新时间")
    @TableField(value = "update_time", fill = FieldFill.INSERT_UPDATE)
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime updateTime;

    /**
     * 申请状态枚举
     */
    public enum Status {
        PENDING("pending", "待审批"),
        APPROVED("approved", "已通过"),
        REJECTED("rejected", "已拒绝");

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