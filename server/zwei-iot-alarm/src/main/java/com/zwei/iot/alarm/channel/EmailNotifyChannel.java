package com.zwei.iot.alarm.channel;

import com.zwei.common.core.domain.entity.SysUser;
import com.zwei.iot.alarm.channel.config.MailConfig;
import com.zwei.iot.alarm.domain.AlarmNotification;
import com.zwei.iot.alarm.service.IAlarmNotificationService;
import com.zwei.system.service.ISysUserService;
import jakarta.mail.MessagingException;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.mail.MailAuthenticationException;
import org.springframework.mail.MailSendException;
import org.springframework.stereotype.Component;

/**
 * EMAIL 渠道（SMTP + Thymeleaf）
 *
 * <p>接收人邮箱通过 {@link ISysUserService#selectUserById(Long)} 动态查询，
 * 因为 {@code AlarmNotification} 表无 recipient_email 列。
 *
 * @author zwei
 */
@Slf4j
@Component
public class EmailNotifyChannel implements INotifyChannel {

    @Autowired private DynamicMailSender dynamicMailSender;
    @Autowired private NotifyConfigLoader configLoader;
    @Autowired private NotifyTemplateService templateService;
    @Autowired private IAlarmNotificationService notificationService;
    @Autowired private ISysUserService sysUserService;

    @Override
    public String getChannel() {
        return "EMAIL";
    }

    @Override
    public void send(AlarmNotification n) {
        // 0) 动态查接收人邮箱（AlarmNotification 无 recipientEmail 字段）
        SysUser user = sysUserService.selectUserById(n.getRecipientId());
        String email = user != null ? user.getEmail() : null;

        // 1) 接收人校验
        String err = NotifyRecipientValidator.validateEmail(email);
        if (err != null) {
            notificationService.markFailed(n.getId(), err,
                String.format("用户 %s 邮箱无效: %s",
                    n.getRecipientName(),
                    StringUtils.defaultString(email, "(空)")));
            return;
        }

        // 2) SMTP 配置校验
        MailConfig cfg = configLoader.loadMailConfig();
        String cfgErr = cfg.validate();
        if (cfgErr != null) {
            notificationService.markFailed(n.getId(), "CHANNEL_NOT_CONFIGURED", cfgErr);
            return;
        }

        // 3) 渲染并发送
        try {
            String subject = templateService.renderEmailSubject(n);
            String html = templateService.renderEmailHtml(n);

            dynamicMailSender.send(email, subject, html, cfg);
            notificationService.markSent(n.getId());

        } catch (MailAuthenticationException e) {
            notificationService.markFailed(n.getId(), "CHANNEL_NOT_CONFIGURED",
                "SMTP 认证失败: " + e.getMessage());
        } catch (MailSendException e) {
            notificationService.markFailed(n.getId(), "PROVIDER_ERROR",
                "邮件发送被拒: " + e.getMessage());
        } catch (MessagingException e) {
            notificationService.markFailed(n.getId(), "NETWORK_ERROR",
                "MessagingException: " + e.getMessage());
        } catch (Exception e) {
            log.error("EMAIL 发送异常 notifId={} recipientId={}",
                n.getId(), n.getRecipientId(), e);
            notificationService.markFailed(n.getId(), "UNKNOWN",
                e.getClass().getSimpleName() + ": " + e.getMessage());
        }
    }
}
