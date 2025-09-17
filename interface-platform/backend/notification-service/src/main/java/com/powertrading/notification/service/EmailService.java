package com.powertrading.notification.service;

import java.io.File;
import java.util.List;
import java.util.Map;

/**
 * 邮件服务接口
 *
 * @author PowerTrading
 * @since 2024-01-15
 */
public interface EmailService {

    /**
     * 发送简单文本邮件
     *
     * @param to      收件人
     * @param subject 主题
     * @param content 内容
     * @return 是否发送成功
     */
    boolean sendSimpleEmail(String to, String subject, String content);

    /**
     * 发送HTML邮件
     *
     * @param to      收件人
     * @param subject 主题
     * @param content HTML内容
     * @return 是否发送成功
     */
    boolean sendHtmlEmail(String to, String subject, String content);

    /**
     * 发送带附件的邮件
     *
     * @param to          收件人
     * @param subject     主题
     * @param content     内容
     * @param attachments 附件列表
     * @return 是否发送成功
     */
    boolean sendEmailWithAttachments(String to, String subject, String content, List<File> attachments);

    /**
     * 使用模板发送邮件
     *
     * @param to           收件人
     * @param subject      主题
     * @param templateName 模板名称
     * @param variables    模板变量
     * @return 是否发送成功
     */
    boolean sendTemplateEmail(String to, String subject, String templateName, Map<String, Object> variables);

    /**
     * 使用模板发送带附件的邮件
     *
     * @param to           收件人
     * @param subject      主题
     * @param templateName 模板名称
     * @param variables    模板变量
     * @param attachments  附件列表
     * @return 是否发送成功
     */
    boolean sendTemplateEmailWithAttachments(String to, String subject, String templateName,
                                              Map<String, Object> variables, List<File> attachments);

    /**
     * 批量发送邮件
     *
     * @param toList  收件人列表
     * @param subject 主题
     * @param content 内容
     * @return 发送结果
     */
    Map<String, Boolean> batchSendEmail(List<String> toList, String subject, String content);

    /**
     * 批量使用模板发送邮件
     *
     * @param toList       收件人列表
     * @param subject      主题
     * @param templateName 模板名称
     * @param variables    模板变量
     * @return 发送结果
     */
    Map<String, Boolean> batchSendTemplateEmail(List<String> toList, String subject,
                                                 String templateName, Map<String, Object> variables);
}