package com.powertrading.interfaces.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.powertrading.interfaces.entity.InterfaceInfo;
import com.powertrading.interfaces.repository.InterfaceInfoRepository;
import com.powertrading.interfaces.service.InterfaceManagementService.*;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureWebMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.http.MediaType;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;

import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

/**
 * 接口管理控制器集成测试
 *
 * @author PowerTrading Team
 * @since 2024-01-15
 */
@SpringBootTest
@AutoConfigureWebMvc
@ActiveProfiles("test")
@Transactional
class InterfaceManagementControllerIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockBean
    private InterfaceInfoRepository interfaceInfoRepository;

    private InterfaceInfo testInterface;
    private InterfaceConfigurationRequest configRequest;

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

        configRequest = new InterfaceConfigurationRequest();
        configRequest.setInterfaceName("测试接口");
        configRequest.setDescription("测试接口描述");
        configRequest.setCategoryId(1L);
        configRequest.setDataSourceId(1L);
        configRequest.setTableName("test_table");
        configRequest.setParameters(Arrays.asList(
            createParameter("id", "integer", true, "ID"),
            createParameter("name", "string", false, "名称")
        ));
    }

    @Test
    @WithMockUser(roles = "ADMIN")
    void testGetInterfacesPaged() throws Exception {
        // 准备测试数据
        List<InterfaceInfo> interfaces = Arrays.asList(testInterface);
        Page<InterfaceInfo> page = new PageImpl<>(interfaces, PageRequest.of(0, 10), 1);
        
        when(interfaceInfoRepository.findAll(any(org.springframework.data.domain.Pageable.class)))
            .thenReturn(page);

        // 执行测试
        mockMvc.perform(get("/api/interfaces")
                .param("page", "1")
                .param("size", "10")
                .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.content[0].interfaceName").value("测试接口"))
                .andExpect(jsonPath("$.data.totalElements").value(1));

        verify(interfaceInfoRepository).findAll(any(org.springframework.data.domain.Pageable.class));
    }

    @Test
    @WithMockUser(roles = "USER")
    void testGetInterfaceById() throws Exception {
        // 准备测试数据
        when(interfaceInfoRepository.findById(1L)).thenReturn(Optional.of(testInterface));

        // 执行测试
        mockMvc.perform(get("/api/interfaces/1")
                .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.id").value(1))
                .andExpect(jsonPath("$.data.interfaceName").value("测试接口"));

        verify(interfaceInfoRepository).findById(1L);
    }

    @Test
    @WithMockUser(roles = "USER")
    void testGetInterfaceByIdNotFound() throws Exception {
        // 准备测试数据
        when(interfaceInfoRepository.findById(999L)).thenReturn(Optional.empty());

        // 执行测试
        mockMvc.perform(get("/api/interfaces/999")
                .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.success").value(false))
                .andExpect(jsonPath("$.message").value(containsString("接口不存在")));

        verify(interfaceInfoRepository).findById(999L);
    }

    @Test
    @WithMockUser(roles = "ADMIN")
    void testUpdateInterfaceConfiguration() throws Exception {
        // 准备测试数据
        when(interfaceInfoRepository.findById(1L)).thenReturn(Optional.of(testInterface));
        when(interfaceInfoRepository.save(any(InterfaceInfo.class))).thenReturn(testInterface);

        // 执行测试
        mockMvc.perform(put("/api/interfaces/1/configuration")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(configRequest)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.interfaceName").value("测试接口"));

        verify(interfaceInfoRepository).findById(1L);
        verify(interfaceInfoRepository).save(any(InterfaceInfo.class));
    }

    @Test
    @WithMockUser(roles = "ADMIN")
    void testUpdateInterfaceConfigurationWithInvalidData() throws Exception {
        // 准备无效的请求数据
        InterfaceConfigurationRequest invalidRequest = new InterfaceConfigurationRequest();
        // 缺少必需字段

        // 执行测试
        mockMvc.perform(put("/api/interfaces/1/configuration")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(invalidRequest)))
                .andExpect(status().isBadRequest());
    }

    @Test
    @WithMockUser(roles = "ADMIN")
    void testUpdateInterfaceParameters() throws Exception {
        // 准备测试数据
        when(interfaceInfoRepository.findById(1L)).thenReturn(Optional.of(testInterface));
        when(interfaceInfoRepository.save(any(InterfaceInfo.class))).thenReturn(testInterface);
        
        ParameterUpdateRequest paramRequest = new ParameterUpdateRequest();
        paramRequest.setParameters(configRequest.getParameters());

        // 执行测试
        mockMvc.perform(put("/api/interfaces/1/parameters")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(paramRequest)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true));

        verify(interfaceInfoRepository).findById(1L);
        verify(interfaceInfoRepository).save(any(InterfaceInfo.class));
    }

    @Test
    @WithMockUser(roles = "ADMIN")
    void testDeleteInterface() throws Exception {
        // 准备测试数据
        when(interfaceInfoRepository.findById(1L)).thenReturn(Optional.of(testInterface));
        doNothing().when(interfaceInfoRepository).deleteById(1L);

        // 执行测试
        mockMvc.perform(delete("/api/interfaces/1")
                .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.message").value("接口删除成功"));

        verify(interfaceInfoRepository).findById(1L);
        verify(interfaceInfoRepository).deleteById(1L);
    }

    @Test
    @WithMockUser(roles = "USER")
    void testDeleteInterfaceWithoutPermission() throws Exception {
        // 执行测试 - 普通用户不应该有删除权限
        mockMvc.perform(delete("/api/interfaces/1")
                .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isForbidden());
    }

    @Test
    @WithMockUser(roles = "USER")
    void testGetInterfaceCategories() throws Exception {
        // 执行测试
        mockMvc.perform(get("/api/interfaces/categories")
                .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data").isArray());
    }

    @Test
    @WithMockUser(roles = "USER")
    void testGetStandardParameterTemplates() throws Exception {
        // 执行测试
        mockMvc.perform(get("/api/interfaces/parameter-templates")
                .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data").isArray());
    }

    @Test
    @WithMockUser(roles = "ADMIN")
    void testCopyInterface() throws Exception {
        // 准备测试数据
        when(interfaceInfoRepository.findById(1L)).thenReturn(Optional.of(testInterface));
        when(interfaceInfoRepository.save(any(InterfaceInfo.class))).thenAnswer(invocation -> {
            InterfaceInfo saved = invocation.getArgument(0);
            saved.setId(2L);
            return saved;
        });
        
        CopyInterfaceRequest copyRequest = new CopyInterfaceRequest();
        copyRequest.setNewName("复制的测试接口");
        copyRequest.setNewDescription("复制的测试接口描述");

        // 执行测试
        mockMvc.perform(post("/api/interfaces/1/copy")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(copyRequest)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.id").value(2))
                .andExpect(jsonPath("$.data.interfaceName").value("复制的测试接口"));

        verify(interfaceInfoRepository).findById(1L);
        verify(interfaceInfoRepository).save(any(InterfaceInfo.class));
    }

    @Test
    @WithMockUser(roles = "USER")
    void testGetInterfaceStatistics() throws Exception {
        // 准备测试数据
        when(interfaceInfoRepository.count()).thenReturn(100L);
        when(interfaceInfoRepository.countByStatus("PUBLISHED")).thenReturn(60L);
        when(interfaceInfoRepository.countByStatus("DRAFT")).thenReturn(30L);
        when(interfaceInfoRepository.countByStatus("OFFLINE")).thenReturn(10L);

        // 执行测试
        mockMvc.perform(get("/api/interfaces/statistics")
                .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.totalCount").value(100))
                .andExpect(jsonPath("$.data.publishedCount").value(60))
                .andExpect(jsonPath("$.data.draftCount").value(30))
                .andExpect(jsonPath("$.data.offlineCount").value(10));

        verify(interfaceInfoRepository).count();
        verify(interfaceInfoRepository).countByStatus("PUBLISHED");
        verify(interfaceInfoRepository).countByStatus("DRAFT");
        verify(interfaceInfoRepository).countByStatus("OFFLINE");
    }

    @Test
    @WithMockUser(roles = "USER")
    void testValidateInterfaceName() throws Exception {
        // 准备测试数据
        when(interfaceInfoRepository.existsByInterfaceName("新接口名称")).thenReturn(false);

        // 执行测试
        mockMvc.perform(get("/api/interfaces/validate-name")
                .param("name", "新接口名称")
                .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.available").value(true));

        verify(interfaceInfoRepository).existsByInterfaceName("新接口名称");
    }

    @Test
    @WithMockUser(roles = "USER")
    void testValidateInterfaceNameExists() throws Exception {
        // 准备测试数据
        when(interfaceInfoRepository.existsByInterfaceName("已存在的接口")).thenReturn(true);

        // 执行测试
        mockMvc.perform(get("/api/interfaces/validate-name")
                .param("name", "已存在的接口")
                .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.available").value(false));

        verify(interfaceInfoRepository).existsByInterfaceName("已存在的接口");
    }

    @Test
    void testUnauthorizedAccess() throws Exception {
        // 执行测试 - 未认证用户不应该有访问权限
        mockMvc.perform(get("/api/interfaces")
                .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isUnauthorized());
    }

    @Test
    @WithMockUser(roles = "USER")
    void testGetInterfacesWithPagination() throws Exception {
        // 准备测试数据
        List<InterfaceInfo> interfaces = Arrays.asList(testInterface);
        Page<InterfaceInfo> page = new PageImpl<>(interfaces, PageRequest.of(1, 5), 10);
        
        when(interfaceInfoRepository.findAll(any(org.springframework.data.domain.Pageable.class)))
            .thenReturn(page);

        // 执行测试
        mockMvc.perform(get("/api/interfaces")
                .param("page", "2")
                .param("size", "5")
                .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.number").value(1)) // Spring Data的页码从0开始
                .andExpect(jsonPath("$.data.size").value(5))
                .andExpect(jsonPath("$.data.totalElements").value(10));

        verify(interfaceInfoRepository).findAll(any(org.springframework.data.domain.Pageable.class));
    }

    @Test
    @WithMockUser(roles = "ADMIN")
    void testUpdateInterfaceConfigurationConcurrency() throws Exception {
        // 准备测试数据 - 模拟并发更新场景
        InterfaceInfo outdatedInterface = new InterfaceInfo();
        outdatedInterface.setId(1L);
        outdatedInterface.setInterfaceName("旧接口名称");
        outdatedInterface.setUpdateTime(LocalDateTime.now().minusMinutes(5));
        
        when(interfaceInfoRepository.findById(1L)).thenReturn(Optional.of(outdatedInterface));
        when(interfaceInfoRepository.save(any(InterfaceInfo.class)))
            .thenThrow(new org.springframework.dao.OptimisticLockingFailureException("版本冲突"));

        // 执行测试
        mockMvc.perform(put("/api/interfaces/1/configuration")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(configRequest)))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.success").value(false))
                .andExpect(jsonPath("$.message").value(containsString("版本冲突")));

        verify(interfaceInfoRepository).findById(1L);
        verify(interfaceInfoRepository).save(any(InterfaceInfo.class));
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
}