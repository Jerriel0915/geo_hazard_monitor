package com.zwei.iot.alarm.channel.config;

import lombok.Builder;
import lombok.Data;
import org.apache.commons.lang3.StringUtils;

@Data
@Builder
public class SmsConfig {
    private String accessKeyId;
    private String accessKeySecret;
    private String signName;
    private String alarmTemplateCode;      // 告警短信模板
    private String offlineTemplateCode;    // 离线短信模板

    /** 关键字段是否齐全 */
    public boolean isConfigured() {
        return StringUtils.isNotBlank(accessKeyId)
            && StringUtils.isNotBlank(accessKeySecret)
            && StringUtils.isNotBlank(signName);
    }

    /** 详细校验，返回缺失字段说明或 null */
    public String validate() {
        if (StringUtils.isBlank(accessKeyId))     return "阿里云 SMS accessKeyId 未配置";
        if (StringUtils.isBlank(accessKeySecret)) return "阿里云 SMS accessKeySecret 未配置";
        if (StringUtils.isBlank(signName))        return "阿里云 SMS 签名未配置";
        return null;
    }

    /** 按 sourceType 选模板 Code */
    public String selectTemplateCode(String sourceType) {
        if ("offline".equalsIgnoreCase(sourceType)) {
            return offlineTemplateCode;
        }
        return alarmTemplateCode;
    }
}
