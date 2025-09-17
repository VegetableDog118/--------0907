package com.powertrading.approval.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import javax.validation.constraints.NotBlank;
import javax.validation.constraints.NotEmpty;
import javax.validation.constraints.NotNull;
import javax.validation.constraints.Positive;
import java.io.Serializable;
import java.util.List;

/**
 * 提交申请请求DTO
 *
 * @author PowerTrading
 * @since 2024-01-15
 */
@Data
@Schema(description = "提交申请请求")
public class SubmitApplicationRequest implements Serializable {

    private static final long serialVersionUID = 1L;

    @Schema(description = "申请接口ID列表", required = true)
    @NotEmpty(message = "申请接口ID列表不能为空")
    private List<String> interfaceIds;

    @Schema(description = "申请理由", required = true)
    @NotBlank(message = "申请理由不能为空")
    private String reason;

    @Schema(description = "业务场景描述")
    private String businessScenario;

    @Schema(description = "预计每日调用次数")
    @Positive(message = "预计每日调用次数必须大于0")
    private Integer estimatedCalls;
}