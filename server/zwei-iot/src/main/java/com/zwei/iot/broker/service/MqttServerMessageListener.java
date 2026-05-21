package com.zwei.iot.broker.service;

import lombok.extern.slf4j.Slf4j;
import net.dreamlu.mica.net.core.ChannelContext;
import net.dreamlu.mica.net.core.Node;
import org.dromara.mica.mqtt.codec.message.MqttPublishMessage;
import org.dromara.mica.mqtt.core.annotation.MqttServerFunction;
import org.springframework.stereotype.Service;

/**
 * 消息监听
 */
@Slf4j
@Service
public class MqttServerMessageListener {
    /**
     * MQTT消息处理函数，匹配 mqtt Topic /test/+，如何需要匹配所以消息，请使用通配符 #
     *
     * @param context        ChannelContext，可选参数
     * @param topic          实际接收到消息的主题名称，可选参数
     * @param publishMessage 完整的MQTT发布消息对象，包含消息头和负载，可选参数
     * @param message        消息负载内容，以字节数组形式提供，可选参数，也可支持对象形式，默认 json 序列化
     */
    @MqttServerFunction("/test/${xxxx}")
    public void func3(ChannelContext context, String topic, MqttPublishMessage publishMessage, byte[] message) {
        // 获取客户端节点信息
        Node clientNode = context.getClientNode();
        // 记录接收到的MQTT消息信息
        log.info("clientNode:{} topic:{} publishMessage:{} message:{}", clientNode, topic, publishMessage, new String(message));
    }

}