package com.zwei.iot.broker.exception;

import com.zwei.common.utils.StringUtils;

import java.io.Serial;
import java.io.Serializable;
import java.util.LinkedHashMap;
import java.util.Map;

/**
 * MQTT 异常上下文信息。
 * <p>
 * 用于在异常抛出点记录与 MQTT 链路相关的关键字段，支持后续日志采集、全链路溯源。
 * <p>
 * 推荐必填字段：
 * <ul>
 *   <li>clientId：客户端标识</li>
 *   <li>topic：主题</li>
 * </ul>
 */
public final class MqttErrorContext implements Serializable {
    @Serial
    private static final long serialVersionUID = 1L;

    private final String clientId;
    private final String topic;
    private final String messageId;
    private final Integer packetId;
    private final Integer qos;
    private final String protocolVersion;
    private final String brokerAddress;
    private final Map<String, Object> attributes;

    private MqttErrorContext(Builder builder) {
        this.clientId = builder.clientId;
        this.topic = builder.topic;
        this.messageId = builder.messageId;
        this.packetId = builder.packetId;
        this.qos = builder.qos;
        this.protocolVersion = builder.protocolVersion;
        this.brokerAddress = builder.brokerAddress;
        this.attributes = builder.attributes == null ? Map.of() : Map.copyOf(builder.attributes);
    }

    public static Builder builder() {
        return new Builder();
    }

    public String getClientId() {
        return clientId;
    }

    public String getTopic() {
        return topic;
    }

    public String getMessageId() {
        return messageId;
    }

    public Integer getPacketId() {
        return packetId;
    }

    public Integer getQos() {
        return qos;
    }

    public String getProtocolVersion() {
        return protocolVersion;
    }

    public String getBrokerAddress() {
        return brokerAddress;
    }

    public Map<String, Object> getAttributes() {
        return attributes;
    }

    public Map<String, Object> toMap() {
        Map<String, Object> map = new LinkedHashMap<>();
        if (StringUtils.isNotBlank(clientId)) {
            map.put("clientId", clientId);
        }
        if (StringUtils.isNotBlank(topic)) {
            map.put("topic", topic);
        }
        if (StringUtils.isNotBlank(messageId)) {
            map.put("messageId", messageId);
        }
        if (packetId != null) {
            map.put("packetId", packetId);
        }
        if (qos != null) {
            map.put("qos", qos);
        }
        if (StringUtils.isNotBlank(protocolVersion)) {
            map.put("protocolVersion", protocolVersion);
        }
        if (StringUtils.isNotBlank(brokerAddress)) {
            map.put("brokerAddress", brokerAddress);
        }
        if (!attributes.isEmpty()) {
            map.put("attributes", attributes);
        }
        return map;
    }

    public static final class Builder {
        private String clientId;
        private String topic;
        private String messageId;
        private Integer packetId;
        private Integer qos;
        private String protocolVersion;
        private String brokerAddress;
        private Map<String, Object> attributes;

        private Builder() {
        }

        public Builder clientId(String clientId) {
            this.clientId = clientId;
            return this;
        }

        public Builder topic(String topic) {
            this.topic = topic;
            return this;
        }

        public Builder messageId(String messageId) {
            this.messageId = messageId;
            return this;
        }

        public Builder packetId(Integer packetId) {
            this.packetId = packetId;
            return this;
        }

        public Builder qos(Integer qos) {
            this.qos = qos;
            return this;
        }

        public Builder protocolVersion(String protocolVersion) {
            this.protocolVersion = protocolVersion;
            return this;
        }

        public Builder brokerAddress(String brokerAddress) {
            this.brokerAddress = brokerAddress;
            return this;
        }

        public Builder putAttribute(String key, Object value) {
            if (StringUtils.isBlank(key)) {
                return this;
            }
            if (value == null) {
                return this;
            }
            if (attributes == null) {
                attributes = new LinkedHashMap<>();
            }
            attributes.put(key, value);
            return this;
        }

        public MqttErrorContext build() {
            return new MqttErrorContext(this);
        }
    }
}
