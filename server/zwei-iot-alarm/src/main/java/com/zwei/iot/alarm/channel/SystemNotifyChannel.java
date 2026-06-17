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
            // 1. SSE 实时推送（当前为全量广播，前端按接收人过滤）
            //    即使推送失败（用户不在线），也算"已落库可查"，不影响状态
            alarmStreamPublisher.publish("alarm-notify", buildPayload(n));

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

    private SsePayload buildPayload(AlarmNotification n) {
        SsePayload p = new SsePayload();
        p.setType("alarm-notify");
        p.setData(new SsePayload.Data(
            n.getId(),
            n.getSourceType(),
            n.getSourceId(),
            n.getTitle(),
            n.getContent(),
            n.getCreateTime(),
            n.getRecipientId()
        ));
        return p;
    }

    /** SSE 消息体（与前端 layout/index.vue onMessage 约定） */
    @lombok.Data
    @lombok.AllArgsConstructor
    @lombok.NoArgsConstructor
    public static class SsePayload {
        private String type;
        private Data data;

        @lombok.Data
        @lombok.AllArgsConstructor
        @lombok.NoArgsConstructor
        public static class Data {
            private Long id;
            private String sourceType;
            private Long sourceId;
            private String title;
            private String content;
            private java.util.Date createTime;
            /** 接收人 ID（前端按此过滤） */
            private Long recipientId;
        }
    }
}
