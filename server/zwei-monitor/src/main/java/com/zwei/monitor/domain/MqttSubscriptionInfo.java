package com.zwei.monitor.domain;

import lombok.Data;

/**
 * MQTT 客户端订阅详情。
 */
@Data
public class MqttSubscriptionInfo {
    private String clientId;
    private String topicFilter;
    private int mqttQoS;
}
