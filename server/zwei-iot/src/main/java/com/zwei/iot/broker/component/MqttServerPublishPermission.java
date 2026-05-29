package com.zwei.iot.broker.component;

import com.zwei.iot.broker.exception.MqttCommunicationException;
import com.zwei.iot.broker.exception.MqttExceptionReporter;
import com.zwei.iot.broker.service.MqttDeviceAuthService;
import lombok.extern.slf4j.Slf4j;
import net.dreamlu.mica.net.core.ChannelContext;
import org.dromara.mica.mqtt.codec.MqttQoS;
import org.dromara.mica.mqtt.core.server.auth.IMqttServerPublishPermission;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

/**
 * MQTT 发布权限控制。
 * <p>
 * 该组件负责拦截设备侧 PUBLISH 请求，统一校验 topic 规范、已鉴权会话、
 * topic 中的设备标识与传感器标识是否和当前连接匹配。
 */
@Component
@Slf4j
public class MqttServerPublishPermission implements IMqttServerPublishPermission {
    private final MqttDeviceAuthService mqttDeviceAuthService;
    private final MqttExceptionReporter mqttExceptionReporter;

    @Autowired
    public MqttServerPublishPermission(MqttDeviceAuthService mqttDeviceAuthService,
                                       MqttExceptionReporter mqttExceptionReporter) {
        this.mqttDeviceAuthService = mqttDeviceAuthService;
        this.mqttExceptionReporter = mqttExceptionReporter;
    }

    /**
     * 校验设备是否具备当前主题的发布权限。
     *
     * @param context 当前连接上下文
     * @param clientId 当前连接 clientId
     * @param topic 发布主题
     * @param qoS 发布 QoS
     * @param retain 是否为保留消息
     * @return {@code true} 表示允许发布
     */
    @Override
    public boolean hasPermission(ChannelContext context, String clientId, String topic, MqttQoS qoS, boolean retain) {
        try {
            return mqttDeviceAuthService.hasPublishPermission(context, clientId, topic, qoS, retain);
        } catch (RuntimeException e) {
            return mqttExceptionReporter.rejectWithError(new MqttCommunicationException.PublishFailed(
                    mqttExceptionReporter.context(clientId, topic, qoS)
                            .putAttribute("retain", retain)
                            .build(),
                    "发布权限校验异常",
                    e
            ), e);
        }
    }
}
