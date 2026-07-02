package com.zwei.common.event;

/**
 * MQTT 异常报文事件。
 * <p>
 * 当一条报文已通过 MQTT 认证（会话已建立）但在后续主题校验或解析/接入环节失败时，
 * 由 iot 模块发布此事件，log 模块异步监听并持久化到 mqtt_exception_log 表，
 * 供管理后台"服务状态 → 异常报文"子页查询与导出。
 * <p>
 * 放在 zwei-common 中使得 iot (发布方) 与 log (消费方) 互不产生直接模块依赖，
 * 与 {@link MqttMessageReceivedEvent} 采用相同的解耦设计。
 */
public class MqttMessageRejectEvent {

    private final String clientId;
    private final String username;
    private final Long deviceId;
    private final String topic;
    private final byte[] payload;
    private final long receiveTime;
    /** 失败阶段: TOPIC / FORMAT / STRATEGY / PARSE / UNKNOWN */
    private final String rejectStage;
    /** 报错内容（异常消息） */
    private final String rejectReason;
    /** 异常堆栈（截断后） */
    private final String errorStack;

    public MqttMessageRejectEvent(String clientId, String username, Long deviceId, String topic,
                                  byte[] payload, long receiveTime,
                                  String rejectStage, String rejectReason, String errorStack) {
        this.clientId = clientId;
        this.username = username;
        this.deviceId = deviceId;
        this.topic = topic;
        this.payload = payload;
        this.receiveTime = receiveTime;
        this.rejectStage = rejectStage;
        this.rejectReason = rejectReason;
        this.errorStack = errorStack;
    }

    public String getClientId() {
        return clientId;
    }

    public String getUsername() {
        return username;
    }

    public Long getDeviceId() {
        return deviceId;
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

    public String getRejectStage() {
        return rejectStage;
    }

    public String getRejectReason() {
        return rejectReason;
    }

    public String getErrorStack() {
        return errorStack;
    }
}
