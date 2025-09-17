package com.powertrading.interfaces.client;

import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

/**
 * 数据源服务客户端
 *
 * @author PowerTrading Team
 * @since 2024-01-15
 */
@FeignClient(name = "datasource-service", path = "/api/v1/datasources")
public interface DataSourceClient {

    /**
     * 获取数据源列表
     *
     * @return 数据源列表
     */
    @GetMapping("/list")
    ApiResponse<List<DataSourceInfo>> getDataSources();

    /**
     * 根据ID获取数据源信息
     *
     * @param dataSourceId 数据源ID
     * @return 数据源信息
     */
    @GetMapping("/{dataSourceId}")
    ApiResponse<DataSourceInfo> getDataSource(@PathVariable("dataSourceId") String dataSourceId);

    /**
     * 获取数据源的表列表
     *
     * @param dataSourceId 数据源ID
     * @return 表列表
     */
    @GetMapping("/{dataSourceId}/tables")
    ApiResponse<List<TableInfo>> getTables(@PathVariable("dataSourceId") String dataSourceId);

    /**
     * 获取表结构信息
     *
     * @param dataSourceId 数据源ID
     * @param tableName 表名
     * @return 表结构信息
     */
    @GetMapping("/{dataSourceId}/tables/{tableName}/structure")
    ApiResponse<TableStructure> getTableStructure(@PathVariable("dataSourceId") String dataSourceId,
                                                 @PathVariable("tableName") String tableName);

    /**
     * 测试数据源连接
     *
     * @param dataSourceId 数据源ID
     * @return 连接测试结果
     */
    @PostMapping("/{dataSourceId}/test")
    ApiResponse<ConnectionTestResult> testConnection(@PathVariable("dataSourceId") String dataSourceId);
    
    /**
     * 获取数据源统计信息
     *
     * @param dataSourceId 数据源ID
     * @return 统计信息
     */
    @GetMapping("/{dataSourceId}/statistics")
    ApiResponse<DataSourceStatistics> getDataSourceStatistics(@PathVariable("dataSourceId") String dataSourceId);

    /**
     * 执行SQL查询
     *
     * @param dataSourceId 数据源ID
     * @param request 查询请求
     * @return 查询结果
     */
    @PostMapping("/{dataSourceId}/query")
    ApiResponse<QueryResult> executeQuery(@PathVariable("dataSourceId") String dataSourceId,
                                        @RequestBody QueryRequest request);

    /**
     * 验证SQL语法
     *
     * @param dataSourceId 数据源ID
     * @param request 验证请求
     * @return 验证结果
     */
    @PostMapping("/{dataSourceId}/validate")
    ApiResponse<ValidationResult> validateSql(@PathVariable("dataSourceId") String dataSourceId,
                                            @RequestBody SqlValidationRequest request);

    /**
     * API响应包装类
     */
    class ApiResponse<T> {
        private String code;
        private String message;
        private T data;
        private Boolean success;
        
        // getters and setters
        public String getCode() { return code; }
        public void setCode(String code) { this.code = code; }
        public String getMessage() { return message; }
        public void setMessage(String message) { this.message = message; }
        public T getData() { return data; }
        public void setData(T data) { this.data = data; }
        public Boolean getSuccess() { return success; }
        public void setSuccess(Boolean success) { this.success = success; }
        public boolean isSuccess() { return Boolean.TRUE.equals(success); }
    }

    /**
     * 数据源信息
     */
    class DataSourceInfo {
        private String id;
        private String sourceName;
        private String sourceType;
        private String host;
        private Integer port;
        private String databaseName;
        private String username;
        private Integer status;
        
        // getters and setters
        public String getId() { return id; }
        public void setId(String id) { this.id = id; }
        public String getSourceName() { return sourceName; }
        public void setSourceName(String sourceName) { this.sourceName = sourceName; }
        public String getSourceType() { return sourceType; }
        public void setSourceType(String sourceType) { this.sourceType = sourceType; }
        public String getHost() { return host; }
        public void setHost(String host) { this.host = host; }
        public Integer getPort() { return port; }
        public void setPort(Integer port) { this.port = port; }
        public String getDatabaseName() { return databaseName; }
        public void setDatabaseName(String databaseName) { this.databaseName = databaseName; }
        public String getUsername() { return username; }
        public void setUsername(String username) { this.username = username; }
        public Integer getStatus() { return status; }
        public void setStatus(Integer status) { this.status = status; }
    }

    /**
     * 表信息
     */
    class TableInfo {
        private String tableName;
        private String tableComment;
        private String tableType;
        private Long tableRows;
        private String engine;
        
        // getters and setters
        public String getTableName() { return tableName; }
        public void setTableName(String tableName) { this.tableName = tableName; }
        public String getTableComment() { return tableComment; }
        public void setTableComment(String tableComment) { this.tableComment = tableComment; }
        public String getTableType() { return tableType; }
        public void setTableType(String tableType) { this.tableType = tableType; }
        public Long getTableRows() { return tableRows; }
        public void setTableRows(Long tableRows) { this.tableRows = tableRows; }
        public String getEngine() { return engine; }
        public void setEngine(String engine) { this.engine = engine; }
    }

    /**
     * 表结构信息
     */
    class TableStructure {
        private String tableName;
        private String tableComment;
        private List<ColumnInfo> columns;
        
        // getters and setters
        public String getTableName() { return tableName; }
        public void setTableName(String tableName) { this.tableName = tableName; }
        public String getTableComment() { return tableComment; }
        public void setTableComment(String tableComment) { this.tableComment = tableComment; }
        public List<ColumnInfo> getColumns() { return columns; }
        public void setColumns(List<ColumnInfo> columns) { this.columns = columns; }
    }

    /**
     * 列信息
     */
    class ColumnInfo {
        private String columnName;
        private String columnType;
        private String dataType;
        private Boolean nullable;
        private String columnDefault;
        private String columnComment;
        private Boolean isPrimaryKey;
        private Boolean isAutoIncrement;
        
        // getters and setters
        public String getColumnName() { return columnName; }
        public void setColumnName(String columnName) { this.columnName = columnName; }
        public String getColumnType() { return columnType; }
        public void setColumnType(String columnType) { this.columnType = columnType; }
        public String getDataType() { return dataType; }
        public void setDataType(String dataType) { this.dataType = dataType; }
        public Boolean getNullable() { return nullable; }
        public void setNullable(Boolean nullable) { this.nullable = nullable; }
        public String getColumnDefault() { return columnDefault; }
        public void setColumnDefault(String columnDefault) { this.columnDefault = columnDefault; }
        public String getColumnComment() { return columnComment; }
        public void setColumnComment(String columnComment) { this.columnComment = columnComment; }
        public Boolean getIsPrimaryKey() { return isPrimaryKey; }
        public void setIsPrimaryKey(Boolean isPrimaryKey) { this.isPrimaryKey = isPrimaryKey; }
        public Boolean getIsAutoIncrement() { return isAutoIncrement; }
        public void setIsAutoIncrement(Boolean isAutoIncrement) { this.isAutoIncrement = isAutoIncrement; }
    }

    /**
     * 查询请求
     */
    class QueryRequest {
        private String dataSourceId;
        private String sql;
        private Map<String, Object> parameters;
        private Integer limit;
        
        // getters and setters
        public String getDataSourceId() { return dataSourceId; }
        public void setDataSourceId(String dataSourceId) { this.dataSourceId = dataSourceId; }
        public String getSql() { return sql; }
        public void setSql(String sql) { this.sql = sql; }
        public Map<String, Object> getParameters() { return parameters; }
        public void setParameters(Map<String, Object> parameters) { this.parameters = parameters; }
        public Integer getLimit() { return limit; }
        public void setLimit(Integer limit) { this.limit = limit; }
    }

    /**
     * 查询结果
     */
    class QueryResult {
        private List<String> columns;
        private List<Map<String, Object>> rows;
        private Integer totalCount;
        private Long executionTime;
        
        // getters and setters
        public List<String> getColumns() { return columns; }
        public void setColumns(List<String> columns) { this.columns = columns; }
        public List<Map<String, Object>> getRows() { return rows; }
        public void setRows(List<Map<String, Object>> rows) { this.rows = rows; }
        public List<Map<String, Object>> getData() { return rows; }
        public Integer getTotalCount() { return totalCount; }
        public void setTotalCount(Integer totalCount) { this.totalCount = totalCount; }
        public Long getExecutionTime() { return executionTime; }
        public void setExecutionTime(Long executionTime) { this.executionTime = executionTime; }
    }

    /**
     * SQL验证请求
     */
    class SqlValidationRequest {
        private String sql;
        private String tableName;
        
        // getters and setters
        public String getSql() { return sql; }
        public void setSql(String sql) { this.sql = sql; }
        public String getTableName() { return tableName; }
        public void setTableName(String tableName) { this.tableName = tableName; }
    }

    /**
     * 验证结果
     */
    class ValidationResult {
        private Boolean valid;
        private String errorMessage;
        private List<String> suggestions;
        
        // getters and setters
        public Boolean getValid() { return valid; }
        public void setValid(Boolean valid) { this.valid = valid; }
        public String getErrorMessage() { return errorMessage; }
        public void setErrorMessage(String errorMessage) { this.errorMessage = errorMessage; }
        public List<String> getSuggestions() { return suggestions; }
        public void setSuggestions(List<String> suggestions) { this.suggestions = suggestions; }
    }
    
    /**
     * 连接测试结果
     */
    class ConnectionTestResult {
        private Boolean success;
        private String message;
        private Long responseTime;
        
        // getters and setters
        public Boolean getSuccess() { return success; }
        public void setSuccess(Boolean success) { this.success = success; }
        public String getMessage() { return message; }
        public void setMessage(String message) { this.message = message; }
        public Long getResponseTime() { return responseTime; }
        public void setResponseTime(Long responseTime) { this.responseTime = responseTime; }
    }
    
    /**
     * 数据源统计信息
     */
    class DataSourceStatistics {
        private Integer totalTables;
        private Long totalRows;
        private String databaseSize;
        private Integer connectionCount;
        
        // getters and setters
        public Integer getTotalTables() { return totalTables; }
        public void setTotalTables(Integer totalTables) { this.totalTables = totalTables; }
        public Long getTotalRows() { return totalRows; }
        public void setTotalRows(Long totalRows) { this.totalRows = totalRows; }
        public String getDatabaseSize() { return databaseSize; }
        public void setDatabaseSize(String databaseSize) { this.databaseSize = databaseSize; }
        public Integer getConnectionCount() { return connectionCount; }
        public void setConnectionCount(Integer connectionCount) { this.connectionCount = connectionCount; }
    }
}