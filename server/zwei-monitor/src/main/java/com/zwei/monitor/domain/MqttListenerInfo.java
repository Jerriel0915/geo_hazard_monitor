package com.zwei.monitor.domain;

import lombok.Builder;
import lombok.Data;

/**
 * MQTT 监听器信息。
 * <p>
 * 描述单个监听器（TCP / WS / HTTP）的绑定地址和端口。
 */
@Data
@Builder
public class MqttListenerInfo {
    /**
     * 监听器类型：mqtt-tcp / mqtt-ws / mqtt-http
     */
    private String type;
    /**
     * 监听地址（空表示 0.0.0.0）
     */
    private String ip;
    /**
     * 监听端口
     */
    private int port;
    /**
     * 是否启用
     */
    private boolean enabled;
    /**
     * 补充说明（如 SSL 状态）
     */
    private String remark;
}
