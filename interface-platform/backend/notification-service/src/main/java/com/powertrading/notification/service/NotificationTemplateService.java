package com.powertrading.notification.service;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.powertrading.notification.entity.NotificationTemplate;

import java.util.List;
import java.util.Map;

/**
 * 通知模板服务接口
 *
 * @author PowerTrading
 * @since 2024-01-15
 */
public interface NotificationTemplateService {

    /**
     * 创建通知模板
     *
     * @param template 模板信息
     * @return 模板ID
     */
    String createTemplate(NotificationTemplate template);

    /**
     * 更新通知模板
     *
     * @param template 模板信息
     * @return 是否成功
     */
    boolean updateTemplate(NotificationTemplate template);

    /**
     * 删除通知模板
     *
     * @param templateId 模板ID
     * @return 是否成功
     */
    boolean deleteTemplate(String templateId);

    /**
     * 根据ID获取模板
     *
     * @param templateId 模板ID
     * @return 模板信息
     */
    NotificationTemplate getTemplateById(String templateId);

    /**
     * 根据模板编码获取模板
     *
     * @param templateCode 模板编码
     * @return 模板信息
     */
    NotificationTemplate getTemplateByCode(String templateCode);

    /**
     * 分页查询模板
     *
     * @param templateName 模板名称
     * @param templateType 模板类型
     * @param status       状态
     * @param pageNum      页码
     * @param pageSize     页大小
     * @return 模板分页数据
     */
    IPage<NotificationTemplate> getTemplates(String templateName, String templateType, Integer status,
                                              int pageNum, int pageSize);

    /**
     * 根据类型获取启用的模板
     *
     * @param templateType 模板类型
     * @return 模板列表
     */
    List<NotificationTemplate> getEnabledTemplatesByType(String templateType);

    /**
     * 启用/禁用模板
     *
     * @param templateId 模板ID
     * @param status     状态
     * @return 是否成功
     */
    boolean updateTemplateStatus(String templateId, Integer status);

    /**
     * 渲染模板内容
     *
     * @param templateCode 模板编码
     * @param variables    变量参数
     * @return 渲染结果
     */
    Map<String, String> renderTemplate(String templateCode, Map<String, Object> variables);

    /**
     * 渲染模板标题
     *
     * @param titleTemplate 标题模板
     * @param variables     变量参数
     * @return 渲染后的标题
     */
    String renderTitle(String titleTemplate, Map<String, Object> variables);

    /**
     * 渲染模板内容
     *
     * @param contentTemplate 内容模板
     * @param variables       变量参数
     * @return 渲染后的内容
     */
    String renderContent(String contentTemplate, Map<String, Object> variables);

    /**
     * 验证模板语法
     *
     * @param template  模板内容
     * @param variables 测试变量
     * @return 验证结果
     */
    Map<String, Object> validateTemplate(String template, Map<String, Object> variables);

    /**
     * 获取模板变量列表
     *
     * @param template 模板内容
     * @return 变量列表
     */
    List<String> extractVariables(String template);

    /**
     * 复制模板
     *
     * @param sourceTemplateId 源模板ID
     * @param newTemplateCode  新模板编码
     * @param newTemplateName  新模板名称
     * @return 新模板ID
     */
    String copyTemplate(String sourceTemplateId, String newTemplateCode, String newTemplateName);

    /**
     * 批量导入模板
     *
     * @param templates 模板列表
     * @return 导入结果
     */
    Map<String, Object> batchImportTemplates(List<NotificationTemplate> templates);

    /**
     * 导出模板
     *
     * @param templateIds 模板ID列表
     * @return 模板数据
     */
    List<NotificationTemplate> exportTemplates(List<String> templateIds);
}