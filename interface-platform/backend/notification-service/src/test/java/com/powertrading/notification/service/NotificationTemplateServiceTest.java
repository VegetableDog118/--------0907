package com.powertrading.notification.service;

import com.powertrading.notification.entity.NotificationTemplate;
import com.powertrading.notification.mapper.NotificationTemplateMapper;
import com.powertrading.notification.service.impl.NotificationTemplateServiceImpl;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDateTime;
import java.util.*;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

/**
 * 通知模板服务测试类
 *
 * @author PowerTrading
 * @since 2024-01-15
 */
@ExtendWith(MockitoExtension.class)
class NotificationTemplateServiceTest {

    @Mock
    private NotificationTemplateMapper templateMapper;

    @InjectMocks
    private NotificationTemplateServiceImpl templateService;

    private NotificationTemplate testTemplate;

    @BeforeEach
    void setUp() {
        testTemplate = new NotificationTemplate();
        testTemplate.setId("test-id-001");
        testTemplate.setTemplateCode("TEST_TEMPLATE");
        testTemplate.setTemplateName("测试模板");
        testTemplate.setTemplateType("email");
        testTemplate.setTitleTemplate("欢迎 ${userName}");
        testTemplate.setContentTemplate("您好 ${userName}，您的订单 ${orderId} 已创建成功！");
        testTemplate.setStatus(1);
        testTemplate.setCreateTime(LocalDateTime.now());
        testTemplate.setUpdateTime(LocalDateTime.now());
    }

    @Test
    void testExtractVariables() {
        // 执行测试
        List<String> variables = templateService.extractVariables("您好 ${userName}，您的订单 ${orderId} 已创建成功！");

        // 验证结果
        assertNotNull(variables);
        assertEquals(2, variables.size());
        assertTrue(variables.contains("userName"));
        assertTrue(variables.contains("orderId"));
        
        System.out.println("✅ 变量提取测试通过: " + variables);
    }

    @Test
    void testValidateTemplate() {
        // 准备测试数据
        String template = "您好 ${userName}，您的订单 ${orderId} 已创建成功！";
        Map<String, Object> variables = new HashMap<>();
        variables.put("userName", "张三");
        variables.put("orderId", "ORDER-001");

        // 执行测试
        Map<String, Object> result = templateService.validateTemplate(template, variables);

        // 验证结果
        assertNotNull(result);
        assertTrue((Boolean) result.get("valid"));
        assertEquals("您好 张三，您的订单 ORDER-001 已创建成功！", result.get("renderedContent"));
        
        @SuppressWarnings("unchecked")
        List<String> extractedVariables = (List<String>) result.get("variables");
        assertEquals(2, extractedVariables.size());
        assertTrue(extractedVariables.contains("userName"));
        assertTrue(extractedVariables.contains("orderId"));
        
        System.out.println("✅ 模板验证测试通过: " + result.get("renderedContent"));
    }

    @Test
    void testRenderTitle() {
        // 准备测试数据
        String titleTemplate = "欢迎 ${userName}";
        Map<String, Object> variables = new HashMap<>();
        variables.put("userName", "张三");

        // 执行测试
        String result = templateService.renderTitle(titleTemplate, variables);

        // 验证结果
        assertEquals("欢迎 张三", result);
        
        System.out.println("✅ 标题渲染测试通过: " + result);
    }

    @Test
    void testRenderContent() {
        // 准备测试数据
        String contentTemplate = "您好 ${userName}，您的订单 ${orderId} 已创建成功！";
        Map<String, Object> variables = new HashMap<>();
        variables.put("userName", "张三");
        variables.put("orderId", "ORDER-001");

        // 执行测试
        String result = templateService.renderContent(contentTemplate, variables);

        // 验证结果
        assertEquals("您好 张三，您的订单 ORDER-001 已创建成功！", result);
        
        System.out.println("✅ 内容渲染测试通过: " + result);
    }

    @Test
    void testGetTemplateById() {
        // Mock 方法调用
        when(templateMapper.selectById("test-id-001")).thenReturn(testTemplate);

        // 执行测试
        NotificationTemplate result = templateService.getTemplateById("test-id-001");

        // 验证结果
        assertNotNull(result);
        assertEquals("TEST_TEMPLATE", result.getTemplateCode());
        assertEquals("测试模板", result.getTemplateName());
        verify(templateMapper).selectById("test-id-001");
        
        System.out.println("✅ 根据ID获取模板测试通过: " + result.getTemplateName());
    }

    @Test
    void testGetTemplateByCode() {
        // Mock 方法调用
        when(templateMapper.selectByTemplateCode("TEST_TEMPLATE")).thenReturn(testTemplate);

        // 执行测试
        NotificationTemplate result = templateService.getTemplateByCode("TEST_TEMPLATE");

        // 验证结果
        assertNotNull(result);
        assertEquals("TEST_TEMPLATE", result.getTemplateCode());
        assertEquals("测试模板", result.getTemplateName());
        verify(templateMapper).selectByTemplateCode("TEST_TEMPLATE");
        
        System.out.println("✅ 根据编码获取模板测试通过: " + result.getTemplateName());
    }

    @Test
    void testRenderTemplate() {
        // 准备测试数据
        Map<String, Object> variables = new HashMap<>();
        variables.put("userName", "张三");
        variables.put("orderId", "ORDER-001");

        // Mock 方法调用
        when(templateMapper.selectByTemplateCode("TEST_TEMPLATE")).thenReturn(testTemplate);

        // 执行测试
        Map<String, String> result = templateService.renderTemplate("TEST_TEMPLATE", variables);

        // 验证结果
        assertNotNull(result);
        assertEquals("欢迎 张三", result.get("title"));
        assertEquals("您好 张三，您的订单 ORDER-001 已创建成功！", result.get("content"));
        assertEquals("email", result.get("templateType"));
        verify(templateMapper).selectByTemplateCode("TEST_TEMPLATE");
        
        System.out.println("✅ 完整模板渲染测试通过:");
        System.out.println("   标题: " + result.get("title"));
        System.out.println("   内容: " + result.get("content"));
    }

    @Test
    void testCreateTemplate() {
        // 准备测试数据
        NotificationTemplate newTemplate = new NotificationTemplate();
        newTemplate.setTemplateCode("NEW_TEMPLATE");
        newTemplate.setTemplateName("新模板");
        newTemplate.setTemplateType("email");
        newTemplate.setTitleTemplate("测试标题");
        newTemplate.setContentTemplate("测试内容");

        // Mock 方法调用
        when(templateMapper.checkTemplateCodeExists("NEW_TEMPLATE", null)).thenReturn(0L);
        when(templateMapper.insert(any(NotificationTemplate.class))).thenAnswer(invocation -> {
            NotificationTemplate template = invocation.getArgument(0);
            template.setId("new-id-001");
            return 1;
        });

        // 执行测试
        String templateId = templateService.createTemplate(newTemplate);

        // 验证结果
        assertNotNull(templateId);
        assertEquals("new-id-001", templateId);
        verify(templateMapper).checkTemplateCodeExists("NEW_TEMPLATE", null);
        verify(templateMapper).insert(any(NotificationTemplate.class));
        
        System.out.println("✅ 创建模板测试通过，新模板ID: " + templateId);
    }

    @Test
    void testExportTemplates() {
        // 准备测试数据
        List<String> templateIds = Arrays.asList("test-id-001");

        // Mock 方法调用
        when(templateMapper.selectById("test-id-001")).thenReturn(testTemplate);

        // 执行测试
        List<NotificationTemplate> result = templateService.exportTemplates(templateIds);

        // 验证结果
        assertNotNull(result);
        assertEquals(1, result.size());
        NotificationTemplate exportedTemplate = result.get(0);
        assertNull(exportedTemplate.getId()); // ID应该被清除
        assertNull(exportedTemplate.getCreateTime()); // 创建时间应该被清除
        assertNull(exportedTemplate.getUpdateTime()); // 更新时间应该被清除
        assertEquals("TEST_TEMPLATE", exportedTemplate.getTemplateCode());
        verify(templateMapper).selectById("test-id-001");
        
        System.out.println("✅ 导出模板测试通过，导出数量: " + result.size());
    }
}