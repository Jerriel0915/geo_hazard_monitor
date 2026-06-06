package com.zwei.iot.broker.exception;

/**
 * MQTT 协议类异常分支。
 * <p>
 * 覆盖不符合 MQTT 3.1.1/5.0 规范的报文、长度、QoS 等协议层错误场景。
 */
public class MqttProtocolException extends MqttServiceException {

    protected MqttProtocolException(MqttErrorCode errorCode, MqttErrorContext context, String message) {
        super(errorCode, context, message);
    }

    protected MqttProtocolException(MqttErrorCode errorCode, MqttErrorContext context, String message, Throwable cause) {
        super(errorCode, context, message, cause);
    }

    /**
     * 报文格式错误异常。
     * <p>
     * 触发场景：报文结构不合法、字段缺失、协议版本不匹配导致解析失败。
     * 必填：clientId（建议）、protocolVersion（建议）。
     */
    public static final class MalformedPacket extends MqttProtocolException {
        public MalformedPacket(MqttErrorContext context, String message) {
            super(MqttErrorCode.PROTO_MALFORMED_PACKET, context, message);
        }

        public MalformedPacket(MqttErrorContext context, String message, Throwable cause) {
            super(MqttErrorCode.PROTO_MALFORMED_PACKET, context, message, cause);
        }
    }

    /**
     * 报文长度超限异常。
     * <p>
     * 触发场景：单条报文超过服务端允许的最大长度（max-bytes-in-message 等）。
     * 必填：clientId（建议）、packetId（可选）。
     */
    public static final class PacketTooLarge extends MqttProtocolException {
        public PacketTooLarge(MqttErrorContext context, String message) {
            super(MqttErrorCode.PROTO_PACKET_TOO_LARGE, context, message);
        }

        public PacketTooLarge(MqttErrorContext context, String message, Throwable cause) {
            super(MqttErrorCode.PROTO_PACKET_TOO_LARGE, context, message, cause);
        }
    }

    /**
     * QoS 等级不支持异常。
     * <p>
     * 触发场景：客户端请求的 QoS 超出服务端可处理范围或与主题策略不兼容。
     * 必填：clientId（建议）、qos（建议）、topic（可选）。
     */
    public static final class QosNotSupported extends MqttProtocolException {
        public QosNotSupported(MqttErrorContext context, String message) {
            super(MqttErrorCode.PROTO_QOS_NOT_SUPPORTED, context, message);
        }

        public QosNotSupported(MqttErrorContext context, String message, Throwable cause) {
            super(MqttErrorCode.PROTO_QOS_NOT_SUPPORTED, context, message, cause);
        }
    }
}

