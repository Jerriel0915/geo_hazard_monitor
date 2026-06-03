package com.zwei.monitor.config;

import lombok.Getter;
import lombok.Setter;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

/**
 * MQTT HTTP API 连接参数。
 * <p>
 * 绑定 Spring 环境中的 mqtt.server.http-listener 配置，
 * 用于通过内部 HTTP 调用 mica-mqtt 管理 API 获取运行状态。
 */
@Setter
@Getter
@Component
@ConfigurationProperties(prefix = "mqtt.server.http-listener")
public class MqttHttpApiProperties {
    /**
     * HTTP 管理端口，默认 18083
     */
    @Value("${mqtt.server.http-listener.port:18083}")
    private int port;
    /**
     * 是否启用
     */
    @Value("${mqtt.server.http-listener.enable:true}")
    private boolean enable;
    /**
     * basic-auth 配置
     */
    private BasicAuth basicAuth = new BasicAuth();

    @Setter
    @Getter
    public static class BasicAuth {
        @Value("${mqtt.server.http-listener.basic-auth.enable:true}")
        private boolean enable;
        @Value("${mqtt.server.http-listener.basic-auth.username:mica}")
        private String username;
        @Value("${mqtt.server.http-listener.basic-auth.password:mica}")
        private String password;
    }

    public String baseUrl() {
        return "http://127.0.0.1:" + port + "/api/v1";
    }
}
