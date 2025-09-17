package com.powertrading.interfaces.service;

import com.powertrading.interfaces.client.DataSourceClient;
import lombok.extern.slf4j.Slf4j;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Service;
import org.springframework.util.CollectionUtils;
import org.springframework.util.StringUtils;

import java.util.*;
import java.util.stream.Collectors;

/**
 * 数据源集成服务
 * 负责与datasource-service的集成，提供数据源管理和数据查询功能
 *
 * @author PowerTrading Team
 * @since 2024-01-15
 */
@Slf4j
@Service
public class DataSourceIntegrationService {

    private static final Logger log = LoggerFactory.getLogger(DataSourceIntegrationService.class);

    @Autowired
    private DataSourceClient dataSourceClient;

    /**
     * 获取可用的数据源列表
     *
     * @return 数据源列表
     */
    @Cacheable(value = "datasource:list", unless = "#result.size() == 0")
    public List<DataSourceInfo> getAvailableDataSources() {
        try {
            DataSourceClient.ApiResponse<List<DataSourceClient.DataSourceInfo>> response = 
                dataSourceClient.getDataSources();
            
            if (!response.isSuccess()) {
                throw new RuntimeException("获取数据源列表失败: " + response.getMessage());
            }
            
            List<DataSourceClient.DataSourceInfo> dataSources = response.getData();
            if (CollectionUtils.isEmpty(dataSources)) {
                return new ArrayList<>();
            }
            
            return dataSources.stream()
                .filter(ds -> "ACTIVE".equals(ds.getStatus())) // 只返回活跃的数据源
                .map(this::convertToDataSourceInfo)
                .collect(Collectors.toList());
            
        } catch (Exception e) {
            log.error("获取可用数据源列表失败", e);
            throw new RuntimeException("获取可用数据源列表失败: " + e.getMessage());
        }
    }

    /**
     * 根据ID获取数据源详情
     *
     * @param dataSourceId 数据源ID
     * @return 数据源详情
     */
    @Cacheable(value = "datasource:detail", key = "#dataSourceId")
    public DataSourceInfo getDataSourceById(String dataSourceId) {
        try {
            DataSourceClient.ApiResponse<DataSourceClient.DataSourceInfo> response = 
                dataSourceClient.getDataSource(dataSourceId);
            
            if (!response.isSuccess()) {
                throw new RuntimeException("获取数据源详情失败: " + response.getMessage());
            }
            
            DataSourceClient.DataSourceInfo dataSource = response.getData();
            if (dataSource == null) {
                throw new RuntimeException("数据源不存在");
            }
            
            return convertToDataSourceInfo(dataSource);
            
        } catch (Exception e) {
            log.error("获取数据源详情失败，数据源ID: {}", dataSourceId, e);
            throw new RuntimeException("获取数据源详情失败: " + e.getMessage());
        }
    }

    /**
     * 获取数据源的表列表
     *
     * @param dataSourceId 数据源ID
     * @return 表列表
     */
    @Cacheable(value = "datasource:tables", key = "#dataSourceId")
    public List<TableInfo> getDataSourceTables(String dataSourceId) {
        try {
            DataSourceClient.ApiResponse<List<DataSourceClient.TableInfo>> response = 
                dataSourceClient.getTables(dataSourceId);
            
            if (!response.isSuccess()) {
                throw new RuntimeException("获取数据表列表失败: " + response.getMessage());
            }
            
            List<DataSourceClient.TableInfo> tables = response.getData();
            if (CollectionUtils.isEmpty(tables)) {
                return new ArrayList<>();
            }
            
            return tables.stream()
                .map(this::convertToTableInfo)
                .sorted(Comparator.comparing(TableInfo::getTableName))
                .collect(Collectors.toList());
            
        } catch (Exception e) {
            log.error("获取数据表列表失败，数据源ID: {}", dataSourceId, e);
            throw new RuntimeException("获取数据表列表失败: " + e.getMessage());
        }
    }

    /**
     * 获取表的字段结构
     *
     * @param dataSourceId 数据源ID
     * @param tableName 表名
     * @return 字段列表
     */
    @Cacheable(value = "datasource:columns", key = "#dataSourceId + ':' + #tableName")
    public List<ColumnInfo> getTableColumns(String dataSourceId, String tableName) {
        try {
            DataSourceClient.ApiResponse<DataSourceClient.TableStructure> response = 
                dataSourceClient.getTableStructure(dataSourceId, tableName);
            
            if (!response.isSuccess()) {
                throw new RuntimeException("获取表字段结构失败: " + response.getMessage());
            }
            
            DataSourceClient.TableStructure tableStructure = response.getData();
            if (tableStructure == null || CollectionUtils.isEmpty(tableStructure.getColumns())) {
                return new ArrayList<>();
            }
            
            return tableStructure.getColumns().stream()
                .map(this::convertToColumnInfo)
                .sorted(Comparator.comparing(ColumnInfo::getColumnName))
                .collect(Collectors.toList());
            
        } catch (Exception e) {
            log.error("获取表字段结构失败，数据源ID: {}, 表名: {}", dataSourceId, tableName, e);
            throw new RuntimeException("获取表字段结构失败: " + e.getMessage());
        }
    }

    /**
     * 执行数据查询
     *
     * @param dataSourceId 数据源ID
     * @param sql SQL语句
     * @param params 查询参数
     * @return 查询结果
     */
    public QueryResult executeQuery(String dataSourceId, String sql, Map<String, Object> params) {
        try {
            DataSourceClient.QueryRequest request = new DataSourceClient.QueryRequest();
            request.setDataSourceId(dataSourceId);
            request.setSql(sql);
            request.setParameters(params);
            
            DataSourceClient.ApiResponse<DataSourceClient.QueryResult> response = 
                dataSourceClient.executeQuery(dataSourceId, request);
            
            if (!response.isSuccess()) {
                throw new RuntimeException("执行数据查询失败: " + response.getMessage());
            }
            
            DataSourceClient.QueryResult queryResult = response.getData();
            return convertToQueryResult(queryResult);
            
        } catch (Exception e) {
            log.error("执行数据查询失败，数据源ID: {}, SQL: {}", dataSourceId, sql, e);
            throw new RuntimeException("执行数据查询失败: " + e.getMessage());
        }
    }

    /**
     * 验证SQL语句
     *
     * @param dataSourceId 数据源ID
     * @param sql SQL语句
     * @return 验证结果
     */
    public SqlValidationResult validateSql(String dataSourceId, String sql) {
        try {
            DataSourceClient.SqlValidationRequest request = new DataSourceClient.SqlValidationRequest();
            request.setSql(sql);
            
            DataSourceClient.ApiResponse<DataSourceClient.ValidationResult> response = 
                dataSourceClient.validateSql(dataSourceId, request);
            
            if (!response.isSuccess()) {
                throw new RuntimeException("SQL验证失败: " + response.getMessage());
            }
            
            DataSourceClient.ValidationResult validationResult = response.getData();
            return convertToSqlValidationResult(validationResult);
            
        } catch (Exception e) {
            log.error("SQL验证失败，数据源ID: {}, SQL: {}", dataSourceId, sql, e);
            throw new RuntimeException("SQL验证失败: " + e.getMessage());
        }
    }

    /**
     * 测试数据源连接
     *
     * @param dataSourceId 数据源ID
     * @return 连接测试结果
     */
    public ConnectionTestResult testDataSourceConnection(String dataSourceId) {
        try {
            DataSourceClient.ApiResponse<DataSourceClient.ConnectionTestResult> response = 
                dataSourceClient.testConnection(dataSourceId);
            
            if (!response.isSuccess()) {
                throw new RuntimeException("测试数据源连接失败: " + response.getMessage());
            }
            
            DataSourceClient.ConnectionTestResult testResult = response.getData();
            return convertToConnectionTestResult(testResult);
            
        } catch (Exception e) {
            log.error("测试数据源连接失败，数据源ID: {}", dataSourceId, e);
            throw new RuntimeException("测试数据源连接失败: " + e.getMessage());
        }
    }

    /**
     * 获取数据源统计信息
     *
     * @param dataSourceId 数据源ID
     * @return 统计信息
     */
    public DataSourceStatistics getDataSourceStatistics(String dataSourceId) {
        try {
            DataSourceClient.ApiResponse<DataSourceClient.DataSourceStatistics> response = 
                dataSourceClient.getDataSourceStatistics(dataSourceId);
            
            if (!response.isSuccess()) {
                throw new RuntimeException("获取数据源统计信息失败: " + response.getMessage());
            }
            
            DataSourceClient.DataSourceStatistics statistics = response.getData();
            return convertToDataSourceStatistics(statistics);
            
        } catch (Exception e) {
            log.error("获取数据源统计信息失败，数据源ID: {}", dataSourceId, e);
            throw new RuntimeException("获取数据源统计信息失败: " + e.getMessage());
        }
    }

    /**
     * 生成表的示例SQL
     *
     * @param dataSourceId 数据源ID
     * @param tableName 表名
     * @param queryType 查询类型（SELECT, COUNT等）
     * @return 示例SQL
     */
    public String generateSampleSql(String dataSourceId, String tableName, String queryType) {
        try {
            // 获取表字段信息
            List<ColumnInfo> columns = getTableColumns(dataSourceId, tableName);
            
            if (CollectionUtils.isEmpty(columns)) {
                throw new RuntimeException("表字段信息为空，无法生成示例SQL");
            }
            
            StringBuilder sql = new StringBuilder();
            
            switch (queryType.toUpperCase()) {
                case "SELECT":
                    sql.append("SELECT ");
                    sql.append(columns.stream()
                        .limit(10) // 限制字段数量
                        .map(ColumnInfo::getColumnName)
                        .collect(Collectors.joining(", ")));
                    sql.append(" FROM ").append(tableName);
                    sql.append(" WHERE 1=1");
                    
                    // 添加常用的查询条件示例
                    Optional<ColumnInfo> dateColumn = columns.stream()
                        .filter(col -> col.getDataType().toLowerCase().contains("date") ||
                                     col.getDataType().toLowerCase().contains("time"))
                        .findFirst();
                    
                    if (dateColumn.isPresent()) {
                        sql.append(" AND ").append(dateColumn.get().getColumnName())
                           .append(" >= '{dataTime}'");
                    }
                    
                    sql.append(" LIMIT 100");
                    break;
                    
                case "COUNT":
                    sql.append("SELECT COUNT(*) as total_count FROM ").append(tableName);
                    sql.append(" WHERE 1=1");
                    break;
                    
                default:
                    sql.append("SELECT * FROM ").append(tableName).append(" LIMIT 10");
                    break;
            }
            
            return sql.toString();
            
        } catch (Exception e) {
            log.error("生成示例SQL失败，数据源ID: {}, 表名: {}, 查询类型: {}", 
                dataSourceId, tableName, queryType, e);
            throw new RuntimeException("生成示例SQL失败: " + e.getMessage());
        }
    }

    /**
     * 转换数据源信息
     */
    private DataSourceInfo convertToDataSourceInfo(DataSourceClient.DataSourceInfo source) {
        DataSourceInfo info = new DataSourceInfo();
        info.setId(source.getId());
        info.setName(source.getSourceName());
        info.setType(source.getSourceType());
        info.setDescription(""); // DataSourceClient.DataSourceInfo没有description字段
        info.setStatus(source.getStatus() != null ? source.getStatus().toString() : "UNKNOWN");
        info.setHost(source.getHost());
        info.setPort(source.getPort());
        info.setDatabase(source.getDatabaseName());
        info.setCreateTime(""); // DataSourceClient.DataSourceInfo没有createTime字段
        info.setUpdateTime(""); // DataSourceClient.DataSourceInfo没有updateTime字段
        return info;
    }

    /**
     * 转换表信息
     */
    private TableInfo convertToTableInfo(DataSourceClient.TableInfo table) {
        TableInfo info = new TableInfo();
        info.setTableName(table.getTableName());
        info.setTableComment(table.getTableComment());
        info.setTableType(table.getTableType());
        info.setRowCount(table.getTableRows());
        info.setCreateTime(""); // DataSourceClient.TableInfo没有createTime字段
        return info;
    }

    /**
     * 转换字段信息
     */
    private ColumnInfo convertToColumnInfo(DataSourceClient.ColumnInfo column) {
        ColumnInfo info = new ColumnInfo();
        info.setColumnName(column.getColumnName());
        info.setDataType(column.getDataType());
        info.setColumnComment(column.getColumnComment());
        info.setIsNullable(column.getNullable());
        info.setColumnDefault(column.getColumnDefault());
        info.setColumnOrder(0); // DataSourceClient.ColumnInfo没有columnOrder字段
        info.setIsPrimaryKey(column.getIsPrimaryKey());
        return info;
    }

    /**
     * 转换查询结果
     */
    private QueryResult convertToQueryResult(DataSourceClient.QueryResult result) {
        QueryResult queryResult = new QueryResult();
        queryResult.setData(result.getData());
        queryResult.setTotalCount(result.getTotalCount() != null ? result.getTotalCount() : 0);
        queryResult.setExecuteTime(result.getExecutionTime() != null ? result.getExecutionTime() : 0);
        queryResult.setColumns(result.getColumns());
        return queryResult;
    }

    /**
     * 转换SQL验证结果
     */
    private SqlValidationResult convertToSqlValidationResult(DataSourceClient.ValidationResult result) {
        SqlValidationResult validationResult = new SqlValidationResult();
        validationResult.setValid(result.getValid() != null ? result.getValid() : false);
        validationResult.setErrorMessage(result.getErrorMessage());
        validationResult.setSuggestions(result.getSuggestions());
        return validationResult;
    }

    /**
     * 转换连接测试结果
     */
    private ConnectionTestResult convertToConnectionTestResult(DataSourceClient.ConnectionTestResult result) {
        ConnectionTestResult testResult = new ConnectionTestResult();
        testResult.setSuccess(result.getSuccess() != null ? result.getSuccess() : false);
        testResult.setMessage(result.getMessage());
        testResult.setResponseTime(result.getResponseTime());
        return testResult;
    }

    /**
     * 转换数据源统计信息
     */
    private DataSourceStatistics convertToDataSourceStatistics(DataSourceClient.DataSourceStatistics statistics) {
        DataSourceStatistics stats = new DataSourceStatistics();
        stats.setTotalTables(statistics.getTotalTables());
        stats.setTotalRows(statistics.getTotalRows());
        stats.setDatabaseSize(statistics.getDatabaseSize());
        stats.setConnectionCount(statistics.getConnectionCount());
        return stats;
    }

    // 内部数据类定义
    
    public static class DataSourceInfo {
        private String id;
        private String name;
        private String type;
        private String description;
        private String status;
        private String host;
        private Integer port;
        private String database;
        private String createTime;
        private String updateTime;
        
        // getters and setters
        public String getId() { return id; }
        public void setId(String id) { this.id = id; }
        public String getName() { return name; }
        public void setName(String name) { this.name = name; }
        public String getType() { return type; }
        public void setType(String type) { this.type = type; }
        public String getDescription() { return description; }
        public void setDescription(String description) { this.description = description; }
        public String getStatus() { return status; }
        public void setStatus(String status) { this.status = status; }
        public String getHost() { return host; }
        public void setHost(String host) { this.host = host; }
        public Integer getPort() { return port; }
        public void setPort(Integer port) { this.port = port; }
        public String getDatabase() { return database; }
        public void setDatabase(String database) { this.database = database; }
        public String getCreateTime() { return createTime; }
        public void setCreateTime(String createTime) { this.createTime = createTime; }
        public String getUpdateTime() { return updateTime; }
        public void setUpdateTime(String updateTime) { this.updateTime = updateTime; }
    }

    public static class TableInfo {
        private String tableName;
        private String tableComment;
        private String tableType;
        private Long rowCount;
        private String createTime;
        
        // getters and setters
        public String getTableName() { return tableName; }
        public void setTableName(String tableName) { this.tableName = tableName; }
        public String getTableComment() { return tableComment; }
        public void setTableComment(String tableComment) { this.tableComment = tableComment; }
        public String getTableType() { return tableType; }
        public void setTableType(String tableType) { this.tableType = tableType; }
        public Long getRowCount() { return rowCount; }
        public void setRowCount(Long rowCount) { this.rowCount = rowCount; }
        public String getCreateTime() { return createTime; }
        public void setCreateTime(String createTime) { this.createTime = createTime; }
    }

    public static class ColumnInfo {
        private String columnName;
        private String dataType;
        private String columnComment;
        private Boolean isNullable;
        private String columnDefault;
        private Integer columnOrder;
        private Boolean isPrimaryKey;
        
        // getters and setters
        public String getColumnName() { return columnName; }
        public void setColumnName(String columnName) { this.columnName = columnName; }
        public String getDataType() { return dataType; }
        public void setDataType(String dataType) { this.dataType = dataType; }
        public String getColumnComment() { return columnComment; }
        public void setColumnComment(String columnComment) { this.columnComment = columnComment; }
        public Boolean getIsNullable() { return isNullable; }
        public void setIsNullable(Boolean isNullable) { this.isNullable = isNullable; }
        public String getColumnDefault() { return columnDefault; }
        public void setColumnDefault(String columnDefault) { this.columnDefault = columnDefault; }
        public Integer getColumnOrder() { return columnOrder; }
        public void setColumnOrder(Integer columnOrder) { this.columnOrder = columnOrder; }
        public Boolean getIsPrimaryKey() { return isPrimaryKey; }
        public void setIsPrimaryKey(Boolean isPrimaryKey) { this.isPrimaryKey = isPrimaryKey; }
    }

    public static class QueryResult {
        private List<Map<String, Object>> data;
        private long totalCount;
        private long executeTime;
        private List<String> columns;
        
        // getters and setters
        public List<Map<String, Object>> getData() { return data; }
        public void setData(List<Map<String, Object>> data) { this.data = data; }
        public long getTotalCount() { return totalCount; }
        public void setTotalCount(long totalCount) { this.totalCount = totalCount; }
        public long getExecuteTime() { return executeTime; }
        public void setExecuteTime(long executeTime) { this.executeTime = executeTime; }
        public List<String> getColumns() { return columns; }
        public void setColumns(List<String> columns) { this.columns = columns; }
    }

    public static class SqlValidationResult {
        private boolean valid;
        private String errorMessage;
        private List<String> suggestions;
        
        // getters and setters
        public boolean isValid() { return valid; }
        public void setValid(boolean valid) { this.valid = valid; }
        public String getErrorMessage() { return errorMessage; }
        public void setErrorMessage(String errorMessage) { this.errorMessage = errorMessage; }
        public List<String> getSuggestions() { return suggestions; }
        public void setSuggestions(List<String> suggestions) { this.suggestions = suggestions; }
    }

    public static class ConnectionTestResult {
        private boolean success;
        private String message;
        private long responseTime;
        
        // getters and setters
        public boolean isSuccess() { return success; }
        public void setSuccess(boolean success) { this.success = success; }
        public String getMessage() { return message; }
        public void setMessage(String message) { this.message = message; }
        public long getResponseTime() { return responseTime; }
        public void setResponseTime(long responseTime) { this.responseTime = responseTime; }
    }

    public static class DataSourceStatistics {
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