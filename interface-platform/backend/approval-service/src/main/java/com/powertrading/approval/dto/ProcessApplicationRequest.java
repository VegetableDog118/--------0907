package com.powertrading.approval.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import javax.validation.constraints.NotBlank;
import javax.validation.constraints.NotEmpty;
import javax.validation.constraints.Pattern;
import java.io.Serializable;
import java.util.List;

/**
 * 处理申请请求DTO
 *
 * @author PowerTrading
 * @since 2024-01-15
 */
@Data
@Schema(description = "处理申请请求")
public class ProcessApplicationRequest implements Serializable {

    private static final long serialVersionUID = 1L;

    @Schema(description = "申请ID列表", required = true)
    @NotEmpty(message = "申请ID列表不能为空")
    private List<String> applicationIds;

    @Schema(description = "处理结果：approved-通过，rejected-拒绝", required = true)
    @NotBlank(message = "处理结果不能为空")
    @Pattern(regexp = "^(approved|rejected)$", message = "处理结果只能是approved或rejected")
    private String action;

    @Schema(description = "处理意见")
    private String comment;
}