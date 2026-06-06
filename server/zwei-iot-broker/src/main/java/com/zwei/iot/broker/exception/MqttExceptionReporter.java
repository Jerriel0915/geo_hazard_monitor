package com.zwei.iot.broker.exception;

import org.dromara.mica.mqtt.codec.MqttQoS;
import org.springframework.stereotype.Component;

/**
 * MQTT 异常收敛器。
 * <p>
 * 统一负责两类工作：
 * <ul>
 *   <li>构造标准化 {@link MqttErrorContext}</li>
 *   <li>输出结构化异常日志，并在 Broker 扩展点中返回统一拒绝结果</li>
 * </ul>
 * <p>
 * 该组件用于替代 MQTT 模块中分散的上下文拼装和日志输出逻辑，
 * 保证鉴权、订阅、发布等链路的错误结构保持一致。
 */
@Component
public class MqttExceptionReporter {

    /**
     * 创建基础上下文构造器。
     *
     * @param clientId 客户端标识
     * @param topic 主题
     * @param qoS QoS
     * @return 已填充基础字段的 builder
     */
    public MqttErrorContext.Builder context(String clientId, String topic, MqttQoS qoS) {
        return MqttErrorContext.builder()
                .clientId(clientId)
                .topic(topic)
                .qos(qoS == null ? null : (int) qoS.value());
    }

    /**
     * 创建仅含 clientId 的上下文构造器。
     *
     * @param clientId 客户端标识
     * @return 已填充 clientId 的 builder
     */
    public MqttErrorContext.Builder context(String clientId) {
        return MqttErrorContext.builder().clientId(clientId);
    }

    /**
     * 记录 debug 级异常日志，并返回拒绝结果。
     *
     * @param exception 业务异常
     * @return 固定返回 {@code false}
     */
    public boolean rejectWithDebug(MqttServiceException exception) {
        if (exception != null) {
            org.slf4j.LoggerFactory.getLogger(MqttExceptionReporter.class).debug("{}", exception.toLogString());
        }
        return false;
    }

    /**
     * 记录 warn 级异常日志，并返回拒绝结果。
     *
     * @param exception 业务异常
     * @return 固定返回 {@code false}
     */
    public boolean rejectWithWarn(MqttServiceException exception) {
        if (exception != null) {
            org.slf4j.LoggerFactory.getLogger(MqttExceptionReporter.class).warn("{}", exception.toLogString());
        }
        return false;
    }

    /**
     * 记录 error 级异常日志，并返回拒绝结果。
     *
     * @param exception 业务异常
     * @param cause 原始异常
     * @return 固定返回 {@code false}
     */
    public boolean rejectWithError(MqttServiceException exception, Throwable cause) {
        if (exception != null) {
            org.slf4j.LoggerFactory.getLogger(MqttExceptionReporter.class).error("{}", exception.toLogString(), cause);
        }
        return false;
    }
}

