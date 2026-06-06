package com.zwei.iot.broker.exception;

/**
 * MQTT 通信类异常分支。
 * <p>
 * 覆盖消息发布/订阅、消息接收与 payload 解析等链路中的错误场景。
 */
public class MqttCommunicationException extends MqttServiceException {

    protected MqttCommunicationException(MqttErrorCode errorCode, MqttErrorContext context, String message) {
        super(errorCode, context, message);
    }

    protected MqttCommunicationException(MqttErrorCode errorCode, MqttErrorContext context, String message, Throwable cause) {
        super(errorCode, context, message, cause);
    }

    /**
     * 消息发布失败异常。
     * <p>
     * 触发场景：PUBLISH 发送失败、写入网络失败、Broker 拒绝发布等。
     * 必填：topic（建议）、clientId（建议）。
     */
    public static final class PublishFailed extends MqttCommunicationException {
        public PublishFailed(MqttErrorContext context, String message) {
            super(MqttErrorCode.COMM_PUBLISH_FAILED, context, message);
        }

        public PublishFailed(MqttErrorContext context, String message, Throwable cause) {
            super(MqttErrorCode.COMM_PUBLISH_FAILED, context, message, cause);
        }
    }

    /**
     * 消息订阅失败异常。
     * <p>
     * 触发场景：SUBSCRIBE 被拒绝、订阅请求失败、订阅参数不合法等。
     * 必填：topic（建议）、clientId（建议）。
     */
    public static final class SubscribeFailed extends MqttCommunicationException {
        public SubscribeFailed(MqttErrorContext context, String message) {
            super(MqttErrorCode.COMM_SUBSCRIBE_FAILED, context, message);
        }

        public SubscribeFailed(MqttErrorContext context, String message, Throwable cause) {
            super(MqttErrorCode.COMM_SUBSCRIBE_FAILED, context, message, cause);
        }
    }

    /**
     * 消息接收超时异常。
     * <p>
     * 触发场景：期待的消息在指定时间窗口内未到达，或 ACK/响应超时。
     * 必填：clientId（建议）、topic（可选）。
     */
    public static final class ReceiveTimeout extends MqttCommunicationException {
        public ReceiveTimeout(MqttErrorContext context, String message) {
            super(MqttErrorCode.COMM_RECEIVE_TIMEOUT, context, message);
        }

        public ReceiveTimeout(MqttErrorContext context, String message, Throwable cause) {
            super(MqttErrorCode.COMM_RECEIVE_TIMEOUT, context, message, cause);
        }
    }

    /**
     * Payload 解析失败异常。
     * <p>
     * 触发场景：payload 非法 JSON/二进制格式不匹配、字段缺失导致解析失败。
     * 必填：topic（建议）、messageId（可选）、packetId（可选）。
     */
    public static final class PayloadParseFailed extends MqttCommunicationException {
        public PayloadParseFailed(MqttErrorContext context, String message) {
            super(MqttErrorCode.COMM_PAYLOAD_PARSE_FAILED, context, message);
        }

        public PayloadParseFailed(MqttErrorContext context, String message, Throwable cause) {
            super(MqttErrorCode.COMM_PAYLOAD_PARSE_FAILED, context, message, cause);
        }
    }
}

