package com.zwei.common.event;

/**
 * 监测内容变更事件 — 触发 sensor_attribute 反向同步。
 * <p>
 * 在 MonitorContentServiceImpl 的 insert/update/delete 方法中发布，
 * 供 zwei-iot-device 的 MonitorContentSyncListener 监听，
 * 将字典变更同步到已有传感器的 sensor_attribute 和 product TSL。
 *
 * @author zwei
 */
public class MonitorContentChangedEvent {

    public enum ChangeType {
        INSERT,
        UPDATE,
        DELETE
    }

    private final Long monitorTypeId;
    private final ChangeType changeType;

    public MonitorContentChangedEvent(Long monitorTypeId, ChangeType changeType) {
        this.monitorTypeId = monitorTypeId;
        this.changeType = changeType;
    }

    public Long getMonitorTypeId() {
        return monitorTypeId;
    }

    public ChangeType getChangeType() {
        return changeType;
    }
}
