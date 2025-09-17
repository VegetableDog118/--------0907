package com.powertrading.datasource.controller;

import com.powertrading.datasource.engine.QueryExecutionEngine;
import com.powertrading.datasource.entity.DataSource;
import com.powertrading.datasource.exception.DataSourceException;
import com.powertrading.datasource.manager.DataSourceManager;
import com.powertrading.datasource.service.DataSourceService;
// import io.swagger.annotations.Api;
// import io.swagger.annotations.ApiOperation;
// import io.swagger.annotations.ApiParam;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import javax.validation.Valid;
import javax.validation.constraints.NotNull;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;

/**
 * 数据源管理控制器
 * 
 * @author PowerTrading Team
 * @version 1.0.0
 */
// @Api(tags = "数据源管理")
@RestController
@RequestMapping("/api/datasources")
@Validated
public class DataSourceController {

    private static final Logger logger = LoggerFactory.getLogger(DataSourceController.class);

    @Autowired
    private DataSourceService dataSourceService;

    @Autowired
    private QueryExecutionEngine queryExecutionEngine;

    /**
     * 创建数据源
     */
    // // @ApiOperation("创建数据源")
    @PostMapping
    public ResponseEntity<ApiResponse<DataSource>> createDataSource(
            /* /* @ApiParam("数据源配置") */ @Valid @RequestBody DataSource dataSource) {
        
        try {
            DataSource createdDataSource = dataSourceService.createDataSource(dataSource);
            return ResponseEntity.ok(ApiResponse.success(createdDataSource, "数据源创建成功"));
        } catch (DataSourceException e) {
            logger.error("创建数据源失败", e);
            return ResponseEntity.badRequest().body(ApiResponse.error(e.getMessage()));
        } catch (Exception e) {
            logger.error("创建数据源异常", e);
            return ResponseEntity.internalServerError().body(ApiResponse.error("系统异常: " + e.getMessage()));
        }
    }

    /**
     * 更新数据源
     */
    // // @ApiOperation("更新数据源")
    @PutMapping("/{id}")
    public ResponseEntity<ApiResponse<DataSource>> updateDataSource(
            /* /* @ApiParam("数据源ID") */ @PathVariable Long id,
            /* /* @ApiParam("数据源配置") */ @Valid @RequestBody DataSource dataSource) {
        
        try {
            DataSource updatedDataSource = dataSourceService.updateDataSource(id, dataSource);
            return ResponseEntity.ok(ApiResponse.success(updatedDataSource, "数据源更新成功"));
        } catch (DataSourceException e) {
            logger.error("更新数据源失败: id={}", id, e);
            return ResponseEntity.badRequest().body(ApiResponse.error(e.getMessage()));
        } catch (Exception e) {
            logger.error("更新数据源异常: id={}", id, e);
            return ResponseEntity.internalServerError().body(ApiResponse.error("系统异常: " + e.getMessage()));
        }
    }

    /**
     * 删除数据源
     */
    // @ApiOperation("删除数据源")
    @DeleteMapping("/{id}")
    public ResponseEntity<ApiResponse<Void>> deleteDataSource(
            /* @ApiParam("数据源ID") */ @PathVariable Long id) {
        
        try {
            dataSourceService.deleteDataSource(id);
            return ResponseEntity.ok(ApiResponse.success(null, "数据源删除成功"));
        } catch (DataSourceException e) {
            logger.error("删除数据源失败: id={}", id, e);
            return ResponseEntity.badRequest().body(ApiResponse.error(e.getMessage()));
        } catch (Exception e) {
            logger.error("删除数据源异常: id={}", id, e);
            return ResponseEntity.internalServerError().body(ApiResponse.error("系统异常: " + e.getMessage()));
        }
    }

    /**
     * 根据ID获取数据源
     */
    // @ApiOperation("根据ID获取数据源")
    @GetMapping("/{id}")
    public ResponseEntity<ApiResponse<DataSource>> getDataSource(
            /* @ApiParam("数据源ID") */ @PathVariable Long id) {
        
        try {
            Optional<DataSource> dataSource = dataSourceService.getDataSourceById(id);
            if (dataSource.isPresent()) {
                return ResponseEntity.ok(ApiResponse.success(dataSource.get(), "获取数据源成功"));
            } else {
                return ResponseEntity.notFound().build();
            }
        } catch (Exception e) {
            logger.error("获取数据源异常: id={}", id, e);
            return ResponseEntity.internalServerError().body(ApiResponse.error("系统异常: " + e.getMessage()));
        }
    }

    /**
     * 分页查询数据源
     */
    // @ApiOperation("分页查询数据源")
    @GetMapping
    public ResponseEntity<ApiResponse<Page<DataSource>>> getDataSources(
            /* @ApiParam("数据源名称") */ @RequestParam(required = false) String name,
            /* @ApiParam("数据源类型") */ @RequestParam(required = false) String type,
            /* @ApiParam("状态") */ @RequestParam(required = false) Integer status,
            /* @ApiParam("页码") */ @RequestParam(defaultValue = "0") int page,
            /* @ApiParam("页大小") */ @RequestParam(defaultValue = "10") int size,
            /* @ApiParam("排序字段") */ @RequestParam(defaultValue = "createdAt") String sortBy,
            /* @ApiParam("排序方向") */ @RequestParam(defaultValue = "desc") String sortDir) {
        
        try {
            Sort.Direction direction = "asc".equalsIgnoreCase(sortDir) ? Sort.Direction.ASC : Sort.Direction.DESC;
            Pageable pageable = PageRequest.of(page, size, Sort.by(direction, sortBy));
            
            Page<DataSource> dataSources = dataSourceService.getDataSources(name, type, status, pageable);
            return ResponseEntity.ok(ApiResponse.success(dataSources, "查询数据源成功"));
        } catch (Exception e) {
            logger.error("查询数据源异常", e);
            return ResponseEntity.internalServerError().body(ApiResponse.error("系统异常: " + e.getMessage()));
        }
    }

    /**
     * 获取启用的数据源列表
     */
    // @ApiOperation("获取启用的数据源列表")
    @GetMapping("/enabled")
    public ResponseEntity<ApiResponse<List<DataSource>>> getEnabledDataSources() {
        try {
            List<DataSource> dataSources = dataSourceService.getEnabledDataSources();
            return ResponseEntity.ok(ApiResponse.success(dataSources, "获取启用数据源成功"));
        } catch (Exception e) {
            logger.error("获取启用数据源异常", e);
            return ResponseEntity.internalServerError().body(ApiResponse.error("系统异常: " + e.getMessage()));
        }
    }

    /**
     * 测试数据源连接
     */
    // @ApiOperation("测试数据源连接")
    @PostMapping("/{id}/test")
    public ResponseEntity<ApiResponse<Boolean>> testConnection(
            /* @ApiParam("数据源ID") */ @PathVariable Long id) {
        
        try {
            boolean connected = dataSourceService.testConnection(id);
            String message = connected ? "连接测试成功" : "连接测试失败";
            return ResponseEntity.ok(ApiResponse.success(connected, message));
        } catch (DataSourceException e) {
            logger.error("测试数据源连接失败: id={}", id, e);
            return ResponseEntity.badRequest().body(ApiResponse.error(e.getMessage()));
        } catch (Exception e) {
            logger.error("测试数据源连接异常: id={}", id, e);
            return ResponseEntity.internalServerError().body(ApiResponse.error("系统异常: " + e.getMessage()));
        }
    }

    /**
     * 启用数据源
     */
    // @ApiOperation("启用数据源")
    @PostMapping("/{id}/enable")
    public ResponseEntity<ApiResponse<Void>> enableDataSource(
            /* @ApiParam("数据源ID") */ @PathVariable Long id) {
        
        try {
            dataSourceService.enableDataSource(id);
            return ResponseEntity.ok(ApiResponse.success(null, "数据源启用成功"));
        } catch (DataSourceException e) {
            logger.error("启用数据源失败: id={}", id, e);
            return ResponseEntity.badRequest().body(ApiResponse.error(e.getMessage()));
        } catch (Exception e) {
            logger.error("启用数据源异常: id={}", id, e);
            return ResponseEntity.internalServerError().body(ApiResponse.error("系统异常: " + e.getMessage()));
        }
    }

    /**
     * 禁用数据源
     */
    // @ApiOperation("禁用数据源")
    @PostMapping("/{id}/disable")
    public ResponseEntity<ApiResponse<Void>> disableDataSource(
            /* @ApiParam("数据源ID") */ @PathVariable Long id) {
        
        try {
            dataSourceService.disableDataSource(id);
            return ResponseEntity.ok(ApiResponse.success(null, "数据源禁用成功"));
        } catch (DataSourceException e) {
            logger.error("禁用数据源失败: id={}", id, e);
            return ResponseEntity.badRequest().body(ApiResponse.error(e.getMessage()));
        } catch (Exception e) {
            logger.error("禁用数据源异常: id={}", id, e);
            return ResponseEntity.internalServerError().body(ApiResponse.error("系统异常: " + e.getMessage()));
        }
    }

    /**
     * 获取数据源连接池信息
     */
    // @ApiOperation("获取数据源连接池信息")
    @GetMapping("/{id}/pool-info")
    public ResponseEntity<ApiResponse<DataSourceManager.DataSourcePoolInfo>> getPoolInfo(
            /* @ApiParam("数据源ID") */ @PathVariable Long id) {
        
        try {
            DataSourceManager.DataSourcePoolInfo poolInfo = dataSourceService.getPoolInfo(id);
            if (poolInfo != null) {
                return ResponseEntity.ok(ApiResponse.success(poolInfo, "获取连接池信息成功"));
            } else {
                return ResponseEntity.notFound().build();
            }
        } catch (Exception e) {
            logger.error("获取连接池信息异常: id={}", id, e);
            return ResponseEntity.internalServerError().body(ApiResponse.error("系统异常: " + e.getMessage()));
        }
    }

    /**
     * 获取所有数据源连接池信息
     */
    // @ApiOperation("获取所有数据源连接池信息")
    @GetMapping("/pool-info")
    public ResponseEntity<ApiResponse<Map<Long, DataSourceManager.DataSourcePoolInfo>>> getAllPoolInfo() {
        try {
            Map<Long, DataSourceManager.DataSourcePoolInfo> poolInfoMap = dataSourceService.getAllPoolInfo();
            return ResponseEntity.ok(ApiResponse.success(poolInfoMap, "获取所有连接池信息成功"));
        } catch (Exception e) {
            logger.error("获取所有连接池信息异常", e);
            return ResponseEntity.internalServerError().body(ApiResponse.error("系统异常: " + e.getMessage()));
        }
    }

    /**
     * 执行查询SQL
     */
    // @ApiOperation("执行查询SQL")
    @PostMapping("/{id}/query")
    public ResponseEntity<ApiResponse<QueryExecutionEngine.QueryResult>> executeQuery(
            /* @ApiParam("数据源ID") */ @PathVariable Long id,
            /* @ApiParam("查询请求") */ @Valid @RequestBody QueryRequest request) {
        
        try {
            QueryExecutionEngine.QueryResult result = queryExecutionEngine.executeQuery(
                id, request.getSql(), request.getParameters());
            return ResponseEntity.ok(ApiResponse.success(result, "查询执行成功"));
        } catch (DataSourceException e) {
            logger.error("执行查询失败: id={}, sql={}", id, request.getSql(), e);
            return ResponseEntity.badRequest().body(ApiResponse.error(e.getMessage()));
        } catch (Exception e) {
            logger.error("执行查询异常: id={}, sql={}", id, request.getSql(), e);
            return ResponseEntity.internalServerError().body(ApiResponse.error("系统异常: " + e.getMessage()));
        }
    }

    /**
     * 异步执行查询SQL
     */
    // @ApiOperation("异步执行查询SQL")
    @PostMapping("/{id}/query-async")
    public ResponseEntity<ApiResponse<String>> executeQueryAsync(
            /* @ApiParam("数据源ID") */ @PathVariable Long id,
            /* @ApiParam("查询请求") */ @Valid @RequestBody QueryRequest request) {
        
        try {
            CompletableFuture<QueryExecutionEngine.QueryResult> future = 
                queryExecutionEngine.executeQueryAsync(id, request.getSql(), request.getParameters());
            
            // 这里可以返回任务ID，客户端可以通过任务ID查询结果
            String taskId = "task_" + System.currentTimeMillis();
            
            return ResponseEntity.ok(ApiResponse.success(taskId, "异步查询已提交"));
        } catch (Exception e) {
            logger.error("提交异步查询异常: id={}, sql={}", id, request.getSql(), e);
            return ResponseEntity.internalServerError().body(ApiResponse.error("系统异常: " + e.getMessage()));
        }
    }

    /**
     * 获取表结构信息
     */
    // @ApiOperation("获取表结构信息")
    @GetMapping("/{id}/tables/{tableName}/metadata")
    public ResponseEntity<ApiResponse<QueryExecutionEngine.TableMetadata>> getTableMetadata(
            /* @ApiParam("数据源ID") */ @PathVariable Long id,
            /* @ApiParam("表名") */ @PathVariable String tableName) {
        
        try {
            QueryExecutionEngine.TableMetadata metadata = queryExecutionEngine.getTableMetadata(id, tableName);
            return ResponseEntity.ok(ApiResponse.success(metadata, "获取表结构信息成功"));
        } catch (DataSourceException e) {
            logger.error("获取表结构信息失败: id={}, tableName={}", id, tableName, e);
            return ResponseEntity.badRequest().body(ApiResponse.error(e.getMessage()));
        } catch (Exception e) {
            logger.error("获取表结构信息异常: id={}, tableName={}", id, tableName, e);
            return ResponseEntity.internalServerError().body(ApiResponse.error("系统异常: " + e.getMessage()));
        }
    }

    /**
     * 获取数据库表名列表
     */
    // @ApiOperation("获取数据库表名列表")
    @GetMapping("/{id}/tables")
    public ResponseEntity<ApiResponse<List<String>>> getTableNames(
            /* @ApiParam("数据源ID") */ @PathVariable Long id,
            /* @ApiParam("模式名") */ @RequestParam(required = false) String schema) {
        
        try {
            List<String> tableNames = queryExecutionEngine.getTableNames(id, schema);
            return ResponseEntity.ok(ApiResponse.success(tableNames, "获取表名列表成功"));
        } catch (DataSourceException e) {
            logger.error("获取表名列表失败: id={}, schema={}", id, schema, e);
            return ResponseEntity.badRequest().body(ApiResponse.error(e.getMessage()));
        } catch (Exception e) {
            logger.error("获取表名列表异常: id={}, schema={}", id, schema, e);
            return ResponseEntity.internalServerError().body(ApiResponse.error("系统异常: " + e.getMessage()));
        }
    }

    /**
     * 查询请求类
     */
    public static class QueryRequest {
        @NotNull(message = "SQL语句不能为空")
        private String sql;
        
        private Map<String, Object> parameters;

        // Getters and Setters
        public String getSql() {
            return sql;
        }

        public void setSql(String sql) {
            this.sql = sql;
        }

        public Map<String, Object> getParameters() {
            return parameters;
        }

        public void setParameters(Map<String, Object> parameters) {
            this.parameters = parameters;
        }
    }

    /**
     * API响应类
     */
    public static class ApiResponse<T> {
        private boolean success;
        private String message;
        private T data;
        private String errorCode;

        public static <T> ApiResponse<T> success(T data, String message) {
            ApiResponse<T> response = new ApiResponse<>();
            response.success = true;
            response.message = message;
            response.data = data;
            return response;
        }

        public static <T> ApiResponse<T> error(String message) {
            ApiResponse<T> response = new ApiResponse<>();
            response.success = false;
            response.message = message;
            return response;
        }

        public static <T> ApiResponse<T> error(String errorCode, String message) {
            ApiResponse<T> response = new ApiResponse<>();
            response.success = false;
            response.message = message;
            response.errorCode = errorCode;
            return response;
        }

        // Getters and Setters
        public boolean isSuccess() {
            return success;
        }

        public void setSuccess(boolean success) {
            this.success = success;
        }

        public String getMessage() {
            return message;
        }

        public void setMessage(String message) {
            this.message = message;
        }

        public T getData() {
            return data;
        }

        public void setData(T data) {
            this.data = data;
        }

        public String getErrorCode() {
            return errorCode;
        }

        public void setErrorCode(String errorCode) {
            this.errorCode = errorCode;
        }
    }
}