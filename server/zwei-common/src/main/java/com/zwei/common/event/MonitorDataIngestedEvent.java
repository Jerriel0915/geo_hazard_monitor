package com.zwei.common.event;

import com.zwei.common.domain.PropertyValue;

import java.util.List;

/**
 * 监测数据落库完成事件 — 告警引擎的触发入口。
 * <p>
 * 在 MonitorIngestConsumerService 中 IoTDB 写入成功后发布，
 * 携带一次报文的完整数据包（ParsedMessage 全部字段），供 alarm 模块监听并执行告警判据匹配。
 *
 * <p>字段来源：
 * <ul>
 *   <li>{@code deviceId} / {@code sensorId} — consumer adapt 阶段已解析的 ID，避免下游重复查 DB</li>
 *   <li>{@code deviceCode} / {@code sensorCode} / {@code sourceType} / {@code receiveTime} / {@code payloadHash} /
 *       {@code properties} — 从 ParsedMessage 复制</li>
 * </ul>
 *
 * <p>注意：一次 MQTT 报文（即使包含多个 PropertyValue）只发布一次本事件。
 *
 * @author zwei
 */
public class MonitorDataIngestedEvent {

    private final Long deviceId;
    private final Long sensorId;
    private final String deviceCode;
    private final String sensorCode;
    private final String sourceType;
    private final long receiveTime;
    private final String payloadHash;
    private final List<PropertyValue> properties;

    public MonitorDataIngestedEvent(Long deviceId, Long sensorId, String deviceCode,
                                    String sensorCode, String sourceType, long receiveTime,
                                    String payloadHash, List<PropertyValue> properties) {
        this.deviceId = deviceId;
        this.sensorId = sensorId;
        this.deviceCode = deviceCode;
        this.sensorCode = sensorCode;
        this.sourceType = sourceType;
        this.receiveTime = receiveTime;
        this.payloadHash = payloadHash;
        this.properties = properties;
    }

    public Long getDeviceId() {
        return deviceId;
    }

    public Long getSensorId() {
        return sensorId;
    }

    public String getDeviceCode() {
        return deviceCode;
    }

    public String getSensorCode() {
        return sensorCode;
    }

    public String getSourceType() {
        return sourceType;
    }

    public long getReceiveTime() {
        return receiveTime;
    }

    public String getPayloadHash() {
        return payloadHash;
    }

    public List<PropertyValue> getProperties() {
        return properties;
    }
}
