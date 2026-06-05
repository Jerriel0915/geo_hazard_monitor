package com.zwei.common.event;

/**
 * 设备离线事件。
 * <p>
 * 在 MQTT 设备连接断开后发布，
 * 由 DeviceOnlineEventListener 消费并写入运维指标表。
 */
public class DeviceOfflineEvent {

    private final Long deviceId;
    private final String clientId;
    private final String clientIp;
    private final String reason;

    public DeviceOfflineEvent(Long deviceId, String clientId, String clientIp, String reason) {
        this.deviceId = deviceId;
        this.clientId = clientId;
        this.clientIp = clientIp;
        this.reason = reason;
    }

    public Long getDeviceId() { return deviceId; }
    public String getClientId() { return clientId; }
    public String getClientIp() { return clientIp; }
    public String getReason() { return reason; }
}
