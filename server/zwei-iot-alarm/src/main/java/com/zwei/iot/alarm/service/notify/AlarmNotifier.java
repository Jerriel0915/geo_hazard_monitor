package com.zwei.iot.alarm.service.notify;

import com.zwei.common.event.AlarmTriggeredEvent;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Service;

/**
 * 告警通知编排器 — 暂时桩化，Plan B 将接入新的通知规则系统。
 */
@Service
public class AlarmNotifier {

    private static final Logger log = LoggerFactory.getLogger(AlarmNotifier.class);

    @EventListener
    public void onAlarmTriggered(AlarmTriggeredEvent event) {
        log.debug("告警通知分发暂未接入新规则系统，跳过 alarmId={}", event.getAlarmId());
    }
}
