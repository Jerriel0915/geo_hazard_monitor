package com.zwei.iot.alarm.channel;

import com.zwei.iot.alarm.domain.AlarmNotification;
import com.zwei.iot.alarm.service.IAlarmNotificationService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * 渠道路由分发器：按 channel 字符串路由到对应 INotifyChannel 实现
 */
@Component
public class NotifyChannelDispatcher {

    private final Map<String, INotifyChannel> channelMap;
    private final IAlarmNotificationService notificationService;

    @Autowired
    public NotifyChannelDispatcher(
            List<INotifyChannel> channels,
            IAlarmNotificationService notificationService) {
        this.notificationService = notificationService;
        this.channelMap = channels.stream()
            .collect(Collectors.toMap(INotifyChannel::getChannel, c -> c));
    }

    public void dispatch(AlarmNotification n) {
        INotifyChannel ch = channelMap.get(n.getChannel());
        if (ch == null) {
            notificationService.markFailed(n.getId(), "UNKNOWN_CHANNEL",
                "未知渠道: " + n.getChannel());
            return;
        }
        ch.send(n);
    }
}
