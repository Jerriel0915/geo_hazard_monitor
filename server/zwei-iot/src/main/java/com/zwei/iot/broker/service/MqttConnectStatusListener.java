package com.zwei.iot.broker.service;

import lombok.extern.slf4j.Slf4j;
import net.dreamlu.mica.net.core.ChannelContext;
import org.dromara.mica.mqtt.core.server.event.IMqttConnectStatusListener;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

/**
 * MQTT 连接状态监听器。
 * <p>
 * 实现 {@link IMqttConnectStatusListener}，统一承接 Broker 的在线/离线回调
 *
 * @author Jerriel
 * @date: 2026-05-20
 */
@Service
@Slf4j
public class MqttConnectStatusListener implements IMqttConnectStatusListener {
    private final MqttDeviceAuthService mqttDeviceAuthService;

    @Autowired
    public MqttConnectStatusListener(MqttDeviceAuthService mqttDeviceAuthService) {
        this.mqttDeviceAuthService = mqttDeviceAuthService;
    }

    /**
     * 连接建立后同步刷新设备在线状态。
     *
     * @param context 当前连接上下文
     * @param clientId 当前连接 clientId
     * @param username 设备认证账号
     */
    @Override
    public void online(ChannelContext context, String clientId, String username) {
        mqttDeviceAuthService.handleClientOnline(context, clientId, username);
        log.info("MqttClientOnline clientId:{}, username:{}", clientId, username);
    }

    /**
     * 连接断开后释放会话并回写设备离线状态。
     *
     * @param context 当前连接上下文
     * @param clientId 当前连接 clientId
     * @param username 设备认证账号
     * @param reason 离线原因
     */
    @Override
    public void offline(ChannelContext context, String clientId, String username, String reason) {
        mqttDeviceAuthService.handleClientOffline(context, clientId, username, reason);
        log.info("MqttClientOffline clientId:{}, username:{}, reason:{}", clientId, username, reason);
    }

}
