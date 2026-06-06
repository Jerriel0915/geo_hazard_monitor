package com.zwei.iot.broker.exception;

/**
 * MQTT 连接类异常分支。
 * <p>
 * 覆盖 MQTT 服务全生命周期中与连接建立、认证、安全握手、Broker 可用性相关的错误场景。
 */
public class MqttConnectionException extends MqttServiceException {

    protected MqttConnectionException(MqttErrorCode errorCode, MqttErrorContext context, String message) {
        super(errorCode, context, message);
    }

    protected MqttConnectionException(MqttErrorCode errorCode, MqttErrorContext context, String message, Throwable cause) {
        super(errorCode, context, message, cause);
    }

    /**
     * 连接超时异常。
     * <p>
     * 触发场景：TCP 连接/CONNECT 握手超时、心跳超时等导致连接无法建立或被关闭。
     * 必填：clientId（建议）、brokerAddress（建议）。
     */
    public static final class Timeout extends MqttConnectionException {
        public Timeout(MqttErrorContext context, String message) {
            super(MqttErrorCode.CONN_TIMEOUT, context, message);
        }

        public Timeout(MqttErrorContext context, String message, Throwable cause) {
            super(MqttErrorCode.CONN_TIMEOUT, context, message, cause);
        }
    }

    /**
     * 认证失败异常。
     * <p>
     * 触发场景：用户名/密码错误、账号被禁用、认证策略拒绝接入。
     * 必填：clientId、messageId（可选）。
     */
    public static final class AuthenticationFailed extends MqttConnectionException {
        public AuthenticationFailed(MqttErrorContext context, String message) {
            super(MqttErrorCode.CONN_AUTH_FAILED, context, message);
        }

        public AuthenticationFailed(MqttErrorContext context, String message, Throwable cause) {
            super(MqttErrorCode.CONN_AUTH_FAILED, context, message, cause);
        }
    }

    /**
     * TLS 握手失败异常。
     * <p>
     * 触发场景：服务端开启 TLS 时，证书校验、加密套件协商失败导致连接无法建立。
     * 必填：clientId（建议）、brokerAddress（建议）。
     */
    public static final class TlsHandshakeFailed extends MqttConnectionException {
        public TlsHandshakeFailed(MqttErrorContext context, String message) {
            super(MqttErrorCode.CONN_TLS_HANDSHAKE_FAILED, context, message);
        }

        public TlsHandshakeFailed(MqttErrorContext context, String message, Throwable cause) {
            super(MqttErrorCode.CONN_TLS_HANDSHAKE_FAILED, context, message, cause);
        }
    }

    /**
     * Broker 不可用异常。
     * <p>
     * 触发场景：Broker 进程不可用、网络不可达、连接被拒绝等。
     * 必填：brokerAddress（建议）。
     */
    public static final class BrokerUnavailable extends MqttConnectionException {
        public BrokerUnavailable(MqttErrorContext context, String message) {
            super(MqttErrorCode.CONN_BROKER_UNAVAILABLE, context, message);
        }

        public BrokerUnavailable(MqttErrorContext context, String message, Throwable cause) {
            super(MqttErrorCode.CONN_BROKER_UNAVAILABLE, context, message, cause);
        }
    }
}

