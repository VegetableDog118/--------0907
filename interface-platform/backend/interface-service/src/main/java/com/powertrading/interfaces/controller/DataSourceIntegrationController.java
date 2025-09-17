package com.powertrading.interfaces.controller;

import com.powertrading.interfaces.service.DataSourceIntegrationService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.extern.slf4j.Slf4j;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import javax.validation.Valid;
import javax.validation.constraints.NotBlank;
import java.util.List;
import java.util.Map;

/**
 * 数据源集成控制器
 * 提供与datasource-service集成的API接口
 *
 * @author PowerTrading Team
 * @since 2024-01-15
 */
@Slf4j
@RestController
@RequestMapping("/api/v1/datasources")
@Tag(name = "数据源集成", description = "与datasource-service集成相关API")
@Validated
public class DataSourceIntegrationController {

    private static final Logger log = LoggerFactory.getLogger(DataSourceIntegrationController.class);

    @Autowired
    private DataSourceIntegrationService dataSourceIntegrationService;

    /**
     * 获取可用的数据源列表
     *
     * @return 数据源列表
     */
    @GetMapping("/available")
    @Operation(summary = "获取可用数据源列表", description = "获取所有可用的数据源列表")
    public ApiResponse<List<DataSourceIntegrationService.DataSourceInfo>> getAvailableDataSources() {
        try {
            List<DataSourceIntegrationService.DataSourceInfo> dataSources = 
                dataSourceIntegrationService.getAvailableDataSources();
            return ApiResponse.success(dataSources);
        } catch (Exception e) {
            log.error("获取可用数据源列表失败", e);
            return ApiResponse.error("获取可用数据源列表失败: " + e.getMessage());
        }
    }

    /**
     * 根据ID获取数据源详情
     *
     * @param dataSourceId 数据源ID
     * @return 数据源详情
     */
    @GetMapping("/{dataSourceId}")
    @Operation(summary = "获取数据源详情", description = "根据ID获取数据源的详细信息")
    public ApiResponse<DataSourceIntegrationService.DataSourceInfo> getDataSourceById(
            @Parameter(description = "数据源ID", required = true)
            @PathVariable @NotBlank(message = "数据源ID不能为空") String dataSourceId) {
        try {
            DataSourceIntegrationService.DataSourceInfo dataSource = 
                dataSourceIntegrationService.getDataSourceById(dataSourceId);
            return ApiResponse.success(dataSource);
        } catch (Exception e) {
            log.error("获取数据源详情失败，数据源ID: {}", dataSourceId, e);
            return ApiResponse.error("获取数据源详情失败: " + e.getMessage());
        }
    }

    /**
     * 获取数据源的表列表
     *
     * @param dataSourceId 数据源ID
     * @return 表列表
     */
    @GetMapping("/{dataSourceId}/tables")
    @Operation(summary = "获取数据源表列表", description = "获取指定数据源的所有表列表")
    public ApiResponse<List<DataSourceIntegrationService.TableInfo>> getDataSourceTables(
            @Parameter(description = "数据源ID", required = true)
            @PathVariable @NotBlank(message = "数据源ID不能为空") String dataSourceId) {
        try {
            List<DataSourceIntegrationService.TableInfo> tables = 
                dataSourceIntegrationService.getDataSourceTables(dataSourceId);
            return ApiResponse.success(tables);
        } catch (Exception e) {
            log.error("获取数据源表列表失败，数据源ID: {}", dataSourceId, e);
            return ApiResponse.error("获取数据源表列表失败: " + e.getMessage());
        }
    }

    /**
     * 获取表的字段结构
     *
     * @param dataSourceId 数据源ID
     * @param tableName 表名
     * @return 字段列表
     */
    @GetMapping("/{dataSourceId}/tables/{tableName}/columns")
    @Operation(summary = "获取表字段结构", description = "获取指定表的字段结构信息")
    public ApiResponse<List<DataSourceIntegrationService.ColumnInfo>> getTableColumns(
            @Parameter(description = "数据源ID", required = true)
            @PathVariable @NotBlank(message = "数据源ID不能为空") String dataSourceId,
            @Parameter(description = "表名", required = true)
            @PathVariable @NotBlank(message = "表名不能为空") String tableName) {
        try {
            List<DataSourceIntegrationService.ColumnInfo> columns = 
                dataSourceIntegrationService.getTableColumns(dataSourceId, tableName);
            return ApiResponse.success(columns);
        } catch (Exception e) {
            log.error("获取表字段结构失败，数据源ID: {}, 表名: {}", dataSourceId, tableName, e);
            return ApiResponse.error("获取表字段结构失败: " + e.getMessage());
        }
    }

    /**
     * 执行数据查询
     *
     * @param request 查询请求
     * @return 查询结果
     */
    @PostMapping("/query")
    @Operation(summary = "执行数据查询", description = "在指定数据源上执行SQL查询")
    public ApiResponse<DataSourceIntegrationService.QueryResult> executeQuery(
            @RequestBody @Valid QueryRequest request) {
        try {
            DataSourceIntegrationService.QueryResult result = 
                dataSourceIntegrationService.executeQuery(
                    request.getDataSourceId(), request.getSql(), request.getParams());
            return ApiResponse.success(result);
        } catch (Exception e) {
            log.error("执行数据查询失败，数据源ID: {}, SQL: {}", 
                request.getDataSourceId(), request.getSql(), e);
            return ApiResponse.error("执行数据查询失败: " + e.getMessage());
        }
    }

    /**
     * 验证SQL语句
     *
     * @param request 验证请求
     * @return 验证结果
     */
    @PostMapping("/validate-sql")
    @Operation(summary = "验证SQL语句", description = "验证SQL语句的语法正确性")
    public ApiResponse<DataSourceIntegrationService.SqlValidationResult> validateSql(
            @RequestBody @Valid SqlValidationRequest request) {
        try {
            DataSourceIntegrationService.SqlValidationResult result = 
                dataSourceIntegrationService.validateSql(request.getDataSourceId(), request.getSql());
            return ApiResponse.success(result);
        } catch (Exception e) {
            log.error("验证SQL语句失败，数据源ID: {}, SQL: {}", 
                request.getDataSourceId(), request.getSql(), e);
            return ApiResponse.error("验证SQL语句失败: " + e.getMessage());
        }
    }

    /**
     * 测试数据源连接
     *
     * @param dataSourceId 数据源ID
     * @return 连接测试结果
     */
    @PostMapping("/{dataSourceId}/test-connection")
    @Operation(summary = "测试数据源连接", description = "测试指定数据源的连接状态")
    public ApiResponse<DataSourceIntegrationService.ConnectionTestResult> testDataSourceConnection(
            @Parameter(description = "数据源ID", required = true)
            @PathVariable @NotBlank(message = "数据源ID不能为空") String dataSourceId) {
        try {
            DataSourceIntegrationService.ConnectionTestResult result = 
                dataSourceIntegrationService.testDataSourceConnection(dataSourceId);
            return ApiResponse.success(result);
        } catch (Exception e) {
            log.error("测试数据源连接失败，数据源ID: {}", dataSourceId, e);
            return ApiResponse.error("测试数据源连接失败: " + e.getMessage());
        }
    }

    /**
     * 获取数据源统计信息
     *
     * @param dataSourceId 数据源ID
     * @return 统计信息
     */
    @GetMapping("/{dataSourceId}/statistics")
    @Operation(summary = "获取数据源统计信息", description = "获取指定数据源的统计信息")
    public ApiResponse<DataSourceIntegrationService.DataSourceStatistics> getDataSourceStatistics(
            @Parameter(description = "数据源ID", required = true)
            @PathVariable @NotBlank(message = "数据源ID不能为空") String dataSourceId) {
        try {
            DataSourceIntegrationService.DataSourceStatistics statistics = 
                dataSourceIntegrationService.getDataSourceStatistics(dataSourceId);
            return ApiResponse.success(statistics);
        } catch (Exception e) {
            log.error("获取数据源统计信息失败，数据源ID: {}", dataSourceId, e);
            return ApiResponse.error("获取数据源统计信息失败: " + e.getMessage());
        }
    }

    /**
     * 生成表的示例SQL
     *
     * @param dataSourceId 数据源ID
     * @param tableName 表名
     * @param queryType 查询类型
     * @return 示例SQL
     */
    @GetMapping("/{dataSourceId}/tables/{tableName}/sample-sql")
    @Operation(summary = "生成示例SQL", description = "为指定表生成示例SQL语句")
    public ApiResponse<String> generateSampleSql(
            @Parameter(description = "数据源ID", required = true)
            @PathVariable @NotBlank(message = "数据源ID不能为空") String dataSourceId,
            @Parameter(description = "表名", required = true)
            @PathVariable @NotBlank(message = "表名不能为空") String tableName,
            @Parameter(description = "查询类型")
            @RequestParam(defaultValue = "SELECT") String queryType) {
        try {
            String sampleSql = dataSourceIntegrationService.generateSampleSql(
                dataSourceId, tableName, queryType);
            return ApiResponse.success(sampleSql);
        } catch (Exception e) {
            log.error("生成示例SQL失败，数据源ID: {}, 表名: {}, 查询类型: {}", 
                dataSourceId, tableName, queryType, e);
            return ApiResponse.error("生成示例SQL失败: " + e.getMessage());
        }
    }

    /**
     * 获取数据源类型列表
     *
     * @return 数据源类型列表
     */
    @GetMapping("/types")
    @Operation(summary = "获取数据源类型列表", description = "获取系统支持的数据源类型列表")
    public ApiResponse<List<DataSourceType>> getDataSourceTypes() {
        try {
            List<DataSourceType> types = List.of(
                new DataSourceType("MYSQL", "MySQL数据库", "mysql-connector-java"),
                new DataSourceType("POSTGRESQL", "PostgreSQL数据库", "postgresql"),
                new DataSourceType("ORACLE", "Oracle数据库", "ojdbc"),
                new DataSourceType("SQLSERVER", "SQL Server数据库", "mssql-jdbc"),
                new DataSourceType("CLICKHOUSE", "ClickHouse数据库", "clickhouse-jdbc")
            );
            return ApiResponse.success(types);
        } catch (Exception e) {
            log.error("获取数据源类型列表失败", e);
            return ApiResponse.error("获取数据源类型列表失败: " + e.getMessage());
        }
    }

    /**
     * 查询请求
     */
    public static class QueryRequest {
        @NotBlank(message = "数据源ID不能为空")
        private String dataSourceId;
        
        @NotBlank(message = "SQL语句不能为空")
        private String sql;
        
        private Map<String, Object> params;
        
        public String getDataSourceId() { return dataSourceId; }
        public void setDataSourceId(String dataSourceId) { this.dataSourceId = dataSourceId; }
        public String getSql() { return sql; }
        public void setSql(String sql) { this.sql = sql; }
        public Map<String, Object> getParams() { return params; }
        public void setParams(Map<String, Object> params) { this.params = params; }
    }

    /**
     * SQL验证请求
     */
    public static class SqlValidationRequest {
        @NotBlank(message = "数据源ID不能为空")
        private String dataSourceId;
        
        @NotBlank(message = "SQL语句不能为空")
        private String sql;
        
        public String getDataSourceId() { return dataSourceId; }
        public void setDataSourceId(String dataSourceId) { this.dataSourceId = dataSourceId; }
        public String getSql() { return sql; }
        public void setSql(String sql) { this.sql = sql; }
    }

    /**
     * 数据源类型
     */
    public static class DataSourceType {
        private String code;
        private String name;
        private String driverClass;
        
        public DataSourceType(String code, String name, String driverClass) {
            this.code = code;
            this.name = name;
            this.driverClass = driverClass;
        }
        
        public String getCode() { return code; }
        public void setCode(String code) { this.code = code; }
        public String getName() { return name; }
        public void setName(String name) { this.name = name; }
        public String getDriverClass() { return driverClass; }
        public void setDriverClass(String driverClass) { this.driverClass = driverClass; }
    }
    
    /**
     * 统一响应结果
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
        
        public static <T> ApiResponse<T> error(String message) {
            ApiResponse<T> response = new ApiResponse<>();
            response.setSuccess(false);
            response.setMessage(message);
            return response;
        }
        
        // getters and setters
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