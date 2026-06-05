package com.zwei.common.event;

/**
 * 设备上线事件。
 * <p>
 * 在 MQTT 设备鉴权成功或连接建立后发布，
 * 由 DeviceOnlineEventListener 消费并写入运维指标表。
 */
public class DeviceOnlineEvent {

    private final Long deviceId;
    private final String clientId;
    private final String clientIp;

    public DeviceOnlineEvent(Long deviceId, String clientId, String clientIp) {
        this.deviceId = deviceId;
        this.clientId = clientId;
        this.clientIp = clientIp;
    }

    public Long getDeviceId() { return deviceId; }
    public String getClientId() { return clientId; }
    public String getClientIp() { return clientIp; }
}
