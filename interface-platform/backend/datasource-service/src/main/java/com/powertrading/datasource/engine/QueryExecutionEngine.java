package com.powertrading.datasource.engine;

import com.powertrading.datasource.exception.DataSourceException;
import com.powertrading.datasource.manager.DataSourceManager;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

import java.sql.*;
import java.time.LocalDateTime;
import java.util.*;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeUnit;
import java.util.regex.Pattern;

/**
 * SQL查询执行引擎
 * 支持动态SQL生成和安全的查询执行
 * 
 * @author PowerTrading Team
 * @version 1.0.0
 */
@Component
public class QueryExecutionEngine {

    private static final Logger logger = LoggerFactory.getLogger(QueryExecutionEngine.class);

    @Autowired
    private DataSourceManager dataSourceManager;

    @Value("${datasource.query.timeout:30000}")
    private int queryTimeout;

    @Value("${datasource.query.max-rows:10000}")
    private int maxRows;

    @Value("${datasource.query.fetch-size:1000}")
    private int fetchSize;

    // SQL注入防护正则表达式
    private static final Pattern SQL_INJECTION_PATTERN = Pattern.compile(
        "(?i).*(union|select|insert|update|delete|drop|create|alter|exec|execute|script|javascript|vbscript).*"
    );

    // 危险关键字列表
    private static final Set<String> DANGEROUS_KEYWORDS = Set.of(
        "DROP", "DELETE", "INSERT", "UPDATE", "CREATE", "ALTER", "EXEC", "EXECUTE",
        "TRUNCATE", "GRANT", "REVOKE", "COMMIT", "ROLLBACK", "SAVEPOINT"
    );

    /**
     * 执行查询SQL
     * 
     * @param dataSourceId 数据源ID
     * @param sql SQL语句
     * @param parameters 参数
     * @return 查询结果
     * @throws DataSourceException 查询异常
     */
    public QueryResult executeQuery(Long dataSourceId, String sql, Map<String, Object> parameters) 
            throws DataSourceException {
        
        logger.info("开始执行查询: dataSourceId={}, sql={}", dataSourceId, sql);
        
        long startTime = System.currentTimeMillis();
        
        try {
            // 验证SQL安全性
            validateSqlSecurity(sql);
            
            // 获取数据库连接
            try (Connection connection = dataSourceManager.getConnection(dataSourceId)) {
                // 处理动态SQL
                String processedSql = processDynamicSql(sql, parameters);
                
                // 执行查询
                QueryResult result = executeSelectQuery(connection, processedSql, parameters);
                
                long executionTime = System.currentTimeMillis() - startTime;
                result.setExecutionTime(executionTime);
                
                logger.info("查询执行完成: dataSourceId={}, executionTime={}ms, rowCount={}", 
                           dataSourceId, executionTime, result.getRowCount());
                
                return result;
            }
        } catch (Exception e) {
            long executionTime = System.currentTimeMillis() - startTime;
            logger.error("查询执行失败: dataSourceId={}, executionTime={}ms, error={}", 
                        dataSourceId, executionTime, e.getMessage(), e);
            throw new DataSourceException("查询执行失败: " + e.getMessage(), e);
        }
    }

    /**
     * 异步执行查询SQL
     * 
     * @param dataSourceId 数据源ID
     * @param sql SQL语句
     * @param parameters 参数
     * @return 异步查询结果
     */
    public CompletableFuture<QueryResult> executeQueryAsync(Long dataSourceId, String sql, 
                                                           Map<String, Object> parameters) {
        return CompletableFuture.supplyAsync(() -> {
            try {
                return executeQuery(dataSourceId, sql, parameters);
            } catch (DataSourceException e) {
                throw new RuntimeException(e);
            }
        });
    }

    /**
     * 执行更新SQL（仅限特定操作）
     * 
     * @param dataSourceId 数据源ID
     * @param sql SQL语句
     * @param parameters 参数
     * @return 影响行数
     * @throws DataSourceException 执行异常
     */
    public int executeUpdate(Long dataSourceId, String sql, Map<String, Object> parameters) 
            throws DataSourceException {
        
        logger.info("开始执行更新: dataSourceId={}, sql={}", dataSourceId, sql);
        
        long startTime = System.currentTimeMillis();
        
        try {
            // 验证SQL安全性（更严格）
            validateUpdateSqlSecurity(sql);
            
            // 获取数据库连接
            try (Connection connection = dataSourceManager.getConnection(dataSourceId)) {
                // 处理动态SQL
                String processedSql = processDynamicSql(sql, parameters);
                
                // 执行更新
                int affectedRows = executeUpdateQuery(connection, processedSql, parameters);
                
                long executionTime = System.currentTimeMillis() - startTime;
                
                logger.info("更新执行完成: dataSourceId={}, executionTime={}ms, affectedRows={}", 
                           dataSourceId, executionTime, affectedRows);
                
                return affectedRows;
            }
        } catch (Exception e) {
            long executionTime = System.currentTimeMillis() - startTime;
            logger.error("更新执行失败: dataSourceId={}, executionTime={}ms, error={}", 
                        dataSourceId, executionTime, e.getMessage(), e);
            throw new DataSourceException("更新执行失败: " + e.getMessage(), e);
        }
    }

    /**
     * 获取表结构信息
     * 
     * @param dataSourceId 数据源ID
     * @param tableName 表名
     * @return 表结构信息
     * @throws DataSourceException 查询异常
     */
    public TableMetadata getTableMetadata(Long dataSourceId, String tableName) throws DataSourceException {
        logger.info("获取表结构信息: dataSourceId={}, tableName={}", dataSourceId, tableName);
        
        try (Connection connection = dataSourceManager.getConnection(dataSourceId)) {
            DatabaseMetaData metaData = connection.getMetaData();
            
            TableMetadata tableMetadata = new TableMetadata();
            tableMetadata.setTableName(tableName);
            tableMetadata.setColumns(getColumnMetadata(metaData, tableName));
            tableMetadata.setPrimaryKeys(getPrimaryKeys(metaData, tableName));
            tableMetadata.setIndexes(getIndexes(metaData, tableName));
            
            return tableMetadata;
        } catch (Exception e) {
            logger.error("获取表结构信息失败: dataSourceId={}, tableName={}", dataSourceId, tableName, e);
            throw new DataSourceException("获取表结构信息失败: " + e.getMessage(), e);
        }
    }

    /**
     * 获取数据库中的所有表名
     * 
     * @param dataSourceId 数据源ID
     * @param schema 模式名（可选）
     * @return 表名列表
     * @throws DataSourceException 查询异常
     */
    public List<String> getTableNames(Long dataSourceId, String schema) throws DataSourceException {
        logger.info("获取表名列表: dataSourceId={}, schema={}", dataSourceId, schema);
        
        try (Connection connection = dataSourceManager.getConnection(dataSourceId)) {
            DatabaseMetaData metaData = connection.getMetaData();
            
            List<String> tableNames = new ArrayList<>();
            
            try (ResultSet rs = metaData.getTables(null, schema, "%", new String[]{"TABLE"})) {
                while (rs.next()) {
                    tableNames.add(rs.getString("TABLE_NAME"));
                }
            }
            
            logger.info("获取表名列表完成: dataSourceId={}, count={}", dataSourceId, tableNames.size());
            return tableNames;
        } catch (Exception e) {
            logger.error("获取表名列表失败: dataSourceId={}", dataSourceId, e);
            throw new DataSourceException("获取表名列表失败: " + e.getMessage(), e);
        }
    }

    /**
     * 验证SQL安全性
     */
    private void validateSqlSecurity(String sql) throws DataSourceException {
        if (!StringUtils.hasText(sql)) {
            throw new DataSourceException("SQL语句不能为空");
        }
        
        String upperSql = sql.toUpperCase().trim();
        
        // 检查是否为SELECT语句
        if (!upperSql.startsWith("SELECT") && !upperSql.startsWith("WITH")) {
            throw new DataSourceException("只允许执行SELECT查询语句");
        }
        
        // 检查危险关键字
        for (String keyword : DANGEROUS_KEYWORDS) {
            if (upperSql.contains(keyword)) {
                throw new DataSourceException("SQL语句包含危险关键字: " + keyword);
            }
        }
        
        // 检查SQL注入模式
        if (SQL_INJECTION_PATTERN.matcher(sql).matches()) {
            throw new DataSourceException("SQL语句可能存在注入风险");
        }
    }

    /**
     * 验证更新SQL安全性（更严格）
     */
    private void validateUpdateSqlSecurity(String sql) throws DataSourceException {
        if (!StringUtils.hasText(sql)) {
            throw new DataSourceException("SQL语句不能为空");
        }
        
        String upperSql = sql.toUpperCase().trim();
        
        // 只允许特定的更新操作
        if (!upperSql.startsWith("UPDATE") && !upperSql.startsWith("INSERT")) {
            throw new DataSourceException("只允许执行UPDATE和INSERT语句");
        }
        
        // 检查是否包含WHERE子句（UPDATE必须有）
        if (upperSql.startsWith("UPDATE") && !upperSql.contains("WHERE")) {
            throw new DataSourceException("UPDATE语句必须包含WHERE条件");
        }
        
        // 检查危险关键字
        Set<String> strictDangerousKeywords = Set.of(
            "DROP", "DELETE", "TRUNCATE", "CREATE", "ALTER", "EXEC", "EXECUTE"
        );
        
        for (String keyword : strictDangerousKeywords) {
            if (upperSql.contains(keyword)) {
                throw new DataSourceException("SQL语句包含禁止的关键字: " + keyword);
            }
        }
    }

    /**
     * 处理动态SQL
     */
    private String processDynamicSql(String sql, Map<String, Object> parameters) {
        if (parameters == null || parameters.isEmpty()) {
            return sql;
        }
        
        String processedSql = sql;
        
        // 处理命名参数 #{paramName}
        for (Map.Entry<String, Object> entry : parameters.entrySet()) {
            String placeholder = "#{" + entry.getKey() + "}";
            if (processedSql.contains(placeholder)) {
                Object value = entry.getValue();
                String replacement = value != null ? value.toString() : "NULL";
                
                // 如果是字符串，添加引号
                if (value instanceof String) {
                    replacement = "'" + replacement.replace("'", "''") + "'";
                }
                
                processedSql = processedSql.replace(placeholder, replacement);
            }
        }
        
        return processedSql;
    }

    /**
     * 执行SELECT查询
     */
    private QueryResult executeSelectQuery(Connection connection, String sql, Map<String, Object> parameters) 
            throws SQLException {
        
        try (PreparedStatement stmt = connection.prepareStatement(sql)) {
            // 设置查询参数
            stmt.setQueryTimeout(queryTimeout / 1000); // 转换为秒
            stmt.setMaxRows(maxRows);
            stmt.setFetchSize(fetchSize);
            
            // 设置参数值
            setParameters(stmt, sql, parameters);
            
            // 执行查询
            try (ResultSet rs = stmt.executeQuery()) {
                return buildQueryResult(rs);
            }
        }
    }

    /**
     * 执行UPDATE查询
     */
    private int executeUpdateQuery(Connection connection, String sql, Map<String, Object> parameters) 
            throws SQLException {
        
        try (PreparedStatement stmt = connection.prepareStatement(sql)) {
            // 设置查询参数
            stmt.setQueryTimeout(queryTimeout / 1000); // 转换为秒
            
            // 设置参数值
            setParameters(stmt, sql, parameters);
            
            // 执行更新
            return stmt.executeUpdate();
        }
    }

    /**
     * 设置PreparedStatement参数
     */
    private void setParameters(PreparedStatement stmt, String sql, Map<String, Object> parameters) 
            throws SQLException {
        
        if (parameters == null || parameters.isEmpty()) {
            return;
        }
        
        // 计算参数占位符数量
        int paramCount = (int) sql.chars().filter(ch -> ch == '?').count();
        
        if (paramCount > 0 && parameters.size() >= paramCount) {
            int index = 1;
            for (Object value : parameters.values()) {
                if (index > paramCount) break;
                
                if (value == null) {
                    stmt.setNull(index, Types.NULL);
                } else if (value instanceof String) {
                    stmt.setString(index, (String) value);
                } else if (value instanceof Integer) {
                    stmt.setInt(index, (Integer) value);
                } else if (value instanceof Long) {
                    stmt.setLong(index, (Long) value);
                } else if (value instanceof Double) {
                    stmt.setDouble(index, (Double) value);
                } else if (value instanceof Boolean) {
                    stmt.setBoolean(index, (Boolean) value);
                } else if (value instanceof java.util.Date) {
                    stmt.setTimestamp(index, new Timestamp(((java.util.Date) value).getTime()));
                } else {
                    stmt.setObject(index, value);
                }
                
                index++;
            }
        }
    }

    /**
     * 构建查询结果
     */
    private QueryResult buildQueryResult(ResultSet rs) throws SQLException {
        QueryResult result = new QueryResult();
        
        // 获取列信息
        ResultSetMetaData metaData = rs.getMetaData();
        int columnCount = metaData.getColumnCount();
        
        List<ColumnInfo> columns = new ArrayList<>();
        for (int i = 1; i <= columnCount; i++) {
            ColumnInfo column = new ColumnInfo();
            column.setName(metaData.getColumnName(i));
            column.setLabel(metaData.getColumnLabel(i));
            column.setType(metaData.getColumnTypeName(i));
            column.setJdbcType(metaData.getColumnType(i));
            column.setNullable(metaData.isNullable(i) == ResultSetMetaData.columnNullable);
            columns.add(column);
        }
        result.setColumns(columns);
        
        // 获取数据行
        List<Map<String, Object>> rows = new ArrayList<>();
        while (rs.next()) {
            Map<String, Object> row = new LinkedHashMap<>();
            for (int i = 1; i <= columnCount; i++) {
                String columnName = metaData.getColumnName(i);
                Object value = rs.getObject(i);
                row.put(columnName, value);
            }
            rows.add(row);
        }
        result.setRows(rows);
        result.setRowCount(rows.size());
        
        return result;
    }

    /**
     * 获取列元数据
     */
    private List<ColumnMetadata> getColumnMetadata(DatabaseMetaData metaData, String tableName) 
            throws SQLException {
        
        List<ColumnMetadata> columns = new ArrayList<>();
        
        try (ResultSet rs = metaData.getColumns(null, null, tableName, null)) {
            while (rs.next()) {
                ColumnMetadata column = new ColumnMetadata();
                column.setColumnName(rs.getString("COLUMN_NAME"));
                column.setDataType(rs.getString("TYPE_NAME"));
                column.setColumnSize(rs.getInt("COLUMN_SIZE"));
                column.setNullable(rs.getInt("NULLABLE") == DatabaseMetaData.columnNullable);
                column.setDefaultValue(rs.getString("COLUMN_DEF"));
                column.setAutoIncrement("YES".equals(rs.getString("IS_AUTOINCREMENT")));
                columns.add(column);
            }
        }
        
        return columns;
    }

    /**
     * 获取主键信息
     */
    private List<String> getPrimaryKeys(DatabaseMetaData metaData, String tableName) throws SQLException {
        List<String> primaryKeys = new ArrayList<>();
        
        try (ResultSet rs = metaData.getPrimaryKeys(null, null, tableName)) {
            while (rs.next()) {
                primaryKeys.add(rs.getString("COLUMN_NAME"));
            }
        }
        
        return primaryKeys;
    }

    /**
     * 获取索引信息
     */
    private List<IndexMetadata> getIndexes(DatabaseMetaData metaData, String tableName) throws SQLException {
        List<IndexMetadata> indexes = new ArrayList<>();
        
        try (ResultSet rs = metaData.getIndexInfo(null, null, tableName, false, false)) {
            while (rs.next()) {
                IndexMetadata index = new IndexMetadata();
                index.setIndexName(rs.getString("INDEX_NAME"));
                index.setColumnName(rs.getString("COLUMN_NAME"));
                index.setUnique(!rs.getBoolean("NON_UNIQUE"));
                indexes.add(index);
            }
        }
        
        return indexes;
    }

    // 内部类定义
    public static class QueryResult {
        private List<ColumnInfo> columns;
        private List<Map<String, Object>> rows;
        private int rowCount;
        private long executionTime;
        private LocalDateTime executeTime = LocalDateTime.now();

        // Getters and Setters
        public List<ColumnInfo> getColumns() { return columns; }
        public void setColumns(List<ColumnInfo> columns) { this.columns = columns; }
        public List<Map<String, Object>> getRows() { return rows; }
        public void setRows(List<Map<String, Object>> rows) { this.rows = rows; }
        public int getRowCount() { return rowCount; }
        public void setRowCount(int rowCount) { this.rowCount = rowCount; }
        public long getExecutionTime() { return executionTime; }
        public void setExecutionTime(long executionTime) { this.executionTime = executionTime; }
        public LocalDateTime getExecuteTime() { return executeTime; }
        public void setExecuteTime(LocalDateTime executeTime) { this.executeTime = executeTime; }
    }

    public static class ColumnInfo {
        private String name;
        private String label;
        private String type;
        private int jdbcType;
        private boolean nullable;

        // Getters and Setters
        public String getName() { return name; }
        public void setName(String name) { this.name = name; }
        public String getLabel() { return label; }
        public void setLabel(String label) { this.label = label; }
        public String getType() { return type; }
        public void setType(String type) { this.type = type; }
        public int getJdbcType() { return jdbcType; }
        public void setJdbcType(int jdbcType) { this.jdbcType = jdbcType; }
        public boolean isNullable() { return nullable; }
        public void setNullable(boolean nullable) { this.nullable = nullable; }
    }

    public static class TableMetadata {
        private String tableName;
        private List<ColumnMetadata> columns;
        private List<String> primaryKeys;
        private List<IndexMetadata> indexes;

        // Getters and Setters
        public String getTableName() { return tableName; }
        public void setTableName(String tableName) { this.tableName = tableName; }
        public List<ColumnMetadata> getColumns() { return columns; }
        public void setColumns(List<ColumnMetadata> columns) { this.columns = columns; }
        public List<String> getPrimaryKeys() { return primaryKeys; }
        public void setPrimaryKeys(List<String> primaryKeys) { this.primaryKeys = primaryKeys; }
        public List<IndexMetadata> getIndexes() { return indexes; }
        public void setIndexes(List<IndexMetadata> indexes) { this.indexes = indexes; }
    }

    public static class ColumnMetadata {
        private String columnName;
        private String dataType;
        private int columnSize;
        private boolean nullable;
        private String defaultValue;
        private boolean autoIncrement;

        // Getters and Setters
        public String getColumnName() { return columnName; }
        public void setColumnName(String columnName) { this.columnName = columnName; }
        public String getDataType() { return dataType; }
        public void setDataType(String dataType) { this.dataType = dataType; }
        public int getColumnSize() { return columnSize; }
        public void setColumnSize(int columnSize) { this.columnSize = columnSize; }
        public boolean isNullable() { return nullable; }
        public void setNullable(boolean nullable) { this.nullable = nullable; }
        public String getDefaultValue() { return defaultValue; }
        public void setDefaultValue(String defaultValue) { this.defaultValue = defaultValue; }
        public boolean isAutoIncrement() { return autoIncrement; }
        public void setAutoIncrement(boolean autoIncrement) { this.autoIncrement = autoIncrement; }
    }

    public static class IndexMetadata {
        private String indexName;
        private String columnName;
        private boolean unique;

        // Getters and Setters
        public String getIndexName() { return indexName; }
        public void setIndexName(String indexName) { this.indexName = indexName; }
        public String getColumnName() { return columnName; }
        public void setColumnName(String columnName) { this.columnName = columnName; }
        public boolean isUnique() { return unique; }
        public void setUnique(boolean unique) { this.unique = unique; }
    }
}