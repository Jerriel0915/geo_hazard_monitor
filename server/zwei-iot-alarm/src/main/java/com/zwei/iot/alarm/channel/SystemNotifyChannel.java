package com.zwei.iot.alarm.channel;

import com.zwei.iot.alarm.domain.AlarmNotification;
import com.zwei.iot.alarm.service.IAlarmNotificationService;
import com.zwei.iot.alarm.service.notify.AlarmStreamPublisher;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

/**
 * SYSTEM 渠道：SSE 推送 + 落库
 *
 * - 不校验接收人（站内消息一定可达）
 * - 用户不在线也算发送成功（read_time 在用户点击消息中心"标记已读"时更新）
 */
@Slf4j
@Component
public class SystemNotifyChannel implements INotifyChannel {

    @Autowired private AlarmStreamPublisher alarmStreamPublisher;
    @Autowired private IAlarmNotificationService notificationService;

    @Override
    public String getChannel() {
        return "SYSTEM";
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void send(AlarmNotification n) {
        try {
            // 1. SSE 定向推送到接收人（publishToUser 按 userId 路由，避免跨用户广播泄漏）
            //    即使推送失败（用户不在线），也算"已落库可查"，不影响状态
            alarmStreamPublisher.publishToUser(n.getRecipientId(), "alarm-notify", buildPayloadMap(n));

            // 2. 标记为已发送
            notificationService.markSent(n.getId());

        } catch (Exception e) {
            log.error("SYSTEM 渠道发送失败 notifId={} recipientId={}",
                n.getId(), n.getRecipientId(), e);
            notificationService.markFailed(n.getId(), "UNKNOWN",
                "SSE 推送异常: " + e.getClass().getSimpleName()
                + ": " + e.getMessage());
        }
    }

    /**
     * 构造 SSE 推送 payload（Map 形式，匹配 publishToUser 签名 + 前端 onMessage 约定）。
     */
    private java.util.Map<String, Object> buildPayloadMap(AlarmNotification n) {
        java.util.Map<String, Object> p = new java.util.LinkedHashMap<>();
        p.put("type", "alarm-notify");
        java.util.Map<String, Object> data = new java.util.LinkedHashMap<>();
        data.put("id", n.getId());
        data.put("sourceType", n.getSourceType());
        data.put("sourceId", n.getSourceId());
        data.put("title", n.getTitle());
        data.put("content", n.getContent());
        data.put("createTime", n.getCreateTime());
        data.put("recipientId", n.getRecipientId());
        p.put("data", data);
        return p;
    }
}
