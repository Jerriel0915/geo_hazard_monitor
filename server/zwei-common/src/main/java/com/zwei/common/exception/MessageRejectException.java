package com.zwei.common.exception;

/**
 * 报文拒绝异常 — 表示一条已认证报文在解析/接入环节被拒绝。
 * <p>
 * 携带 {@link #rejectStage} 标识失败阶段（TOPIC/FORMAT/STRATEGY/PARSE），
 * 由 {@code MonitorIngestFacade} 在同步解析路径抛出，
 * {@code MqttServerMessageListener} 捕获后提取阶段并发布
 * {@link com.zwei.common.event.MqttMessageRejectEvent}。
 *
 * @author zwei
 */
public class MessageRejectException extends RuntimeException {

    private static final long serialVersionUID = 1L;

    /** 失败阶段: TOPIC / FORMAT / STRATEGY / PARSE */
    private final String rejectStage;

    public MessageRejectException(String rejectStage, String reason) {
        super(reason);
        this.rejectStage = rejectStage;
    }

    public String getRejectStage() {
        return rejectStage;
    }
}
