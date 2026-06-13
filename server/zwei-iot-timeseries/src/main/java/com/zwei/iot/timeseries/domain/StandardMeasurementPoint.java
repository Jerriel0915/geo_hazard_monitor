package com.zwei.iot.timeseries.domain;

import lombok.Builder;

import java.io.Serial;
import java.io.Serializable;

/**
 * 统一标准化时序点。
 *
 * <p>承载 MQTT 报文的解析结果，按统一结构写入 Redis Stream 缓冲队列
 * 并最终持久化至 IoTDB。所有字段在写入时一次性确定，不支持后续变更。</p>
 *
 * <p>字段说明：</p>
 * <ul>
 *   <li>{@code deviceId} — 设备ID，对应 device 表主键</li>
 *   <li>{@code sensorCode} — 传感器编码，全局唯一</li>
 *   <li>{@code sensorId} — 传感器主键ID</li>
 *   <li>{@code attrCode} — 指标编码，如 value、temperature</li>
 *   <li>{@code attrName} — 指标中文名称</li>
 *   <li>{@code unit} — 数据单位</li>
 *   <li>{@code dataTime} — 数据发生时间，毫秒时间戳</li>
 *   <li>{@code value} — 指标数值</li>
 *   <li>{@code quality} — 质量码，0=正常，非0=异常</li>
 *   <li>{@code reportTime} — 设备上报时间，毫秒时间戳</li>
 *   <li>{@code receiveTime} — 服务端接收时间，毫秒时间戳</li>
 *   <li>{@code sourceType} — 数据来源，sys=通用协议，gb=国标协议</li>
 *   <li>{@code payloadHash} — 原始报文 SHA-256 摘要，用于去重</li>
 * </ul>
 *
 */
@Builder
public record StandardMeasurementPoint(
        Long deviceId,
        String sensorCode,
        Long sensorId,
        String attrCode,
        String attrName,
        String unit,
        long dataTime,
        Double value,
        Integer quality,
        long reportTime,
        long receiveTime,
        String sourceType,
        String payloadHash
) implements Serializable {
    @Serial
    private static final long serialVersionUID = 1L;
}
