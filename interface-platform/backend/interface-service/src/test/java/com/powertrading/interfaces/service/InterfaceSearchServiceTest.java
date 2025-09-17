package com.powertrading.interfaces.service;

import com.powertrading.interfaces.entity.InterfaceInfo;
import com.powertrading.interfaces.repository.InterfaceInfoRepository;
import com.powertrading.interfaces.service.InterfaceSearchService.*;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;

import java.time.LocalDateTime;
import java.util.*;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

/**
 * 接口搜索服务测试
 *
 * @author PowerTrading Team
 * @since 2024-01-15
 */
@ExtendWith(MockitoExtension.class)
class InterfaceSearchServiceTest {

    @Mock
    private InterfaceInfoRepository interfaceInfoRepository;

    @InjectMocks
    private InterfaceSearchService interfaceSearchService;

    private InterfaceInfo testInterface1;
    private InterfaceInfo testInterface2;
    private InterfaceSearchRequest searchRequest;

    @BeforeEach
    void setUp() {
        // 准备测试数据
        testInterface1 = new InterfaceInfo();
        testInterface1.setId(1L);
        testInterface1.setInterfaceName("用户查询接口");
        testInterface1.setInterfacePath("/api/user/query");
        testInterface1.setDescription("查询用户信息的接口");
        testInterface1.setStatus("PUBLISHED");
        testInterface1.setCategoryId(1L);
        testInterface1.setDataSourceId(1L);
        testInterface1.setCreatedBy("admin");
        testInterface1.setCreateTime(LocalDateTime.now().minusDays(1));
        testInterface1.setUpdateTime(LocalDateTime.now());
        testInterface1.setTags("用户,查询");

        testInterface2 = new InterfaceInfo();
        testInterface2.setId(2L);
        testInterface2.setInterfaceName("订单统计接口");
        testInterface2.setInterfacePath("/api/order/statistics");
        testInterface2.setDescription("统计订单数据的接口");
        testInterface2.setStatus("DRAFT");
        testInterface2.setCategoryId(2L);
        testInterface2.setDataSourceId(2L);
        testInterface2.setCreatedBy("user1");
        testInterface2.setCreateTime(LocalDateTime.now().minusDays(2));
        testInterface2.setUpdateTime(LocalDateTime.now().minusHours(1));
        testInterface2.setTags("订单,统计");

        searchRequest = new InterfaceSearchRequest();
        searchRequest.setPage(1);
        searchRequest.setSize(10);
    }

    @Test
    void testSearchInterfacesWithKeyword() {
        // 准备测试数据
        searchRequest.setKeyword("用户");
        List<InterfaceInfo> interfaces = Arrays.asList(testInterface1);
        Page<InterfaceInfo> page = new PageImpl<>(interfaces, 
            org.springframework.data.domain.PageRequest.of(0, 10), 1);
        
        when(interfaceInfoRepository.findAll(any(Specification.class), any(Pageable.class)))
            .thenReturn(page);

        // 执行测试
        InterfaceSearchResult result = interfaceSearchService.searchInterfaces(searchRequest);

        // 验证结果
        assertNotNull(result);
        assertEquals(1, result.getTotalCount());
        assertEquals(1, result.getInterfaces().size());
        assertEquals(testInterface1.getInterfaceName(), result.getInterfaces().get(0).getInterfaceName());
        assertEquals(1, result.getCurrentPage());
        assertEquals(10, result.getPageSize());
        assertTrue(result.getSearchStats().isHasKeyword());
        
        verify(interfaceInfoRepository).findAll(any(Specification.class), any(Pageable.class));
    }

    @Test
    void testSearchInterfacesWithCategoryFilter() {
        // 准备测试数据
        searchRequest.setCategoryId(1L);
        List<InterfaceInfo> interfaces = Arrays.asList(testInterface1);
        Page<InterfaceInfo> page = new PageImpl<>(interfaces, 
            org.springframework.data.domain.PageRequest.of(0, 10), 1);
        
        when(interfaceInfoRepository.findAll(any(Specification.class), any(Pageable.class)))
            .thenReturn(page);

        // 执行测试
        InterfaceSearchResult result = interfaceSearchService.searchInterfaces(searchRequest);

        // 验证结果
        assertNotNull(result);
        assertEquals(1, result.getTotalCount());
        assertEquals(1, result.getInterfaces().size());
        assertFalse(result.getSearchStats().isHasKeyword());
        assertTrue(result.getSearchStats().isHasFilters());
        
        verify(interfaceInfoRepository).findAll(any(Specification.class), any(Pageable.class));
    }

    @Test
    void testSearchInterfacesWithStatusFilter() {
        // 准备测试数据
        searchRequest.setStatus("PUBLISHED");
        List<InterfaceInfo> interfaces = Arrays.asList(testInterface1);
        Page<InterfaceInfo> page = new PageImpl<>(interfaces, 
            org.springframework.data.domain.PageRequest.of(0, 10), 1);
        
        when(interfaceInfoRepository.findAll(any(Specification.class), any(Pageable.class)))
            .thenReturn(page);

        // 执行测试
        InterfaceSearchResult result = interfaceSearchService.searchInterfaces(searchRequest);

        // 验证结果
        assertNotNull(result);
        assertEquals(1, result.getTotalCount());
        assertEquals(1, result.getInterfaces().size());
        assertEquals("PUBLISHED", result.getInterfaces().get(0).getStatus());
        
        verify(interfaceInfoRepository).findAll(any(Specification.class), any(Pageable.class));
    }

    @Test
    void testSearchInterfacesWithMultipleFilters() {
        // 准备测试数据
        searchRequest.setKeyword("查询");
        searchRequest.setCategoryId(1L);
        searchRequest.setStatus("PUBLISHED");
        searchRequest.setCreatedBy("admin");
        
        List<InterfaceInfo> interfaces = Arrays.asList(testInterface1);
        Page<InterfaceInfo> page = new PageImpl<>(interfaces, 
            org.springframework.data.domain.PageRequest.of(0, 10), 1);
        
        when(interfaceInfoRepository.findAll(any(Specification.class), any(Pageable.class)))
            .thenReturn(page);

        // 执行测试
        InterfaceSearchResult result = interfaceSearchService.searchInterfaces(searchRequest);

        // 验证结果
        assertNotNull(result);
        assertEquals(1, result.getTotalCount());
        assertTrue(result.getSearchStats().isHasKeyword());
        assertTrue(result.getSearchStats().isHasFilters());
        
        verify(interfaceInfoRepository).findAll(any(Specification.class), any(Pageable.class));
    }

    @Test
    void testSearchInterfacesWithTimeRangeFilter() {
        // 准备测试数据
        LocalDateTime startTime = LocalDateTime.now().minusDays(3);
        LocalDateTime endTime = LocalDateTime.now();
        
        searchRequest.setCreateTimeStart(startTime);
        searchRequest.setCreateTimeEnd(endTime);
        
        List<InterfaceInfo> interfaces = Arrays.asList(testInterface1, testInterface2);
        Page<InterfaceInfo> page = new PageImpl<>(interfaces, 
            org.springframework.data.domain.PageRequest.of(0, 10), 2);
        
        when(interfaceInfoRepository.findAll(any(Specification.class), any(Pageable.class)))
            .thenReturn(page);

        // 执行测试
        InterfaceSearchResult result = interfaceSearchService.searchInterfaces(searchRequest);

        // 验证结果
        assertNotNull(result);
        assertEquals(2, result.getTotalCount());
        assertTrue(result.getSearchStats().isHasFilters());
        
        verify(interfaceInfoRepository).findAll(any(Specification.class), any(Pageable.class));
    }

    @Test
    void testSearchInterfacesWithTagsFilter() {
        // 准备测试数据
        searchRequest.setTags(Arrays.asList("用户", "查询"));
        
        List<InterfaceInfo> interfaces = Arrays.asList(testInterface1);
        Page<InterfaceInfo> page = new PageImpl<>(interfaces, 
            org.springframework.data.domain.PageRequest.of(0, 10), 1);
        
        when(interfaceInfoRepository.findAll(any(Specification.class), any(Pageable.class)))
            .thenReturn(page);

        // 执行测试
        InterfaceSearchResult result = interfaceSearchService.searchInterfaces(searchRequest);

        // 验证结果
        assertNotNull(result);
        assertEquals(1, result.getTotalCount());
        assertTrue(result.getSearchStats().isHasFilters());
        
        verify(interfaceInfoRepository).findAll(any(Specification.class), any(Pageable.class));
    }

    @Test
    void testSearchInterfacesWithCustomSorting() {
        // 准备测试数据
        searchRequest.setSortBy("interfaceName");
        searchRequest.setSortOrder("asc");
        
        List<InterfaceInfo> interfaces = Arrays.asList(testInterface1, testInterface2);
        Page<InterfaceInfo> page = new PageImpl<>(interfaces, 
            org.springframework.data.domain.PageRequest.of(0, 10), 2);
        
        when(interfaceInfoRepository.findAll(any(Specification.class), any(Pageable.class)))
            .thenReturn(page);

        // 执行测试
        InterfaceSearchResult result = interfaceSearchService.searchInterfaces(searchRequest);

        // 验证结果
        assertNotNull(result);
        assertEquals(2, result.getTotalCount());
        
        verify(interfaceInfoRepository).findAll(any(Specification.class), any(Pageable.class));
    }

    @Test
    void testSearchInterfacesWithPagination() {
        // 准备测试数据
        searchRequest.setPage(2);
        searchRequest.setSize(5);
        
        List<InterfaceInfo> interfaces = Arrays.asList(testInterface2);
        Page<InterfaceInfo> page = new PageImpl<>(interfaces, 
            org.springframework.data.domain.PageRequest.of(1, 5), 6); // 总共6条记录
        
        when(interfaceInfoRepository.findAll(any(Specification.class), any(Pageable.class)))
            .thenReturn(page);

        // 执行测试
        InterfaceSearchResult result = interfaceSearchService.searchInterfaces(searchRequest);

        // 验证结果
        assertNotNull(result);
        assertEquals(6, result.getTotalCount());
        assertEquals(2, result.getTotalPages());
        assertEquals(2, result.getCurrentPage());
        assertEquals(5, result.getPageSize());
        assertFalse(result.isHasNext());
        assertTrue(result.isHasPrevious());
        
        verify(interfaceInfoRepository).findAll(any(Specification.class), any(Pageable.class));
    }

    @Test
    void testGetSearchSuggestions() {
        // 准备测试数据
        String keyword = "用户";
        List<InterfaceInfo> nameMatches = Arrays.asList(testInterface1);
        List<InterfaceInfo> descMatches = Arrays.asList(testInterface1);
        
        when(interfaceInfoRepository.findTop10ByInterfaceNameContainingIgnoreCase(keyword))
            .thenReturn(nameMatches);
        when(interfaceInfoRepository.findTop10ByDescriptionContainingIgnoreCase(keyword))
            .thenReturn(descMatches);

        // 执行测试
        List<String> result = interfaceSearchService.getSearchSuggestions(keyword);

        // 验证结果
        assertNotNull(result);
        assertFalse(result.isEmpty());
        assertTrue(result.contains(testInterface1.getInterfaceName()));
        
        verify(interfaceInfoRepository).findTop10ByInterfaceNameContainingIgnoreCase(keyword);
        verify(interfaceInfoRepository).findTop10ByDescriptionContainingIgnoreCase(keyword);
    }

    @Test
    void testGetSearchSuggestionsWithShortKeyword() {
        // 执行测试
        List<String> result = interfaceSearchService.getSearchSuggestions("a");

        // 验证结果
        assertNotNull(result);
        assertTrue(result.isEmpty());
        
        // 验证没有调用repository方法
        verify(interfaceInfoRepository, never()).findTop10ByInterfaceNameContainingIgnoreCase(anyString());
        verify(interfaceInfoRepository, never()).findTop10ByDescriptionContainingIgnoreCase(anyString());
    }

    @Test
    void testGetSearchSuggestionsWithEmptyKeyword() {
        // 执行测试
        List<String> result = interfaceSearchService.getSearchSuggestions("");

        // 验证结果
        assertNotNull(result);
        assertTrue(result.isEmpty());
        
        // 验证没有调用repository方法
        verify(interfaceInfoRepository, never()).findTop10ByInterfaceNameContainingIgnoreCase(anyString());
        verify(interfaceInfoRepository, never()).findTop10ByDescriptionContainingIgnoreCase(anyString());
    }

    @Test
    void testGetPopularKeywords() {
        // 执行测试
        List<String> result = interfaceSearchService.getPopularKeywords();

        // 验证结果
        assertNotNull(result);
        assertFalse(result.isEmpty());
        assertTrue(result.contains("数据查询"));
        assertTrue(result.contains("统计报表"));
        assertTrue(result.contains("实时数据"));
    }

    @Test
    void testGetFilterOptions() {
        // 执行测试
        FilterOptions result = interfaceSearchService.getFilterOptions();

        // 验证结果
        assertNotNull(result);
        assertNotNull(result.getStatusOptions());
        assertNotNull(result.getCategoryOptions());
        assertNotNull(result.getSortOptions());
        
        // 验证状态选项
        assertTrue(result.getStatusOptions().stream()
            .anyMatch(option -> "DRAFT".equals(option.getValue())));
        assertTrue(result.getStatusOptions().stream()
            .anyMatch(option -> "PUBLISHED".equals(option.getValue())));
        assertTrue(result.getStatusOptions().stream()
            .anyMatch(option -> "OFFLINE".equals(option.getValue())));
        
        // 验证排序选项
        assertTrue(result.getSortOptions().stream()
            .anyMatch(option -> "updateTime".equals(option.getValue())));
        assertTrue(result.getSortOptions().stream()
            .anyMatch(option -> "createTime".equals(option.getValue())));
        assertTrue(result.getSortOptions().stream()
            .anyMatch(option -> "interfaceName".equals(option.getValue())));
    }

    @Test
    void testSearchInterfacesEmptyResult() {
        // 准备测试数据
        searchRequest.setKeyword("不存在的接口");
        Page<InterfaceInfo> emptyPage = new PageImpl<>(Collections.emptyList(), 
            org.springframework.data.domain.PageRequest.of(0, 10), 0);
        
        when(interfaceInfoRepository.findAll(any(Specification.class), any(Pageable.class)))
            .thenReturn(emptyPage);

        // 执行测试
        InterfaceSearchResult result = interfaceSearchService.searchInterfaces(searchRequest);

        // 验证结果
        assertNotNull(result);
        assertEquals(0, result.getTotalCount());
        assertTrue(result.getInterfaces().isEmpty());
        assertEquals(0, result.getTotalPages());
        assertFalse(result.isHasNext());
        assertFalse(result.isHasPrevious());
        
        verify(interfaceInfoRepository).findAll(any(Specification.class), any(Pageable.class));
    }

    @Test
    void testSearchInterfacesWithInvalidPageSize() {
        // 准备测试数据 - 测试页面大小限制
        searchRequest.setSize(200); // 超过最大限制
        
        List<InterfaceInfo> interfaces = Arrays.asList(testInterface1);
        Page<InterfaceInfo> page = new PageImpl<>(interfaces, 
            org.springframework.data.domain.PageRequest.of(0, 100), 1); // 应该被限制为100
        
        when(interfaceInfoRepository.findAll(any(Specification.class), any(Pageable.class)))
            .thenReturn(page);

        // 执行测试
        InterfaceSearchResult result = interfaceSearchService.searchInterfaces(searchRequest);

        // 验证结果
        assertNotNull(result);
        assertEquals(100, result.getPageSize()); // 应该被限制为100
        
        verify(interfaceInfoRepository).findAll(any(Specification.class), any(Pageable.class));
    }
}