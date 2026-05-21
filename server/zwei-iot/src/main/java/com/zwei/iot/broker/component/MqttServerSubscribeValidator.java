package com.zwei.iot.broker.component;

import lombok.extern.slf4j.Slf4j;
import net.dreamlu.mica.net.core.ChannelContext;
import org.dromara.mica.mqtt.codec.MqttQoS;
import org.dromara.mica.mqtt.core.server.auth.IMqttServerSubscribeValidator;
import org.springframework.stereotype.Component;

/**
 *
 * @author Jerriel
 * @date: 2026-05-20
 */
@Component
@Slf4j
public class MqttServerSubscribeValidator implements IMqttServerSubscribeValidator {
    /**
     * 是否可以订阅
     *
     * @param context     ChannelContext
     * @param clientId    客户端 id
     * @param topicFilter 订阅 topic
     * @param qoS         MqttQoS
     * @return 是否可以订阅
     */
    @Override
    public boolean isValid(ChannelContext context, String clientId, String topicFilter, MqttQoS qoS) {
        return false;
    }
}
