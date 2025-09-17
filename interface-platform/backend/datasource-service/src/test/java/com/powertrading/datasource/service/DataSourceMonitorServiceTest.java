package com.powertrading.datasource.service;

import com.powertrading.datasource.entity.DataSource;
import com.powertrading.datasource.manager.DataSourceManager;
import com.powertrading.datasource.repository.DataSourceRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import java.sql.Connection;
import java.sql.SQLException;
import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

/**
 * 数据源监控服务单元测试
 * 
 * @author PowerTrading Team
 * @version 1.0.0
 */
@ExtendWith(MockitoExtension.class)
class DataSourceMonitorServiceTest {

    @Mock
    private DataSourceRepository dataSourceRepository;

    @Mock
    private DataSourceManager dataSourceManager;

    @Mock
    private Connection connection;

    @InjectMocks
    private DataSourceMonitorService monitorService;

    private DataSource testDataSource;

    @BeforeEach
    void setUp() {
        // 设置配置属性
        ReflectionTestUtils.setField(monitorService, "healthCheckInterval", 60000L);
        ReflectionTestUtils.setField(monitorService, "connectionTimeout", 5000);
        ReflectionTestUtils.setField(monitorService, "slowQueryThreshold", 1000L);
        ReflectionTestUtils.setField(monitorService, "alertThreshold", 5);
        
        // 创建测试数据源
        testDataSource = new DataSource();
        testDataSource.setId(1L);
        testDataSource.setName("test-datasource");
        testDataSource.setUrl("jdbc:mysql://localhost:3306/test");
        testDataSource.setUsername("test");
        testDataSource.setPassword("password");
        testDataSource.setDriverClassName("com.mysql.cj.jdbc.Driver");
        testDataSource.setStatus("ACTIVE");
    }

    @Test
    void testCheckDataSourceHealth_Healthy() throws Exception {
        // Given
        when(dataSourceRepository.findById(1L)).thenReturn(Optional.of(testDataSource));
        when(dataSourceManager.getConnection(1L)).thenReturn(connection);
        when(connection.isValid(anyInt())).thenReturn(true);

        // When
        DataSourceMonitorService.HealthStatus result = monitorService.checkDataSourceHealth(1L);

        // Then
        assertNotNull(result);
        assertEquals(1L, result.getDataSourceId());
        assertEquals("HEALTHY", result.getStatus());
        assertNull(result.getErrorMessage());
        assertTrue(result.getResponseTime() >= 0);
        assertNotNull(result.getCheckTime());
        
        verify(dataSourceRepository).findById(1L);
        verify(dataSourceManager).getConnection(1L);
        verify(connection).isValid(anyInt());
    }

    @Test
    void testCheckDataSourceHealth_Unhealthy() throws Exception {
        // Given
        when(dataSourceRepository.findById(1L)).thenReturn(Optional.of(testDataSource));
        when(dataSourceManager.getConnection(1L)).thenThrow(new SQLException("Connection failed"));

        // When
        DataSourceMonitorService.HealthStatus result = monitorService.checkDataSourceHealth(1L);

        // Then
        assertNotNull(result);
        assertEquals(1L, result.getDataSourceId());
        assertEquals("UNHEALTHY", result.getStatus());
        assertEquals("Connection failed", result.getErrorMessage());
        assertTrue(result.getResponseTime() >= 0);
        assertNotNull(result.getCheckTime());
        
        verify(dataSourceRepository).findById(1L);
        verify(dataSourceManager).getConnection(1L);
    }

    @Test
    void testCheckDataSourceHealth_DataSourceNotFound() {
        // Given
        when(dataSourceRepository.findById(1L)).thenReturn(Optional.empty());

        // When
        DataSourceMonitorService.HealthStatus result = monitorService.checkDataSourceHealth(1L);

        // Then
        assertNotNull(result);
        assertEquals(1L, result.getDataSourceId());
        assertEquals("NOT_FOUND", result.getStatus());
        assertEquals("数据源不存在", result.getErrorMessage());
        
        verify(dataSourceRepository).findById(1L);
        verify(dataSourceManager, never()).getConnection(any());
    }

    @Test
    void testCheckAllDataSourcesHealth() throws Exception {
        // Given
        DataSource dataSource2 = new DataSource();
        dataSource2.setId(2L);
        dataSource2.setName("test-datasource-2");
        dataSource2.setStatus("ACTIVE");
        
        when(dataSourceRepository.findByStatus("ACTIVE")).thenReturn(Arrays.asList(testDataSource, dataSource2));
        when(dataSourceManager.getConnection(1L)).thenReturn(connection);
        when(dataSourceManager.getConnection(2L)).thenThrow(new SQLException("Connection timeout"));
        when(connection.isValid(anyInt())).thenReturn(true);

        // When
        List<DataSourceMonitorService.HealthStatus> results = monitorService.checkAllDataSourcesHealth();

        // Then
        assertNotNull(results);
        assertEquals(2, results.size());
        
        // 验证第一个数据源健康状态
        DataSourceMonitorService.HealthStatus status1 = results.stream()
                .filter(s -> s.getDataSourceId().equals(1L))
                .findFirst().orElse(null);
        assertNotNull(status1);
        assertEquals("HEALTHY", status1.getStatus());
        
        // 验证第二个数据源健康状态
        DataSourceMonitorService.HealthStatus status2 = results.stream()
                .filter(s -> s.getDataSourceId().equals(2L))
                .findFirst().orElse(null);
        assertNotNull(status2);
        assertEquals("UNHEALTHY", status2.getStatus());
        assertEquals("Connection timeout", status2.getErrorMessage());
        
        verify(dataSourceRepository).findByStatus("ACTIVE");
        verify(dataSourceManager).getConnection(1L);
        verify(dataSourceManager).getConnection(2L);
    }

    @Test
    void testGetDataSourceMetrics() {
        // Given
        Long dataSourceId = 1L;
        
        // 模拟一些监控指标数据
        invokeRecordConnectionSuccess(dataSourceId);
        invokeRecordConnectionSuccess(dataSourceId);
        invokeRecordConnectionFailure(dataSourceId, "Connection timeout");
        invokeRecordQueryExecution(dataSourceId, 500L);
        invokeRecordQueryExecution(dataSourceId, 1500L); // 慢查询
        invokeRecordQueryExecution(dataSourceId, 300L);

        // When
        DataSourceMonitorService.DataSourceMetrics metrics = monitorService.getDataSourceMetrics(dataSourceId);

        // Then
        assertNotNull(metrics);
        assertEquals(dataSourceId, metrics.getDataSourceId());
        assertEquals(2, metrics.getConnectionSuccessCount());
        assertEquals(1, metrics.getConnectionFailureCount());
        assertEquals(3, metrics.getQueryCount());
        assertEquals(1, metrics.getSlowQueryCount());
        assertTrue(metrics.getAverageQueryTime() > 0);
        assertTrue(metrics.getMaxQueryTime() >= metrics.getAverageQueryTime());
        assertTrue(metrics.getMinQueryTime() <= metrics.getAverageQueryTime());
        assertNotNull(metrics.getLastUpdateTime());
    }

    @Test
    void testRecordQueryExecution_NormalQuery() {
        // Given
        Long dataSourceId = 1L;
        Long executionTime = 500L;

        // When
        monitorService.recordQueryExecution(dataSourceId, executionTime);

        // Then
        DataSourceMonitorService.DataSourceMetrics metrics = monitorService.getDataSourceMetrics(dataSourceId);
        assertEquals(1, metrics.getQueryCount());
        assertEquals(0, metrics.getSlowQueryCount());
        assertEquals(executionTime, metrics.getAverageQueryTime());
    }

    @Test
    void testRecordQueryExecution_SlowQuery() {
        // Given
        Long dataSourceId = 1L;
        Long executionTime = 2000L; // 超过慢查询阈值

        // When
        monitorService.recordQueryExecution(dataSourceId, executionTime);

        // Then
        DataSourceMonitorService.DataSourceMetrics metrics = monitorService.getDataSourceMetrics(dataSourceId);
        assertEquals(1, metrics.getQueryCount());
        assertEquals(1, metrics.getSlowQueryCount());
        assertEquals(executionTime, metrics.getAverageQueryTime());
    }

    @Test
    void testRecordConnectionSuccess() {
        // Given
        Long dataSourceId = 1L;

        // When
        monitorService.recordConnectionSuccess(dataSourceId);
        monitorService.recordConnectionSuccess(dataSourceId);

        // Then
        DataSourceMonitorService.DataSourceMetrics metrics = monitorService.getDataSourceMetrics(dataSourceId);
        assertEquals(2, metrics.getConnectionSuccessCount());
        assertEquals(0, metrics.getConnectionFailureCount());
    }

    @Test
    void testRecordConnectionFailure() {
        // Given
        Long dataSourceId = 1L;
        String errorMessage = "Connection timeout";

        // When
        monitorService.recordConnectionFailure(dataSourceId, errorMessage);

        // Then
        DataSourceMonitorService.DataSourceMetrics metrics = monitorService.getDataSourceMetrics(dataSourceId);
        assertEquals(0, metrics.getConnectionSuccessCount());
        assertEquals(1, metrics.getConnectionFailureCount());
    }

    @Test
    void testGetHealthReport() throws Exception {
        // Given
        when(dataSourceRepository.findByStatus("ACTIVE")).thenReturn(Arrays.asList(testDataSource));
        when(dataSourceManager.getConnection(1L)).thenReturn(connection);
        when(connection.isValid(anyInt())).thenReturn(true);
        
        // 添加一些监控数据
        invokeRecordConnectionSuccess(1L);
        invokeRecordConnectionSuccess(1L);
        invokeRecordQueryExecution(1L, 500L);
        invokeRecordQueryExecution(1L, 1500L);

        // When
        DataSourceMonitorService.HealthReport report = monitorService.getHealthReport();

        // Then
        assertNotNull(report);
        assertEquals(1, report.getTotalDataSources());
        assertEquals(1, report.getHealthyDataSources());
        assertEquals(0, report.getUnhealthyDataSources());
        assertEquals(1, report.getDataSourceStatuses().size());
        assertEquals(1, report.getDataSourceMetrics().size());
        assertNotNull(report.getGeneratedAt());
        
        DataSourceMonitorService.HealthStatus status = report.getDataSourceStatuses().get(0);
        assertEquals(1L, status.getDataSourceId());
        assertEquals("HEALTHY", status.getStatus());
        
        DataSourceMonitorService.DataSourceMetrics metrics = report.getDataSourceMetrics().get(0);
        assertEquals(1L, metrics.getDataSourceId());
        assertEquals(2, metrics.getConnectionSuccessCount());
        assertEquals(2, metrics.getQueryCount());
        assertEquals(1, metrics.getSlowQueryCount());
    }

    @Test
    void testClearMetrics() {
        // Given
        Long dataSourceId = 1L;
        invokeRecordConnectionSuccess(dataSourceId);
        invokeRecordQueryExecution(dataSourceId, 500L);
        
        // 验证指标存在
        DataSourceMonitorService.DataSourceMetrics metricsBefore = monitorService.getDataSourceMetrics(dataSourceId);
        assertEquals(1, metricsBefore.getConnectionSuccessCount());
        assertEquals(1, metricsBefore.getQueryCount());

        // When
        monitorService.clearMetrics(dataSourceId);

        // Then
        DataSourceMonitorService.DataSourceMetrics metricsAfter = monitorService.getDataSourceMetrics(dataSourceId);
        assertEquals(0, metricsAfter.getConnectionSuccessCount());
        assertEquals(0, metricsAfter.getConnectionFailureCount());
        assertEquals(0, metricsAfter.getQueryCount());
        assertEquals(0, metricsAfter.getSlowQueryCount());
    }

    @Test
    void testClearAllMetrics() {
        // Given
        invokeRecordConnectionSuccess(1L);
        invokeRecordConnectionSuccess(2L);
        invokeRecordQueryExecution(1L, 500L);
        invokeRecordQueryExecution(2L, 800L);

        // When
        monitorService.clearAllMetrics();

        // Then
        DataSourceMonitorService.DataSourceMetrics metrics1 = monitorService.getDataSourceMetrics(1L);
        DataSourceMonitorService.DataSourceMetrics metrics2 = monitorService.getDataSourceMetrics(2L);
        
        assertEquals(0, metrics1.getConnectionSuccessCount());
        assertEquals(0, metrics1.getQueryCount());
        assertEquals(0, metrics2.getConnectionSuccessCount());
        assertEquals(0, metrics2.getQueryCount());
    }

    @Test
    void testAlertThreshold() {
        // Given
        Long dataSourceId = 1L;
        
        // 记录多次连接失败，超过告警阈值
        for (int i = 0; i < 6; i++) {
            invokeRecordConnectionFailure(dataSourceId, "Connection failed");
        }

        // When
        DataSourceMonitorService.DataSourceMetrics metrics = monitorService.getDataSourceMetrics(dataSourceId);

        // Then
        assertEquals(6, metrics.getConnectionFailureCount());
        // 在实际实现中，这里应该触发告警机制
        assertTrue(metrics.getConnectionFailureCount() > 5); // 超过告警阈值
    }

    // 辅助方法：通过反射调用私有方法或直接调用公共方法
    private void invokeRecordConnectionSuccess(Long dataSourceId) {
        monitorService.recordConnectionSuccess(dataSourceId);
    }

    private void invokeRecordConnectionFailure(Long dataSourceId, String errorMessage) {
        monitorService.recordConnectionFailure(dataSourceId, errorMessage);
    }

    private void invokeRecordQueryExecution(Long dataSourceId, Long executionTime) {
        monitorService.recordQueryExecution(dataSourceId, executionTime);
    }
}