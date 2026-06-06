package com.zwei.iot.broker.exception;

/**
 * MQTT 业务处理类异常分支。
 * <p>
 * 覆盖权限控制、主题合法性、重复订阅、消息幂等去重等业务处理链路中的错误场景。
 */
public class MqttBusinessException extends MqttServiceException {

    protected MqttBusinessException(MqttErrorCode errorCode, MqttErrorContext context, String message) {
        super(errorCode, context, message);
    }

    protected MqttBusinessException(MqttErrorCode errorCode, MqttErrorContext context, String message, Throwable cause) {
        super(errorCode, context, message, cause);
    }

    /**
     * 权限不足异常。
     * <p>
     * 触发场景：设备发布/订阅越权，或访问未授权主题。
     * 必填：clientId（建议）、topic（建议）。
     */
    public static final class PermissionDenied extends MqttBusinessException {
        public PermissionDenied(MqttErrorContext context, String message) {
            super(MqttErrorCode.BIZ_PERMISSION_DENIED, context, message);
        }

        public PermissionDenied(MqttErrorContext context, String message, Throwable cause) {
            super(MqttErrorCode.BIZ_PERMISSION_DENIED, context, message, cause);
        }
    }

    /**
     * 主题非法异常。
     * <p>
     * 触发场景：topic 不符合约定（如 sys/gb 主题规范）、包含非法字符、层级不合法等。
     * 必填：topic（建议）、clientId（建议）。
     */
    public static final class InvalidTopic extends MqttBusinessException {
        public InvalidTopic(MqttErrorContext context, String message) {
            super(MqttErrorCode.BIZ_INVALID_TOPIC, context, message);
        }

        public InvalidTopic(MqttErrorContext context, String message, Throwable cause) {
            super(MqttErrorCode.BIZ_INVALID_TOPIC, context, message, cause);
        }
    }

    /**
     * 重复订阅异常。
     * <p>
     * 触发场景：客户端对同一 topicFilter 重复订阅且不允许覆盖/重复登记。
     * 必填：clientId（建议）、topic（建议）。
     */
    public static final class DuplicateSubscription extends MqttBusinessException {
        public DuplicateSubscription(MqttErrorContext context, String message) {
            super(MqttErrorCode.BIZ_DUPLICATE_SUBSCRIPTION, context, message);
        }

        public DuplicateSubscription(MqttErrorContext context, String message, Throwable cause) {
            super(MqttErrorCode.BIZ_DUPLICATE_SUBSCRIPTION, context, message, cause);
        }
    }

    /**
     * 消息重复消费异常。
     * <p>
     * 触发场景：幂等键命中、重复投递导致同一业务消息被重复处理。
     * 必填：messageId（建议）、clientId（可选）、topic（可选）。
     */
    public static final class DuplicateConsume extends MqttBusinessException {
        public DuplicateConsume(MqttErrorContext context, String message) {
            super(MqttErrorCode.BIZ_DUPLICATE_CONSUME, context, message);
        }

        public DuplicateConsume(MqttErrorContext context, String message, Throwable cause) {
            super(MqttErrorCode.BIZ_DUPLICATE_CONSUME, context, message, cause);
        }
    }
}

