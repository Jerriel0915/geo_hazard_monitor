package com.zwei.log.domain.model;

import lombok.Data;

/**
 * MQTT 数据日志条目。
 * <p>
 * 记录每条经平台转发的设备监测消息的关键元数据，用于实时故障排查和流量观测。
 */
@Data
public class LogMqttMessage {
    /**
     * 接收时间戳（毫秒）
     */
    private long receiveTime;
    /**
     * MQTT Client ID
     */
    private String clientId;
    /**
     * 设备认证用户名（deviceCode 对应的 authUsername）
     */
    private String username;
    /**
     * 发布主题
     */
    private String topic;
    /**
     * 消息负载内容（截断后，最多 500 字符）
     */
    private String payload;
    /**
     * 原始报文大小（Byte）
     */
    private int payloadSize;
}
