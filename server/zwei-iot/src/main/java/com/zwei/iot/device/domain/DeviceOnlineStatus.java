package com.zwei.iot.device.domain;

/**
 * 设备在线状态（运维指标独立存储）。
 * <p>
 * 与 device 业务主表解耦，专门存储运维视角的在线/离线/上报时间等指标。
 */
public class DeviceOnlineStatus {

    private Long id;
    private Long deviceId;
    private String clientId;
    private Integer status;
    private String onlineAt;
    private String offlineAt;
    private String lastReportAt;
    private Integer sessionDurationSec;

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public Long getDeviceId() { return deviceId; }
    public void setDeviceId(Long deviceId) { this.deviceId = deviceId; }

    public String getClientId() { return clientId; }
    public void setClientId(String clientId) { this.clientId = clientId; }

    public Integer getStatus() { return status; }
    public void setStatus(Integer status) { this.status = status; }

    public String getOnlineAt() { return onlineAt; }
    public void setOnlineAt(String onlineAt) { this.onlineAt = onlineAt; }

    public String getOfflineAt() { return offlineAt; }
    public void setOfflineAt(String offlineAt) { this.offlineAt = offlineAt; }

    public String getLastReportAt() { return lastReportAt; }
    public void setLastReportAt(String lastReportAt) { this.lastReportAt = lastReportAt; }

    public Integer getSessionDurationSec() { return sessionDurationSec; }
    public void setSessionDurationSec(Integer sessionDurationSec) { this.sessionDurationSec = sessionDurationSec; }
}
