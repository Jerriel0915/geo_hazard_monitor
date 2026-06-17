package com.zwei.iot.alarm.service.notify;

import com.zwei.common.event.AlarmTriggeredEvent;
import com.zwei.common.event.DeviceOfflineEvent;
import com.zwei.common.core.domain.entity.SysUser;
import com.zwei.iot.alarm.channel.NotifyChannelDispatcher;
import com.zwei.iot.alarm.dispatch.domain.AlarmDispatchRule;
import com.zwei.iot.alarm.dispatch.service.IAlarmRecipientResolver;
import com.zwei.iot.alarm.dispatch.service.IAlarmRuleMatcher;
import com.zwei.iot.alarm.domain.AlarmNotification;
import com.zwei.iot.alarm.service.IAlarmNotificationService;
import com.zwei.system.service.ISysUserService;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.event.EventListener;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Date;
import java.util.HashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * 告警通知编排器 — 双事件监听 + 用户×渠道去重。
 *
 * <p>监听 {@link AlarmTriggeredEvent}（告警触发）与 {@link DeviceOfflineEvent}（设备离线），
 * 通过 {@link IAlarmRuleMatcher} 匹配分发规则，经 {@link IAlarmRecipientResolver} 展开收件人，
 * 按 userId|channel 去重后批量入库并分发到各通知渠道。</p>
 */
@Slf4j
@Component
public class AlarmNotifier {

    @Autowired
    private IAlarmRuleMatcher ruleMatcher;

    @Autowired
    private IAlarmRecipientResolver recipientResolver;

    @Autowired
    private IAlarmNotificationService notificationService;

    @Autowired
    private NotifyChannelDispatcher channelDispatcher;

    @Autowired
    private ISysUserService userService;

    @EventListener
    @Async("alarmNotifyExecutor")
    public void onAlarmTriggered(AlarmTriggeredEvent event) {
        try {
            log.info("收到告警事件 alarmId={} hazardPointId={} level={}",
                event.getAlarmId(), event.getHazardPointId(), event.getAlarmLevel());
            dispatchForAlarm(event);
        } catch (Exception e) {
            log.error("告警通知处理失败 alarmId={}", event.getAlarmId(), e);
        }
    }

    @EventListener
    @Async("alarmNotifyExecutor")
    public void onDeviceOffline(DeviceOfflineEvent event) {
        try {
            log.info("收到设备离线事件 deviceId={}", event.getDeviceId());
            dispatchForOffline(event);
        } catch (Exception e) {
            log.error("离线通知处理失败 deviceId={}", event.getDeviceId(), e);
        }
    }

    private void dispatchForAlarm(AlarmTriggeredEvent event) {
        List<AlarmDispatchRule> rules = ruleMatcher.matchAlarmRules(
            event.getHazardPointId(),
            event.getAlarmLevel() == null ? null : String.valueOf(event.getAlarmLevel()));

        if (rules == null || rules.isEmpty()) {
            log.debug("无匹配告警规则 alarmId={}", event.getAlarmId());
            return;
        }

        String title = "[告警] " + StringUtils.defaultString(event.getAlarmType(), "告警通知");
        String content = String.format("等级:%s | %s",
            event.getAlarmLevel(),
            StringUtils.defaultString(event.getAlarmMessage(), "-"));

        Collection<AlarmNotification> notifications = buildAndDedup(
            rules, "alarm", event.getAlarmId(), title, content);

        dispatch(notifications);
    }

    private void dispatchForOffline(DeviceOfflineEvent event) {
        List<AlarmDispatchRule> rules = ruleMatcher.matchOfflineRules(event.getDeviceId());

        if (rules == null || rules.isEmpty()) {
            log.debug("无匹配离线规则 deviceId={}", event.getDeviceId());
            return;
        }

        String clientId = StringUtils.defaultString(event.getClientId(), "-");
        String title = "[设备离线] " + clientId;
        String content = String.format("设备(clientId=%s) | 原因:%s | 时间:%s",
            clientId,
            StringUtils.defaultString(event.getReason(), "-"),
            new Date());

        Collection<AlarmNotification> notifications = buildAndDedup(
            rules, "offline", event.getDeviceId(), title, content);

        dispatch(notifications);
    }

    private Collection<AlarmNotification> buildAndDedup(
            List<AlarmDispatchRule> rules,
            String sourceType,
            Long sourceId,
            String title,
            String content) {

        Map<String, AlarmNotification> dedup = new HashMap<>();

        for (AlarmDispatchRule rule : rules) {
            Set<Long> userIds = recipientResolver.resolveUserIds(rule.getId());
            if (userIds == null || userIds.isEmpty()) {
                continue;
            }

            Set<String> channels = parseChannels(rule.getChannels());
            if (channels.isEmpty()) {
                continue;
            }

            for (Long userId : userIds) {
                SysUser user = userService.selectUserById(userId);
                if (user == null) {
                    continue;
                }
                if ("1".equals(user.getStatus())) {
                    continue;   // 0=正常 1=停用
                }

                for (String channel : channels) {
                    String key = userId + "|" + channel;
                    if (dedup.containsKey(key)) {
                        continue;
                    }

                    AlarmNotification n = new AlarmNotification();
                    n.setSourceType(sourceType);
                    n.setSourceId(sourceId);
                    n.setAlarmId(sourceId);   // 兼容旧字段
                    n.setDispatchRuleId(rule.getId());
                    n.setRecipientId(userId);
                    n.setRecipientName(user.getUserName());
                    n.setRecipientPhone(user.getPhonenumber());
                    n.setChannel(channel);
                    n.setTitle(title);
                    n.setContent(content);
                    n.setStatus(AlarmNotification.STATUS_PENDING);
                    dedup.put(key, n);
                }
            }
        }
        return dedup.values();
    }

    private void dispatch(Collection<AlarmNotification> notifications) {
        if (notifications.isEmpty()) {
            return;
        }

        // 1) 批量落库 (uk_notif_dedup 唯一键兜底)
        List<AlarmNotification> list = new ArrayList<>(notifications);
        try {
            notificationService.batchCreate(list);
        } catch (DuplicateKeyException e) {
            log.warn("通知整批重复被忽略（事件已处理） sourceId={}",
                list.isEmpty() ? null : list.get(0).getSourceId(), e);
            return;
        }

        // 2) 逐条分发到渠道
        for (AlarmNotification n : list) {
            try {
                channelDispatcher.dispatch(n);
            } catch (Exception e) {
                log.error("通知分发失败 notifId={} channel={}",
                    n.getId(), n.getChannel(), e);
                notificationService.markFailed(n.getId(), "UNKNOWN",
                    "分发异常: " + e.getClass().getSimpleName()
                    + ": " + e.getMessage());
            }
        }
    }

    private Set<String> parseChannels(String channelsCsv) {
        if (StringUtils.isBlank(channelsCsv)) {
            return Collections.emptySet();
        }
        Set<String> set = new LinkedHashSet<>();
        for (String c : channelsCsv.split(",")) {
            if (StringUtils.isNotBlank(c)) {
                set.add(c.trim());
            }
        }
        return set;
    }
}
