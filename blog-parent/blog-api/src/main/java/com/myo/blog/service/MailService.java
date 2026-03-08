package com.myo.blog.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

/**
 * 邮件服务组件
 * 专门用于处理耗时的邮件发送任务
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class MailService {


    private final JavaMailSender mailSender;

    // 从 application.yml 读取发件人配置
    @Value("${spring.mail.username}")
    private String fromEmail;

    /**
     * 异步发送邮件方法
     * @Async 注解：告诉 Spring 开启一个新线程来执行这个方法，不要阻塞主线程
     * * @param to      收件人邮箱
     * @param subject 邮件主题
     * @param content 邮件正文
     */
    @Async
    public void sendMailAsync(String to, String subject, String content) {
        try {
            // 1. 创建简单邮件消息对象
            SimpleMailMessage message = new SimpleMailMessage();

            // 2. 设置邮件基本信息
            message.setFrom(fromEmail); // 发件人
            message.setTo(to);          // 收件人
            message.setSubject(subject);// 主题
            message.setText(content);   // 正文内容

            // 3. 发送邮件 (这一步最耗时，但现在它在后台运行了)
            mailSender.send(message);
            log.info(">>> 邮件已异步发送给：{}", to);

        } catch (Exception e) {
            // 异步方法中的异常通常只能打印日志，无法抛给 Controller
            log.error(">>> 邮件发送失败：{}", e.getMessage());
        }
    }
}