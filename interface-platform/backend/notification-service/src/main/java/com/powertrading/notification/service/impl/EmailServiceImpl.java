package com.powertrading.notification.service.impl;

import com.powertrading.notification.service.EmailService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.FileSystemResource;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.stereotype.Service;
import org.thymeleaf.TemplateEngine;
import org.thymeleaf.context.Context;

import javax.mail.MessagingException;
import javax.mail.internet.MimeMessage;
import java.io.File;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;

/**
 * 邮件服务实现类
 *
 * @author PowerTrading
 * @since 2024-01-15
 */
@Slf4j
@Service
public class EmailServiceImpl implements EmailService {

    @Autowired
    private JavaMailSender mailSender;

    @Autowired
    private TemplateEngine templateEngine;

    @Value("${spring.mail.username}")
    private String from;

    @Value("${notification.email.from-name:电力交易中心接口平台}")
    private String fromName;

    @Override
    public boolean sendSimpleEmail(String to, String subject, String content) {
        try {
            SimpleMailMessage message = new SimpleMailMessage();
            message.setFrom(from);
            message.setTo(to);
            message.setSubject(subject);
            message.setText(content);
            
            mailSender.send(message);
            log.info("简单邮件发送成功，收件人：{}, 主题：{}", to, subject);
            return true;
        } catch (Exception e) {
            log.error("简单邮件发送失败，收件人：{}, 主题：{}, 错误：{}", to, subject, e.getMessage(), e);
            return false;
        }
    }

    @Override
    public boolean sendHtmlEmail(String to, String subject, String content) {
        try {
            MimeMessage message = mailSender.createMimeMessage();
            MimeMessageHelper helper = new MimeMessageHelper(message, true, "UTF-8");
            
            helper.setFrom(from, fromName);
            helper.setTo(to);
            helper.setSubject(subject);
            helper.setText(content, true);
            
            mailSender.send(message);
            log.info("HTML邮件发送成功，收件人：{}, 主题：{}", to, subject);
            return true;
        } catch (Exception e) {
            log.error("HTML邮件发送失败，收件人：{}, 主题：{}, 错误：{}", to, subject, e.getMessage(), e);
            return false;
        }
    }

    @Override
    public boolean sendEmailWithAttachments(String to, String subject, String content, List<File> attachments) {
        try {
            MimeMessage message = mailSender.createMimeMessage();
            MimeMessageHelper helper = new MimeMessageHelper(message, true, "UTF-8");
            
            helper.setFrom(from, fromName);
            helper.setTo(to);
            helper.setSubject(subject);
            helper.setText(content, true);
            
            // 添加附件
            if (attachments != null && !attachments.isEmpty()) {
                for (File attachment : attachments) {
                    if (attachment.exists() && attachment.isFile()) {
                        FileSystemResource file = new FileSystemResource(attachment);
                        helper.addAttachment(attachment.getName(), file);
                    }
                }
            }
            
            mailSender.send(message);
            log.info("带附件邮件发送成功，收件人：{}, 主题：{}, 附件数量：{}", to, subject, 
                    attachments != null ? attachments.size() : 0);
            return true;
        } catch (Exception e) {
            log.error("带附件邮件发送失败，收件人：{}, 主题：{}, 错误：{}", to, subject, e.getMessage(), e);
            return false;
        }
    }

    @Override
    public boolean sendTemplateEmail(String to, String subject, String templateName, Map<String, Object> variables) {
        try {
            // 使用Thymeleaf模板引擎处理模板
            Context context = new Context();
            if (variables != null) {
                context.setVariables(variables);
            }
            
            String content = templateEngine.process(templateName, context);
            return sendHtmlEmail(to, subject, content);
        } catch (Exception e) {
            log.error("模板邮件发送失败，收件人：{}, 主题：{}, 模板：{}, 错误：{}", 
                    to, subject, templateName, e.getMessage(), e);
            return false;
        }
    }

    @Override
    public boolean sendTemplateEmailWithAttachments(String to, String subject, String templateName,
                                                     Map<String, Object> variables, List<File> attachments) {
        try {
            // 使用Thymeleaf模板引擎处理模板
            Context context = new Context();
            if (variables != null) {
                context.setVariables(variables);
            }
            
            String content = templateEngine.process(templateName, context);
            return sendEmailWithAttachments(to, subject, content, attachments);
        } catch (Exception e) {
            log.error("带附件模板邮件发送失败，收件人：{}, 主题：{}, 模板：{}, 错误：{}", 
                    to, subject, templateName, e.getMessage(), e);
            return false;
        }
    }

    @Override
    public Map<String, Boolean> batchSendEmail(List<String> toList, String subject, String content) {
        Map<String, Boolean> results = new ConcurrentHashMap<>();
        
        if (toList == null || toList.isEmpty()) {
            return results;
        }
        
        // 使用CompletableFuture并行发送邮件
        List<CompletableFuture<Void>> futures = toList.stream()
                .map(to -> CompletableFuture.runAsync(() -> {
                    boolean success = sendHtmlEmail(to, subject, content);
                    results.put(to, success);
                }))
                .toList();
        
        // 等待所有邮件发送完成
        CompletableFuture.allOf(futures.toArray(new CompletableFuture[0])).join();
        
        log.info("批量邮件发送完成，总数：{}, 成功：{}, 失败：{}", 
                toList.size(), 
                results.values().stream().mapToInt(b -> b ? 1 : 0).sum(),
                results.values().stream().mapToInt(b -> b ? 0 : 1).sum());
        
        return results;
    }

    @Override
    public Map<String, Boolean> batchSendTemplateEmail(List<String> toList, String subject,
                                                        String templateName, Map<String, Object> variables) {
        Map<String, Boolean> results = new ConcurrentHashMap<>();
        
        if (toList == null || toList.isEmpty()) {
            return results;
        }
        
        // 使用CompletableFuture并行发送邮件
        List<CompletableFuture<Void>> futures = toList.stream()
                .map(to -> CompletableFuture.runAsync(() -> {
                    boolean success = sendTemplateEmail(to, subject, templateName, variables);
                    results.put(to, success);
                }))
                .toList();
        
        // 等待所有邮件发送完成
        CompletableFuture.allOf(futures.toArray(new CompletableFuture[0])).join();
        
        log.info("批量模板邮件发送完成，总数：{}, 成功：{}, 失败：{}", 
                toList.size(), 
                results.values().stream().mapToInt(b -> b ? 1 : 0).sum(),
                results.values().stream().mapToInt(b -> b ? 0 : 1).sum());
        
        return results;
    }
}