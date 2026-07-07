package com.zwei.iot.alarm.service.notify;

import com.zwei.common.core.domain.entity.SysUser;
import com.zwei.common.event.AlarmTriggeredEvent;
import com.zwei.common.event.DeviceOfflineEvent;
import com.zwei.iot.alarm.channel.AlarmChannelDispatcher;
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

import java.util.*;

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

    /** SYSTEM 渠道标识 — 站内消息，发送即达，创建时直接置为"已发送" */
    private static final String CHANNEL_SYSTEM = "SYSTEM";

    @Autowired
    private IAlarmRuleMatcher ruleMatcher;

    @Autowired
    private IAlarmRecipientResolver recipientResolver;

    @Autowired
    private IAlarmNotificationService notificationService;

    @Autowired
    private AlarmChannelDispatcher channelDispatcher;

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
        String alarmType = StringUtils.defaultIfBlank(event.getAlarmType(), "THRESHOLD");

        List<AlarmDispatchRule> rules = ruleMatcher.matchAlarmRules(
            event.getHazardPointId(),
            event.getAlarmLevel() == null ? null : String.valueOf(event.getAlarmLevel()),
            alarmType);

        if (rules == null || rules.isEmpty()) {
            log.debug("无匹配告警规则 alarmId={} type={}", event.getAlarmId(), alarmType);
            return;
        }

        boolean isComprehensive = "COMPREHENSIVE".equals(alarmType);
        String sourceType = isComprehensive ? "comprehensive" : "threshold";
        String typeName = isComprehensive ? "综合告警" : "阈值告警";
        String title = "[" + typeName + "] "
            + StringUtils.defaultString(event.getAlarmMessage(), "告警通知");
        String content = String.format("等级:%s | %s",
            event.getAlarmLevel(),
            StringUtils.defaultString(event.getAlarmMessage(), "-"));

        Collection<AlarmNotification> notifications = buildAndDedup(
            rules, sourceType, event.getAlarmId(), title, content);

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
                    // SYSTEM 渠道（站内消息）一定可达，默认"已发送"，避免与 SystemNotifyChannel.send() 之间的竞态窗口显示成"待发送"
                    n.setStatus(CHANNEL_SYSTEM.equals(channel)
                        ? AlarmNotification.STATUS_SENT
                        : AlarmNotification.STATUS_PENDING);
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

        List<AlarmNotification> list = new ArrayList<>(notifications);

        // 1) 主动去重: 查询 DB 中已存在的通知 (同一 sourceType + sourceId 组合), 筛掉重复 key
        //    避免静默期后重触发时因 uk_notif_dedup 整批失败导致通知丢失
        AlarmNotification first = list.get(0);
        Long sourceId = first.getSourceId();
        String sourceType = first.getSourceType();
        if (sourceId != null && sourceType != null) {
            List<AlarmNotification> existing = notificationService.selectByAlarmId(sourceId);
            if (existing != null && !existing.isEmpty()) {
                Set<String> existingKeys = new HashSet<>();
                for (AlarmNotification en : existing) {
                    if (sourceType.equals(en.getSourceType())) {
                        existingKeys.add(en.getSourceType() + "|" + en.getSourceId()
                                + "|" + en.getRecipientId() + "|" + en.getChannel());
                    }
                }
                list = new ArrayList<>();
                for (AlarmNotification n : notifications) {
                    String key = n.getSourceType() + "|" + n.getSourceId()
                            + "|" + n.getRecipientId() + "|" + n.getChannel();
                    if (!existingKeys.contains(key)) {
                        list.add(n);
                    }
                }
                if (list.isEmpty()) {
                    log.debug("通知全部重复已跳过 sourceId={} sourceType={}", sourceId, sourceType);
                    return;
                }
            }
        }

        // 2) 批量落库 (uk_notif_dedup 唯一键兜底并发)
        try {
            notificationService.batchCreate(list);
        } catch (DuplicateKeyException e) {
            log.warn("通知整批重复被忽略（并发冲突） sourceId={}",
                list.isEmpty() ? null : list.get(0).getSourceId(), e);
            return;
        }

        // 3) 逐条分发到渠道
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
