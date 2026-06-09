package com.zwei.iot.alarm.service.notify;

import com.alibaba.fastjson2.JSON;
import com.alibaba.fastjson2.JSONArray;
import com.alibaba.fastjson2.JSONObject;
import com.zwei.common.event.AlarmTriggeredEvent;
import com.zwei.iot.alarm.domain.AlarmDispatchRule;
import com.zwei.iot.alarm.domain.AlarmNotification;
import com.zwei.iot.alarm.service.IAlarmDispatchService;
import com.zwei.iot.alarm.service.IAlarmNotificationService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Date;
import java.util.List;

/**
 * 告警通知编排器 — 监听 AlarmTriggeredEvent，匹配分发规则并创建通知记录。
 * <p>
 * SYSTEM 渠道通知由 {@link AlarmStreamPublisher} 通过 SSE 实时推送；
 * SMS/EMAIL 渠道创建通知记录（状态=待发送），预留后续对接第三方服务。
 *
 * @author zwei
 */
@Service
public class AlarmNotifier {

    private static final Logger log = LoggerFactory.getLogger(AlarmNotifier.class);

    private final IAlarmDispatchService dispatchService;
    private final IAlarmNotificationService notificationService;

    public AlarmNotifier(IAlarmDispatchService dispatchService,
                         IAlarmNotificationService notificationService) {
        this.dispatchService = dispatchService;
        this.notificationService = notificationService;
    }

    /**
     * 监听告警触发事件，执行通知分发。
     */
    @EventListener
    public void onAlarmTriggered(AlarmTriggeredEvent event) {
        try {
            dispatch(event);
        } catch (Exception e) {
            log.error("告警通知分发失败 alarmId={}", event.getAlarmId(), e);
        }
    }

    private void dispatch(AlarmTriggeredEvent event) {
        List<AlarmDispatchRule> rules = dispatchService.selectEnabledRules();
        if (rules.isEmpty()) {
            log.debug("无启用的分发规则，跳过通知创建 alarmId={}", event.getAlarmId());
            return;
        }

        List<AlarmNotification> notifications = new ArrayList<>();
        for (AlarmDispatchRule rule : rules) {
            if (!matches(event, rule)) {
                continue;
            }
            if (!isInTimeWindow(rule.getTimeWindow())) {
                continue;
            }
            notifications.addAll(buildNotifications(event, rule));
        }

        if (!notifications.isEmpty()) {
            notificationService.batchCreate(notifications);
            log.info("告警通知已创建: alarmId={}, 通知数={}", event.getAlarmId(), notifications.size());
        }
    }

    private boolean matches(AlarmTriggeredEvent event, AlarmDispatchRule rule) {
        // 检查告警等级匹配
        if (rule.getAlarmLevels() != null && !rule.getAlarmLevels().isEmpty()) {
            List<String> levels = Arrays.asList(rule.getAlarmLevels().split(","));
            if (!levels.contains(String.valueOf(event.getAlarmLevel()))) {
                return false;
            }
        }
        // 检查告警类型匹配
        if (rule.getAlarmTypes() != null && !rule.getAlarmTypes().isEmpty()) {
            List<String> types = Arrays.asList(rule.getAlarmTypes().split(","));
            if (!types.contains(event.getAlarmType())) {
                return false;
            }
        }
        return true;
    }

    private boolean isInTimeWindow(String timeWindow) {
        if (timeWindow == null || timeWindow.isEmpty()) {
            return true; // 全天
        }
        // 简单实现：暂不限制时间窗口，预留后续扩展
        return true;
    }

    private List<AlarmNotification> buildNotifications(AlarmTriggeredEvent event, AlarmDispatchRule rule) {
        List<AlarmNotification> list = new ArrayList<>();
        String[] channels = rule.getChannels() != null
                ? rule.getChannels().split(",") : new String[]{"SYSTEM"};
        List<Recipient> recipients = parseRecipients(rule.getRecipientsJson());

        for (String channel : channels) {
            String ch = channel.trim();
            if (recipients.isEmpty()) {
                // 无具体接收人 → 仅创建 SYSTEM 通道的广播通知
                if ("SYSTEM".equals(ch)) {
                    list.add(createNotification(event, rule, null, "SYSTEM"));
                }
            } else {
                for (Recipient r : recipients) {
                    list.add(createNotification(event, rule, r, ch));
                }
            }
        }
        return list;
    }

    private AlarmNotification createNotification(AlarmTriggeredEvent event, AlarmDispatchRule rule,
                                                 Recipient recipient, String channel) {
        return AlarmNotification.builder()
                .alarmId(event.getAlarmId())
                .dispatchRuleId(rule.getId())
                .recipientId(recipient != null ? recipient.userId : 0L)
                .recipientName(recipient != null ? recipient.name : "系统")
                .recipientPhone(recipient != null ? recipient.phone : null)
                .channel(channel)
                .title("告警通知: " + event.getAlarmMessage())
                .content(event.getAlarmMessage())
                .status("SYSTEM".equals(channel) ? 2 : 1) // SYSTEM 直接标记已发送，其他待发送
                .sendTime("SYSTEM".equals(channel) ? new Date() : null)
                .createTime(new Date())
                .build();
    }

    private List<Recipient> parseRecipients(String recipientsJson) {
        if (recipientsJson == null || recipientsJson.isEmpty()) {
            return new ArrayList<>();
        }
        try {
            List<Recipient> list = new ArrayList<>();
            JSONArray array = JSON.parseArray(recipientsJson);
            for (int i = 0; i < array.size(); i++) {
                JSONObject obj = array.getJSONObject(i);
                list.add(new Recipient(
                        obj.getLong("userId"),
                        obj.getString("name"),
                        obj.getString("phone")
                ));
            }
            return list;
        } catch (Exception e) {
            log.warn("解析接收人JSON失败: {}", recipientsJson);
            return new ArrayList<>();
        }
    }

    private record Recipient(Long userId, String name, String phone) {
    }
}
