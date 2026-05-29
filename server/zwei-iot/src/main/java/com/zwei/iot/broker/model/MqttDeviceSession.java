package com.zwei.iot.broker.model;

import java.time.LocalDateTime;

/**
 * MQTT 设备认证会话快照。
 *
 * @param deviceId 设备主键
 * @param authUsername 当前用于鉴权的设备账号
 * @param clientId Broker 当前活跃连接的 clientId
 * @param clientIp 客户端来源 IP
 * @param authenticatedAt 本次鉴权成功时间
 */
public record MqttDeviceSession(
        Long deviceId,
        String deviceCode,
        String authUsername,
        String clientId,
        String clientIp,
        LocalDateTime authenticatedAt
) {
}
