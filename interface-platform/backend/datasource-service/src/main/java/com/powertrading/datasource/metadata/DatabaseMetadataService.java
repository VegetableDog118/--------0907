package com.powertrading.datasource.metadata;

import com.powertrading.datasource.cache.QueryCacheService;
import com.powertrading.datasource.engine.QueryExecutionEngine;
import com.powertrading.datasource.exception.DataSourceException;
import com.powertrading.datasource.manager.DataSourceManager;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.sql.*;
import java.time.LocalDateTime;
import java.util.*;
import java.util.stream.Collectors;

/**
 * 数据库元数据服务
 * 提供数据库表结构、索引、约束等元数据信息的统一访问接口
 * 
 * @author PowerTrading Team
 * @version 1.0.0
 */
@Service
public class DatabaseMetadataService {

    private static final Logger logger = LoggerFactory.getLogger(DatabaseMetadataService.class);

    @Autowired
    private DataSourceManager dataSourceManager;

    @Autowired
    private QueryCacheService queryCacheService;

    /**
     * 获取数据库基本信息
     * 
     * @param dataSourceId 数据源ID
     * @return 数据库信息
     * @throws DataSourceException 数据源异常
     */
    public DatabaseInfo getDatabaseInfo(Long dataSourceId) throws DataSourceException {
        logger.debug("获取数据库基本信息: dataSourceId={}", dataSourceId);
        
        try (Connection connection = dataSourceManager.getConnection(dataSourceId)) {
            DatabaseMetaData metaData = connection.getMetaData();
            
            DatabaseInfo dbInfo = new DatabaseInfo();
            dbInfo.setDataSourceId(dataSourceId);
            dbInfo.setDatabaseProductName(metaData.getDatabaseProductName());
            dbInfo.setDatabaseProductVersion(metaData.getDatabaseProductVersion());
            dbInfo.setDriverName(metaData.getDriverName());
            dbInfo.setDriverVersion(metaData.getDriverVersion());
            dbInfo.setUrl(metaData.getURL());
            dbInfo.setUserName(metaData.getUserName());
            dbInfo.setMaxConnections(metaData.getMaxConnections());
            dbInfo.setMaxTableNameLength(metaData.getMaxTableNameLength());
            dbInfo.setMaxColumnNameLength(metaData.getMaxColumnNameLength());
            dbInfo.setSupportsBatchUpdates(metaData.supportsBatchUpdates());
            dbInfo.setSupportsTransactions(metaData.supportsTransactions());
            dbInfo.setRetrieveTime(LocalDateTime.now());
            
            return dbInfo;
            
        } catch (SQLException e) {
            logger.error("获取数据库基本信息失败: dataSourceId={}", dataSourceId, e);
            throw new DataSourceException("获取数据库基本信息失败: " + e.getMessage(), e);
        }
    }

    /**
     * 获取数据库模式列表
     * 
     * @param dataSourceId 数据源ID
     * @return 模式列表
     * @throws DataSourceException 数据源异常
     */
    public List<SchemaInfo> getSchemas(Long dataSourceId) throws DataSourceException {
        logger.debug("获取数据库模式列表: dataSourceId={}", dataSourceId);
        
        try (Connection connection = dataSourceManager.getConnection(dataSourceId)) {
            DatabaseMetaData metaData = connection.getMetaData();
            List<SchemaInfo> schemas = new ArrayList<>();
            
            try (ResultSet rs = metaData.getSchemas()) {
                while (rs.next()) {
                    SchemaInfo schema = new SchemaInfo();
                    schema.setSchemaName(rs.getString("TABLE_SCHEM"));
                    schema.setCatalogName(rs.getString("TABLE_CATALOG"));
                    schemas.add(schema);
                }
            }
            
            // 如果没有模式信息，尝试获取目录信息
            if (schemas.isEmpty()) {
                try (ResultSet rs = metaData.getCatalogs()) {
                    while (rs.next()) {
                        SchemaInfo schema = new SchemaInfo();
                        schema.setSchemaName(rs.getString("TABLE_CAT"));
                        schema.setCatalogName(rs.getString("TABLE_CAT"));
                        schemas.add(schema);
                    }
                }
            }
            
            logger.debug("获取到模式数量: {}", schemas.size());
            return schemas;
            
        } catch (SQLException e) {
            logger.error("获取数据库模式列表失败: dataSourceId={}", dataSourceId, e);
            throw new DataSourceException("获取数据库模式列表失败: " + e.getMessage(), e);
        }
    }

    /**
     * 获取表列表
     * 
     * @param dataSourceId 数据源ID
     * @param schemaName 模式名
     * @param tableNamePattern 表名模式
     * @param tableTypes 表类型
     * @return 表列表
     * @throws DataSourceException 数据源异常
     */
    public List<TableInfo> getTables(Long dataSourceId, String schemaName, String tableNamePattern, 
                                   String[] tableTypes) throws DataSourceException {
        
        logger.debug("获取表列表: dataSourceId={}, schema={}, pattern={}", dataSourceId, schemaName, tableNamePattern);
        
        // 尝试从缓存获取
        List<TableInfo> cachedTables = getCachedTableList(dataSourceId, schemaName, tableNamePattern);
        if (cachedTables != null) {
            return cachedTables;
        }
        
        try (Connection connection = dataSourceManager.getConnection(dataSourceId)) {
            DatabaseMetaData metaData = connection.getMetaData();
            List<TableInfo> tables = new ArrayList<>();
            
            String[] types = tableTypes != null ? tableTypes : new String[]{"TABLE", "VIEW"};
            String pattern = StringUtils.hasText(tableNamePattern) ? tableNamePattern : "%";
            
            try (ResultSet rs = metaData.getTables(null, schemaName, pattern, types)) {
                while (rs.next()) {
                    TableInfo table = new TableInfo();
                    table.setDataSourceId(dataSourceId);
                    table.setCatalogName(rs.getString("TABLE_CAT"));
                    table.setSchemaName(rs.getString("TABLE_SCHEM"));
                    table.setTableName(rs.getString("TABLE_NAME"));
                    table.setTableType(rs.getString("TABLE_TYPE"));
                    table.setRemarks(rs.getString("REMARKS"));
                    
                    // 获取表的行数（可选）
                    try {
                        table.setRowCount(getTableRowCount(connection, table.getSchemaName(), table.getTableName()));
                    } catch (Exception e) {
                        logger.debug("获取表行数失败: {}.{}", table.getSchemaName(), table.getTableName());
                    }
                    
                    tables.add(table);
                }
            }
            
            // 缓存结果
            cacheTableList(dataSourceId, schemaName, tableNamePattern, tables);
            
            logger.debug("获取到表数量: {}", tables.size());
            return tables;
            
        } catch (SQLException e) {
            logger.error("获取表列表失败: dataSourceId={}", dataSourceId, e);
            throw new DataSourceException("获取表列表失败: " + e.getMessage(), e);
        }
    }

    /**
     * 获取表的详细信息
     * 
     * @param dataSourceId 数据源ID
     * @param schemaName 模式名
     * @param tableName 表名
     * @return 表详细信息
     * @throws DataSourceException 数据源异常
     */
    public TableDetailInfo getTableDetail(Long dataSourceId, String schemaName, String tableName) 
            throws DataSourceException {
        
        logger.debug("获取表详细信息: dataSourceId={}, schema={}, table={}", dataSourceId, schemaName, tableName);
        
        // 尝试从缓存获取
        QueryExecutionEngine.TableMetadata cachedMetadata = 
            queryCacheService.getCachedTableMetadata(dataSourceId, tableName);
        if (cachedMetadata != null) {
            return convertToTableDetailInfo(cachedMetadata, dataSourceId, schemaName);
        }
        
        try (Connection connection = dataSourceManager.getConnection(dataSourceId)) {
            DatabaseMetaData metaData = connection.getMetaData();
            
            TableDetailInfo tableDetail = new TableDetailInfo();
            tableDetail.setDataSourceId(dataSourceId);
            tableDetail.setSchemaName(schemaName);
            tableDetail.setTableName(tableName);
            
            // 获取列信息
            List<ColumnDetailInfo> columns = getColumnDetails(metaData, schemaName, tableName);
            tableDetail.setColumns(columns);
            
            // 获取主键信息
            List<String> primaryKeys = getPrimaryKeys(metaData, schemaName, tableName);
            tableDetail.setPrimaryKeys(primaryKeys);
            
            // 获取外键信息
            List<ForeignKeyInfo> foreignKeys = getForeignKeys(metaData, schemaName, tableName);
            tableDetail.setForeignKeys(foreignKeys);
            
            // 获取索引信息
            List<IndexDetailInfo> indexes = getIndexDetails(metaData, schemaName, tableName);
            tableDetail.setIndexes(indexes);
            
            // 获取表统计信息
            TableStatistics statistics = getTableStatistics(connection, schemaName, tableName);
            tableDetail.setStatistics(statistics);
            
            tableDetail.setRetrieveTime(LocalDateTime.now());
            
            // 缓存结果
            QueryExecutionEngine.TableMetadata metadata = convertToTableMetadata(tableDetail);
            queryCacheService.cacheTableMetadata(dataSourceId, tableName, metadata);
            
            return tableDetail;
            
        } catch (SQLException e) {
            logger.error("获取表详细信息失败: dataSourceId={}, table={}", dataSourceId, tableName, e);
            throw new DataSourceException("获取表详细信息失败: " + e.getMessage(), e);
        }
    }

    /**
     * 获取列详细信息
     * 
     * @param dataSourceId 数据源ID
     * @param schemaName 模式名
     * @param tableName 表名
     * @param columnName 列名
     * @return 列详细信息
     * @throws DataSourceException 数据源异常
     */
    public ColumnDetailInfo getColumnDetail(Long dataSourceId, String schemaName, String tableName, 
                                          String columnName) throws DataSourceException {
        
        logger.debug("获取列详细信息: dataSourceId={}, table={}, column={}", dataSourceId, tableName, columnName);
        
        try (Connection connection = dataSourceManager.getConnection(dataSourceId)) {
            DatabaseMetaData metaData = connection.getMetaData();
            
            try (ResultSet rs = metaData.getColumns(null, schemaName, tableName, columnName)) {
                if (rs.next()) {
                    ColumnDetailInfo column = new ColumnDetailInfo();
                    column.setDataSourceId(dataSourceId);
                    column.setSchemaName(schemaName);
                    column.setTableName(tableName);
                    column.setColumnName(rs.getString("COLUMN_NAME"));
                    column.setDataType(rs.getString("TYPE_NAME"));
                    column.setJdbcType(rs.getInt("DATA_TYPE"));
                    column.setColumnSize(rs.getInt("COLUMN_SIZE"));
                    column.setDecimalDigits(rs.getInt("DECIMAL_DIGITS"));
                    column.setNullable(rs.getInt("NULLABLE") == DatabaseMetaData.columnNullable);
                    column.setDefaultValue(rs.getString("COLUMN_DEF"));
                    column.setRemarks(rs.getString("REMARKS"));
                    column.setOrdinalPosition(rs.getInt("ORDINAL_POSITION"));
                    column.setAutoIncrement("YES".equals(rs.getString("IS_AUTOINCREMENT")));
                    
                    return column;
                }
            }
            
            throw new DataSourceException("列不存在: " + columnName);
            
        } catch (SQLException e) {
            logger.error("获取列详细信息失败: dataSourceId={}, column={}", dataSourceId, columnName, e);
            throw new DataSourceException("获取列详细信息失败: " + e.getMessage(), e);
        }
    }

    /**
     * 搜索表和列
     * 
     * @param dataSourceId 数据源ID
     * @param keyword 关键字
     * @param searchScope 搜索范围
     * @return 搜索结果
     * @throws DataSourceException 数据源异常
     */
    public SearchResult searchTablesAndColumns(Long dataSourceId, String keyword, SearchScope searchScope) 
            throws DataSourceException {
        
        logger.debug("搜索表和列: dataSourceId={}, keyword={}, scope={}", dataSourceId, keyword, searchScope);
        
        SearchResult result = new SearchResult();
        result.setKeyword(keyword);
        result.setSearchScope(searchScope);
        result.setSearchTime(LocalDateTime.now());
        
        if (!StringUtils.hasText(keyword)) {
            return result;
        }
        
        try (Connection connection = dataSourceManager.getConnection(dataSourceId)) {
            DatabaseMetaData metaData = connection.getMetaData();
            
            // 搜索表名
            if (searchScope == SearchScope.ALL || searchScope == SearchScope.TABLES_ONLY) {
                List<TableInfo> matchedTables = searchTables(metaData, dataSourceId, keyword);
                result.setMatchedTables(matchedTables);
            }
            
            // 搜索列名
            if (searchScope == SearchScope.ALL || searchScope == SearchScope.COLUMNS_ONLY) {
                List<ColumnSearchResult> matchedColumns = searchColumns(metaData, dataSourceId, keyword);
                result.setMatchedColumns(matchedColumns);
            }
            
            result.setTotalMatches(result.getMatchedTables().size() + result.getMatchedColumns().size());
            
            return result;
            
        } catch (SQLException e) {
            logger.error("搜索表和列失败: dataSourceId={}", dataSourceId, e);
            throw new DataSourceException("搜索表和列失败: " + e.getMessage(), e);
        }
    }

    /**
     * 生成表的DDL语句
     * 
     * @param dataSourceId 数据源ID
     * @param schemaName 模式名
     * @param tableName 表名
     * @return DDL语句
     * @throws DataSourceException 数据源异常
     */
    public String generateTableDDL(Long dataSourceId, String schemaName, String tableName) 
            throws DataSourceException {
        
        logger.debug("生成表DDL: dataSourceId={}, table={}", dataSourceId, tableName);
        
        TableDetailInfo tableDetail = getTableDetail(dataSourceId, schemaName, tableName);
        
        StringBuilder ddl = new StringBuilder();
        ddl.append("CREATE TABLE ");
        
        if (StringUtils.hasText(schemaName)) {
            ddl.append(schemaName).append(".");
        }
        
        ddl.append(tableName).append(" (\n");
        
        // 添加列定义
        List<String> columnDefs = new ArrayList<>();
        for (ColumnDetailInfo column : tableDetail.getColumns()) {
            StringBuilder columnDef = new StringBuilder();
            columnDef.append("  ").append(column.getColumnName()).append(" ");
            columnDef.append(column.getDataType());
            
            if (column.getColumnSize() > 0 && needsSize(column.getDataType())) {
                columnDef.append("(").append(column.getColumnSize());
                if (column.getDecimalDigits() > 0) {
                    columnDef.append(",").append(column.getDecimalDigits());
                }
                columnDef.append(")");
            }
            
            if (!column.isNullable()) {
                columnDef.append(" NOT NULL");
            }
            
            if (StringUtils.hasText(column.getDefaultValue())) {
                columnDef.append(" DEFAULT ").append(column.getDefaultValue());
            }
            
            if (column.isAutoIncrement()) {
                columnDef.append(" AUTO_INCREMENT");
            }
            
            columnDefs.add(columnDef.toString());
        }
        
        ddl.append(String.join(",\n", columnDefs));
        
        // 添加主键约束
        if (!tableDetail.getPrimaryKeys().isEmpty()) {
            ddl.append(",\n  PRIMARY KEY (");
            ddl.append(String.join(", ", tableDetail.getPrimaryKeys()));
            ddl.append(")");
        }
        
        ddl.append("\n);");
        
        // 添加索引
        for (IndexDetailInfo index : tableDetail.getIndexes()) {
            if (!index.isPrimaryKey()) {
                ddl.append("\n\nCREATE ");
                if (index.isUnique()) {
                    ddl.append("UNIQUE ");
                }
                ddl.append("INDEX ").append(index.getIndexName());
                ddl.append(" ON ");
                if (StringUtils.hasText(schemaName)) {
                    ddl.append(schemaName).append(".");
                }
                ddl.append(tableName);
                ddl.append(" (").append(String.join(", ", index.getColumnNames())).append(");");
            }
        }
        
        return ddl.toString();
    }

    /**
     * 获取数据库统计信息
     * 
     * @param dataSourceId 数据源ID
     * @return 数据库统计信息
     * @throws DataSourceException 数据源异常
     */
    public DatabaseStatistics getDatabaseStatistics(Long dataSourceId) throws DataSourceException {
        logger.debug("获取数据库统计信息: dataSourceId={}", dataSourceId);
        
        try (Connection connection = dataSourceManager.getConnection(dataSourceId)) {
            DatabaseStatistics stats = new DatabaseStatistics();
            stats.setDataSourceId(dataSourceId);
            
            // 获取表数量
            DatabaseMetaData metaData = connection.getMetaData();
            int tableCount = 0;
            int viewCount = 0;
            
            try (ResultSet rs = metaData.getTables(null, null, "%", new String[]{"TABLE", "VIEW"})) {
                while (rs.next()) {
                    String tableType = rs.getString("TABLE_TYPE");
                    if ("TABLE".equals(tableType)) {
                        tableCount++;
                    } else if ("VIEW".equals(tableType)) {
                        viewCount++;
                    }
                }
            }
            
            stats.setTableCount(tableCount);
            stats.setViewCount(viewCount);
            
            // 获取数据库大小（如果支持）
            try {
                long databaseSize = getDatabaseSize(connection);
                stats.setDatabaseSize(databaseSize);
            } catch (Exception e) {
                logger.debug("获取数据库大小失败: {}", e.getMessage());
            }
            
            stats.setStatisticsTime(LocalDateTime.now());
            
            return stats;
            
        } catch (SQLException e) {
            logger.error("获取数据库统计信息失败: dataSourceId={}", dataSourceId, e);
            throw new DataSourceException("获取数据库统计信息失败: " + e.getMessage(), e);
        }
    }

    // 私有辅助方法
    
    private List<ColumnDetailInfo> getColumnDetails(DatabaseMetaData metaData, String schemaName, String tableName) 
            throws SQLException {
        List<ColumnDetailInfo> columns = new ArrayList<>();
        
        try (ResultSet rs = metaData.getColumns(null, schemaName, tableName, null)) {
            while (rs.next()) {
                ColumnDetailInfo column = new ColumnDetailInfo();
                column.setColumnName(rs.getString("COLUMN_NAME"));
                column.setDataType(rs.getString("TYPE_NAME"));
                column.setJdbcType(rs.getInt("DATA_TYPE"));
                column.setColumnSize(rs.getInt("COLUMN_SIZE"));
                column.setDecimalDigits(rs.getInt("DECIMAL_DIGITS"));
                column.setNullable(rs.getInt("NULLABLE") == DatabaseMetaData.columnNullable);
                column.setDefaultValue(rs.getString("COLUMN_DEF"));
                column.setRemarks(rs.getString("REMARKS"));
                column.setOrdinalPosition(rs.getInt("ORDINAL_POSITION"));
                column.setAutoIncrement("YES".equals(rs.getString("IS_AUTOINCREMENT")));
                columns.add(column);
            }
        }
        
        return columns;
    }

    private List<String> getPrimaryKeys(DatabaseMetaData metaData, String schemaName, String tableName) 
            throws SQLException {
        List<String> primaryKeys = new ArrayList<>();
        
        try (ResultSet rs = metaData.getPrimaryKeys(null, schemaName, tableName)) {
            while (rs.next()) {
                primaryKeys.add(rs.getString("COLUMN_NAME"));
            }
        }
        
        return primaryKeys;
    }

    private List<ForeignKeyInfo> getForeignKeys(DatabaseMetaData metaData, String schemaName, String tableName) 
            throws SQLException {
        List<ForeignKeyInfo> foreignKeys = new ArrayList<>();
        
        try (ResultSet rs = metaData.getImportedKeys(null, schemaName, tableName)) {
            while (rs.next()) {
                ForeignKeyInfo fk = new ForeignKeyInfo();
                fk.setForeignKeyName(rs.getString("FK_NAME"));
                fk.setColumnName(rs.getString("FKCOLUMN_NAME"));
                fk.setReferencedTableName(rs.getString("PKTABLE_NAME"));
                fk.setReferencedColumnName(rs.getString("PKCOLUMN_NAME"));
                fk.setUpdateRule(rs.getInt("UPDATE_RULE"));
                fk.setDeleteRule(rs.getInt("DELETE_RULE"));
                foreignKeys.add(fk);
            }
        }
        
        return foreignKeys;
    }

    private List<IndexDetailInfo> getIndexDetails(DatabaseMetaData metaData, String schemaName, String tableName) 
            throws SQLException {
        Map<String, IndexDetailInfo> indexMap = new HashMap<>();
        
        try (ResultSet rs = metaData.getIndexInfo(null, schemaName, tableName, false, false)) {
            while (rs.next()) {
                String indexName = rs.getString("INDEX_NAME");
                if (indexName == null) continue;
                
                IndexDetailInfo index = indexMap.computeIfAbsent(indexName, k -> {
                    IndexDetailInfo idx = new IndexDetailInfo();
                    idx.setIndexName(indexName);
                    try {
                        idx.setUnique(!rs.getBoolean("NON_UNIQUE"));
                    } catch (SQLException e) {
                        idx.setUnique(false); // 默认值
                    }
                    idx.setColumnNames(new ArrayList<>());
                    return idx;
                });
                
                String columnName = rs.getString("COLUMN_NAME");
                if (columnName != null) {
                    index.getColumnNames().add(columnName);
                }
            }
        }
        
        return new ArrayList<>(indexMap.values());
    }

    private TableStatistics getTableStatistics(Connection connection, String schemaName, String tableName) {
        TableStatistics stats = new TableStatistics();
        
        try {
            // 获取行数
            long rowCount = getTableRowCount(connection, schemaName, tableName);
            stats.setRowCount(rowCount);
            
            // 获取表大小（如果支持）
            long tableSize = getTableSize(connection, schemaName, tableName);
            stats.setTableSize(tableSize);
            
        } catch (Exception e) {
            logger.debug("获取表统计信息失败: {}.{}", schemaName, tableName);
        }
        
        stats.setLastUpdated(LocalDateTime.now());
        return stats;
    }

    private long getTableRowCount(Connection connection, String schemaName, String tableName) throws SQLException {
        String sql = "SELECT COUNT(*) FROM ";
        if (StringUtils.hasText(schemaName)) {
            sql += schemaName + ".";
        }
        sql += tableName;
        
        try (PreparedStatement stmt = connection.prepareStatement(sql);
             ResultSet rs = stmt.executeQuery()) {
            if (rs.next()) {
                return rs.getLong(1);
            }
        }
        
        return 0;
    }

    private long getTableSize(Connection connection, String schemaName, String tableName) {
        // 不同数据库的表大小查询方式不同，这里提供MySQL的示例
        try {
            String sql = "SELECT data_length + index_length as table_size " +
                        "FROM information_schema.tables " +
                        "WHERE table_schema = ? AND table_name = ?";
            
            try (PreparedStatement stmt = connection.prepareStatement(sql)) {
                stmt.setString(1, schemaName);
                stmt.setString(2, tableName);
                
                try (ResultSet rs = stmt.executeQuery()) {
                    if (rs.next()) {
                        return rs.getLong("table_size");
                    }
                }
            }
        } catch (Exception e) {
            logger.debug("获取表大小失败: {}.{}", schemaName, tableName);
        }
        
        return 0;
    }

    private long getDatabaseSize(Connection connection) throws SQLException {
        // MySQL示例
        String sql = "SELECT SUM(data_length + index_length) as database_size " +
                    "FROM information_schema.tables";
        
        try (PreparedStatement stmt = connection.prepareStatement(sql);
             ResultSet rs = stmt.executeQuery()) {
            if (rs.next()) {
                return rs.getLong("database_size");
            }
        }
        
        return 0;
    }

    private List<TableInfo> searchTables(DatabaseMetaData metaData, Long dataSourceId, String keyword) 
            throws SQLException {
        List<TableInfo> matchedTables = new ArrayList<>();
        
        try (ResultSet rs = metaData.getTables(null, null, "%" + keyword + "%", new String[]{"TABLE", "VIEW"})) {
            while (rs.next()) {
                TableInfo table = new TableInfo();
                table.setDataSourceId(dataSourceId);
                table.setSchemaName(rs.getString("TABLE_SCHEM"));
                table.setTableName(rs.getString("TABLE_NAME"));
                table.setTableType(rs.getString("TABLE_TYPE"));
                table.setRemarks(rs.getString("REMARKS"));
                matchedTables.add(table);
            }
        }
        
        return matchedTables;
    }

    private List<ColumnSearchResult> searchColumns(DatabaseMetaData metaData, Long dataSourceId, String keyword) 
            throws SQLException {
        List<ColumnSearchResult> matchedColumns = new ArrayList<>();
        
        try (ResultSet rs = metaData.getColumns(null, null, "%", "%" + keyword + "%")) {
            while (rs.next()) {
                ColumnSearchResult column = new ColumnSearchResult();
                column.setDataSourceId(dataSourceId);
                column.setSchemaName(rs.getString("TABLE_SCHEM"));
                column.setTableName(rs.getString("TABLE_NAME"));
                column.setColumnName(rs.getString("COLUMN_NAME"));
                column.setDataType(rs.getString("TYPE_NAME"));
                column.setRemarks(rs.getString("REMARKS"));
                matchedColumns.add(column);
            }
        }
        
        return matchedColumns;
    }

    private boolean needsSize(String dataType) {
        String upperType = dataType.toUpperCase();
        return upperType.contains("CHAR") || upperType.contains("DECIMAL") || 
               upperType.contains("NUMERIC") || upperType.contains("FLOAT");
    }

    private List<TableInfo> getCachedTableList(Long dataSourceId, String schemaName, String tableNamePattern) {
        // 实现缓存逻辑
        return null;
    }

    private void cacheTableList(Long dataSourceId, String schemaName, String tableNamePattern, List<TableInfo> tables) {
        // 实现缓存逻辑
    }

    private TableDetailInfo convertToTableDetailInfo(QueryExecutionEngine.TableMetadata metadata, 
                                                   Long dataSourceId, String schemaName) {
        TableDetailInfo detail = new TableDetailInfo();
        detail.setDataSourceId(dataSourceId);
        detail.setSchemaName(schemaName);
        detail.setTableName(metadata.getTableName());
        
        // 转换列信息
        List<ColumnDetailInfo> columns = metadata.getColumns().stream()
            .map(this::convertToColumnDetailInfo)
            .collect(Collectors.toList());
        detail.setColumns(columns);
        
        detail.setPrimaryKeys(metadata.getPrimaryKeys());
        detail.setRetrieveTime(LocalDateTime.now());
        
        return detail;
    }

    private ColumnDetailInfo convertToColumnDetailInfo(QueryExecutionEngine.ColumnMetadata metadata) {
        ColumnDetailInfo detail = new ColumnDetailInfo();
        detail.setColumnName(metadata.getColumnName());
        detail.setDataType(metadata.getDataType());
        detail.setColumnSize(metadata.getColumnSize());
        detail.setNullable(metadata.isNullable());
        detail.setDefaultValue(metadata.getDefaultValue());
        detail.setAutoIncrement(metadata.isAutoIncrement());
        return detail;
    }

    private QueryExecutionEngine.TableMetadata convertToTableMetadata(TableDetailInfo detail) {
        QueryExecutionEngine.TableMetadata metadata = new QueryExecutionEngine.TableMetadata();
        metadata.setTableName(detail.getTableName());
        metadata.setPrimaryKeys(detail.getPrimaryKeys());
        
        List<QueryExecutionEngine.ColumnMetadata> columns = detail.getColumns().stream()
            .map(this::convertToColumnMetadata)
            .collect(Collectors.toList());
        metadata.setColumns(columns);
        
        return metadata;
    }

    private QueryExecutionEngine.ColumnMetadata convertToColumnMetadata(ColumnDetailInfo detail) {
        QueryExecutionEngine.ColumnMetadata metadata = new QueryExecutionEngine.ColumnMetadata();
        metadata.setColumnName(detail.getColumnName());
        metadata.setDataType(detail.getDataType());
        metadata.setColumnSize(detail.getColumnSize());
        metadata.setNullable(detail.isNullable());
        metadata.setDefaultValue(detail.getDefaultValue());
        metadata.setAutoIncrement(detail.isAutoIncrement());
        return metadata;
    }

    // 枚举和内部类定义
    public enum SearchScope {
        ALL, TABLES_ONLY, COLUMNS_ONLY
    }

    // 数据类定义（省略getter/setter方法）
    public static class DatabaseInfo {
        private Long dataSourceId;
        private String databaseProductName;
        private String databaseProductVersion;
        private String driverName;
        private String driverVersion;
        private String url;
        private String userName;
        private int maxConnections;
        private int maxTableNameLength;
        private int maxColumnNameLength;
        private boolean supportsBatchUpdates;
        private boolean supportsTransactions;
        private LocalDateTime retrieveTime;

        // Getters and Setters
        public Long getDataSourceId() { return dataSourceId; }
        public void setDataSourceId(Long dataSourceId) { this.dataSourceId = dataSourceId; }
        public String getDatabaseProductName() { return databaseProductName; }
        public void setDatabaseProductName(String databaseProductName) { this.databaseProductName = databaseProductName; }
        public String getDatabaseProductVersion() { return databaseProductVersion; }
        public void setDatabaseProductVersion(String databaseProductVersion) { this.databaseProductVersion = databaseProductVersion; }
        public String getDriverName() { return driverName; }
        public void setDriverName(String driverName) { this.driverName = driverName; }
        public String getDriverVersion() { return driverVersion; }
        public void setDriverVersion(String driverVersion) { this.driverVersion = driverVersion; }
        public String getUrl() { return url; }
        public void setUrl(String url) { this.url = url; }
        public String getUserName() { return userName; }
        public void setUserName(String userName) { this.userName = userName; }
        public int getMaxConnections() { return maxConnections; }
        public void setMaxConnections(int maxConnections) { this.maxConnections = maxConnections; }
        public int getMaxTableNameLength() { return maxTableNameLength; }
        public void setMaxTableNameLength(int maxTableNameLength) { this.maxTableNameLength = maxTableNameLength; }
        public int getMaxColumnNameLength() { return maxColumnNameLength; }
        public void setMaxColumnNameLength(int maxColumnNameLength) { this.maxColumnNameLength = maxColumnNameLength; }
        public boolean isSupportsBatchUpdates() { return supportsBatchUpdates; }
        public void setSupportsBatchUpdates(boolean supportsBatchUpdates) { this.supportsBatchUpdates = supportsBatchUpdates; }
        public boolean isSupportsTransactions() { return supportsTransactions; }
        public void setSupportsTransactions(boolean supportsTransactions) { this.supportsTransactions = supportsTransactions; }
        public LocalDateTime getRetrieveTime() { return retrieveTime; }
        public void setRetrieveTime(LocalDateTime retrieveTime) { this.retrieveTime = retrieveTime; }
    }

    public static class SchemaInfo {
        private String schemaName;
        private String catalogName;

        // Getters and Setters
        public String getSchemaName() { return schemaName; }
        public void setSchemaName(String schemaName) { this.schemaName = schemaName; }
        public String getCatalogName() { return catalogName; }
        public void setCatalogName(String catalogName) { this.catalogName = catalogName; }
    }

    public static class TableInfo {
        private Long dataSourceId;
        private String catalogName;
        private String schemaName;
        private String tableName;
        private String tableType;
        private String remarks;
        private Long rowCount;

        // Getters and Setters
        public Long getDataSourceId() { return dataSourceId; }
        public void setDataSourceId(Long dataSourceId) { this.dataSourceId = dataSourceId; }
        public String getCatalogName() { return catalogName; }
        public void setCatalogName(String catalogName) { this.catalogName = catalogName; }
        public String getSchemaName() { return schemaName; }
        public void setSchemaName(String schemaName) { this.schemaName = schemaName; }
        public String getTableName() { return tableName; }
        public void setTableName(String tableName) { this.tableName = tableName; }
        public String getTableType() { return tableType; }
        public void setTableType(String tableType) { this.tableType = tableType; }
        public String getRemarks() { return remarks; }
        public void setRemarks(String remarks) { this.remarks = remarks; }
        public Long getRowCount() { return rowCount; }
        public void setRowCount(Long rowCount) { this.rowCount = rowCount; }
    }

    public static class TableDetailInfo {
        private Long dataSourceId;
        private String schemaName;
        private String tableName;
        private List<ColumnDetailInfo> columns;
        private List<String> primaryKeys;
        private List<ForeignKeyInfo> foreignKeys;
        private List<IndexDetailInfo> indexes;
        private TableStatistics statistics;
        private LocalDateTime retrieveTime;

        // Getters and Setters
        public Long getDataSourceId() { return dataSourceId; }
        public void setDataSourceId(Long dataSourceId) { this.dataSourceId = dataSourceId; }
        public String getSchemaName() { return schemaName; }
        public void setSchemaName(String schemaName) { this.schemaName = schemaName; }
        public String getTableName() { return tableName; }
        public void setTableName(String tableName) { this.tableName = tableName; }
        public List<ColumnDetailInfo> getColumns() { return columns; }
        public void setColumns(List<ColumnDetailInfo> columns) { this.columns = columns; }
        public List<String> getPrimaryKeys() { return primaryKeys; }
        public void setPrimaryKeys(List<String> primaryKeys) { this.primaryKeys = primaryKeys; }
        public List<ForeignKeyInfo> getForeignKeys() { return foreignKeys; }
        public void setForeignKeys(List<ForeignKeyInfo> foreignKeys) { this.foreignKeys = foreignKeys; }
        public List<IndexDetailInfo> getIndexes() { return indexes; }
        public void setIndexes(List<IndexDetailInfo> indexes) { this.indexes = indexes; }
        public TableStatistics getStatistics() { return statistics; }
        public void setStatistics(TableStatistics statistics) { this.statistics = statistics; }
        public LocalDateTime getRetrieveTime() { return retrieveTime; }
        public void setRetrieveTime(LocalDateTime retrieveTime) { this.retrieveTime = retrieveTime; }
    }

    public static class ColumnDetailInfo {
        private Long dataSourceId;
        private String schemaName;
        private String tableName;
        private String columnName;
        private String dataType;
        private int jdbcType;
        private int columnSize;
        private int decimalDigits;
        private boolean nullable;
        private String defaultValue;
        private String remarks;
        private int ordinalPosition;
        private boolean autoIncrement;

        // Getters and Setters
        public Long getDataSourceId() { return dataSourceId; }
        public void setDataSourceId(Long dataSourceId) { this.dataSourceId = dataSourceId; }
        public String getSchemaName() { return schemaName; }
        public void setSchemaName(String schemaName) { this.schemaName = schemaName; }
        public String getTableName() { return tableName; }
        public void setTableName(String tableName) { this.tableName = tableName; }
        public String getColumnName() { return columnName; }
        public void setColumnName(String columnName) { this.columnName = columnName; }
        public String getDataType() { return dataType; }
        public void setDataType(String dataType) { this.dataType = dataType; }
        public int getJdbcType() { return jdbcType; }
        public void setJdbcType(int jdbcType) { this.jdbcType = jdbcType; }
        public int getColumnSize() { return columnSize; }
        public void setColumnSize(int columnSize) { this.columnSize = columnSize; }
        public int getDecimalDigits() { return decimalDigits; }
        public void setDecimalDigits(int decimalDigits) { this.decimalDigits = decimalDigits; }
        public boolean isNullable() { return nullable; }
        public void setNullable(boolean nullable) { this.nullable = nullable; }
        public String getDefaultValue() { return defaultValue; }
        public void setDefaultValue(String defaultValue) { this.defaultValue = defaultValue; }
        public String getRemarks() { return remarks; }
        public void setRemarks(String remarks) { this.remarks = remarks; }
        public int getOrdinalPosition() { return ordinalPosition; }
        public void setOrdinalPosition(int ordinalPosition) { this.ordinalPosition = ordinalPosition; }
        public boolean isAutoIncrement() { return autoIncrement; }
        public void setAutoIncrement(boolean autoIncrement) { this.autoIncrement = autoIncrement; }
    }

    public static class ForeignKeyInfo {
        private String foreignKeyName;
        private String columnName;
        private String referencedTableName;
        private String referencedColumnName;
        private int updateRule;
        private int deleteRule;

        // Getters and Setters
        public String getForeignKeyName() { return foreignKeyName; }
        public void setForeignKeyName(String foreignKeyName) { this.foreignKeyName = foreignKeyName; }
        public String getColumnName() { return columnName; }
        public void setColumnName(String columnName) { this.columnName = columnName; }
        public String getReferencedTableName() { return referencedTableName; }
        public void setReferencedTableName(String referencedTableName) { this.referencedTableName = referencedTableName; }
        public String getReferencedColumnName() { return referencedColumnName; }
        public void setReferencedColumnName(String referencedColumnName) { this.referencedColumnName = referencedColumnName; }
        public int getUpdateRule() { return updateRule; }
        public void setUpdateRule(int updateRule) { this.updateRule = updateRule; }
        public int getDeleteRule() { return deleteRule; }
        public void setDeleteRule(int deleteRule) { this.deleteRule = deleteRule; }
    }

    public static class IndexDetailInfo {
        private String indexName;
        private boolean unique;
        private boolean primaryKey;
        private List<String> columnNames;

        // Getters and Setters
        public String getIndexName() { return indexName; }
        public void setIndexName(String indexName) { this.indexName = indexName; }
        public boolean isUnique() { return unique; }
        public void setUnique(boolean unique) { this.unique = unique; }
        public boolean isPrimaryKey() { return primaryKey; }
        public void setPrimaryKey(boolean primaryKey) { this.primaryKey = primaryKey; }
        public List<String> getColumnNames() { return columnNames; }
        public void setColumnNames(List<String> columnNames) { this.columnNames = columnNames; }
    }

    public static class TableStatistics {
        private long rowCount;
        private long tableSize;
        private LocalDateTime lastUpdated;

        // Getters and Setters
        public long getRowCount() { return rowCount; }
        public void setRowCount(long rowCount) { this.rowCount = rowCount; }
        public long getTableSize() { return tableSize; }
        public void setTableSize(long tableSize) { this.tableSize = tableSize; }
        public LocalDateTime getLastUpdated() { return lastUpdated; }
        public void setLastUpdated(LocalDateTime lastUpdated) { this.lastUpdated = lastUpdated; }
    }

    public static class DatabaseStatistics {
        private Long dataSourceId;
        private int tableCount;
        private int viewCount;
        private long databaseSize;
        private LocalDateTime statisticsTime;

        // Getters and Setters
        public Long getDataSourceId() { return dataSourceId; }
        public void setDataSourceId(Long dataSourceId) { this.dataSourceId = dataSourceId; }
        public int getTableCount() { return tableCount; }
        public void setTableCount(int tableCount) { this.tableCount = tableCount; }
        public int getViewCount() { return viewCount; }
        public void setViewCount(int viewCount) { this.viewCount = viewCount; }
        public long getDatabaseSize() { return databaseSize; }
        public void setDatabaseSize(long databaseSize) { this.databaseSize = databaseSize; }
        public LocalDateTime getStatisticsTime() { return statisticsTime; }
        public void setStatisticsTime(LocalDateTime statisticsTime) { this.statisticsTime = statisticsTime; }
    }

    public static class SearchResult {
        private String keyword;
        private SearchScope searchScope;
        private List<TableInfo> matchedTables = new ArrayList<>();
        private List<ColumnSearchResult> matchedColumns = new ArrayList<>();
        private int totalMatches;
        private LocalDateTime searchTime;

        // Getters and Setters
        public String getKeyword() { return keyword; }
        public void setKeyword(String keyword) { this.keyword = keyword; }
        public SearchScope getSearchScope() { return searchScope; }
        public void setSearchScope(SearchScope searchScope) { this.searchScope = searchScope; }
        public List<TableInfo> getMatchedTables() { return matchedTables; }
        public void setMatchedTables(List<TableInfo> matchedTables) { this.matchedTables = matchedTables; }
        public List<ColumnSearchResult> getMatchedColumns() { return matchedColumns; }
        public void setMatchedColumns(List<ColumnSearchResult> matchedColumns) { this.matchedColumns = matchedColumns; }
        public int getTotalMatches() { return totalMatches; }
        public void setTotalMatches(int totalMatches) { this.totalMatches = totalMatches; }
        public LocalDateTime getSearchTime() { return searchTime; }
        public void setSearchTime(LocalDateTime searchTime) { this.searchTime = searchTime; }
    }

    public static class ColumnSearchResult {
        private Long dataSourceId;
        private String schemaName;
        private String tableName;
        private String columnName;
        private String dataType;
        private String remarks;

        // Getters and Setters
        public Long getDataSourceId() { return dataSourceId; }
        public void setDataSourceId(Long dataSourceId) { this.dataSourceId = dataSourceId; }
        public String getSchemaName() { return schemaName; }
        public void setSchemaName(String schemaName) { this.schemaName = schemaName; }
        public String getTableName() { return tableName; }
        public void setTableName(String tableName) { this.tableName = tableName; }
        public String getColumnName() { return columnName; }
        public void setColumnName(String columnName) { this.columnName = columnName; }
        public String getDataType() { return dataType; }
        public void setDataType(String dataType) { this.dataType = dataType; }
        public String getRemarks() { return remarks; }
        public void setRemarks(String remarks) { this.remarks = remarks; }
    }
}