package com.zwei.monitor.controller;

import com.zwei.common.core.domain.AjaxResult;
import com.zwei.monitor.client.MqttHttpApiClient;
import com.zwei.monitor.domain.MqttConfigInfo;
import com.zwei.monitor.domain.MqttListenerInfo;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.ArrayList;
import java.util.List;

/**
 * MQTT 服务器指标接口
 * <p>
 * 提供 MQTT 服务器运行统计、监听器配置和运行参数等只读查询。
 */
@RestController
@RequestMapping("/api/v1/monitor/mqtt")
public class MqttStatsController {

    private final MqttHttpApiClient mqttHttpApiClient;

    // ---- 监听器配置（来自 application.yml mqtt.server.*） ----
    @Value("${mqtt.server.mqtt-listener.enable:false}")
    private boolean tcpEnabled;
    @Value("${mqtt.server.mqtt-listener.port:1883}")
    private int tcpPort;

    @Value("${mqtt.server.ws-listener.enable:false}")
    private boolean wsEnabled;
    @Value("${mqtt.server.ws-listener.port:8083}")
    private int wsPort;

    @Value("${mqtt.server.mqtt-ssl-listener.enable:false}")
    private boolean sslEnabled;
    @Value("${mqtt.server.mqtt-ssl-listener.port:8883}")
    private int sslPort;

    @Value("${mqtt.server.http-listener.enable:false}")
    private boolean httpEnabled;
    @Value("${mqtt.server.http-listener.port:18083}")
    private int httpPort;

    // ---- 运行配置 ----
    @Value("${mqtt.server.heartbeat-timeout:120000}")
    private long heartbeatTimeout;
    @Value("${mqtt.server.read-buffer-size:8KB}")
    private String readBufferSize;
    @Value("${mqtt.server.max-bytes-in-message:10MB}")
    private String maxBytesInMessage;
    @Value("${mqtt.server.auth.enable:false}")
    private boolean authEnabled;
    @Value("${mqtt.server.debug:false}")
    private boolean debug;
    @Value("${mqtt.server.stat-enable:false}")
    private boolean statEnable;

    public MqttStatsController(MqttHttpApiClient mqttHttpApiClient) {
        this.mqttHttpApiClient = mqttHttpApiClient;
    }

    /**
     * 获取 MQTT 服务器全量统计指标
     */
    @PreAuthorize("@ss.hasPermi('monitor:mqtt:list')")
    @GetMapping("/stats")
    public AjaxResult getStats() {
        return AjaxResult.success(mqttHttpApiClient.getStats());
    }

    /**
     * 获取所有监听器配置（类型、地址、端口、启用状态）
     */
    @PreAuthorize("@ss.hasPermi('monitor:mqtt:list')")
    @GetMapping("/listeners")
    public AjaxResult getListeners() {
        List<MqttListenerInfo> listeners = new ArrayList<>();
        listeners.add(MqttListenerInfo.builder()
                .type("mqtt-tcp").ip("0.0.0.0").port(tcpPort).enabled(tcpEnabled)
                .remark("设备 MQTT 直连").build());
        listeners.add(MqttListenerInfo.builder()
                .type("mqtt-ws").ip("0.0.0.0").port(wsPort).enabled(wsEnabled)
                .remark("浏览器 WebSocket 接入").build());
        listeners.add(MqttListenerInfo.builder()
                .type("mqtt-ssl").ip("0.0.0.0").port(sslPort).enabled(sslEnabled)
                .remark("SSL/TLS 加密接入").build());
        listeners.add(MqttListenerInfo.builder()
                .type("mqtt-http").ip("127.0.0.1").port(httpPort).enabled(httpEnabled)
                .remark("HTTP 管理 API (内部)").build());
        return AjaxResult.success(listeners);
    }

    /**
     * 获取 MQTT 服务器运行配置参数
     */
    @PreAuthorize("@ss.hasPermi('monitor:mqtt:list')")
    @GetMapping("/config")
    public AjaxResult getConfig() {
        MqttConfigInfo config = MqttConfigInfo.builder()
                .heartbeatTimeout(heartbeatTimeout)
                .readBufferSize(readBufferSize)
                .maxBytesInMessage(maxBytesInMessage)
                .authEnabled(authEnabled)
                .debug(debug)
                .statEnable(statEnable)
                .build();
        return AjaxResult.success(config);
    }
}
