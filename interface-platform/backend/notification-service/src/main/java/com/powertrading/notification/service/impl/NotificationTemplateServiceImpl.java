package com.powertrading.notification.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.powertrading.notification.entity.NotificationTemplate;
import com.powertrading.notification.mapper.NotificationTemplateMapper;
import com.powertrading.notification.service.NotificationTemplateService;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.time.LocalDateTime;
import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * 通知模板服务实现类
 *
 * @author PowerTrading
 * @since 2024-01-15
 */
@Service
@RequiredArgsConstructor
public class NotificationTemplateServiceImpl implements NotificationTemplateService {

    private static final Logger log = LoggerFactory.getLogger(NotificationTemplateServiceImpl.class);
    
    private final NotificationTemplateMapper templateMapper;

    /**
     * 模板变量匹配正则表达式
     */
    private static final Pattern VARIABLE_PATTERN = Pattern.compile("\\$\\{([^}]+)\\}");

    @Override
    @Transactional(rollbackFor = Exception.class)
    public String createTemplate(NotificationTemplate template) {
        log.info("创建通知模板: {}", template.getTemplateName());
        
        // 验证模板编码唯一性
        if (!isTemplateCodeUnique(template.getTemplateCode(), null)) {
            throw new RuntimeException("模板编码已存在: " + template.getTemplateCode());
        }
        
        // 设置默认值
        template.setId(null); // 让数据库自动生成ID
        template.setStatus(NotificationTemplate.Status.ENABLED.getCode());
        template.setCreateTime(LocalDateTime.now());
        template.setUpdateTime(LocalDateTime.now());
        
        // 保存模板
        int result = templateMapper.insert(template);
        if (result > 0) {
            log.info("模板创建成功，ID: {}", template.getId());
            return template.getId();
        } else {
            throw new RuntimeException("模板创建失败");
        }
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public boolean updateTemplate(NotificationTemplate template) {
        log.info("更新通知模板: {}", template.getId());
        
        // 检查模板是否存在
        NotificationTemplate existingTemplate = templateMapper.selectById(template.getId());
        if (existingTemplate == null) {
            throw new RuntimeException("模板不存在: " + template.getId());
        }
        
        // 验证模板编码唯一性（排除当前模板）
        if (!isTemplateCodeUnique(template.getTemplateCode(), template.getId())) {
            throw new RuntimeException("模板编码已存在: " + template.getTemplateCode());
        }
        
        // 更新时间
        template.setUpdateTime(LocalDateTime.now());
        
        // 更新模板
        int result = templateMapper.updateById(template);
        if (result > 0) {
            log.info("模板更新成功: {}", template.getId());
            return true;
        } else {
            log.error("模板更新失败: {}", template.getId());
            return false;
        }
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public boolean deleteTemplate(String templateId) {
        log.info("删除通知模板: {}", templateId);
        
        // 检查模板是否存在
        NotificationTemplate template = templateMapper.selectById(templateId);
        if (template == null) {
            throw new RuntimeException("模板不存在: " + templateId);
        }
        
        // 删除模板
        int result = templateMapper.deleteById(templateId);
        if (result > 0) {
            log.info("模板删除成功: {}", templateId);
            return true;
        } else {
            log.error("模板删除失败: {}", templateId);
            return false;
        }
    }

    @Override
    public NotificationTemplate getTemplateById(String templateId) {
        log.debug("根据ID获取模板: {}", templateId);
        
        if (!StringUtils.hasText(templateId)) {
            throw new IllegalArgumentException("模板ID不能为空");
        }
        
        return templateMapper.selectById(templateId);
    }

    @Override
    public NotificationTemplate getTemplateByCode(String templateCode) {
        log.debug("根据编码获取模板: {}", templateCode);
        
        if (!StringUtils.hasText(templateCode)) {
            throw new IllegalArgumentException("模板编码不能为空");
        }
        
        return templateMapper.selectByTemplateCode(templateCode);
    }

    @Override
    public IPage<NotificationTemplate> getTemplates(String templateName, String templateType, Integer status, int pageNum, int pageSize) {
        log.debug("分页查询模板: name={}, type={}, status={}, page={}, size={}", 
                templateName, templateType, status, pageNum, pageSize);
        
        Page<NotificationTemplate> page = new Page<>(pageNum, pageSize);
        return templateMapper.selectTemplates(page, templateName, templateType, status);
    }

    @Override
    public List<NotificationTemplate> getEnabledTemplatesByType(String templateType) {
        log.debug("根据类型获取启用模板: {}", templateType);
        
        if (!StringUtils.hasText(templateType)) {
            throw new IllegalArgumentException("模板类型不能为空");
        }
        
        return templateMapper.selectEnabledByType(templateType);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public boolean updateTemplateStatus(String templateId, Integer status) {
        log.info("更新模板状态: templateId={}, status={}", templateId, status);
        
        if (!StringUtils.hasText(templateId)) {
            throw new IllegalArgumentException("模板ID不能为空");
        }
        
        if (status == null || (status != 0 && status != 1)) {
            throw new IllegalArgumentException("状态值无效，必须为0或1");
        }
        
        // 检查模板是否存在
        NotificationTemplate template = templateMapper.selectById(templateId);
        if (template == null) {
            throw new RuntimeException("模板不存在: " + templateId);
        }
        
        // 更新状态
        template.setStatus(status);
        template.setUpdateTime(LocalDateTime.now());
        
        int result = templateMapper.updateById(template);
        if (result > 0) {
            log.info("模板状态更新成功: {}", templateId);
            return true;
        } else {
            log.error("模板状态更新失败: {}", templateId);
            return false;
        }
    }

    @Override
    public Map<String, String> renderTemplate(String templateCode, Map<String, Object> variables) {
        log.debug("渲染模板: templateCode={}", templateCode);
        
        if (!StringUtils.hasText(templateCode)) {
            throw new IllegalArgumentException("模板编码不能为空");
        }
        
        // 获取模板
        NotificationTemplate template = templateMapper.selectByTemplateCode(templateCode);
        if (template == null) {
            throw new RuntimeException("模板不存在: " + templateCode);
        }
        
        if (!NotificationTemplate.Status.ENABLED.getCode().equals(template.getStatus())) {
            throw new RuntimeException("模板未启用: " + templateCode);
        }
        
        // 渲染标题和内容
        String renderedTitle = renderTitle(template.getTitleTemplate(), variables);
        String renderedContent = renderContent(template.getContentTemplate(), variables);
        
        Map<String, String> result = new HashMap<>();
        result.put("title", renderedTitle);
        result.put("content", renderedContent);
        result.put("templateType", template.getTemplateType());
        
        log.debug("模板渲染完成: templateCode={}", templateCode);
        return result;
    }

    @Override
    public String renderTitle(String titleTemplate, Map<String, Object> variables) {
        log.debug("渲染标题模板: {}", titleTemplate);
        
        if (!StringUtils.hasText(titleTemplate)) {
            return "";
        }
        
        return replaceVariables(titleTemplate, variables);
    }

    @Override
    public String renderContent(String contentTemplate, Map<String, Object> variables) {
        log.debug("渲染内容模板: {}", contentTemplate);
        
        if (!StringUtils.hasText(contentTemplate)) {
            return "";
        }
        
        return replaceVariables(contentTemplate, variables);
    }

    @Override
    public Map<String, Object> validateTemplate(String template, Map<String, Object> variables) {
        log.debug("验证模板语法: {}", template);
        
        Map<String, Object> result = new HashMap<>();
        List<String> errors = new ArrayList<>();
        List<String> warnings = new ArrayList<>();
        
        try {
            if (!StringUtils.hasText(template)) {
                errors.add("模板内容不能为空");
            } else {
                // 提取模板中的变量
                List<String> templateVariables = extractVariables(template);
                
                // 检查变量格式
                for (String variable : templateVariables) {
                    if (!isValidVariableName(variable)) {
                        errors.add("无效的变量名: " + variable);
                    }
                }
                
                // 检查是否有未提供的变量
                if (variables != null) {
                    for (String variable : templateVariables) {
                        if (!variables.containsKey(variable)) {
                            warnings.add("缺少变量值: " + variable);
                        }
                    }
                }
                
                // 尝试渲染模板
                try {
                    String rendered = replaceVariables(template, variables != null ? variables : new HashMap<>());
                    result.put("renderedContent", rendered);
                } catch (Exception e) {
                    errors.add("模板渲染失败: " + e.getMessage());
                }
                
                result.put("variables", templateVariables);
            }
            
            result.put("valid", errors.isEmpty());
            result.put("errors", errors);
            result.put("warnings", warnings);
            
        } catch (Exception e) {
            log.error("模板验证异常", e);
            errors.add("模板验证异常: " + e.getMessage());
            result.put("valid", false);
            result.put("errors", errors);
            result.put("warnings", warnings);
        }
        
        return result;
    }

    @Override
    public List<String> extractVariables(String template) {
        log.debug("提取模板变量: {}", template);
        
        if (!StringUtils.hasText(template)) {
            return new ArrayList<>();
        }
        
        Set<String> variables = new HashSet<>();
        Matcher matcher = VARIABLE_PATTERN.matcher(template);
        
        while (matcher.find()) {
            String variableName = matcher.group(1).trim();
            if (StringUtils.hasText(variableName)) {
                variables.add(variableName);
            }
        }
        
        return new ArrayList<>(variables);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public String copyTemplate(String sourceTemplateId, String newTemplateCode, String newTemplateName) {
        log.info("复制模板: sourceId={}, newCode={}, newName={}", sourceTemplateId, newTemplateCode, newTemplateName);
        
        if (!StringUtils.hasText(sourceTemplateId)) {
            throw new IllegalArgumentException("源模板ID不能为空");
        }
        
        if (!StringUtils.hasText(newTemplateCode)) {
            throw new IllegalArgumentException("新模板编码不能为空");
        }
        
        if (!StringUtils.hasText(newTemplateName)) {
            throw new IllegalArgumentException("新模板名称不能为空");
        }
        
        // 获取源模板
        NotificationTemplate sourceTemplate = templateMapper.selectById(sourceTemplateId);
        if (sourceTemplate == null) {
            throw new RuntimeException("源模板不存在: " + sourceTemplateId);
        }
        
        // 验证新模板编码唯一性
        if (!isTemplateCodeUnique(newTemplateCode, null)) {
            throw new RuntimeException("模板编码已存在: " + newTemplateCode);
        }
        
        // 创建新模板
        NotificationTemplate newTemplate = new NotificationTemplate();
        newTemplate.setTemplateCode(newTemplateCode);
        newTemplate.setTemplateName(newTemplateName);
        newTemplate.setTemplateType(sourceTemplate.getTemplateType());
        newTemplate.setTitleTemplate(sourceTemplate.getTitleTemplate());
        newTemplate.setContentTemplate(sourceTemplate.getContentTemplate());
        newTemplate.setVariables(sourceTemplate.getVariables());
        newTemplate.setStatus(NotificationTemplate.Status.ENABLED.getCode());
        newTemplate.setCreateTime(LocalDateTime.now());
        newTemplate.setUpdateTime(LocalDateTime.now());
        
        // 保存新模板
        int result = templateMapper.insert(newTemplate);
        if (result > 0) {
            log.info("模板复制成功，新模板ID: {}", newTemplate.getId());
            return newTemplate.getId();
        } else {
            throw new RuntimeException("模板复制失败");
        }
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public Map<String, Object> batchImportTemplates(List<NotificationTemplate> templates) {
        log.info("批量导入模板，数量: {}", templates != null ? templates.size() : 0);
        
        Map<String, Object> result = new HashMap<>();
        List<String> successList = new ArrayList<>();
        List<String> failureList = new ArrayList<>();
        List<String> errorMessages = new ArrayList<>();
        
        if (templates == null || templates.isEmpty()) {
            result.put("success", false);
            result.put("message", "导入模板列表为空");
            return result;
        }
        
        for (NotificationTemplate template : templates) {
            try {
                // 验证必填字段
                if (!StringUtils.hasText(template.getTemplateCode())) {
                    failureList.add("模板编码为空");
                    continue;
                }
                
                if (!StringUtils.hasText(template.getTemplateName())) {
                    failureList.add(template.getTemplateCode() + ": 模板名称为空");
                    continue;
                }
                
                // 检查模板编码是否已存在
                if (!isTemplateCodeUnique(template.getTemplateCode(), null)) {
                    failureList.add(template.getTemplateCode() + ": 模板编码已存在");
                    continue;
                }
                
                // 设置默认值
                template.setId(null);
                template.setStatus(template.getStatus() != null ? template.getStatus() : NotificationTemplate.Status.ENABLED.getCode());
                template.setCreateTime(LocalDateTime.now());
                template.setUpdateTime(LocalDateTime.now());
                
                // 保存模板
                int insertResult = templateMapper.insert(template);
                if (insertResult > 0) {
                    successList.add(template.getTemplateCode());
                    log.debug("模板导入成功: {}", template.getTemplateCode());
                } else {
                    failureList.add(template.getTemplateCode() + ": 保存失败");
                }
                
            } catch (Exception e) {
                log.error("导入模板失败: {}", template.getTemplateCode(), e);
                failureList.add(template.getTemplateCode() + ": " + e.getMessage());
                errorMessages.add(e.getMessage());
            }
        }
        
        result.put("success", !successList.isEmpty());
        result.put("totalCount", templates.size());
        result.put("successCount", successList.size());
        result.put("failureCount", failureList.size());
        result.put("successList", successList);
        result.put("failureList", failureList);
        result.put("errorMessages", errorMessages);
        
        log.info("批量导入完成，成功: {}, 失败: {}", successList.size(), failureList.size());
        return result;
    }

    @Override
    public List<NotificationTemplate> exportTemplates(List<String> templateIds) {
        log.info("导出模板，ID列表: {}", templateIds);
        
        if (templateIds == null || templateIds.isEmpty()) {
            throw new IllegalArgumentException("模板ID列表不能为空");
        }
        
        List<NotificationTemplate> templates = new ArrayList<>();
        
        for (String templateId : templateIds) {
            try {
                NotificationTemplate template = templateMapper.selectById(templateId);
                if (template != null) {
                    // 清除不需要导出的字段
                    template.setId(null);
                    template.setCreateTime(null);
                    template.setUpdateTime(null);
                    
                    templates.add(template);
                    log.debug("模板导出成功: {}", template.getTemplateCode());
                } else {
                    log.warn("模板不存在，跳过导出: {}", templateId);
                }
            } catch (Exception e) {
                log.error("导出模板失败: {}", templateId, e);
            }
        }
        
        log.info("模板导出完成，成功导出: {} 个", templates.size());
        return templates;
    }

    /**
     * 替换模板变量
     *
     * @param template  模板内容
     * @param variables 变量参数
     * @return 替换后的内容
     */
    private String replaceVariables(String template, Map<String, Object> variables) {
        if (!StringUtils.hasText(template) || variables == null || variables.isEmpty()) {
            return template;
        }

        String result = template;
        Matcher matcher = VARIABLE_PATTERN.matcher(template);
        while (matcher.find()) {
            String variableName = matcher.group(1);
            Object value = variables.get(variableName);
            if (value != null) {
                result = result.replace("${" + variableName + "}", value.toString());
            }
        }
        return result;
    }

    /**
     * 验证模板编码唯一性
     *
     * @param templateCode 模板编码
     * @param excludeId    排除的ID
     * @return 是否唯一
     */
    private boolean isTemplateCodeUnique(String templateCode, String excludeId) {
        Long count = templateMapper.checkTemplateCodeExists(templateCode, excludeId);
        return count == null || count == 0;
    }

    /**
     * 验证变量名是否有效
     *
     * @param variableName 变量名
     * @return 是否有效
     */
    private boolean isValidVariableName(String variableName) {
        if (!StringUtils.hasText(variableName)) {
            return false;
        }
        
        // 变量名只能包含字母、数字、下划线，且不能以数字开头
        return variableName.matches("^[a-zA-Z_][a-zA-Z0-9_]*$");
    }
}