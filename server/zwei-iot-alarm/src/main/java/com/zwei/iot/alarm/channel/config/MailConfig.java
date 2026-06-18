package com.zwei.iot.alarm.channel.config;

import lombok.Builder;
import lombok.Data;
import org.apache.commons.lang3.StringUtils;

@Data
@Builder
public class MailConfig {
    private String host;
    private Integer port;
    private String username;
    private String password;
    private String from;
    private Boolean ssl;

    public boolean isConfigured() {
        return StringUtils.isNotBlank(host)
            && port != null
            && StringUtils.isNotBlank(username)
            && StringUtils.isNotBlank(password)
            && StringUtils.isNotBlank(from);
    }

    public String validate() {
        if (StringUtils.isBlank(host))     return "SMTP 主机未配置";
        if (port == null)                  return "SMTP 端口未配置";
        if (StringUtils.isBlank(username)) return "SMTP 用户名未配置";
        if (StringUtils.isBlank(password)) return "SMTP 授权码未配置";
        if (StringUtils.isBlank(from))     return "发件人邮箱未配置";
        return null;
    }

    public boolean isSsl() {
        return Boolean.TRUE.equals(ssl);
    }
}
