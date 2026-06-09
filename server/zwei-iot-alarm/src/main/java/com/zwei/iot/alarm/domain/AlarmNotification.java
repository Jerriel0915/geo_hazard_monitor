package com.zwei.iot.alarm.domain;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.experimental.SuperBuilder;

import java.io.Serial;
import java.io.Serializable;
import java.util.Date;

/**
 * 告警通知记录表 alarm_notification
 *
 * @author zwei
 */
@Setter
@Getter
@SuperBuilder
@NoArgsConstructor
@AllArgsConstructor
public class AlarmNotification implements Serializable {
    @Serial
    private static final long serialVersionUID = 1L;

    /**
     * 主键ID
     */
    private Long id;
    /**
     * 告警记录ID
     */
    private Long alarmId;
    /**
     * 匹配的分发规则ID
     */
    private Long dispatchRuleId;
    /**
     * 接收人ID
     */
    private Long recipientId;
    /**
     * 接收人名称
     */
    private String recipientName;
    /**
     * 接收人电话
     */
    private String recipientPhone;
    /**
     * 通知渠道
     */
    private String channel;
    /**
     * 通知标题
     */
    private String title;
    /**
     * 通知内容
     */
    private String content;
    /**
     * 状态: 1=待发送 2=已发送 3=发送失败
     */
    private Integer status;
    /**
     * 发送时间
     */
    private Date sendTime;
    /**
     * 错误信息
     */
    private String errorMsg;
    /**
     * 创建时间
     */
    private Date createTime;
}
