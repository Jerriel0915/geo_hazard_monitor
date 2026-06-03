package com.zwei.monitor.domain;

import lombok.Data;

/**
 * MQTT 客户端连接详情。
 * <p>
 * 基础字段来自 mica-mqtt HTTP API，扩展字段由 {@code MqttSessionEnrichService} 补全。
 */
@Data
public class MqttClientInfo {
    /**
     * MQTT clientId
     */
    private String clientId;
    /**
     * 设备认证账号（即 deviceCode）
     */
    private String username;
    /**
     * 是否已连接
     */
    private boolean connected;
    /**
     * 客户端 IP
     */
    private String ipAddress;
    /**
     * 客户端端口
     */
    private int port;
    /**
     * 协议名称（MQTT / MQTT over WebSocket）
     */
    private String protoName;
    /**
     * MQTT 协议版本
     */
    private int protoVer;
    /**
     * 连接创建时间戳（毫秒）
     */
    private long createdAt;
    /**
     * 连接成功时间戳（毫秒）
     */
    private long connectedAt;

    // ---- 扩展字段（由 MqttSessionEnrichService 补全） ----

    /**
     * 设备 ID
     */
    private Long deviceId;
    /**
     * 设备名称
     */
    private String deviceName;
    /**
     * 设备编码
     */
    private String deviceCode;
    /**
     * 设备运行状态（1-运行中，2-已停止）
     */
    private Integer deviceRunStatus;
    /**
     * 关联的隐患点名称
     */
    private String hazardPointName;
    /**
     * 设备最近鉴权 IP
     */
    private String lastAuthIp;
    /**
     * 设备最近鉴权时间
     */
    private String lastAuthTime;
}
