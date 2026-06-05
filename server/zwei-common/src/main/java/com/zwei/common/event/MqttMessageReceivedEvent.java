package com.zwei.common.event;

/**
 * MQTT 监测消息接收事件。
 * <p>
 * 在 iot 模块的 MQTT 消息监听器解析并校验消息后发布，
 * 由 log 模块异步监听并记录到内存环形缓冲区。
 * <p>
 * 放在 zwei-common 中使得 iot 和 log 两个模块都可以依赖此事件类型，
 * 而不产生互相之间的直接模块依赖。
 */
public class MqttMessageReceivedEvent {

    private final String clientId;
    private final String username;
    private final String topic;
    private final byte[] payload;
    private final long receiveTime;

    public MqttMessageReceivedEvent(String clientId, String username, String topic,
                                    byte[] payload, long receiveTime) {
        this.clientId = clientId;
        this.username = username;
        this.topic = topic;
        this.payload = payload;
        this.receiveTime = receiveTime;
    }

    public String getClientId() {
        return clientId;
    }

    public String getUsername() {
        return username;
    }

    public String getTopic() {
        return topic;
    }

    public byte[] getPayload() {
        return payload;
    }

    public long getReceiveTime() {
        return receiveTime;
    }
}
