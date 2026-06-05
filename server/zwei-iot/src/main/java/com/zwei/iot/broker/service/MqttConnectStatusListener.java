package com.zwei.iot.broker.service;

import com.zwei.common.event.DeviceOfflineEvent;
import com.zwei.common.event.DeviceOnlineEvent;
import lombok.extern.slf4j.Slf4j;
import net.dreamlu.mica.net.core.ChannelContext;
import org.dromara.mica.mqtt.core.server.event.IMqttConnectStatusListener;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;

/**
 * MQTT 连接状态监听器。
 * <p>
 * 实现 {@link IMqttConnectStatusListener}，统一承接 Broker 的在线/离线回调，
 * 通过 Spring 事件机制通知在线状态服务记录运维指标。
 *
 * @author Jerriel
 * @date: 2026-05-20
 */
@Service
@Slf4j
public class MqttConnectStatusListener implements IMqttConnectStatusListener {
    private final MqttDeviceAuthService mqttDeviceAuthService;
    private final ApplicationEventPublisher eventPublisher;

    @Autowired
    public MqttConnectStatusListener(MqttDeviceAuthService mqttDeviceAuthService,
                                     ApplicationEventPublisher eventPublisher) {
        this.mqttDeviceAuthService = mqttDeviceAuthService;
        this.eventPublisher = eventPublisher;
    }

    /**
     * 连接建立后同步刷新设备在线状态，并发布上线事件供运维指标记录。
     *
     * @param context 当前连接上下文
     * @param clientId 当前连接 clientId
     * @param username 设备认证账号
     */
    @Override
    public void online(ChannelContext context, String clientId, String username) {
        try {
            mqttDeviceAuthService.handleClientOnline(context, clientId, username);
            // 从已认证会话中获取 deviceId 发布上线事件
            eventPublisher.publishEvent(new DeviceOnlineEvent(
                    resolveDeviceId(context), clientId, resolveClientIp(context)));
            log.info("MqttClientOnline clientId:{}, username:{}", clientId, username);
        } catch (Exception e) {
            log.error("处理设备上线事件失败。clientId={}, username={}", clientId, username, e);
        }
    }

    /**
     * 连接断开后释放会话、回写离线状态，并发布离线事件供运维指标记录。
     *
     * @param context 当前连接上下文
     * @param clientId 当前连接 clientId
     * @param username 设备认证账号
     * @param reason 离线原因
     */
    @Override
    public void offline(ChannelContext context, String clientId, String username, String reason) {
        try {
            mqttDeviceAuthService.handleClientOffline(context, clientId, username, reason);
            eventPublisher.publishEvent(new DeviceOfflineEvent(
                    resolveDeviceId(context), clientId, resolveClientIp(context), reason));
            log.info("MqttClientOffline clientId:{}, username:{}, reason:{}", clientId, username, reason);
        } catch (Exception e) {
            log.error("处理设备离线事件失败。clientId={}, username={}", clientId, username, e);
        }
    }

    private Long resolveDeviceId(ChannelContext context) {
        if (context == null || context.getUserId() == null) {
            return 0L;
        }
        try {
            return Long.parseLong(context.getUserId());
        } catch (NumberFormatException e) {
            log.debug("解析 deviceId 失败 userId={}", context.getUserId());
            return 0L;
        }
    }

    private String resolveClientIp(ChannelContext context) {
        if (context == null) {
            return null;
        }
        if (context.getProxyClientNode() != null && context.getProxyClientNode().getIp() != null) {
            return context.getProxyClientNode().getIp();
        }
        return context.getClientNode() == null ? null : context.getClientNode().getIp();
    }
}
