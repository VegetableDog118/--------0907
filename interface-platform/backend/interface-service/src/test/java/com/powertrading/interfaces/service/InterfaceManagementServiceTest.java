package com.powertrading.interfaces.service;

import com.powertrading.interfaces.entity.InterfaceInfo;
import com.powertrading.interfaces.repository.InterfaceInfoRepository;
import com.powertrading.interfaces.service.InterfaceManagementService.*;
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

import java.time.LocalDateTime;
import java.util.*;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

/**
 * 接口管理服务测试
 *
 * @author PowerTrading Team
 * @since 2024-01-15
 */
@ExtendWith(MockitoExtension.class)
class InterfaceManagementServiceTest {

    @Mock
    private InterfaceInfoRepository interfaceInfoRepository;

    @Mock
    private InterfaceCategoryService interfaceCategoryService;

    @InjectMocks
    private InterfaceManagementService interfaceManagementService;

    private InterfaceInfo testInterface;
    private InterfaceConfigurationRequest testRequest;

    @BeforeEach
    void setUp() {
        // 准备测试数据
        testInterface = new InterfaceInfo();
        testInterface.setId(1L);
        testInterface.setInterfaceName("测试接口");
        testInterface.setInterfacePath("/api/test");
        testInterface.setDescription("测试接口描述");
        testInterface.setStatus("DRAFT");
        testInterface.setCategoryId(1L);
        testInterface.setDataSourceId(1L);
        testInterface.setCreatedBy("testUser");
        testInterface.setCreateTime(LocalDateTime.now());
        testInterface.setUpdateTime(LocalDateTime.now());

        testRequest = new InterfaceConfigurationRequest();
        testRequest.setInterfaceName("测试接口");
        testRequest.setDescription("测试接口描述");
        testRequest.setCategoryId(1L);
        testRequest.setDataSourceId(1L);
        testRequest.setTableName("test_table");
        testRequest.setParameters(Arrays.asList(
            createParameter("id", "integer", true, "ID"),
            createParameter("name", "string", false, "名称")
        ));
    }

    @Test
    void testGetInterfacesPaged() {
        // 准备测试数据
        List<InterfaceInfo> interfaces = Arrays.asList(testInterface);
        Page<InterfaceInfo> page = new PageImpl<>(interfaces, PageRequest.of(0, 10), 1);
        
        when(interfaceInfoRepository.findAll(any(Pageable.class))).thenReturn(page);

        // 执行测试
        Page<InterfaceInfo> result = interfaceManagementService.getInterfacesPaged(1, 10);

        // 验证结果
        assertNotNull(result);
        assertEquals(1, result.getTotalElements());
        assertEquals(1, result.getContent().size());
        assertEquals(testInterface.getInterfaceName(), result.getContent().get(0).getInterfaceName());
        
        verify(interfaceInfoRepository).findAll(any(Pageable.class));
    }

    @Test
    void testGetInterfaceById() {
        // 准备测试数据
        when(interfaceInfoRepository.findById(1L)).thenReturn(Optional.of(testInterface));

        // 执行测试
        InterfaceInfo result = interfaceManagementService.getInterfaceById(1L);

        // 验证结果
        assertNotNull(result);
        assertEquals(testInterface.getId(), result.getId());
        assertEquals(testInterface.getInterfaceName(), result.getInterfaceName());
        
        verify(interfaceInfoRepository).findById(1L);
    }

    @Test
    void testGetInterfaceByIdNotFound() {
        // 准备测试数据
        when(interfaceInfoRepository.findById(999L)).thenReturn(Optional.empty());

        // 执行测试并验证异常
        assertThrows(RuntimeException.class, () -> {
            interfaceManagementService.getInterfaceById(999L);
        });
        
        verify(interfaceInfoRepository).findById(999L);
    }

    @Test
    void testUpdateInterfaceConfiguration() {
        // 准备测试数据
        when(interfaceInfoRepository.findById(1L)).thenReturn(Optional.of(testInterface));
        when(interfaceInfoRepository.save(any(InterfaceInfo.class))).thenReturn(testInterface);

        // 执行测试
        InterfaceInfo result = interfaceManagementService.updateInterfaceConfiguration(1L, testRequest);

        // 验证结果
        assertNotNull(result);
        assertEquals(testRequest.getInterfaceName(), result.getInterfaceName());
        assertEquals(testRequest.getDescription(), result.getDescription());
        
        verify(interfaceInfoRepository).findById(1L);
        verify(interfaceInfoRepository).save(any(InterfaceInfo.class));
    }

    @Test
    void testUpdateInterfaceParameters() {
        // 准备测试数据
        when(interfaceInfoRepository.findById(1L)).thenReturn(Optional.of(testInterface));
        when(interfaceInfoRepository.save(any(InterfaceInfo.class))).thenReturn(testInterface);
        
        ParameterUpdateRequest paramRequest = new ParameterUpdateRequest();
        paramRequest.setParameters(testRequest.getParameters());

        // 执行测试
        InterfaceInfo result = interfaceManagementService.updateInterfaceParameters(1L, paramRequest);

        // 验证结果
        assertNotNull(result);
        
        verify(interfaceInfoRepository).findById(1L);
        verify(interfaceInfoRepository).save(any(InterfaceInfo.class));
    }

    @Test
    void testDeleteInterface() {
        // 准备测试数据
        when(interfaceInfoRepository.findById(1L)).thenReturn(Optional.of(testInterface));
        doNothing().when(interfaceInfoRepository).deleteById(1L);

        // 执行测试
        assertDoesNotThrow(() -> {
            interfaceManagementService.deleteInterface(1L);
        });
        
        verify(interfaceInfoRepository).findById(1L);
        verify(interfaceInfoRepository).deleteById(1L);
    }

    @Test
    void testDeleteInterfaceNotFound() {
        // 准备测试数据
        when(interfaceInfoRepository.findById(999L)).thenReturn(Optional.empty());

        // 执行测试并验证异常
        assertThrows(RuntimeException.class, () -> {
            interfaceManagementService.deleteInterface(999L);
        });
        
        verify(interfaceInfoRepository).findById(999L);
        verify(interfaceInfoRepository, never()).deleteById(anyLong());
    }

    @Test
    void testGetInterfaceCategories() {
        // 准备测试数据
        List<InterfaceCategoryService.CategoryInfo> categories = Arrays.asList(
            createCategory(1L, "数据查询"),
            createCategory(2L, "统计分析")
        );
        when(interfaceCategoryService.getAllCategories()).thenReturn(categories);

        // 执行测试
        List<InterfaceCategoryService.CategoryInfo> result = interfaceManagementService.getInterfaceCategories();

        // 验证结果
        assertNotNull(result);
        assertEquals(2, result.size());
        assertEquals("数据查询", result.get(0).getName());
        assertEquals("统计分析", result.get(1).getName());
        
        verify(interfaceCategoryService).getAllCategories();
    }

    @Test
    void testGetStandardParameterTemplates() {
        // 执行测试
        List<ParameterTemplate> result = interfaceManagementService.getStandardParameterTemplates();

        // 验证结果
        assertNotNull(result);
        assertFalse(result.isEmpty());
        
        // 验证包含基本模板
        assertTrue(result.stream().anyMatch(t -> "基础查询".equals(t.getName())));
        assertTrue(result.stream().anyMatch(t -> "分页查询".equals(t.getName())));
        assertTrue(result.stream().anyMatch(t -> "时间范围查询".equals(t.getName())));
    }

    @Test
    void testCopyInterface() {
        // 准备测试数据
        when(interfaceInfoRepository.findById(1L)).thenReturn(Optional.of(testInterface));
        when(interfaceInfoRepository.save(any(InterfaceInfo.class))).thenAnswer(invocation -> {
            InterfaceInfo saved = invocation.getArgument(0);
            saved.setId(2L); // 模拟新生成的ID
            return saved;
        });
        
        CopyInterfaceRequest copyRequest = new CopyInterfaceRequest();
        copyRequest.setNewName("复制的测试接口");
        copyRequest.setNewDescription("复制的测试接口描述");

        // 执行测试
        InterfaceInfo result = interfaceManagementService.copyInterface(1L, copyRequest);

        // 验证结果
        assertNotNull(result);
        assertEquals(2L, result.getId());
        assertEquals(copyRequest.getNewName(), result.getInterfaceName());
        assertEquals(copyRequest.getNewDescription(), result.getDescription());
        assertEquals("DRAFT", result.getStatus()); // 复制的接口应该是草稿状态
        
        verify(interfaceInfoRepository).findById(1L);
        verify(interfaceInfoRepository).save(any(InterfaceInfo.class));
    }

    @Test
    void testGetInterfaceStatistics() {
        // 准备测试数据
        when(interfaceInfoRepository.count()).thenReturn(100L);
        when(interfaceInfoRepository.countByStatus("PUBLISHED")).thenReturn(60L);
        when(interfaceInfoRepository.countByStatus("DRAFT")).thenReturn(30L);
        when(interfaceInfoRepository.countByStatus("OFFLINE")).thenReturn(10L);

        // 执行测试
        InterfaceStatistics result = interfaceManagementService.getInterfaceStatistics();

        // 验证结果
        assertNotNull(result);
        assertEquals(100L, result.getTotalCount());
        assertEquals(60L, result.getPublishedCount());
        assertEquals(30L, result.getDraftCount());
        assertEquals(10L, result.getOfflineCount());
        
        verify(interfaceInfoRepository).count();
        verify(interfaceInfoRepository).countByStatus("PUBLISHED");
        verify(interfaceInfoRepository).countByStatus("DRAFT");
        verify(interfaceInfoRepository).countByStatus("OFFLINE");
    }

    @Test
    void testValidateInterfaceName() {
        // 测试名称不存在的情况
        when(interfaceInfoRepository.existsByInterfaceName("新接口名称")).thenReturn(false);
        
        boolean result1 = interfaceManagementService.validateInterfaceName("新接口名称");
        assertTrue(result1);
        
        // 测试名称已存在的情况
        when(interfaceInfoRepository.existsByInterfaceName("已存在的接口")).thenReturn(true);
        
        boolean result2 = interfaceManagementService.validateInterfaceName("已存在的接口");
        assertFalse(result2);
        
        verify(interfaceInfoRepository).existsByInterfaceName("新接口名称");
        verify(interfaceInfoRepository).existsByInterfaceName("已存在的接口");
    }

    /**
     * 创建参数配置
     */
    private InterfaceConfigurationRequest.ParameterConfiguration createParameter(
            String name, String type, boolean required, String description) {
        InterfaceConfigurationRequest.ParameterConfiguration param = 
            new InterfaceConfigurationRequest.ParameterConfiguration();
        param.setParamName(name);
        param.setParamType(type);
        param.setRequired(required);
        param.setDescription(description);
        return param;
    }

    /**
     * 创建分类信息
     */
    private InterfaceCategoryService.CategoryInfo createCategory(Long id, String name) {
        InterfaceCategoryService.CategoryInfo category = new InterfaceCategoryService.CategoryInfo();
        category.setId(id);
        category.setName(name);
        category.setDescription(name + "分类");
        category.setStatus("ACTIVE");
        return category;
    }
}