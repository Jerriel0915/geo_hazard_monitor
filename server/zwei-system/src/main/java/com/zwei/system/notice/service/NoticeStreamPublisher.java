package com.zwei.system.notice.service;

import com.zwei.common.event.NoticeCreatedEvent;
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
 * 通知 SSE 推送器。
 * <p>
 * 复用 LogStreamPublisher 的 CopyOnWriteArrayList + SseEmitter 模式，
 * 将新建通知公告实时推送给所有已订阅的在线用户。
 */
@Component
public class NoticeStreamPublisher {

    private static final Logger log = LoggerFactory.getLogger(NoticeStreamPublisher.class);

    private final List<SseEmitter> emitters = new CopyOnWriteArrayList<>();

    private static final long SSE_TIMEOUT_MS = 300_000L;

    /**
     * 订阅通知 SSE 流。
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
            log.debug("SSE ready 事件发送失败，移除订阅: {}", e.getMessage());
        }
        log.debug("通知SSE订阅已建立，当前订阅数: {}", emitters.size());
        return emitter;
    }

    /**
     * 监听通知创建事件，推送给所有订阅者。
     */
    @EventListener
    public void onNoticeCreated(NoticeCreatedEvent event) {
        Map<String, Object> data = new LinkedHashMap<>();
        data.put("noticeId", event.getNoticeId());
        data.put("title", event.getTitle());
        data.put("content", event.getContent() != null ? stripHtml(event.getContent()) : "");
        data.put("type", event.getType());
        data.put("createTime", event.getCreateTime());

        int sent = 0;
        for (SseEmitter emitter : emitters) {
            try {
                emitter.send(SseEmitter.event().name("notice").data(data));
                sent++;
            } catch (IOException e) {
                emitters.remove(emitter);
                log.debug("SSE通知推送失败，移除订阅: {}", e.getMessage());
            }
        }
        log.debug("SSE通知已推送: noticeId={}, 目标={}/{}", event.getNoticeId(), sent, emitters.size());
    }

    private String stripHtml(String html) {
        return html.replaceAll("<[^>]*>", "").replaceAll("\\s+", " ").trim();
    }

    /** 当前活跃订阅数 */
    public int getActiveCount() {
        return emitters.size();
    }
}
