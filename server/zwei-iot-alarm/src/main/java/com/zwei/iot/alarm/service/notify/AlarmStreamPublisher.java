package com.zwei.iot.alarm.service.notify;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArrayList;

/**
 * 告警 SSE 实时推送器。
 * <p>
 * 复用 {@code NoticeStreamPublisher} 的 CopyOnWriteArrayList + SseEmitter 模式，
 * 监听 {@link AlarmTriggeredEvent} 并实时推送给所有已订阅的前端用户。
 *
 * <p>支持两种推送模式：
 * <ul>
 *   <li>{@link #publish(String, Object)} —— 全量广播（兼容旧前端，按接收人/权限二次过滤）</li>
 *   <li>{@link #publishToUser(Long, String, Map)} —— 单点定向推送（需订阅时绑定 userId）</li>
 * </ul>
 *
 * @author zwei
 */
@Component
public class AlarmStreamPublisher {

    private static final Logger log = LoggerFactory.getLogger(AlarmStreamPublisher.class);

    /** 全量广播订阅列表（向后兼容旧前端） */
    private final List<SseEmitter> emitters = new CopyOnWriteArrayList<>();

    /** 按 userId 索引的订阅映射（同一用户可多端订阅） */
    private final Map<Long, List<SseEmitter>> userEmitters = new ConcurrentHashMap<>();

    /** 反向索引：emitter → userId，避免 removeEmitter 遍历所有 userEmitters.values() */
    private final Map<SseEmitter, Long> emitterUserId = new ConcurrentHashMap<>();

    private static final long SSE_TIMEOUT_MS = 300_000L;
    private static final long HEARTBEAT_INTERVAL_MS = 25_000L;

    /**
     * 订阅告警 SSE 流（未绑定 userId，仅参与全量广播）。
     */
    public SseEmitter subscribe() {
        SseEmitter emitter = new SseEmitter(SSE_TIMEOUT_MS);
        emitters.add(emitter);
        emitter.onCompletion(() -> removeEmitter(emitter));
        emitter.onTimeout(() -> removeEmitter(emitter));
        emitter.onError(e -> removeEmitter(emitter));
        sendReady(emitter);
        log.debug("告警SSE订阅已建立（匿名），当前订阅数: {}", emitters.size());
        return emitter;
    }

    /**
     * 订阅告警 SSE 流并绑定 userId（同时参与全量广播 + 定向推送）。
     */
    public SseEmitter subscribe(Long userId) {
        SseEmitter emitter = subscribe();
        if (userId != null) {
            userEmitters.computeIfAbsent(userId, k -> new CopyOnWriteArrayList<>()).add(emitter);
            emitterUserId.put(emitter, userId);
            log.debug("告警SSE订阅绑定 userId={}", userId);
        }
        return emitter;
    }

    /**
     * 监听告警触发事件，实时推送给所有订阅者。
     * @deprecated 告警事件已由 {@link AlarmNotifier} 监听，不再需要此方法。
     */
    // @EventListener
    // public void onAlarmTriggered(AlarmTriggeredEvent event) {
    //     Map<String, Object> data = new LinkedHashMap<>();
    //     data.put("alarmId", event.getAlarmId());
    //     data.put("hazardPointId", event.getHazardPointId());
    //     data.put("alarmLevel", event.getAlarmLevel());
    //     data.put("alarmType", event.getAlarmType());
    //     data.put("alarmMessage", event.getAlarmMessage());

    //     int sent = 0;
    //     for (SseEmitter emitter : emitters) {
    //         try {
    //             emitter.send(SseEmitter.event().name("alarm").data(data));
    //             sent++;
    //         } catch (IOException e) {
    //             emitters.remove(emitter);
    //             log.debug("告警SSE推送失败，移除订阅: {}", e.getMessage());
    //         }
    //     }
    //     log.debug("告警SSE已推送: alarmId={}, 目标={}/{}", event.getAlarmId(), sent, emitters.size());
    // }

    /**
     * 向所有 SSE 订阅者广播一条事件（用于通知推送等）。
     *
     * @param eventName SSE 事件名（前端用此字段区分类型）
     * @param data      事件数据
     */
    public void publish(String eventName, Object data) {
        int sent = 0;
        for (SseEmitter emitter : emitters) {
            try {
                emitter.send(SseEmitter.event().name(eventName).data(data));
                sent++;
            } catch (Exception e) {
                removeEmitter(emitter);
                log.debug("SSE 事件 [{}] 推送失败，移除订阅: {}", eventName, e.getMessage());
            }
        }
        log.debug("SSE 事件 [{}] 已推送: 目标={}/当前订阅={}", eventName, sent, emitters.size());
    }

    /**
     * 向指定用户推送事件（用于 SYSTEM 通知渠道定向投递）。
     * <p>
     * 若目标用户当前无在线订阅，事件已被落库到 alarm_notification 表，不会丢失；
     * 用户下次拉取 recent 接口即可看到。
     *
     * @param userId    接收用户 ID
     * @param eventType 事件类型（如 "alarm-notify"）
     * @param data      事件数据
     */
    public void publishToUser(Long userId, String eventType, Map<String, Object> data) {
        if (userId == null) {
            return;
        }
        List<SseEmitter> targets = userEmitters.get(userId);
        if (targets == null || targets.isEmpty()) {
            log.debug("publishToUser 目标 userId={} 无在线订阅，事件 {} 已落库不丢失", userId, eventType);
            return;
        }
        int sent = 0;
        for (SseEmitter emitter : targets) {
            try {
                emitter.send(SseEmitter.event().name(eventType).data(data));
                sent++;
            } catch (Exception e) {
                removeEmitter(emitter);
                log.debug("用户 {} SSE 推送失败，移除订阅: {}", userId, e.getMessage());
            }
        }
        log.debug("publishToUser userId={} event={} 推送 {}/{}", userId, eventType, sent, targets.size());
    }

    /**
     * 当前活跃订阅数（全量广播列表）。
     */
    public int getActiveCount() {
        return emitters.size();
    }

    /**
     * 定时心跳：每 25s 向所有 emitter 发送保活注释，防止 Nginx 60s 空闲超时断开连接；
     * 发送失败时移除已断开的 emitter。
     */
    @Scheduled(fixedDelay = HEARTBEAT_INTERVAL_MS)
    public void heartbeat() {
        for (SseEmitter emitter : emitters) {
            try {
                emitter.send(SseEmitter.event().comment("keep-alive"));
            } catch (Exception e) {
                removeEmitter(emitter);
                log.debug("告警SSE心跳发送失败，移除订阅: {}", e.getMessage());
            }
        }
    }

    // ============= private =============

    /**
     * 统一移除 emitter（同时从全量广播列表和 userId 绑定列表中移除）。
     * <p>使用反向索引 emitterUserId 实现 O(1) 查找，避免遍历所有 userEmitters.values()。
     */
    private void removeEmitter(SseEmitter emitter) {
        emitters.remove(emitter);
        Long userId = emitterUserId.remove(emitter);
        if (userId != null) {
            List<SseEmitter> list = userEmitters.get(userId);
            if (list != null) {
                list.remove(emitter);
                if (list.isEmpty()) {
                    userEmitters.remove(userId);
                }
            }
        }
    }

    private void sendReady(SseEmitter emitter) {
        try {
            Map<String, String> ready = new LinkedHashMap<>();
            ready.put("type", "ready");
            ready.put("message", "connected");
            emitter.send(SseEmitter.event().name("ready").data(ready));
        } catch (Exception e) {
            removeEmitter(emitter);
            log.debug("告警SSE ready事件发送失败: {}", e.getMessage());
        }
    }
}
