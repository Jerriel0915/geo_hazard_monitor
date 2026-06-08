package com.zwei.iot.broker.service;

import lombok.extern.slf4j.Slf4j;
import net.dreamlu.mica.net.core.ChannelContext;
import org.dromara.mica.mqtt.codec.MqttQoS;
import org.dromara.mica.mqtt.core.server.event.IMqttSessionListener;
import org.springframework.stereotype.Service;

/**
 * MQTT 会话级事件监听器 — 订阅/取消订阅回调。
 *
 * <p>实现 mica-mqtt 的 {@link IMqttSessionListener}，接收设备侧 SUBSCRIBE / UNSUBSCRIBE 事件。
 * 当前为预留扩展点，暂不做业务处理（订阅权限校验由 {@link MqttServerSubscribeValidator} 负责）。
 *
 * <p>后续可扩展场景：
 * <ul>
 *   <li>记录设备订阅偏好，用于指令下发时的主题选择</li>
 *   <li>订阅审计日志（追踪设备侧何时订阅/取消订阅了哪些主题）</li>
 * </ul>
 *
 * @author Jerriel
 * @date: 2026-05-20
 */
@Service
@Slf4j
public class MqttSessionListener implements IMqttSessionListener {
    /**
     * 设备订阅主题回调。
     * <p>触发时机：设备侧 SUBSCRIBE 请求通过 {@link MqttServerSubscribeValidator} 校验后。</p>
     *
     * @param context     ChannelContext
     * @param clientId    clientId
     * @param topicFilter 订阅的主题过滤器
     * @param mqttQoS     订阅 QoS 等级
     */
    @Override
    public void onSubscribed(ChannelContext context, String clientId, String topicFilter, MqttQoS mqttQoS) {
        // 预留扩展点：当前不做业务处理
    }

    /**
     * 设备取消订阅主题回调。
     *
     * @param context     ChannelContext
     * @param clientId    clientId
     * @param topicFilter 取消订阅的主题过滤器
     */
    @Override
    public void onUnsubscribed(ChannelContext context, String clientId, String topicFilter) {
        // 预留扩展点：当前不做业务处理
    }
}
