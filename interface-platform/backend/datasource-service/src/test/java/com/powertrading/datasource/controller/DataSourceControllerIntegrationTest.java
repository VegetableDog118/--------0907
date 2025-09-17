package com.powertrading.datasource.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.powertrading.datasource.entity.DataSource;
import com.powertrading.datasource.repository.DataSourceRepository;
import com.powertrading.datasource.service.DataSourceService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

/**
 * 数据源控制器集成测试
 * 
 * @author PowerTrading Team
 * @version 1.0.0
 */
@WebMvcTest(DataSourceController.class)
class DataSourceControllerIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private DataSourceService dataSourceService;

    @MockBean
    private DataSourceRepository dataSourceRepository;

    @Autowired
    private ObjectMapper objectMapper;

    private DataSource testDataSource;

    @BeforeEach
    void setUp() {
        testDataSource = new DataSource();
        testDataSource.setId(1L);
        testDataSource.setName("test-datasource");
        testDataSource.setUrl("jdbc:mysql://localhost:3306/test");
        testDataSource.setUsername("test");
        testDataSource.setPassword("password");
        testDataSource.setDriverClassName("com.mysql.cj.jdbc.Driver");
        testDataSource.setStatus("ACTIVE");
        testDataSource.setCreatedAt(LocalDateTime.now());
        testDataSource.setUpdatedAt(LocalDateTime.now());
    }

    @Test
    void testCreateDataSource_Success() throws Exception {
        // Given
        when(dataSourceService.createDataSource(any(DataSource.class))).thenReturn(testDataSource);

        // When & Then
        mockMvc.perform(post("/api/datasources")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(testDataSource)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.id").value(1))
                .andExpect(jsonPath("$.data.name").value("test-datasource"))
                .andExpect(jsonPath("$.data.url").value("jdbc:mysql://localhost:3306/test"))
                .andExpect(jsonPath("$.data.username").value("test"))
                .andExpect(jsonPath("$.data.status").value("ACTIVE"));

        verify(dataSourceService).createDataSource(any(DataSource.class));
    }

    @Test
    void testCreateDataSource_ValidationError() throws Exception {
        // Given
        DataSource invalidDataSource = new DataSource();
        // 缺少必要字段

        // When & Then
        mockMvc.perform(post("/api/datasources")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(invalidDataSource)))
                .andExpect(status().isBadRequest());

        verify(dataSourceService, never()).createDataSource(any(DataSource.class));
    }

    @Test
    void testGetDataSource_Success() throws Exception {
        // Given
        when(dataSourceService.getDataSourceById(1L)).thenReturn(testDataSource);

        // When & Then
        mockMvc.perform(get("/api/datasources/1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpected(jsonPath("$.data.id").value(1))
                .andExpect(jsonPath("$.data.name").value("test-datasource"))
                .andExpect(jsonPath("$.data.status").value("ACTIVE"));

        verify(dataSourceService).getDataSourceById(1L);
    }

    @Test
    void testGetDataSource_NotFound() throws Exception {
        // Given
        when(dataSourceService.getDataSourceById(999L)).thenReturn(null);

        // When & Then
        mockMvc.perform(get("/api/datasources/999"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.success").value(false))
                .andExpect(jsonPath("$.message").value("数据源不存在"));

        verify(dataSourceService).getDataSourceById(999L);
    }

    @Test
    void testUpdateDataSource_Success() throws Exception {
        // Given
        DataSource updatedDataSource = new DataSource();
        updatedDataSource.setId(1L);
        updatedDataSource.setName("updated-datasource");
        updatedDataSource.setUrl("jdbc:mysql://localhost:3306/updated");
        updatedDataSource.setUsername("updated");
        updatedDataSource.setPassword("newpassword");
        updatedDataSource.setDriverClassName("com.mysql.cj.jdbc.Driver");
        updatedDataSource.setStatus("ACTIVE");

        when(dataSourceService.updateDataSource(eq(1L), any(DataSource.class))).thenReturn(updatedDataSource);

        // When & Then
        mockMvc.perform(put("/api/datasources/1")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(updatedDataSource)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.name").value("updated-datasource"))
                .andExpect(jsonPath("$.data.url").value("jdbc:mysql://localhost:3306/updated"));

        verify(dataSourceService).updateDataSource(eq(1L), any(DataSource.class));
    }

    @Test
    void testDeleteDataSource_Success() throws Exception {
        // Given
        doNothing().when(dataSourceService).deleteDataSource(1L);

        // When & Then
        mockMvc.perform(delete("/api/datasources/1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.message").value("数据源删除成功"));

        verify(dataSourceService).deleteDataSource(1L);
    }

    @Test
    void testGetDataSources_Success() throws Exception {
        // Given
        DataSource dataSource2 = new DataSource();
        dataSource2.setId(2L);
        dataSource2.setName("test-datasource-2");
        dataSource2.setStatus("ACTIVE");

        Page<DataSource> page = new PageImpl<>(Arrays.asList(testDataSource, dataSource2), 
                PageRequest.of(0, 10), 2);
        when(dataSourceService.getDataSources(any(), any())).thenReturn(page);

        // When & Then
        mockMvc.perform(get("/api/datasources")
                .param("page", "0")
                .param("size", "10"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.content").isArray())
                .andExpect(jsonPath("$.data.content.length()").value(2))
                .andExpect(jsonPath("$.data.totalElements").value(2))
                .andExpect(jsonPath("$.data.totalPages").value(1))
                .andExpect(jsonPath("$.data.content[0].id").value(1))
                .andExpect(jsonPath("$.data.content[1].id").value(2));

        verify(dataSourceService).getDataSources(any(), any());
    }

    @Test
    void testGetDataSources_WithFilters() throws Exception {
        // Given
        Page<DataSource> page = new PageImpl<>(Arrays.asList(testDataSource), 
                PageRequest.of(0, 10), 1);
        when(dataSourceService.getDataSources(any(), any())).thenReturn(page);

        // When & Then
        mockMvc.perform(get("/api/datasources")
                .param("name", "test")
                .param("status", "ACTIVE")
                .param("page", "0")
                .param("size", "10"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.content.length()").value(1));

        verify(dataSourceService).getDataSources(any(), any());
    }

    @Test
    void testTestConnection_Success() throws Exception {
        // Given
        when(dataSourceService.testConnection(1L)).thenReturn(true);

        // When & Then
        mockMvc.perform(post("/api/datasources/1/test"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data").value(true))
                .andExpect(jsonPath("$.message").value("连接测试成功"));

        verify(dataSourceService).testConnection(1L);
    }

    @Test
    void testTestConnection_Failure() throws Exception {
        // Given
        when(dataSourceService.testConnection(1L)).thenReturn(false);

        // When & Then
        mockMvc.perform(post("/api/datasources/1/test"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(false))
                .andExpect(jsonPath("$.data").value(false))
                .andExpect(jsonPath("$.message").value("连接测试失败"));

        verify(dataSourceService).testConnection(1L);
    }

    @Test
    void testGetConnectionPoolInfo_Success() throws Exception {
        // Given
        Map<String, Object> poolInfo = new HashMap<>();
        poolInfo.put("activeConnections", 5);
        poolInfo.put("idleConnections", 10);
        poolInfo.put("totalConnections", 15);
        poolInfo.put("maxPoolSize", 20);
        
        when(dataSourceService.getConnectionPoolInfo(1L)).thenReturn(poolInfo);

        // When & Then
        mockMvc.perform(get("/api/datasources/1/pool-info"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.activeConnections").value(5))
                .andExpect(jsonPath("$.data.idleConnections").value(10))
                .andExpect(jsonPath("$.data.totalConnections").value(15))
                .andExpect(jsonPath("$.data.maxPoolSize").value(20));

        verify(dataSourceService).getConnectionPoolInfo(1L);
    }

    @Test
    void testExecuteQuery_Success() throws Exception {
        // Given
        DataSourceController.QueryRequest queryRequest = new DataSourceController.QueryRequest();
        queryRequest.setSql("SELECT * FROM users WHERE id = ?");
        queryRequest.setParameters(Map.of("id", 1));
        
        Map<String, Object> queryResult = new HashMap<>();
        queryResult.put("columns", Arrays.asList("id", "name", "email"));
        queryResult.put("rows", Arrays.asList(
                Map.of("id", 1, "name", "Alice", "email", "alice@example.com")
        ));
        queryResult.put("rowCount", 1);
        
        when(dataSourceService.executeQuery(eq(1L), anyString(), any())).thenReturn(queryResult);

        // When & Then
        mockMvc.perform(post("/api/datasources/1/query")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(queryRequest)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.rowCount").value(1))
                .andExpect(jsonPath("$.data.columns.length()").value(3))
                .andExpect(jsonPath("$.data.rows.length()").value(1));

        verify(dataSourceService).executeQuery(eq(1L), anyString(), any());
    }

    @Test
    void testExecuteQuery_InvalidSql() throws Exception {
        // Given
        DataSourceController.QueryRequest queryRequest = new DataSourceController.QueryRequest();
        queryRequest.setSql("DROP TABLE users"); // 危险SQL
        queryRequest.setParameters(new HashMap<>());

        // When & Then
        mockMvc.perform(post("/api/datasources/1/query")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(queryRequest)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.success").value(false))
                .andExpect(jsonPath("$.message").exists());

        verify(dataSourceService, never()).executeQuery(any(), any(), any());
    }

    @Test
    void testExecuteQuery_EmptySql() throws Exception {
        // Given
        DataSourceController.QueryRequest queryRequest = new DataSourceController.QueryRequest();
        queryRequest.setSql(""); // 空SQL
        queryRequest.setParameters(new HashMap<>());

        // When & Then
        mockMvc.perform(post("/api/datasources/1/query")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(queryRequest)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.success").value(false))
                .andExpect(jsonPath("$.message").value("SQL语句不能为空"));

        verify(dataSourceService, never()).executeQuery(any(), any(), any());
    }

    @Test
    void testGetTableNames_Success() throws Exception {
        // Given
        when(dataSourceService.getTableNames(1L, "test_schema"))
                .thenReturn(Arrays.asList("users", "orders", "products"));

        // When & Then
        mockMvc.perform(get("/api/datasources/1/tables")
                .param("schema", "test_schema"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data").isArray())
                .andExpect(jsonPath("$.data.length()").value(3))
                .andExpect(jsonPath("$.data[0]").value("users"))
                .andExpect(jsonPath("$.data[1]").value("orders"))
                .andExpect(jsonPath("$.data[2]").value("products"));

        verify(dataSourceService).getTableNames(1L, "test_schema");
    }

    @Test
    void testGetTableMetadata_Success() throws Exception {
        // Given
        Map<String, Object> tableMetadata = new HashMap<>();
        tableMetadata.put("tableName", "users");
        tableMetadata.put("columns", Arrays.asList(
                Map.of("name", "id", "type", "BIGINT", "nullable", false),
                Map.of("name", "name", "type", "VARCHAR", "nullable", true)
        ));
        tableMetadata.put("primaryKeys", Arrays.asList("id"));
        
        when(dataSourceService.getTableMetadata(1L, "users")).thenReturn(tableMetadata);

        // When & Then
        mockMvc.perform(get("/api/datasources/1/tables/users/metadata"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.tableName").value("users"))
                .andExpect(jsonPath("$.data.columns").isArray())
                .andExpect(jsonPath("$.data.columns.length()").value(2))
                .andExpect(jsonPath("$.data.primaryKeys").isArray())
                .andExpect(jsonPath("$.data.primaryKeys[0]").value("id"));

        verify(dataSourceService).getTableMetadata(1L, "users");
    }

    @Test
    void testHandleException() throws Exception {
        // Given
        when(dataSourceService.getDataSourceById(1L))
                .thenThrow(new RuntimeException("Database connection failed"));

        // When & Then
        mockMvc.perform(get("/api/datasources/1"))
                .andExpect(status().isInternalServerError())
                .andExpect(jsonPath("$.success").value(false))
                .andExpect(jsonPath("$.message").exists());

        verify(dataSourceService).getDataSourceById(1L);
    }

    @Test
    void testInvalidPathVariable() throws Exception {
        // When & Then
        mockMvc.perform(get("/api/datasources/invalid"))
                .andExpect(status().isBadRequest());
    }

    @Test
    void testMissingRequestBody() throws Exception {
        // When & Then
        mockMvc.perform(post("/api/datasources")
                .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isBadRequest());
    }

    @Test
    void testInvalidContentType() throws Exception {
        // When & Then
        mockMvc.perform(post("/api/datasources")
                .contentType(MediaType.TEXT_PLAIN)
                .content("invalid content"))
                .andExpect(status().isUnsupportedMediaType());
    }
}