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
import java.util.Map;

/**
 * 通知模板实体类
 *
 * @author PowerTrading
 * @since 2024-01-15
 */
@Data
@EqualsAndHashCode(callSuper = false)
@Accessors(chain = true)
@TableName("notification_templates")
@Schema(description = "通知模板")
public class NotificationTemplate implements Serializable {

    private static final long serialVersionUID = 1L;

    @Schema(description = "模板ID")
    @TableId(value = "id", type = IdType.ASSIGN_ID)
    private String id;

    @Schema(description = "模板编码")
    @TableField("template_code")
    @NotBlank(message = "模板编码不能为空")
    private String templateCode;

    @Schema(description = "模板名称")
    @TableField("template_name")
    @NotBlank(message = "模板名称不能为空")
    private String templateName;

    @Schema(description = "模板类型：email-邮件，sms-短信，system-系统")
    @TableField("template_type")
    @NotBlank(message = "模板类型不能为空")
    private String templateType;

    @Schema(description = "标题模板")
    @TableField("title_template")
    @NotBlank(message = "标题模板不能为空")
    private String titleTemplate;

    @Schema(description = "内容模板")
    @TableField("content_template")
    @NotBlank(message = "内容模板不能为空")
    private String contentTemplate;

    @Schema(description = "模板变量定义")
    @TableField("variables")
    private Map<String, Object> variables;

    @Schema(description = "状态：1-启用，0-禁用")
    @TableField("status")
    private Integer status;

    @Schema(description = "创建时间")
    @TableField(value = "create_time", fill = FieldFill.INSERT)
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime createTime;

    @Schema(description = "更新时间")
    @TableField(value = "update_time", fill = FieldFill.INSERT_UPDATE)
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime updateTime;

    /**
     * 模板类型枚举
     */
    public enum Type {
        EMAIL("email", "邮件"),
        SMS("sms", "短信"),
        SYSTEM("system", "系统");

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
     * 状态枚举
     */
    public enum Status {
        DISABLED(0, "禁用"),
        ENABLED(1, "启用");

        private final Integer code;
        private final String desc;

        Status(Integer code, String desc) {
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