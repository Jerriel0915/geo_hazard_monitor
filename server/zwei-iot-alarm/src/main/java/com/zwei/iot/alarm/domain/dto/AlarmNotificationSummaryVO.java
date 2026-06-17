package com.zwei.iot.alarm.domain.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.io.Serial;
import java.io.Serializable;

/**
 * 通知中心未读汇总 VO。
 *
 * @author zwei
 */
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AlarmNotificationSummaryVO implements Serializable {
    @Serial
    private static final long serialVersionUID = 1L;

    /** 当前用户未读事件数 */
    private int unreadCount;
    /** 查询时间戳 */
    private long timestamp;
}
