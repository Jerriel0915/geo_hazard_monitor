package com.zwei.iot.broker.exception;

/**
 * MQTT 鉴权与服务侧通用错误码定义。
 * <p>
 * 错误码约定：
 * <ul>
 *   <li>前缀固定为 MQTT</li>
 *   <li>分支：CONN / COMM / PROTO / BIZ</li>
 *   <li>编号：三位数字，从 001 起</li>
 * </ul>
 * 用于日志采集、链路排障以及前端错误展示的稳定标识。
 */
public enum MqttErrorCode {
    CONN_TIMEOUT("MQTT-CONN-001"),
    CONN_AUTH_FAILED("MQTT-CONN-002"),
    CONN_TLS_HANDSHAKE_FAILED("MQTT-CONN-003"),
    CONN_BROKER_UNAVAILABLE("MQTT-CONN-004"),

    COMM_PUBLISH_FAILED("MQTT-COMM-001"),
    COMM_SUBSCRIBE_FAILED("MQTT-COMM-002"),
    COMM_RECEIVE_TIMEOUT("MQTT-COMM-003"),
    COMM_PAYLOAD_PARSE_FAILED("MQTT-COMM-004"),

    PROTO_MALFORMED_PACKET("MQTT-PROTO-001"),
    PROTO_PACKET_TOO_LARGE("MQTT-PROTO-002"),
    PROTO_QOS_NOT_SUPPORTED("MQTT-PROTO-003"),

    BIZ_PERMISSION_DENIED("MQTT-BIZ-001"),
    BIZ_INVALID_TOPIC("MQTT-BIZ-002"),
    BIZ_DUPLICATE_SUBSCRIPTION("MQTT-BIZ-003"),
    BIZ_DUPLICATE_CONSUME("MQTT-BIZ-004");

    private final String code;

    MqttErrorCode(String code) {
        this.code = code;
    }

    public String getCode() {
        return code;
    }
}

