package com.powertrading.approval.dto;

import com.fasterxml.jackson.annotation.JsonFormat;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import javax.validation.constraints.Min;
import javax.validation.constraints.Pattern;
import java.io.Serializable;
import java.time.LocalDateTime;

/**
 * 申请查询请求DTO
 *
 * @author PowerTrading
 * @since 2024-01-15
 */
@Data
@Schema(description = "申请查询请求")
public class ApplicationQueryRequest implements Serializable {

    private static final long serialVersionUID = 1L;

    @Schema(description = "页码", example = "1")
    @Min(value = 1, message = "页码必须大于0")
    private Integer pageNum = 1;

    @Schema(description = "每页大小", example = "10")
    @Min(value = 1, message = "每页大小必须大于0")
    private Integer pageSize = 10;

    @Schema(description = "用户ID")
    private String userId;

    @Schema(description = "申请状态：pending-待审批，approved-已通过，rejected-已拒绝")
    @Pattern(regexp = "^(pending|approved|rejected)$", message = "申请状态只能是pending、approved或rejected")
    private String status;

    @Schema(description = "开始时间")
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime startTime;

    @Schema(description = "结束时间")
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime endTime;

    @Schema(description = "关键词搜索")
    private String keyword;
}