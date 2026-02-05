package com.zwei.module.iot.mqtt.listener;


import com.alibaba.fastjson2.JSONException;
import com.alibaba.fastjson2.JSONObject;
import com.zwei.iot.storage.core.IDbStructureData;
import lombok.extern.slf4j.Slf4j;
import org.dromara.mica.mqtt.codec.MqttPublishMessage;
import org.dromara.mica.mqtt.spring.server.MqttServerFunction;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.tio.core.ChannelContext;
import org.tio.core.Node;

import java.nio.charset.StandardCharsets;

/**
 *  mqtt server 消息监听
 * 
 * @author linx
 */
@Slf4j
@Service
public class MqttServerMessageListener {
    private final IDbStructureData dbStructureData;

    @Autowired
    public MqttServerMessageListener(IDbStructureData dbStructureData) {
        this.dbStructureData = dbStructureData;
    }

    /**
     * 监听属性上报事件
	 *
	 * @param context        ChannelContext，可选参数
	 * @param topic          实际接收到消息的主题名称，可选参数
	 * @param publishMessage 完整的MQTT发布消息对象，包含消息头和负载，可选参数
	 * @param message        消息负载内容，以字节数组形式提供，可选参数，也可支持对象形式，默认 json 序列化
	 */
    @MqttServerFunction("/device/${productKey}/${deviceKey}/thing/event/property/post")
    public void propertyPostListen(ChannelContext context, String topic, MqttPublishMessage publishMessage, byte[] message) {
		// 获取客户端节点信息
		Node clientNode = context.getClientNode();
        // 记录接收到的MQTT消息原始信息
		log.info("clientNode:{} topic:{} publishMessage:{} message:{}", clientNode, topic, publishMessage, new String(message));
        // 统一编码
        String msg = new String(message, StandardCharsets.UTF_8);

        // 消息序列化
        JSONObject object;
        try {
            object = JSONObject.parseObject(msg);
        } catch (JSONException e) {
            log.warn("Error when parsing message, msg={}, err={}", msg, e.getMessage());
            return;
        }
        if (object == null) {
            log.warn("Message is empty or null!");
            return;
        }

        // TODO 根据产品物模型确认属性字段是否有效，然后将属性信息存入数据库


    }

}