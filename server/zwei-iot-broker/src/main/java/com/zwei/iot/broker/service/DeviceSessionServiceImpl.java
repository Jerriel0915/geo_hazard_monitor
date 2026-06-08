package com.zwei.iot.broker.service;

import com.zwei.iot.broker.component.MqttDeviceSessionRegistry;
import com.zwei.iot.broker.model.MqttDeviceSession;
import com.zwei.iot.device.service.IDeviceSessionService;
import lombok.extern.slf4j.Slf4j;
import org.dromara.mica.mqtt.core.server.MqttServer;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.stereotype.Service;

import java.util.Optional;

/**
 * 设备 MQTT 会话管理服务实现。
 * <p>
 * 提供跨模块的 MQTT 会话级别操作，如密码重置后强制断连。
 * 通过 {@link MqttDeviceSessionRegistry} 定位设备当前 session，
 * 再调用 mica-mqtt {@link MqttServer#disconnect(String)} 执行断连。
 *
 * @author Jerriel
 * @date: 2026-06-08
 */
@Service
@Slf4j
public class DeviceSessionServiceImpl implements IDeviceSessionService {

    private final MqttDeviceSessionRegistry sessionRegistry;
    private final ObjectProvider<MqttServer> mqttServerProvider;

    public DeviceSessionServiceImpl(MqttDeviceSessionRegistry sessionRegistry,
                                    ObjectProvider<MqttServer> mqttServerProvider) {
        this.sessionRegistry = sessionRegistry;
        this.mqttServerProvider = mqttServerProvider;
    }

    @Override
    public boolean disconnectDevice(Long deviceId) {
        Optional<MqttDeviceSession> sessionOpt = sessionRegistry.getByDeviceId(deviceId);
        if (sessionOpt.isEmpty()) {
            log.info("[MQTT-SESSION] 设备当前无活跃会话，无需断开。deviceId={}", deviceId);
            return true;
        }
        MqttDeviceSession session = sessionOpt.get();
        MqttServer mqttServer = mqttServerProvider.getIfAvailable();
        if (mqttServer == null) {
            log.warn("[MQTT-SESSION] Broker 实例不可用，无法断开设备。deviceId={}", deviceId);
            return false;
        }
        boolean disconnected = mqttServer.disconnect(session.clientId());
        if (disconnected) {
            sessionRegistry.removeByClientId(session.clientId());
            log.info("[MQTT-SESSION] 已强制断开设备连接。deviceId={}, clientId={}", deviceId, session.clientId());
        } else {
            log.warn("[MQTT-SESSION] 断开设备连接失败。deviceId={}, clientId={}", deviceId, session.clientId());
        }
        return disconnected;
    }
}
