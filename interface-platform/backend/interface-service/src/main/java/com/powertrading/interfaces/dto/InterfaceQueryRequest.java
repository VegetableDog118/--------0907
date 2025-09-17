package com.powertrading.interfaces.dto;

import lombok.Data;

import javax.validation.constraints.Min;
import javax.validation.constraints.NotNull;
import java.time.LocalDateTime;
import java.util.List;

/**
 * 接口查询请求DTO
 *
 * @author PowerTrading Team
 * @since 2024-01-15
 */
@Data
public class InterfaceQueryRequest {

    /**
     * 页码
     */
    @NotNull(message = "页码不能为空")
    @Min(value = 1, message = "页码必须大于0")
    private Integer page = 1;

    /**
     * 每页大小
     */
    @NotNull(message = "每页大小不能为空")
    @Min(value = 1, message = "每页大小必须大于0")
    private Integer size = 10;

    /**
     * 接口名称（模糊搜索）
     */
    private String interfaceName;

    /**
     * 接口路径（精确搜索）
     */
    private String interfacePath;

    /**
     * 接口状态列表
     */
    private List<String> statusList;

    /**
     * 分类ID列表
     */
    private List<String> categoryIds;

    /**
     * 创建人列表
     */
    private List<String> createByList;

    /**
     * 创建时间开始
     */
    private LocalDateTime createTimeStart;

    /**
     * 创建时间结束
     */
    private LocalDateTime createTimeEnd;

    /**
     * 更新时间开始
     */
    private LocalDateTime updateTimeStart;

    /**
     * 更新时间结束
     */
    private LocalDateTime updateTimeEnd;

    /**
     * 关键词搜索（在名称、描述中搜索）
     */
    private String keyword;

    /**
     * 排序字段
     */
    private String sortField = "createTime";

    /**
     * 排序方向：asc-升序，desc-降序
     */
    private String sortDirection = "desc";

    /**
     * 是否只查询已发布的接口
     */
    private Boolean publishedOnly = false;

    /**
     * 数据源ID
     */
    private String dataSourceId;

    /**
     * 表名
     */
    private String tableName;
    
    // 手动添加getter方法以解决编译问题
    public Integer getPage() {
        return this.page;
    }
    
    public Integer getSize() {
        return this.size;
    }
}