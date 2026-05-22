package com.zwei.iot.broker.service;

import lombok.extern.slf4j.Slf4j;
import org.dromara.mica.mqtt.spring.server.event.MqttClientOfflineEvent;
import org.dromara.mica.mqtt.spring.server.event.MqttClientOnlineEvent;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Service;

/**
 * 连接状态监听
 *
 * @author Jerriel
 * @date: 2026-05-20
 */
@Service
@Slf4j
public class MqttConnectStatusListener {
    private static final Logger logger = LoggerFactory.getLogger(MqttConnectStatusListener.class);

    @EventListener
    public void online(MqttClientOnlineEvent event) {
        logger.info("MqttClientOnlineEvent:{}", event);
    }

    @EventListener
    public void offline(MqttClientOfflineEvent event) {
        logger.info("MqttClientOfflineEvent:{}", event);
    }

}
