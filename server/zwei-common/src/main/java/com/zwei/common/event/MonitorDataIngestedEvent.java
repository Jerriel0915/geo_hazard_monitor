package com.zwei.common.event;

/**
 * 监测数据落库完成事件 — 告警引擎的触发入口。
 * <p>
 * 在 MonitorIngestConsumerService.processRecord() 中 IoTDB 写入成功后发布，
 * 供 alarm 模块监听并执行告警判据匹配。
 *
 * @author zwei
 */
public class MonitorDataIngestedEvent {

    private final Long deviceId;
    private final Long sensorId;
    private final String sensorCode;
    private final String attrCode;
    private final Double value;
    private final Long dataTime;     // epoch millis
    private final String sourceType; // "sys" or "gb"

    public MonitorDataIngestedEvent(Long deviceId, Long sensorId, String sensorCode,
                                    String attrCode, Double value, Long dataTime,
                                    String sourceType) {
        this.deviceId = deviceId;
        this.sensorId = sensorId;
        this.sensorCode = sensorCode;
        this.attrCode = attrCode;
        this.value = value;
        this.dataTime = dataTime;
        this.sourceType = sourceType;
    }

    public Long getDeviceId() {
        return deviceId;
    }

    public Long getSensorId() {
        return sensorId;
    }

    public String getSensorCode() {
        return sensorCode;
    }

    public String getAttrCode() {
        return attrCode;
    }

    public Double getValue() {
        return value;
    }

    public Long getDataTime() {
        return dataTime;
    }

    public String getSourceType() {
        return sourceType;
    }
}
