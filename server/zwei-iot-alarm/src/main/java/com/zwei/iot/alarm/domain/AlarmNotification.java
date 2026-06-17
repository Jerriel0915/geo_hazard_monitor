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

    // ===== 状态码常量 =====
    /** 待发送 */
    public static final int STATUS_PENDING                = 1;
    /** 已发送 */
    public static final int STATUS_SENT                   = 2;
    /** 发送失败 */
    public static final int STATUS_FAILED                 = 3;
    /** 接收人无效（电话/邮箱缺失或格式错误） */
    public static final int STATUS_INVALID_RECIPIENT      = 4;
    /** 渠道未配置 */
    public static final int STATUS_CHANNEL_NOT_CONFIGURED = 5;

    /**
     * 根据错误码推导 status
     *
     * @param errorCode 错误码 (如 RECIPIENT_PHONE_MISSING / CHANNEL_NOT_CONFIGURED)
     * @return 状态码常量
     */
    public static int statusFromErrorCode(String errorCode) {
        return switch (errorCode) {
            case "RECIPIENT_PHONE_MISSING", "RECIPIENT_PHONE_INVALID",
                 "RECIPIENT_EMAIL_MISSING", "RECIPIENT_EMAIL_INVALID" -> STATUS_INVALID_RECIPIENT;
            case "CHANNEL_NOT_CONFIGURED"                            -> STATUS_CHANNEL_NOT_CONFIGURED;
            default                                                  -> STATUS_FAILED;
        };
    }

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
     * 状态: 1=待发送 2=已发送 3=发送失败 4=接收人无效 5=渠道未配置
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
     * 已读时间
     */
    private Date readTime;
    /**
     * 来源类型: alarm / offline
     */
    private String sourceType;
    /**
     * 来源 ID（alarm_record.id 或 device.id）
     */
    private Long sourceId;
    /**
     * 创建时间
     */
    private Date createTime;
}
