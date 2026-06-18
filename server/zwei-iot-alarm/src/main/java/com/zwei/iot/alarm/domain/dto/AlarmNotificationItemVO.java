package com.zwei.iot.alarm.domain.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.io.Serial;
import java.io.Serializable;
import java.util.Date;

/**
 * 通知中心事件列表项 VO（前端展示用）。
 *
 * @author zwei
 */
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AlarmNotificationItemVO implements Serializable {
    @Serial
    private static final long serialVersionUID = 1L;

    /** 通知记录 ID */
    private Long id;
    /** 来源类型：alarm / offline */
    private String sourceType;
    /** 来源 ID（alarm_record.id 或 device.id） */
    private Long sourceId;
    /** 通知标题 */
    private String title;
    /** 通知正文 */
    private String content;
    /** 接收人名称 */
    private String recipientName;
    /** 已读时间（NULL=未读） */
    private Date readTime;
    /** 创建时间（事件时间） */
    private Date createTime;
}
