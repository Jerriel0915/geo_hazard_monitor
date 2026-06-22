package com.zwei.common.event;

import com.zwei.common.domain.ParsedMessageSnapshot;
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
 *   <li>{@code dataTime} — 报文业务时间（ParsedMessage.dataTime），告警引擎判 TTL/时序窗口用</li>
 *   <li>{@code prevSnapshot} — 同设备+传感器上一条报文的精简快照（由 consumer 阶段缓存维护），
 *       用于 prev 维度判据；可为 {@code null}（首次上报或缓存失效）</li>
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
    private final long dataTime;
    private final ParsedMessageSnapshot prevSnapshot;

    public MonitorDataIngestedEvent(Long deviceId, Long sensorId, String deviceCode,
                                    String sensorCode, String sourceType, long receiveTime,
                                    String payloadHash, List<PropertyValue> properties,
                                    long dataTime, ParsedMessageSnapshot prevSnapshot) {
        this.deviceId = deviceId;
        this.sensorId = sensorId;
        this.deviceCode = deviceCode;
        this.sensorCode = sensorCode;
        this.sourceType = sourceType;
        this.receiveTime = receiveTime;
        this.payloadHash = payloadHash;
        this.properties = properties;
        this.dataTime = dataTime;
        this.prevSnapshot = prevSnapshot;
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

    public long getDataTime() {
        return dataTime;
    }

    public ParsedMessageSnapshot getPrevSnapshot() {
        return prevSnapshot;
    }
}
