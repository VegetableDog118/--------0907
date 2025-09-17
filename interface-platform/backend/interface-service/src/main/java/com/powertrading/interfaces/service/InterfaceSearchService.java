package com.powertrading.interfaces.service;

import com.powertrading.interfaces.entity.Interface;
import com.powertrading.interfaces.mapper.InterfaceMapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;
import java.time.LocalDateTime;
import java.util.*;
import java.util.stream.Collectors;

/**
 * 接口搜索服务
 *
 * @author PowerTrading Team
 * @since 2024-01-15
 */
@Service
public class InterfaceSearchService {

    @Autowired
    private InterfaceMapper interfaceMapper;

    /**
     * 搜索接口
     *
     * @param searchRequest 搜索请求
     * @return 搜索结果
     */
    public InterfaceSearchResult searchInterfaces(InterfaceSearchRequest searchRequest) {
        // 暂时返回空结果，需要实现具体的MyBatis Plus查询逻辑
        InterfaceSearchResult result = new InterfaceSearchResult();
        result.setInterfaces(Collections.emptyList());
        result.setTotalCount(0);
        result.setTotalPages(0);
        result.setCurrentPage(1);
        result.setPageSize(searchRequest.getSize());
        result.setHasNext(false);
        result.setHasPrevious(false);
        
        // 添加搜索统计信息
        result.setSearchStats(buildSearchStats(searchRequest, 0));
        
        return result;
    }



    /**
     * 构建搜索统计信息
     */
    private SearchStats buildSearchStats(InterfaceSearchRequest request, long totalCount) {
        SearchStats stats = new SearchStats();
        stats.setTotalCount(totalCount);
        stats.setSearchTime(System.currentTimeMillis());
        stats.setHasKeyword(StringUtils.hasText(request.getKeyword()));
        stats.setHasFilters(hasFilters(request));
        
        return stats;
    }

    /**
     * 检查是否有筛选条件
     */
    private boolean hasFilters(InterfaceSearchRequest request) {
        return request.getCategoryId() != null ||
               StringUtils.hasText(request.getStatus()) ||
               request.getDataSourceId() != null ||
               StringUtils.hasText(request.getCreatedBy()) ||
               request.getCreateTimeStart() != null ||
               request.getCreateTimeEnd() != null ||
               request.getUpdateTimeStart() != null ||
               request.getUpdateTimeEnd() != null ||
               (request.getTags() != null && !request.getTags().isEmpty());
    }

    /**
     * 获取搜索建议
     *
     * @param keyword 关键词
     * @return 搜索建议列表
     */
    public List<String> getSearchSuggestions(String keyword) {
        if (!StringUtils.hasText(keyword) || keyword.length() < 2) {
            return Collections.emptyList();
        }
        
        // 从接口名称中获取建议（暂时返回空列表，需要实现具体的查询逻辑）
        List<String> nameSuggestions = Collections.emptyList();
        
        // 从描述中获取建议（暂时返回空列表，需要实现具体的查询逻辑）
        List<String> descSuggestions = Collections.emptyList();
        
        // 合并并去重
        Set<String> allSuggestions = new LinkedHashSet<>();
        allSuggestions.addAll(nameSuggestions);
        allSuggestions.addAll(descSuggestions);
        
        return allSuggestions.stream()
            .limit(10)
            .collect(Collectors.toList());
    }

    /**
     * 获取热门搜索关键词
     *
     * @return 热门关键词列表
     */
    public List<String> getPopularKeywords() {
        // 这里可以从搜索日志中统计热门关键词
        // 暂时返回一些预定义的热门关键词
        return Arrays.asList(
            "数据查询", "统计报表", "实时数据", "历史数据", 
            "用户信息", "订单数据", "财务数据", "系统监控"
        );
    }

    /**
     * 获取筛选选项
     *
     * @return 筛选选项
     */
    public FilterOptions getFilterOptions() {
        FilterOptions options = new FilterOptions();
        
        // 获取所有状态选项
        options.setStatusOptions(Arrays.asList(
            new FilterOption("DRAFT", "草稿"),
            new FilterOption("PUBLISHED", "已发布"),
            new FilterOption("OFFLINE", "已下线")
        ));
        
        // 获取所有分类选项（这里应该从分类服务获取）
        options.setCategoryOptions(Arrays.asList(
            new FilterOption("1", "数据查询"),
            new FilterOption("2", "统计分析"),
            new FilterOption("3", "系统管理")
        ));
        
        // 获取排序选项
        options.setSortOptions(Arrays.asList(
            new FilterOption("updateTime", "更新时间"),
            new FilterOption("createTime", "创建时间"),
            new FilterOption("interfaceName", "接口名称"),
            new FilterOption("callCount", "调用次数")
        ));
        
        return options;
    }

    /**
     * 接口搜索请求
     */
    public static class InterfaceSearchRequest {
        private String keyword;
        private Long categoryId;
        private String status;
        private Long dataSourceId;
        private String createdBy;
        private LocalDateTime createTimeStart;
        private LocalDateTime createTimeEnd;
        private LocalDateTime updateTimeStart;
        private LocalDateTime updateTimeEnd;
        private List<String> tags;
        private String sortBy = "updateTime";
        private String sortOrder = "desc";
        private int page = 1;
        private int size = 20;
        
        // Getters and Setters
        public String getKeyword() { return keyword; }
        public void setKeyword(String keyword) { this.keyword = keyword; }
        public Long getCategoryId() { return categoryId; }
        public void setCategoryId(Long categoryId) { this.categoryId = categoryId; }
        public String getStatus() { return status; }
        public void setStatus(String status) { this.status = status; }
        public Long getDataSourceId() { return dataSourceId; }
        public void setDataSourceId(Long dataSourceId) { this.dataSourceId = dataSourceId; }
        public String getCreatedBy() { return createdBy; }
        public void setCreatedBy(String createdBy) { this.createdBy = createdBy; }
        public LocalDateTime getCreateTimeStart() { return createTimeStart; }
        public void setCreateTimeStart(LocalDateTime createTimeStart) { this.createTimeStart = createTimeStart; }
        public LocalDateTime getCreateTimeEnd() { return createTimeEnd; }
        public void setCreateTimeEnd(LocalDateTime createTimeEnd) { this.createTimeEnd = createTimeEnd; }
        public LocalDateTime getUpdateTimeStart() { return updateTimeStart; }
        public void setUpdateTimeStart(LocalDateTime updateTimeStart) { this.updateTimeStart = updateTimeStart; }
        public LocalDateTime getUpdateTimeEnd() { return updateTimeEnd; }
        public void setUpdateTimeEnd(LocalDateTime updateTimeEnd) { this.updateTimeEnd = updateTimeEnd; }
        public List<String> getTags() { return tags; }
        public void setTags(List<String> tags) { this.tags = tags; }
        public String getSortBy() { return sortBy; }
        public void setSortBy(String sortBy) { this.sortBy = sortBy; }
        public String getSortOrder() { return sortOrder; }
        public void setSortOrder(String sortOrder) { this.sortOrder = sortOrder; }
        public int getPage() { return page; }
        public void setPage(int page) { this.page = page; }
        public int getSize() { return size; }
        public void setSize(int size) { this.size = size; }
    }

    /**
     * 接口搜索结果
     */
    public static class InterfaceSearchResult {
        private List<Interface> interfaces;
        private long totalCount;
        private int totalPages;
        private int currentPage;
        private int pageSize;
        private boolean hasNext;
        private boolean hasPrevious;
        private SearchStats searchStats;
        
        // Getters and Setters
        public List<Interface> getInterfaces() { return interfaces; }
        public void setInterfaces(List<Interface> interfaces) { this.interfaces = interfaces; }
        public long getTotalCount() { return totalCount; }
        public void setTotalCount(long totalCount) { this.totalCount = totalCount; }
        public int getTotalPages() { return totalPages; }
        public void setTotalPages(int totalPages) { this.totalPages = totalPages; }
        public int getCurrentPage() { return currentPage; }
        public void setCurrentPage(int currentPage) { this.currentPage = currentPage; }
        public int getPageSize() { return pageSize; }
        public void setPageSize(int pageSize) { this.pageSize = pageSize; }
        public boolean isHasNext() { return hasNext; }
        public void setHasNext(boolean hasNext) { this.hasNext = hasNext; }
        public boolean isHasPrevious() { return hasPrevious; }
        public void setHasPrevious(boolean hasPrevious) { this.hasPrevious = hasPrevious; }
        public SearchStats getSearchStats() { return searchStats; }
        public void setSearchStats(SearchStats searchStats) { this.searchStats = searchStats; }
    }

    /**
     * 搜索统计信息
     */
    public static class SearchStats {
        private long totalCount;
        private long searchTime;
        private boolean hasKeyword;
        private boolean hasFilters;
        
        // Getters and Setters
        public long getTotalCount() { return totalCount; }
        public void setTotalCount(long totalCount) { this.totalCount = totalCount; }
        public long getSearchTime() { return searchTime; }
        public void setSearchTime(long searchTime) { this.searchTime = searchTime; }
        public boolean isHasKeyword() { return hasKeyword; }
        public void setHasKeyword(boolean hasKeyword) { this.hasKeyword = hasKeyword; }
        public boolean isHasFilters() { return hasFilters; }
        public void setHasFilters(boolean hasFilters) { this.hasFilters = hasFilters; }
    }

    /**
     * 筛选选项
     */
    public static class FilterOptions {
        private List<FilterOption> statusOptions;
        private List<FilterOption> categoryOptions;
        private List<FilterOption> sortOptions;
        
        // Getters and Setters
        public List<FilterOption> getStatusOptions() { return statusOptions; }
        public void setStatusOptions(List<FilterOption> statusOptions) { this.statusOptions = statusOptions; }
        public List<FilterOption> getCategoryOptions() { return categoryOptions; }
        public void setCategoryOptions(List<FilterOption> categoryOptions) { this.categoryOptions = categoryOptions; }
        public List<FilterOption> getSortOptions() { return sortOptions; }
        public void setSortOptions(List<FilterOption> sortOptions) { this.sortOptions = sortOptions; }
    }

    /**
     * 筛选选项
     */
    public static class FilterOption {
        private String value;
        private String label;
        
        public FilterOption() {}
        
        public FilterOption(String value, String label) {
            this.value = value;
            this.label = label;
        }
        
        // Getters and Setters
        public String getValue() { return value; }
        public void setValue(String value) { this.value = value; }
        public String getLabel() { return label; }
        public void setLabel(String label) { this.label = label; }
    }
}