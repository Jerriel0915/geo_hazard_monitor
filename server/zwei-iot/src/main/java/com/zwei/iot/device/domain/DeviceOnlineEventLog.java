package com.zwei.iot.device.domain;

/**
 * 设备上下线事件日志（历史明细）。
 * <p>
 * 每次设备上下线事件追加一条记录，支持掉线历史追溯。
 */
public class DeviceOnlineEventLog {

    private Long id;
    private Long deviceId;
    private String eventType;
    private String clientId;
    private String clientIp;
    private String eventTime;
    private String reason;

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public Long getDeviceId() { return deviceId; }
    public void setDeviceId(Long deviceId) { this.deviceId = deviceId; }

    public String getEventType() { return eventType; }
    public void setEventType(String eventType) { this.eventType = eventType; }

    public String getClientId() { return clientId; }
    public void setClientId(String clientId) { this.clientId = clientId; }

    public String getClientIp() { return clientIp; }
    public void setClientIp(String clientIp) { this.clientIp = clientIp; }

    public String getEventTime() { return eventTime; }
    public void setEventTime(String eventTime) { this.eventTime = eventTime; }

    public String getReason() { return reason; }
    public void setReason(String reason) { this.reason = reason; }
}
