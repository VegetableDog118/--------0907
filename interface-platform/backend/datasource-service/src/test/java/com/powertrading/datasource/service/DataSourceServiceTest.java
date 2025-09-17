package com.powertrading.datasource.service;

import com.powertrading.datasource.entity.DataSource;
import com.powertrading.datasource.exception.DataSourceException;
import com.powertrading.datasource.manager.DataSourceManager;
import com.powertrading.datasource.repository.DataSourceRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;

import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

/**
 * 数据源服务单元测试
 * 
 * @author PowerTrading Team
 * @version 1.0.0
 */
@ExtendWith(MockitoExtension.class)
class DataSourceServiceTest {

    @Mock
    private DataSourceRepository dataSourceRepository;

    @Mock
    private DataSourceManager dataSourceManager;

    @InjectMocks
    private DataSourceService dataSourceService;

    private DataSource testDataSource;

    @BeforeEach
    void setUp() {
        testDataSource = new DataSource();
        testDataSource.setId(1L);
        testDataSource.setName("test-datasource");
        testDataSource.setType("mysql");
        testDataSource.setUrl("jdbc:mysql://localhost:3306/test");
        testDataSource.setUsername("testuser");
        testDataSource.setPassword("testpass");
        testDataSource.setStatus(1);
        testDataSource.setHealthStatus(1);
        testDataSource.setCreatedAt(LocalDateTime.now());
        testDataSource.setUpdatedAt(LocalDateTime.now());
    }

    @Test
    void testCreateDataSource_Success() throws DataSourceException {
        // Given
        when(dataSourceRepository.findByName(testDataSource.getName())).thenReturn(Optional.empty());
        when(dataSourceRepository.save(any(DataSource.class))).thenReturn(testDataSource);
        doNothing().when(dataSourceManager).addDataSource(any(DataSource.class));

        // When
        DataSource result = dataSourceService.createDataSource(testDataSource);

        // Then
        assertNotNull(result);
        assertEquals(testDataSource.getName(), result.getName());
        assertEquals(testDataSource.getType(), result.getType());
        verify(dataSourceRepository).findByName(testDataSource.getName());
        verify(dataSourceRepository).save(any(DataSource.class));
        verify(dataSourceManager).addDataSource(any(DataSource.class));
    }

    @Test
    void testCreateDataSource_DuplicateName() {
        // Given
        when(dataSourceRepository.findByName(testDataSource.getName()))
            .thenReturn(Optional.of(testDataSource));

        // When & Then
        DataSourceException exception = assertThrows(DataSourceException.class, () -> {
            dataSourceService.createDataSource(testDataSource);
        });
        
        assertTrue(exception.getMessage().contains("数据源名称已存在"));
        verify(dataSourceRepository).findByName(testDataSource.getName());
        verify(dataSourceRepository, never()).save(any(DataSource.class));
    }

    @Test
    void testUpdateDataSource_Success() throws DataSourceException {
        // Given
        DataSource updatedDataSource = new DataSource();
        updatedDataSource.setName("updated-datasource");
        updatedDataSource.setType("postgresql");
        updatedDataSource.setUrl("jdbc:postgresql://localhost:5432/test");
        updatedDataSource.setUsername("newuser");
        updatedDataSource.setPassword("newpass");

        when(dataSourceRepository.findById(1L)).thenReturn(Optional.of(testDataSource));
        when(dataSourceRepository.existsByNameAndIdNot(updatedDataSource.getName(), 1L))
            .thenReturn(false);
        when(dataSourceRepository.save(any(DataSource.class))).thenReturn(testDataSource);
        doNothing().when(dataSourceManager).updateDataSource(any(DataSource.class));

        // When
        DataSource result = dataSourceService.updateDataSource(1L, updatedDataSource);

        // Then
        assertNotNull(result);
        verify(dataSourceRepository).findById(1L);
        verify(dataSourceRepository).existsByNameAndIdNot(updatedDataSource.getName(), 1L);
        verify(dataSourceRepository).save(any(DataSource.class));
        verify(dataSourceManager).updateDataSource(any(DataSource.class));
    }

    @Test
    void testUpdateDataSource_NotFound() {
        // Given
        when(dataSourceRepository.findById(1L)).thenReturn(Optional.empty());

        // When & Then
        DataSourceException exception = assertThrows(DataSourceException.class, () -> {
            dataSourceService.updateDataSource(1L, testDataSource);
        });
        
        assertTrue(exception.getMessage().contains("数据源不存在"));
        verify(dataSourceRepository).findById(1L);
        verify(dataSourceRepository, never()).save(any(DataSource.class));
    }

    @Test
    void testDeleteDataSource_Success() throws DataSourceException {
        // Given
        when(dataSourceRepository.findById(1L)).thenReturn(Optional.of(testDataSource));
        doNothing().when(dataSourceManager).removeDataSource(1L);
        doNothing().when(dataSourceRepository).deleteById(1L);

        // When
        dataSourceService.deleteDataSource(1L);

        // Then
        verify(dataSourceRepository).findById(1L);
        verify(dataSourceManager).removeDataSource(1L);
        verify(dataSourceRepository).deleteById(1L);
    }

    @Test
    void testGetDataSourceById_Found() {
        // Given
        when(dataSourceRepository.findById(1L)).thenReturn(Optional.of(testDataSource));

        // When
        Optional<DataSource> result = dataSourceService.getDataSourceById(1L);

        // Then
        assertTrue(result.isPresent());
        assertEquals(testDataSource.getName(), result.get().getName());
        verify(dataSourceRepository).findById(1L);
    }

    @Test
    void testGetDataSourceById_NotFound() {
        // Given
        when(dataSourceRepository.findById(1L)).thenReturn(Optional.empty());

        // When
        Optional<DataSource> result = dataSourceService.getDataSourceById(1L);

        // Then
        assertFalse(result.isPresent());
        verify(dataSourceRepository).findById(1L);
    }

    @Test
    void testGetDataSourceByName_Found() {
        // Given
        when(dataSourceRepository.findByName("test-datasource"))
            .thenReturn(Optional.of(testDataSource));

        // When
        Optional<DataSource> result = dataSourceService.getDataSourceByName("test-datasource");

        // Then
        assertTrue(result.isPresent());
        assertEquals(testDataSource.getName(), result.get().getName());
        verify(dataSourceRepository).findByName("test-datasource");
    }

    @Test
    void testGetDataSources_WithFilters() {
        // Given
        List<DataSource> dataSources = Arrays.asList(testDataSource);
        Page<DataSource> page = new PageImpl<>(dataSources);
        Pageable pageable = PageRequest.of(0, 10);
        
        when(dataSourceRepository.findAll(any(Specification.class), eq(pageable)))
            .thenReturn(page);

        // When
        Page<DataSource> result = dataSourceService.getDataSources(
            "test", "mysql", 1, pageable);

        // Then
        assertNotNull(result);
        assertEquals(1, result.getContent().size());
        assertEquals(testDataSource.getName(), result.getContent().get(0).getName());
        verify(dataSourceRepository).findAll(any(Specification.class), eq(pageable));
    }

    @Test
    void testGetEnabledDataSources() {
        // Given
        List<DataSource> enabledDataSources = Arrays.asList(testDataSource);
        when(dataSourceRepository.findEnabledDataSources()).thenReturn(enabledDataSources);

        // When
        List<DataSource> result = dataSourceService.getEnabledDataSources();

        // Then
        assertNotNull(result);
        assertEquals(1, result.size());
        assertEquals(testDataSource.getName(), result.get(0).getName());
        verify(dataSourceRepository).findEnabledDataSources();
    }

    @Test
    void testTestConnection_Success() throws DataSourceException {
        // Given
        when(dataSourceRepository.findById(1L)).thenReturn(Optional.of(testDataSource));
        when(dataSourceManager.testConnection(1L)).thenReturn(true);
        doNothing().when(dataSourceRepository).updateHealthCheckInfo(
            eq(1L), any(LocalDateTime.class), eq(1), isNull());
        doNothing().when(dataSourceRepository).updateLastConnectedAt(
            eq(1L), any(LocalDateTime.class));

        // When
        boolean result = dataSourceService.testConnection(1L);

        // Then
        assertTrue(result);
        verify(dataSourceRepository).findById(1L);
        verify(dataSourceManager).testConnection(1L);
        verify(dataSourceRepository).updateHealthCheckInfo(
            eq(1L), any(LocalDateTime.class), eq(1), isNull());
        verify(dataSourceRepository).updateLastConnectedAt(
            eq(1L), any(LocalDateTime.class));
    }

    @Test
    void testTestConnection_Failure() throws DataSourceException {
        // Given
        when(dataSourceRepository.findById(1L)).thenReturn(Optional.of(testDataSource));
        when(dataSourceManager.testConnection(1L)).thenReturn(false);
        doNothing().when(dataSourceRepository).updateHealthCheckInfo(
            eq(1L), any(LocalDateTime.class), eq(2), eq("连接测试失败"));

        // When
        boolean result = dataSourceService.testConnection(1L);

        // Then
        assertFalse(result);
        verify(dataSourceRepository).findById(1L);
        verify(dataSourceManager).testConnection(1L);
        verify(dataSourceRepository).updateHealthCheckInfo(
            eq(1L), any(LocalDateTime.class), eq(2), eq("连接测试失败"));
    }

    @Test
    void testEnableDataSource_Success() throws DataSourceException {
        // Given
        when(dataSourceRepository.findById(1L)).thenReturn(Optional.of(testDataSource));
        when(dataSourceRepository.save(any(DataSource.class))).thenReturn(testDataSource);
        doNothing().when(dataSourceManager).addDataSource(any(DataSource.class));

        // When
        dataSourceService.enableDataSource(1L);

        // Then
        verify(dataSourceRepository).findById(1L);
        verify(dataSourceRepository).save(any(DataSource.class));
        verify(dataSourceManager).addDataSource(any(DataSource.class));
    }

    @Test
    void testDisableDataSource_Success() throws DataSourceException {
        // Given
        when(dataSourceRepository.findById(1L)).thenReturn(Optional.of(testDataSource));
        when(dataSourceRepository.save(any(DataSource.class))).thenReturn(testDataSource);
        doNothing().when(dataSourceManager).removeDataSource(1L);

        // When
        dataSourceService.disableDataSource(1L);

        // Then
        verify(dataSourceRepository).findById(1L);
        verify(dataSourceRepository).save(any(DataSource.class));
        verify(dataSourceManager).removeDataSource(1L);
    }

    @Test
    void testGetPoolInfo() {
        // Given
        DataSourceManager.DataSourcePoolInfo poolInfo = 
            new DataSourceManager.DataSourcePoolInfo(
                1L, "test-datasource", "mysql", 5, 3, 8, 0, false, LocalDateTime.now());
        when(dataSourceManager.getPoolInfo(1L)).thenReturn(poolInfo);

        // When
        DataSourceManager.DataSourcePoolInfo result = dataSourceService.getPoolInfo(1L);

        // Then
        assertNotNull(result);
        assertEquals(poolInfo.getDataSourceId(), result.getDataSourceId());
        assertEquals(poolInfo.getName(), result.getName());
        verify(dataSourceManager).getPoolInfo(1L);
    }

    @Test
    void testSetDefaultValues() {
        // Given
        DataSource dataSource = new DataSource();
        dataSource.setName("test");
        dataSource.setType("mysql");
        dataSource.setUrl("jdbc:mysql://localhost:3306/test");
        dataSource.setUsername("user");
        dataSource.setPassword("pass");
        // 不设置默认值

        when(dataSourceRepository.findByName(dataSource.getName())).thenReturn(Optional.empty());
        when(dataSourceRepository.save(any(DataSource.class))).thenAnswer(invocation -> {
            DataSource saved = invocation.getArgument(0);
            saved.setId(1L);
            return saved;
        });
        doNothing().when(dataSourceManager).addDataSource(any(DataSource.class));

        // When
        try {
            DataSource result = dataSourceService.createDataSource(dataSource);

            // Then
            assertNotNull(result.getStatus());
            assertEquals(1, result.getStatus()); // 默认启用
            assertNotNull(result.getHealthStatus());
            assertEquals(0, result.getHealthStatus()); // 默认未知
            assertNotNull(result.getMinPoolSize());
            assertEquals(2, result.getMinPoolSize());
            assertNotNull(result.getMaxPoolSize());
            assertEquals(10, result.getMaxPoolSize());
        } catch (DataSourceException e) {
            fail("Should not throw exception");
        }
    }

    @Test
    void testCreateDataSource_WithManagerException() throws DataSourceException {
        // Given
        when(dataSourceRepository.findByName(testDataSource.getName())).thenReturn(Optional.empty());
        when(dataSourceRepository.save(any(DataSource.class))).thenReturn(testDataSource);
        doThrow(new RuntimeException("Connection failed"))
            .when(dataSourceManager).addDataSource(any(DataSource.class));

        // When & Then
        DataSourceException exception = assertThrows(DataSourceException.class, () -> {
            dataSourceService.createDataSource(testDataSource);
        });
        
        assertTrue(exception.getMessage().contains("数据源创建失败"));
        verify(dataSourceRepository).findByName(testDataSource.getName());
        verify(dataSourceRepository).save(any(DataSource.class));
        verify(dataSourceManager).addDataSource(any(DataSource.class));
    }
}