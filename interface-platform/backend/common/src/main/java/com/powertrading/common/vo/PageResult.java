package com.powertrading.common.vo;

import lombok.Data;
import lombok.EqualsAndHashCode;

import java.util.List;

/**
 * 分页响应结果类
 *
 * @param <T> 数据类型
 */
@Data
@EqualsAndHashCode(callSuper = true)
public class PageResult<T> extends Result<List<T>> {

    /**
     * 当前页码
     */
    private Integer currentPage;

    /**
     * 每页大小
     */
    private Integer pageSize;

    /**
     * 总记录数
     */
    private Long total;

    /**
     * 总页数
     */
    private Integer totalPages;

    /**
     * 是否有下一页
     */
    private Boolean hasNext;

    /**
     * 是否有上一页
     */
    private Boolean hasPrevious;

    public PageResult() {
        super();
    }

    public PageResult(List<T> data, Integer currentPage, Integer pageSize, Long total) {
        super(200, "查询成功", data);
        this.currentPage = currentPage;
        this.pageSize = pageSize;
        this.total = total;
        this.totalPages = (int) Math.ceil((double) total / pageSize);
        this.hasNext = currentPage < totalPages;
        this.hasPrevious = currentPage > 1;
    }

    /**
     * 成功分页响应
     */
    public static <T> PageResult<T> success(List<T> data, Integer currentPage, Integer pageSize, Long total) {
        return new PageResult<>(data, currentPage, pageSize, total);
    }

    /**
     * 空分页响应
     */
    public static <T> PageResult<T> empty(Integer currentPage, Integer pageSize) {
        return new PageResult<>(List.of(), currentPage, pageSize, 0L);
    }
}