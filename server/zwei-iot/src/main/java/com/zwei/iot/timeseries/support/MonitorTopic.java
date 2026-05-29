package com.zwei.iot.timeseries.support;


/**
 * MQTT 监测数据主题信息。
 *
 * <p>从 MQTT 主题字符串中解析出的结构化表示，包含来源类型、设备编号和传感器编号。
 * 用于在解析链路中传递上下文，避免重复解析主题字符串。</p>
 *
 * <p>字段说明：</p>
 * <ul>
 *   <li>{@code sourceType} — 来源类型，sys=通用协议，gb=国标协议</li>
 *   <li>{@code deviceCode} — 设备编号，对应 device 表 code 字段</li>
 *   <li>{@code sensorNo} — 传感器编号，设备下唯一</li>
 * </ul>
 */
public record MonitorTopic(String sourceType, String deviceCode, String sensorNo) {
}
