package com.zwei.iot.alarm.channel;

import com.zwei.iot.alarm.channel.config.MailConfig;
import jakarta.mail.MessagingException;
import jakarta.mail.internet.MimeMessage;
import lombok.extern.slf4j.Slf4j;
import org.springframework.mail.javamail.JavaMailSenderImpl;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.stereotype.Component;

import java.util.Objects;
import java.util.Properties;

/**
 * 动态 JavaMailSender（基于 sys_config 配置实时构建，不走 spring.mail.* 自动配置）
 *
 * <p>采用 double-checked locking 缓存 {@link JavaMailSenderImpl} 实例，
 * 仅在 host+username 变化时重建。
 *
 * @author zwei
 */
@Slf4j
@Component
public class DynamicMailSender {

    private volatile JavaMailSenderImpl sender;
    private volatile String cachedKey;   // host + username 标识

    /**
     * 发送 HTML 邮件
     *
     * @param to       收件人邮箱
     * @param subject  主题
     * @param htmlBody HTML 正文
     * @param cfg      SMTP 配置
     */
    public void send(String to, String subject, String htmlBody, MailConfig cfg)
            throws MessagingException {
        JavaMailSenderImpl s = getOrBuild(cfg);
        MimeMessage mime = s.createMimeMessage();
        MimeMessageHelper helper = new MimeMessageHelper(mime, true, "UTF-8");
        helper.setFrom(cfg.getFrom());
        helper.setTo(to);
        helper.setSubject(subject);
        helper.setText(htmlBody, true);   // HTML
        s.send(mime);
    }

    private JavaMailSenderImpl getOrBuild(MailConfig cfg) {
        if (!cfg.isConfigured()) {
            throw new IllegalStateException("SMTP 配置不完整");
        }
        String key = cfg.getHost() + ":" + cfg.getUsername();
        if (sender != null && Objects.equals(cachedKey, key)) {
            return sender;
        }
        synchronized (this) {
            if (sender == null || !Objects.equals(cachedKey, key)) {
                JavaMailSenderImpl s = new JavaMailSenderImpl();
                s.setHost(cfg.getHost());
                s.setPort(cfg.getPort());
                s.setUsername(cfg.getUsername());
                s.setPassword(cfg.getPassword());
                s.setDefaultEncoding("UTF-8");

                Properties props = s.getJavaMailProperties();
                props.put("mail.transport.protocol", "smtp");
                props.put("mail.smtp.auth", "true");
                props.put("mail.smtp.ssl.enable", String.valueOf(cfg.isSsl()));
                props.put("mail.smtp.starttls.enable", String.valueOf(!cfg.isSsl()));
                if (cfg.isSsl()) {
                    props.put("mail.smtp.socketFactory.class", "javax.net.ssl.SSLSocketFactory");
                    props.put("mail.smtp.socketFactory.fallback", "false");
                }
                props.put("mail.smtp.timeout", "5000");
                props.put("mail.smtp.connectiontimeout", "5000");
                props.put("mail.smtp.writetimeout", "5000");

                sender = s;
                cachedKey = key;
                log.info("JavaMailSender 已重建 host={} username={}",
                    cfg.getHost(), cfg.getUsername());
            }
        }
        return sender;
    }
}
