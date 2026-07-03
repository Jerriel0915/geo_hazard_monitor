package com.zwei.iot.broker.service;

import com.zwei.common.event.MqttMessageReceivedEvent;
import com.zwei.common.event.MqttMessageRejectEvent;
import com.zwei.common.utils.StringUtils;
import com.zwei.iot.broker.component.MqttDeviceSessionRegistry;
import com.zwei.iot.broker.model.MqttDeviceSession;
import com.zwei.iot.timeseries.service.MonitorIngestFacade;
import lombok.extern.slf4j.Slf4j;
import net.dreamlu.mica.net.core.ChannelContext;
import net.dreamlu.mica.net.core.Node;
import org.dromara.mica.mqtt.codec.message.MqttPublishMessage;
import org.dromara.mica.mqtt.core.annotation.MqttServerFunction;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;

import java.util.Optional;

/**
 * MQTT 消息监听器 — 设备数据上报入口。
 *
 * <p>通过 {@code @MqttServerFunction("#")} 注解匹配所有主题的 PUBLISH 消息，
 * 统一路由到监测数据接入链路。
 *
 * <h3>消息处理流程</h3>
 * <ol>
 *   <li><b>设备定位</b>：从连接上下文中取 clientId → 查会话注册中心 → 获取已认证 deviceId（先确认认证）</li>
 *   <li><b>主题过滤</b>：已认证但主题非 sys/v1/** 或 gb/v1/** → 发布 {@link MqttMessageRejectEvent}（异常报文）</li>
 *   <li><b>事件发布</b>：发布 {@link MqttMessageReceivedEvent} 供 log 模块异步记录消息日志</li>
 *   <li><b>数据接入</b>：委托 {@link MonitorIngestFacade#ingest} 完成解析+入队，失败时发布 {@link MqttMessageRejectEvent}</li>
 * </ol>
 *
 * <h3>解耦设计</h3>
 * 消息日志（zwei-log）与消息处理（zwei-iot-broker）通过 Spring 事件机制完全解耦：
 * 本监听器只发布事件，不感知日志存储细节。
 */
@Slf4j
@Service
public class MqttServerMessageListener {
    private final MonitorIngestFacade monitorIngestFacade;
    private final MqttDeviceSessionRegistry sessionRegistry;
    private final ApplicationEventPublisher eventPublisher;

    @Autowired
    public MqttServerMessageListener(MonitorIngestFacade monitorIngestFacade,
                                     MqttDeviceSessionRegistry sessionRegistry,
                                     ApplicationEventPublisher eventPublisher) {
        this.monitorIngestFacade = monitorIngestFacade;
        this.sessionRegistry = sessionRegistry;
        this.eventPublisher = eventPublisher;
    }

    /**
     * MQTT 监测数据处理函数。
     *
     * @param context        ChannelContext，可选参数
     * @param topic          实际接收到消息的主题名称，可选参数
     * @param publishMessage 完整的MQTT发布消息对象，包含消息头和负载，可选参数
     * @param message        消息负载内容，以字节数组形式提供，可选参数，也可支持对象形式，默认 json 序列化
     */
    @MqttServerFunction("#")
    public void onMessage(ChannelContext context, String topic, MqttPublishMessage publishMessage, byte[] message) {
        if (context == null) {
            log.warn("监测消息缺少连接上下文，跳过。topic={}", sanitize(topic));
            return;
        }
        Node clientNode = context.getClientNode();
        // 通过 bindContext 中写入的 clientId 从会话注册中心获取已认证设备信息，
        // 避免 Long → String → Long 的往返转换及 NumberFormatException 风险。
        String clientId = context.getBsId();
        if (StringUtils.isBlank(clientId)) {
            log.warn("监测消息缺少客户端标识，跳过。topic={}", sanitize(topic));
            return;
        }
        Optional<MqttDeviceSession> session = sessionRegistry.getByClientId(clientId);
        if (session.isEmpty()) {
            log.warn("监测消息未找到已认证会话，跳过。topic={}, clientId={}", sanitize(topic), sanitize(clientId));
            return;
        }
        Long deviceId = session.get().deviceId();
        String username = session.get().authUsername();
        long receiveTime = System.currentTimeMillis();
        log.debug("收到监测主题消息 clientNode={}, topic={}", sanitize(String.valueOf(clientNode)), sanitize(topic));
        // 已认证但主题不匹配监测协议 → 记录异常报文（不进入数据日志）
        if (topic == null || (!topic.startsWith("sys/v1/") && !topic.startsWith("gb/v1/"))) {
            publishReject(clientId, username, deviceId, topic, message, receiveTime,
                    "TOPIC", "主题不匹配监测协议前缀 (sys/v1/ 或 gb/v1/)", null);
            return;
        }
        eventPublisher.publishEvent(new MqttMessageReceivedEvent(
                clientId, username, topic, message, receiveTime));
        // ingest() 异步提交到线程池，不阻塞 MQTT IO 线程；
        // Groovy 脚本执行、异常报文发布均在工作线程中完成
        monitorIngestFacade.ingest(topic, message, deviceId);
    }

    /** 发布异常报文事件，供 log 模块异步持久化 */
    private void publishReject(String clientId, String username, Long deviceId, String topic,
                               byte[] message, long receiveTime,
                               String rejectStage, String rejectReason, String errorStack) {
        try {
            eventPublisher.publishEvent(new MqttMessageRejectEvent(
                    clientId, username, deviceId, topic, message, receiveTime,
                    rejectStage, rejectReason, errorStack));
        } catch (Exception ex) {
            log.warn("发布异常报文事件失败。topic={}, stage={}", sanitize(topic), rejectStage, ex);
        }
    }

    /** 转义日志参数中的控制字符（\r\n\t 等），防止日志注入伪造日志行 */
    private static String sanitize(String s) {
        if (s == null) return null;
        return s.replaceAll("[\r\n\t]", "?");
    }
}
