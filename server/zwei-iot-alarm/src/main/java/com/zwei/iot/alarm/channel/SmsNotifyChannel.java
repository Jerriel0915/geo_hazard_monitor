package com.zwei.iot.alarm.channel;

import com.aliyun.dysmsapi20170525.models.SendSmsResponse;
import com.zwei.iot.alarm.channel.config.SmsConfig;
import com.zwei.iot.alarm.domain.AlarmNotification;
import com.zwei.iot.alarm.service.IAlarmNotificationService;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.util.Map;

/**
 * SMS 渠道（阿里云）
 *
 * @author zwei
 */
@Slf4j
@Component
public class SmsNotifyChannel implements INotifyChannel {

    @Autowired private AliyunSmsClient aliyunSmsClient;
    @Autowired private NotifyConfigLoader configLoader;
    @Autowired private NotifyTemplateService templateService;
    @Autowired private IAlarmNotificationService notificationService;

    @Override
    public String getChannel() {
        return "SMS";
    }

    @Override
    public void send(AlarmNotification n) {
        // 1) 接收人校验
        String recipientErr = NotifyRecipientValidator.validatePhone(n.getRecipientPhone());
        if (recipientErr != null) {
            notificationService.markFailed(n.getId(), recipientErr,
                String.format("用户 %s 手机号无效: %s",
                    n.getRecipientName(),
                    StringUtils.defaultString(n.getRecipientPhone(), "(空)")));
            return;
        }

        // 2) 渠道配置校验
        SmsConfig cfg = configLoader.loadSmsConfig();
        String cfgErr = cfg.validate();
        if (cfgErr != null) {
            notificationService.markFailed(n.getId(), "CHANNEL_NOT_CONFIGURED",
                cfgErr);
            return;
        }

        // 3) 模板参数
        Map<String, String> templateParams = templateService.buildSmsParams(n);
        String templateCode = cfg.selectTemplateCode(n.getSourceType());
        if (StringUtils.isBlank(templateCode)) {
            notificationService.markFailed(n.getId(), "CHANNEL_NOT_CONFIGURED",
                "sourceType=" + n.getSourceType() + " 的短信模板Code未配置");
            return;
        }

        // 4) 调用阿里云
        try {
            SendSmsResponse resp = aliyunSmsClient.send(
                n.getRecipientPhone(), templateCode, templateParams, cfg);

            if (resp == null || resp.getBody() == null) {
                notificationService.markFailed(n.getId(), "PROVIDER_ERROR",
                    "阿里云返回空响应");
                return;
            }

            String code = resp.getBody().getCode();
            if ("OK".equals(code)) {
                notificationService.markSent(n.getId());
            } else {
                // 业务错误（如 isv.MOBILE_NUMBER_ILLEGAL、isv.BUSINESS_LIMIT_CONTROL）
                notificationService.markFailed(n.getId(), "PROVIDER_ERROR",
                    "阿里云: " + code + " - " + resp.getBody().getMessage());
            }
        } catch (Exception e) {
            log.error("SMS 发送异常 notifId={} phone={}",
                n.getId(), n.getRecipientPhone(), e);
            notificationService.markFailed(n.getId(), "NETWORK_ERROR",
                e.getClass().getSimpleName() + ": " + e.getMessage());
        }
    }
}
