package com.zwei.iot.broker.service;

import com.zwei.common.event.DeviceOfflineEvent;
import com.zwei.iot.broker.component.MqttDeviceSessionRegistry;
import com.zwei.iot.broker.model.MqttDeviceSession;
import com.zwei.iot.device.service.IDeviceSessionService;
import lombok.extern.slf4j.Slf4j;
import org.dromara.mica.mqtt.core.server.MqttServer;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;

import java.util.Optional;

/**
 * 设备 MQTT 会话管理服务实现。
 * <p>
 * 提供跨模块的 MQTT 会话级别操作，如密码重置后强制断连。
 * 通过 {@link MqttDeviceSessionRegistry} 定位设备当前 session，
 * 再调用 mica-mqtt {@link MqttServer#disconnect(String)} 执行断连。
 *
 * <h3>离线状态同步</h3>
 * 服务端主动断连（{@code disconnect()}）时，mica-mqtt 回调中的 ChannelContext 可能为 null，
 * 导致 {@code resolveDeviceId()} 返回 0L，DeviceOfflineEvent 携带错误的 deviceId。
 * 因此本方法在断连后直接发布 DeviceOfflineEvent（含正确 deviceId），
 * 而非依赖 mica-mqtt 的回调链路。
 *
 * @author Jerriel
 * @date: 2026-06-08
 */
@Service
@Slf4j
public class DeviceSessionServiceImpl implements IDeviceSessionService {

    private final MqttDeviceSessionRegistry sessionRegistry;
    private final ObjectProvider<MqttServer> mqttServerProvider;
    private final ApplicationEventPublisher eventPublisher;

    public DeviceSessionServiceImpl(MqttDeviceSessionRegistry sessionRegistry,
                                    ObjectProvider<MqttServer> mqttServerProvider,
                                    ApplicationEventPublisher eventPublisher) {
        this.sessionRegistry = sessionRegistry;
        this.mqttServerProvider = mqttServerProvider;
        this.eventPublisher = eventPublisher;
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
        String clientId = session.clientId();
        String clientIp = session.clientIp();
        boolean disconnected = mqttServer.disconnect(clientId);
        if (disconnected) {
            sessionRegistry.removeByClientId(clientId);
            // 服务端主动断连时 mica-mqtt 回调中 context 可能为 null，导致 DeviceOfflineEvent 丢失。
            // 这里直接发布正确 deviceId 的事件，确保 device_online_status 和 event_log 被正确更新。
            eventPublisher.publishEvent(new DeviceOfflineEvent(deviceId, clientId, clientIp, "FORCE_OFFLINE"));
            log.info("[MQTT-SESSION] 已强制断开设备连接。deviceId={}, clientId={}", deviceId, clientId);
        } else {
            log.warn("[MQTT-SESSION] 断开设备连接失败。deviceId={}, clientId={}", deviceId, clientId);
        }
        return disconnected;
    }
}
