package com.zwei.log.mqtt.exception.domain;

import lombok.Data;

import java.util.Date;

/**
 * MQTT 异常报文日志条目。
 * <p>
 * 记录已通过 MQTT 认证但在主题校验或解析/接入环节失败的报文元数据，
 * 用于服务状态页"异常报文"子页查询、排查与导出。
 *
 * @author zwei
 */
@Data
public class ExceptionMessageLog {
    private Long id;
    /** 报文接收时间（毫秒精度） */
    private Date receiveTime;
    private String clientId;
    private String username;
    private Long deviceId;
    private String topic;
    /** 报文内容（截断 500 字符） */
    private String payload;
    private Integer payloadSize;
    /** 失败阶段: TOPIC / FORMAT / STRATEGY / PARSE / UNKNOWN */
    private String rejectStage;
    /** 报错内容（异常消息，截断 500 字符） */
    private String rejectReason;
    /** 异常堆栈（截断 2000 字符） */
    private String errorStack;
    private Date createTime;
}
