package com.zwei.iot.broker.config;

import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

/**
 * MQTT 鉴权中心配置。
 * <p>
 * 用于集中管理 MQTT 设备认证策略，避免在鉴权服务中硬编码失败阈值、
 * 封禁时长和重复登录处理策略。
 */
@Setter
@Getter
@Component
@ConfigurationProperties(prefix = "mqtt.auth-center")
public class MqttAuthCenterProperties {
    /**
     * 是否校验设备接入协议必须为 MQTT。
     */
    private boolean enforceMqttProtocol = false;

    /**
     * 连续失败阈值，达到后临时封禁。
     */
    private int failureThreshold = 5;

    /**
     * 临时封禁时长，单位秒。
     */
    private long banDurationSeconds = 600;

    /**
     * 是否踢掉旧连接。
     */
    private boolean disconnectPreviousClient = true;

}
