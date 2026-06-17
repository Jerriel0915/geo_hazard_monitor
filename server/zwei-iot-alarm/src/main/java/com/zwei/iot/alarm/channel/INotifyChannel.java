package com.zwei.iot.alarm.channel;

import com.zwei.iot.alarm.domain.AlarmNotification;

/**
 * 通知渠道策略接口
 *
 * 实现类：
 *   - SystemNotifyChannel: SSE 推送 + 落库
 *   - SmsNotifyChannel:    阿里云 SMS
 *   - EmailNotifyChannel:  SMTP 邮件
 */
public interface INotifyChannel {

    /** 渠道标识：SYSTEM / SMS / EMAIL */
    String getChannel();

    /**
     * 实际发送 + 状态回写
     *
     * 实现内部应：
     *   - 成功：notificationService.markSent(id)
     *   - 失败：notificationService.markFailed(id, errorCode, description)
     */
    void send(AlarmNotification notification);
}
