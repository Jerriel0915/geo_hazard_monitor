package com.zwei.iot.alarm.channel;

import com.aliyun.dysmsapi20170525.Client;
import com.aliyun.dysmsapi20170525.models.SendSmsRequest;
import com.aliyun.dysmsapi20170525.models.SendSmsResponse;
import com.aliyun.teaopenapi.models.Config;
import com.zwei.iot.alarm.channel.config.SmsConfig;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.Map;
import java.util.Objects;

/**
 * 阿里云短信客户端（懒加载单例，凭证变更时重建）
 *
 * @author zwei
 */
@Slf4j
@Component
public class AliyunSmsClient {

    private volatile Client client;
    private volatile String cachedKey;   // accessKeyId 标识，变更时重建 client

    /**
     * 发送短信
     */
    public SendSmsResponse send(String phone, String templateCode,
                                Map<String, String> templateParams,
                                SmsConfig cfg) throws Exception {
        Client c = getOrBuildClient(cfg);

        SendSmsRequest req = new SendSmsRequest()
            .setPhoneNumbers(phone)
            .setSignName(cfg.getSignName())
            .setTemplateCode(templateCode);

        if (templateParams != null && !templateParams.isEmpty()) {
            req.setTemplateParam(com.alibaba.fastjson2.JSON.toJSONString(templateParams));
        }

        return c.sendSms(req);
    }

    private Client getOrBuildClient(SmsConfig cfg) throws Exception {
        if (!cfg.isConfigured()) {
            throw new IllegalStateException("阿里云 SMS 配置不完整");
        }
        if (client != null && Objects.equals(cachedKey, cfg.getAccessKeyId())) {
            return client;
        }
        synchronized (this) {
            if (client == null || !Objects.equals(cachedKey, cfg.getAccessKeyId())) {
                Config config = new Config()
                    .setAccessKeyId(cfg.getAccessKeyId())
                    .setAccessKeySecret(cfg.getAccessKeySecret())
                    .setEndpoint("dysmsapi.aliyuncs.com");
                client = new Client(config);
                cachedKey = cfg.getAccessKeyId();
                log.info("阿里云 SMS 客户端已重建 accessKeyId={}",
                    maskKey(cfg.getAccessKeyId()));
            }
        }
        return client;
    }

    private String maskKey(String key) {
        if (key == null || key.length() < 6) return "***";
        return key.substring(0, 4) + "***" + key.substring(key.length() - 2);
    }
}
