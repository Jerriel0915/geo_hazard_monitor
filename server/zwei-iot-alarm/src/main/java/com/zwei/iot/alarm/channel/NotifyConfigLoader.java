package com.zwei.iot.alarm.channel;

import com.zwei.iot.alarm.channel.config.MailConfig;
import com.zwei.iot.alarm.channel.config.SmsConfig;
import com.zwei.system.service.ISysConfigService;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Component;

/**
 * 从 sys_config 加载 SMS/EMAIL 配置（带缓存）
 *
 * sys_config key 列表：
 *   notify.sms.access-key-id
 *   notify.sms.access-key-secret
 *   notify.sms.sign-name
 *   notify.sms.template.alarm
 *   notify.sms.template.offline
 *   notify.mail.host
 *   notify.mail.port
 *   notify.mail.username
 *   notify.mail.password
 *   notify.mail.from
 *   notify.mail.ssl
 */
@Component
public class NotifyConfigLoader {

    @Autowired
    private ISysConfigService sysConfigService;

    @Cacheable(value = "notify:config", key = "'sms'")
    public SmsConfig loadSmsConfig() {
        return SmsConfig.builder()
            .accessKeyId(get("notify.sms.access-key-id"))
            .accessKeySecret(get("notify.sms.access-key-secret"))
            .signName(get("notify.sms.sign-name"))
            .alarmTemplateCode(get("notify.sms.template.alarm"))
            .offlineTemplateCode(get("notify.sms.template.offline"))
            .build();
    }

    @Cacheable(value = "notify:config", key = "'mail'")
    public MailConfig loadMailConfig() {
        String portStr = get("notify.mail.port");
        String sslStr = get("notify.mail.ssl");
        return MailConfig.builder()
            .host(get("notify.mail.host"))
            .port(StringUtils.isNotBlank(portStr) ? Integer.valueOf(portStr) : null)
            .username(get("notify.mail.username"))
            .password(get("notify.mail.password"))
            .from(get("notify.mail.from"))
            .ssl(StringUtils.isNotBlank(sslStr) ? Boolean.valueOf(sslStr) : true)
            .build();
    }

    /** 清除全部 notify 配置缓存（配置变更时调用） */
    @CacheEvict(value = "notify:config", allEntries = true)
    public void evictAll() {}

    private String get(String key) {
        return sysConfigService.selectConfigByKey(key);
    }
}
