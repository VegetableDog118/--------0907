package com.powertrading.datasource.engine;

import com.powertrading.datasource.exception.DataSourceException;
import com.powertrading.datasource.manager.DataSourceManager;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import java.sql.*;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

/**
 * 查询执行引擎单元测试
 * 
 * @author PowerTrading Team
 * @version 1.0.0
 */
@ExtendWith(MockitoExtension.class)
class QueryExecutionEngineTest {

    @Mock
    private DataSourceManager dataSourceManager;

    @Mock
    private Connection connection;

    @Mock
    private PreparedStatement preparedStatement;

    @Mock
    private ResultSet resultSet;

    @Mock
    private ResultSetMetaData resultSetMetaData;

    @Mock
    private DatabaseMetaData databaseMetaData;

    @InjectMocks
    private QueryExecutionEngine queryExecutionEngine;

    @BeforeEach
    void setUp() {
        // 设置配置属性
        ReflectionTestUtils.setField(queryExecutionEngine, "queryTimeout", 30000);
        ReflectionTestUtils.setField(queryExecutionEngine, "maxRows", 10000);
        ReflectionTestUtils.setField(queryExecutionEngine, "fetchSize", 1000);
    }

    @Test
    void testExecuteQuery_Success() throws Exception {
        // Given
        Long dataSourceId = 1L;
        String sql = "SELECT id, name FROM users WHERE status = ?";
        Map<String, Object> parameters = new HashMap<>();
        parameters.put("status", 1);

        when(dataSourceManager.getConnection(dataSourceId)).thenReturn(connection);
        when(connection.prepareStatement(anyString())).thenReturn(preparedStatement);
        when(preparedStatement.executeQuery()).thenReturn(resultSet);
        when(resultSet.getMetaData()).thenReturn(resultSetMetaData);
        
        // 模拟结果集元数据
        when(resultSetMetaData.getColumnCount()).thenReturn(2);
        when(resultSetMetaData.getColumnName(1)).thenReturn("id");
        when(resultSetMetaData.getColumnName(2)).thenReturn("name");
        when(resultSetMetaData.getColumnLabel(1)).thenReturn("id");
        when(resultSetMetaData.getColumnLabel(2)).thenReturn("name");
        when(resultSetMetaData.getColumnTypeName(1)).thenReturn("BIGINT");
        when(resultSetMetaData.getColumnTypeName(2)).thenReturn("VARCHAR");
        when(resultSetMetaData.getColumnType(1)).thenReturn(Types.BIGINT);
        when(resultSetMetaData.getColumnType(2)).thenReturn(Types.VARCHAR);
        when(resultSetMetaData.isNullable(1)).thenReturn(ResultSetMetaData.columnNoNulls);
        when(resultSetMetaData.isNullable(2)).thenReturn(ResultSetMetaData.columnNullable);
        
        // 模拟结果集数据
        when(resultSet.next()).thenReturn(true, true, false);
        when(resultSet.getObject(1)).thenReturn(1L, 2L);
        when(resultSet.getObject(2)).thenReturn("Alice", "Bob");

        // When
        QueryExecutionEngine.QueryResult result = queryExecutionEngine.executeQuery(dataSourceId, sql, parameters);

        // Then
        assertNotNull(result);
        assertEquals(2, result.getRowCount());
        assertEquals(2, result.getColumns().size());
        assertEquals(2, result.getRows().size());
        
        // 验证列信息
        assertEquals("id", result.getColumns().get(0).getName());
        assertEquals("name", result.getColumns().get(1).getName());
        assertEquals("BIGINT", result.getColumns().get(0).getType());
        assertEquals("VARCHAR", result.getColumns().get(1).getType());
        
        // 验证数据行
        assertEquals(1L, result.getRows().get(0).get("id"));
        assertEquals("Alice", result.getRows().get(0).get("name"));
        assertEquals(2L, result.getRows().get(1).get("id"));
        assertEquals("Bob", result.getRows().get(1).get("name"));
        
        verify(dataSourceManager).getConnection(dataSourceId);
        verify(connection).prepareStatement(anyString());
        verify(preparedStatement).executeQuery();
    }

    @Test
    void testExecuteQuery_SqlInjectionPrevention() {
        // Given
        Long dataSourceId = 1L;
        String maliciousSql = "SELECT * FROM users; DROP TABLE users; --";
        Map<String, Object> parameters = new HashMap<>();

        // When & Then
        DataSourceException exception = assertThrows(DataSourceException.class, () -> {
            queryExecutionEngine.executeQuery(dataSourceId, maliciousSql, parameters);
        });
        
        assertTrue(exception.getMessage().contains("只允许执行SELECT查询语句"));
        verify(dataSourceManager, never()).getConnection(any());
    }

    @Test
    void testExecuteQuery_DangerousKeywords() {
        // Given
        Long dataSourceId = 1L;
        String dangerousSql = "SELECT * FROM users WHERE id = 1 AND DROP TABLE test";
        Map<String, Object> parameters = new HashMap<>();

        // When & Then
        DataSourceException exception = assertThrows(DataSourceException.class, () -> {
            queryExecutionEngine.executeQuery(dataSourceId, dangerousSql, parameters);
        });
        
        assertTrue(exception.getMessage().contains("SQL语句包含危险关键字"));
        verify(dataSourceManager, never()).getConnection(any());
    }

    @Test
    void testExecuteUpdate_Success() throws Exception {
        // Given
        Long dataSourceId = 1L;
        String sql = "UPDATE users SET name = ? WHERE id = ?";
        Map<String, Object> parameters = new HashMap<>();
        parameters.put("name", "Updated Name");
        parameters.put("id", 1L);

        when(dataSourceManager.getConnection(dataSourceId)).thenReturn(connection);
        when(connection.prepareStatement(anyString())).thenReturn(preparedStatement);
        when(preparedStatement.executeUpdate()).thenReturn(1);

        // When
        int result = queryExecutionEngine.executeUpdate(dataSourceId, sql, parameters);

        // Then
        assertEquals(1, result);
        verify(dataSourceManager).getConnection(dataSourceId);
        verify(connection).prepareStatement(anyString());
        verify(preparedStatement).executeUpdate();
    }

    @Test
    void testExecuteUpdate_WithoutWhereClause() {
        // Given
        Long dataSourceId = 1L;
        String sql = "UPDATE users SET name = 'test'";
        Map<String, Object> parameters = new HashMap<>();

        // When & Then
        DataSourceException exception = assertThrows(DataSourceException.class, () -> {
            queryExecutionEngine.executeUpdate(dataSourceId, sql, parameters);
        });
        
        assertTrue(exception.getMessage().contains("UPDATE语句必须包含WHERE条件"));
        verify(dataSourceManager, never()).getConnection(any());
    }

    @Test
    void testExecuteUpdate_ProhibitedOperation() {
        // Given
        Long dataSourceId = 1L;
        String sql = "DELETE FROM users WHERE id = 1";
        Map<String, Object> parameters = new HashMap<>();

        // When & Then
        DataSourceException exception = assertThrows(DataSourceException.class, () -> {
            queryExecutionEngine.executeUpdate(dataSourceId, sql, parameters);
        });
        
        assertTrue(exception.getMessage().contains("只允许执行UPDATE和INSERT语句"));
        verify(dataSourceManager, never()).getConnection(any());
    }

    @Test
    void testGetTableMetadata_Success() throws Exception {
        // Given
        Long dataSourceId = 1L;
        String tableName = "users";

        when(dataSourceManager.getConnection(dataSourceId)).thenReturn(connection);
        when(connection.getMetaData()).thenReturn(databaseMetaData);
        
        // 模拟列信息
        ResultSet columnsResultSet = mock(ResultSet.class);
        when(databaseMetaData.getColumns(null, null, tableName, null)).thenReturn(columnsResultSet);
        when(columnsResultSet.next()).thenReturn(true, true, false);
        when(columnsResultSet.getString("COLUMN_NAME")).thenReturn("id", "name");
        when(columnsResultSet.getString("TYPE_NAME")).thenReturn("BIGINT", "VARCHAR");
        when(columnsResultSet.getInt("COLUMN_SIZE")).thenReturn(20, 255);
        when(columnsResultSet.getInt("NULLABLE")).thenReturn(DatabaseMetaData.columnNoNulls, DatabaseMetaData.columnNullable);
        when(columnsResultSet.getString("COLUMN_DEF")).thenReturn(null, null);
        when(columnsResultSet.getString("IS_AUTOINCREMENT")).thenReturn("YES", "NO");
        
        // 模拟主键信息
        ResultSet primaryKeysResultSet = mock(ResultSet.class);
        when(databaseMetaData.getPrimaryKeys(null, null, tableName)).thenReturn(primaryKeysResultSet);
        when(primaryKeysResultSet.next()).thenReturn(true, false);
        when(primaryKeysResultSet.getString("COLUMN_NAME")).thenReturn("id");
        
        // 模拟索引信息
        ResultSet indexResultSet = mock(ResultSet.class);
        when(databaseMetaData.getIndexInfo(null, null, tableName, false, false)).thenReturn(indexResultSet);
        when(indexResultSet.next()).thenReturn(false);

        // When
        QueryExecutionEngine.TableMetadata result = queryExecutionEngine.getTableMetadata(dataSourceId, tableName);

        // Then
        assertNotNull(result);
        assertEquals(tableName, result.getTableName());
        assertEquals(2, result.getColumns().size());
        assertEquals(1, result.getPrimaryKeys().size());
        assertEquals("id", result.getPrimaryKeys().get(0));
        
        // 验证列信息
        QueryExecutionEngine.ColumnMetadata idColumn = result.getColumns().get(0);
        assertEquals("id", idColumn.getColumnName());
        assertEquals("BIGINT", idColumn.getDataType());
        assertEquals(20, idColumn.getColumnSize());
        assertFalse(idColumn.isNullable());
        assertTrue(idColumn.isAutoIncrement());
        
        QueryExecutionEngine.ColumnMetadata nameColumn = result.getColumns().get(1);
        assertEquals("name", nameColumn.getColumnName());
        assertEquals("VARCHAR", nameColumn.getDataType());
        assertEquals(255, nameColumn.getColumnSize());
        assertTrue(nameColumn.isNullable());
        assertFalse(nameColumn.isAutoIncrement());
        
        verify(dataSourceManager).getConnection(dataSourceId);
        verify(connection).getMetaData();
    }

    @Test
    void testGetTableNames_Success() throws Exception {
        // Given
        Long dataSourceId = 1L;
        String schema = "test_schema";

        when(dataSourceManager.getConnection(dataSourceId)).thenReturn(connection);
        when(connection.getMetaData()).thenReturn(databaseMetaData);
        
        ResultSet tablesResultSet = mock(ResultSet.class);
        when(databaseMetaData.getTables(null, schema, "%", new String[]{"TABLE"})).thenReturn(tablesResultSet);
        when(tablesResultSet.next()).thenReturn(true, true, true, false);
        when(tablesResultSet.getString("TABLE_NAME")).thenReturn("users", "orders", "products");

        // When
        List<String> result = queryExecutionEngine.getTableNames(dataSourceId, schema);

        // Then
        assertNotNull(result);
        assertEquals(3, result.size());
        assertTrue(result.contains("users"));
        assertTrue(result.contains("orders"));
        assertTrue(result.contains("products"));
        
        verify(dataSourceManager).getConnection(dataSourceId);
        verify(connection).getMetaData();
    }

    @Test
    void testProcessDynamicSql() {
        // Given
        String sql = "SELECT * FROM users WHERE status = #{status} AND name = #{name}";
        Map<String, Object> parameters = new HashMap<>();
        parameters.put("status", 1);
        parameters.put("name", "Alice");

        // When
        String result = invokeProcessDynamicSql(sql, parameters);

        // Then
        assertEquals("SELECT * FROM users WHERE status = 1 AND name = 'Alice'", result);
    }

    @Test
    void testProcessDynamicSql_WithNullValue() {
        // Given
        String sql = "SELECT * FROM users WHERE status = #{status}";
        Map<String, Object> parameters = new HashMap<>();
        parameters.put("status", null);

        // When
        String result = invokeProcessDynamicSql(sql, parameters);

        // Then
        assertEquals("SELECT * FROM users WHERE status = NULL", result);
    }

    @Test
    void testProcessDynamicSql_WithStringEscaping() {
        // Given
        String sql = "SELECT * FROM users WHERE name = #{name}";
        Map<String, Object> parameters = new HashMap<>();
        parameters.put("name", "O'Connor");

        // When
        String result = invokeProcessDynamicSql(sql, parameters);

        // Then
        assertEquals("SELECT * FROM users WHERE name = 'O''Connor'", result);
    }

    @Test
    void testExecuteQueryAsync_Success() throws Exception {
        // Given
        Long dataSourceId = 1L;
        String sql = "SELECT COUNT(*) FROM users";
        Map<String, Object> parameters = new HashMap<>();

        when(dataSourceManager.getConnection(dataSourceId)).thenReturn(connection);
        when(connection.prepareStatement(anyString())).thenReturn(preparedStatement);
        when(preparedStatement.executeQuery()).thenReturn(resultSet);
        when(resultSet.getMetaData()).thenReturn(resultSetMetaData);
        
        when(resultSetMetaData.getColumnCount()).thenReturn(1);
        when(resultSetMetaData.getColumnName(1)).thenReturn("count");
        when(resultSetMetaData.getColumnLabel(1)).thenReturn("count");
        when(resultSetMetaData.getColumnTypeName(1)).thenReturn("BIGINT");
        when(resultSetMetaData.getColumnType(1)).thenReturn(Types.BIGINT);
        when(resultSetMetaData.isNullable(1)).thenReturn(ResultSetMetaData.columnNoNulls);
        
        when(resultSet.next()).thenReturn(true, false);
        when(resultSet.getObject(1)).thenReturn(100L);

        // When
        var future = queryExecutionEngine.executeQueryAsync(dataSourceId, sql, parameters);
        QueryExecutionEngine.QueryResult result = future.get();

        // Then
        assertNotNull(result);
        assertEquals(1, result.getRowCount());
        assertEquals(100L, result.getRows().get(0).get("count"));
    }

    @Test
    void testConnectionTimeout() throws Exception {
        // Given
        Long dataSourceId = 1L;
        String sql = "SELECT * FROM users";
        Map<String, Object> parameters = new HashMap<>();

        when(dataSourceManager.getConnection(dataSourceId)).thenThrow(new SQLException("Connection timeout"));

        // When & Then
        DataSourceException exception = assertThrows(DataSourceException.class, () -> {
            queryExecutionEngine.executeQuery(dataSourceId, sql, parameters);
        });
        
        assertTrue(exception.getMessage().contains("查询执行失败"));
        verify(dataSourceManager).getConnection(dataSourceId);
    }

    // 辅助方法：通过反射调用私有方法
    private String invokeProcessDynamicSql(String sql, Map<String, Object> parameters) {
        try {
            var method = QueryExecutionEngine.class.getDeclaredMethod("processDynamicSql", String.class, Map.class);
            method.setAccessible(true);
            return (String) method.invoke(queryExecutionEngine, sql, parameters);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }
}