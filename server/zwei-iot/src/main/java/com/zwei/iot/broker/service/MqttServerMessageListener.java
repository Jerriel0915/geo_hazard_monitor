package com.zwei.iot.broker.service;

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
import org.springframework.stereotype.Service;

import java.util.Optional;

/**
 * 消息监听。
 *
 * <p>为设备接入闭环增加正式监测主题转发逻辑，统一路由到时序写入链路。</p>
 */
@Slf4j
@Service
public class MqttServerMessageListener {
    private final MonitorIngestFacade monitorIngestFacade;
    private final MqttDeviceSessionRegistry sessionRegistry;

    @Autowired
    public MqttServerMessageListener(MonitorIngestFacade monitorIngestFacade,
                                     MqttDeviceSessionRegistry sessionRegistry) {
        this.monitorIngestFacade = monitorIngestFacade;
        this.sessionRegistry = sessionRegistry;
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
        Node clientNode = context.getClientNode();
        if (topic == null || (!topic.startsWith("sys/v1/") && !topic.startsWith("gb/v1/"))) {
            return;
        }
        // 通过 bindContext 中写入的 clientId 从会话注册中心获取已认证设备信息，
        // 避免 Long → String → Long 的往返转换及 NumberFormatException 风险。
        String clientId = context.getBsId();
        if (StringUtils.isBlank(clientId)) {
            log.warn("监测消息缺少客户端标识，跳过。topic={}", topic);
            return;
        }
        Optional<MqttDeviceSession> session = sessionRegistry.getByClientId(clientId);
        if (session.isEmpty()) {
            log.warn("监测消息未找到已认证会话，跳过。topic={}, clientId={}", topic, clientId);
            return;
        }
        Long deviceId = session.get().deviceId();
        log.debug("收到监测主题消息 clientNode={}, topic={}", clientNode, topic);
        monitorIngestFacade.ingest(topic, message, deviceId);
    }
}
