package com.zwei.iot.alarm.service.notify;

import com.zwei.common.event.AlarmTriggeredEvent;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import java.io.IOException;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CopyOnWriteArrayList;

/**
 * 告警 SSE 实时推送器。
 * <p>
 * 复用 {@code NoticeStreamPublisher} 的 CopyOnWriteArrayList + SseEmitter 模式，
 * 监听 {@link AlarmTriggeredEvent} 并实时推送给所有已订阅的前端用户。
 *
 * @author zwei
 */
@Component
public class AlarmStreamPublisher {

    private static final Logger log = LoggerFactory.getLogger(AlarmStreamPublisher.class);

    private final List<SseEmitter> emitters = new CopyOnWriteArrayList<>();

    private static final long SSE_TIMEOUT_MS = 300_000L;

    /**
     * 订阅告警 SSE 流。
     */
    public SseEmitter subscribe() {
        SseEmitter emitter = new SseEmitter(SSE_TIMEOUT_MS);
        emitters.add(emitter);
        emitter.onCompletion(() -> emitters.remove(emitter));
        emitter.onTimeout(() -> emitters.remove(emitter));
        emitter.onError(e -> emitters.remove(emitter));
        try {
            Map<String, String> ready = new LinkedHashMap<>();
            ready.put("type", "ready");
            ready.put("message", "connected");
            emitter.send(SseEmitter.event().name("ready").data(ready));
        } catch (IOException e) {
            emitters.remove(emitter);
            log.debug("告警SSE ready事件发送失败: {}", e.getMessage());
        }
        log.debug("告警SSE订阅已建立，当前订阅数: {}", emitters.size());
        return emitter;
    }

    /**
     * 监听告警触发事件，实时推送给所有订阅者。
     */
    @EventListener
    public void onAlarmTriggered(AlarmTriggeredEvent event) {
        Map<String, Object> data = new LinkedHashMap<>();
        data.put("alarmId", event.getAlarmId());
        data.put("hazardPointId", event.getHazardPointId());
        data.put("alarmLevel", event.getAlarmLevel());
        data.put("alarmType", event.getAlarmType());
        data.put("alarmMessage", event.getAlarmMessage());

        int sent = 0;
        for (SseEmitter emitter : emitters) {
            try {
                emitter.send(SseEmitter.event().name("alarm").data(data));
                sent++;
            } catch (IOException e) {
                emitters.remove(emitter);
                log.debug("告警SSE推送失败，移除订阅: {}", e.getMessage());
            }
        }
        log.debug("告警SSE已推送: alarmId={}, 目标={}/{}", event.getAlarmId(), sent, emitters.size());
    }

    /**
     * 当前活跃订阅数
     */
    public int getActiveCount() {
        return emitters.size();
    }
}
