package com.powertrading.interfaces.controller;

import com.powertrading.interfaces.service.InterfaceSearchService;
import com.powertrading.interfaces.service.InterfaceSearchService.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import javax.validation.Valid;
import java.time.LocalDateTime;
import java.util.List;

/**
 * 接口搜索控制器
 *
 * @author PowerTrading Team
 * @since 2024-01-15
 */
@RestController
@RequestMapping("/api/interfaces/search")
@CrossOrigin(origins = "*")
public class InterfaceSearchController {

    @Autowired
    private InterfaceSearchService interfaceSearchService;

    /**
     * 搜索接口
     *
     * @param request 搜索请求
     * @return 搜索结果
     */
    @PostMapping
    @PreAuthorize("hasRole('USER')")
    public ResponseEntity<ApiResponse<InterfaceSearchResult>> searchInterfaces(
            @Valid @RequestBody SearchRequest request) {
        try {
            // 转换请求对象
            InterfaceSearchRequest searchRequest = convertToSearchRequest(request);
            
            // 执行搜索
            InterfaceSearchResult result = interfaceSearchService.searchInterfaces(searchRequest);
            
            return ResponseEntity.ok(ApiResponse.success(result));
        } catch (Exception e) {
            return ResponseEntity.badRequest()
                .body(ApiResponse.error("搜索失败: " + e.getMessage()));
        }
    }

    /**
     * 快速搜索（GET方式）
     *
     * @param keyword 关键词
     * @param categoryId 分类ID
     * @param status 状态
     * @param page 页码
     * @param size 页面大小
     * @return 搜索结果
     */
    @GetMapping
    @PreAuthorize("hasRole('USER')")
    public ResponseEntity<ApiResponse<InterfaceSearchResult>> quickSearch(
            @RequestParam(required = false) String keyword,
            @RequestParam(required = false) Long categoryId,
            @RequestParam(required = false) String status,
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "20") int size) {
        try {
            InterfaceSearchRequest searchRequest = new InterfaceSearchRequest();
            searchRequest.setKeyword(keyword);
            searchRequest.setCategoryId(categoryId);
            searchRequest.setStatus(status);
            searchRequest.setPage(page);
            searchRequest.setSize(size);
            
            InterfaceSearchResult result = interfaceSearchService.searchInterfaces(searchRequest);
            
            return ResponseEntity.ok(ApiResponse.success(result));
        } catch (Exception e) {
            return ResponseEntity.badRequest()
                .body(ApiResponse.error("快速搜索失败: " + e.getMessage()));
        }
    }

    /**
     * 获取搜索建议
     *
     * @param keyword 关键词
     * @return 搜索建议列表
     */
    @GetMapping("/suggestions")
    @PreAuthorize("hasRole('USER')")
    public ResponseEntity<ApiResponse<List<String>>> getSearchSuggestions(
            @RequestParam String keyword) {
        try {
            List<String> suggestions = interfaceSearchService.getSearchSuggestions(keyword);
            return ResponseEntity.ok(ApiResponse.success(suggestions));
        } catch (Exception e) {
            return ResponseEntity.badRequest()
                .body(ApiResponse.error("获取搜索建议失败: " + e.getMessage()));
        }
    }

    /**
     * 获取热门搜索关键词
     *
     * @return 热门关键词列表
     */
    @GetMapping("/popular")
    @PreAuthorize("hasRole('USER')")
    public ResponseEntity<ApiResponse<List<String>>> getPopularKeywords() {
        try {
            List<String> keywords = interfaceSearchService.getPopularKeywords();
            return ResponseEntity.ok(ApiResponse.success(keywords));
        } catch (Exception e) {
            return ResponseEntity.badRequest()
                .body(ApiResponse.error("获取热门关键词失败: " + e.getMessage()));
        }
    }

    /**
     * 获取筛选选项
     *
     * @return 筛选选项
     */
    @GetMapping("/filters")
    @PreAuthorize("hasRole('USER')")
    public ResponseEntity<ApiResponse<FilterOptions>> getFilterOptions() {
        try {
            FilterOptions options = interfaceSearchService.getFilterOptions();
            return ResponseEntity.ok(ApiResponse.success(options));
        } catch (Exception e) {
            return ResponseEntity.badRequest()
                .body(ApiResponse.error("获取筛选选项失败: " + e.getMessage()));
        }
    }

    /**
     * 高级搜索
     *
     * @param request 高级搜索请求
     * @return 搜索结果
     */
    @PostMapping("/advanced")
    @PreAuthorize("hasRole('USER')")
    public ResponseEntity<ApiResponse<InterfaceSearchResult>> advancedSearch(
            @Valid @RequestBody AdvancedSearchRequest request) {
        try {
            InterfaceSearchRequest searchRequest = convertToAdvancedSearchRequest(request);
            InterfaceSearchResult result = interfaceSearchService.searchInterfaces(searchRequest);
            
            return ResponseEntity.ok(ApiResponse.success(result));
        } catch (Exception e) {
            return ResponseEntity.badRequest()
                .body(ApiResponse.error("高级搜索失败: " + e.getMessage()));
        }
    }

    /**
     * 获取搜索历史
     *
     * @return 搜索历史列表
     */
    @GetMapping("/history")
    @PreAuthorize("hasRole('USER')")
    public ResponseEntity<ApiResponse<List<SearchHistoryItem>>> getSearchHistory() {
        try {
            // 这里应该从用户搜索历史中获取
            // 暂时返回空列表
            List<SearchHistoryItem> history = List.of();
            return ResponseEntity.ok(ApiResponse.success(history));
        } catch (Exception e) {
            return ResponseEntity.badRequest()
                .body(ApiResponse.error("获取搜索历史失败: " + e.getMessage()));
        }
    }

    /**
     * 清空搜索历史
     *
     * @return 操作结果
     */
    @DeleteMapping("/history")
    @PreAuthorize("hasRole('USER')")
    public ResponseEntity<ApiResponse<Void>> clearSearchHistory() {
        try {
            // 这里应该清空用户的搜索历史
            return ResponseEntity.ok(ApiResponse.success(null, "搜索历史已清空"));
        } catch (Exception e) {
            return ResponseEntity.badRequest()
                .body(ApiResponse.error("清空搜索历史失败: " + e.getMessage()));
        }
    }

    /**
     * 转换搜索请求
     */
    private InterfaceSearchRequest convertToSearchRequest(SearchRequest request) {
        InterfaceSearchRequest searchRequest = new InterfaceSearchRequest();
        searchRequest.setKeyword(request.getKeyword());
        searchRequest.setCategoryId(request.getCategoryId());
        searchRequest.setStatus(request.getStatus());
        searchRequest.setDataSourceId(request.getDataSourceId());
        searchRequest.setCreatedBy(request.getCreatedBy());
        searchRequest.setTags(request.getTags());
        searchRequest.setSortBy(request.getSortBy());
        searchRequest.setSortOrder(request.getSortOrder());
        searchRequest.setPage(request.getPage());
        searchRequest.setSize(request.getSize());
        
        return searchRequest;
    }

    /**
     * 转换高级搜索请求
     */
    private InterfaceSearchRequest convertToAdvancedSearchRequest(AdvancedSearchRequest request) {
        InterfaceSearchRequest searchRequest = new InterfaceSearchRequest();
        searchRequest.setKeyword(request.getKeyword());
        searchRequest.setCategoryId(request.getCategoryId());
        searchRequest.setStatus(request.getStatus());
        searchRequest.setDataSourceId(request.getDataSourceId());
        searchRequest.setCreatedBy(request.getCreatedBy());
        searchRequest.setCreateTimeStart(request.getCreateTimeStart());
        searchRequest.setCreateTimeEnd(request.getCreateTimeEnd());
        searchRequest.setUpdateTimeStart(request.getUpdateTimeStart());
        searchRequest.setUpdateTimeEnd(request.getUpdateTimeEnd());
        searchRequest.setTags(request.getTags());
        searchRequest.setSortBy(request.getSortBy());
        searchRequest.setSortOrder(request.getSortOrder());
        searchRequest.setPage(request.getPage());
        searchRequest.setSize(request.getSize());
        
        return searchRequest;
    }

    /**
     * 基础搜索请求
     */
    public static class SearchRequest {
        private String keyword;
        private Long categoryId;
        private String status;
        private Long dataSourceId;
        private String createdBy;
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
     * 高级搜索请求
     */
    public static class AdvancedSearchRequest {
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
     * 搜索历史项
     */
    public static class SearchHistoryItem {
        private String keyword;
        private LocalDateTime searchTime;
        private int resultCount;
        
        // Getters and Setters
        public String getKeyword() { return keyword; }
        public void setKeyword(String keyword) { this.keyword = keyword; }
        public LocalDateTime getSearchTime() { return searchTime; }
        public void setSearchTime(LocalDateTime searchTime) { this.searchTime = searchTime; }
        public int getResultCount() { return resultCount; }
        public void setResultCount(int resultCount) { this.resultCount = resultCount; }
    }

    /**
     * API响应包装类
     */
    public static class ApiResponse<T> {
        private boolean success;
        private String message;
        private T data;
        private long timestamp;
        
        public ApiResponse() {
            this.timestamp = System.currentTimeMillis();
        }
        
        public static <T> ApiResponse<T> success(T data) {
            ApiResponse<T> response = new ApiResponse<>();
            response.setSuccess(true);
            response.setMessage("操作成功");
            response.setData(data);
            return response;
        }
        
        public static <T> ApiResponse<T> success(T data, String message) {
            ApiResponse<T> response = new ApiResponse<>();
            response.setSuccess(true);
            response.setMessage(message);
            response.setData(data);
            return response;
        }
        
        public static <T> ApiResponse<T> error(String message) {
            ApiResponse<T> response = new ApiResponse<>();
            response.setSuccess(false);
            response.setMessage(message);
            return response;
        }
        
        // Getters and Setters
        public boolean isSuccess() { return success; }
        public void setSuccess(boolean success) { this.success = success; }
        public String getMessage() { return message; }
        public void setMessage(String message) { this.message = message; }
        public T getData() { return data; }
        public void setData(T data) { this.data = data; }
        public long getTimestamp() { return timestamp; }
        public void setTimestamp(long timestamp) { this.timestamp = timestamp; }
    }
}