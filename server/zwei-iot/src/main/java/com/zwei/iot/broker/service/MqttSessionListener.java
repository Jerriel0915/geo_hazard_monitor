package com.zwei.iot.broker.service;

import lombok.extern.slf4j.Slf4j;
import net.dreamlu.mica.net.core.ChannelContext;
import org.dromara.mica.mqtt.codec.MqttQoS;
import org.dromara.mica.mqtt.core.server.event.IMqttSessionListener;
import org.springframework.stereotype.Service;

/**
 *
 * @author Jerriel
 * @date: 2026-05-20
 */
@Service
@Slf4j
public class MqttSessionListener implements IMqttSessionListener {
    /**
     * 订阅
     *
     * @param context     ChannelContext
     * @param clientId    clientId
     * @param topicFilter topicFilter
     * @param mqttQoS     MqttQoS
     */
    @Override
    public void onSubscribed(ChannelContext context, String clientId, String topicFilter, MqttQoS mqttQoS) {

    }

    /**
     * 取消订阅
     *
     * @param context     ChannelContext
     * @param clientId    clientId
     * @param topicFilter topicFilter
     */
    @Override
    public void onUnsubscribed(ChannelContext context, String clientId, String topicFilter) {

    }
}
