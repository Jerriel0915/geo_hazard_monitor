package com.zwei.log.infrastructure.push.sse;

import java.io.IOException;
import java.util.Collections;
import java.util.List;
import java.util.Set;
import java.util.concurrent.CopyOnWriteArrayList;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.web.context.request.async.AsyncRequestNotUsableException;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;
import com.zwei.log.application.service.LogReplayService;
import com.zwei.log.domain.enums.LogType;
import com.zwei.log.domain.model.AbstractLogRecord;
import com.zwei.log.infrastructure.config.LogModuleProperties;

/**
 * 日志SSE推送器
 *
 * @author zwei
 */
@Component
public class LogStreamPublisher {

    private static final Logger log = LoggerFactory.getLogger(LogStreamPublisher.class);
    private static final long HEARTBEAT_INTERVAL_MS = 25_000L;

    private final List<LogSubscription> subscriptions = new CopyOnWriteArrayList<>();
    private final LogModuleProperties properties;
    private final LogReplayService logReplayService;

    public LogStreamPublisher(LogModuleProperties properties, LogReplayService logReplayService) {
        this.properties = properties;
        this.logReplayService = logReplayService;
    }

    public SseEmitter subscribe(String subscriberKey, Set<LogType> logTypes, List<AbstractLogRecord> replayRecords, Long resumeEventId) {
        SseEmitter emitter = new SseEmitter(properties.getSseTimeoutMs());
        LogSubscription subscription = new LogSubscription(emitter, logTypes, subscriberKey);
        emitter.onCompletion(() -> removeSubscription(subscription));
        emitter.onTimeout(() -> removeSubscription(subscription));
        emitter.onError(error -> removeSubscription(subscription));
        try {
            emitter.send(SseEmitter.event()
                .name("ready")
                .reconnectTime(properties.getSseRetryMs())
                .data(Collections.singletonMap("resumeEventId", resumeEventId)));
            if (replayRecords != null) {
                for (AbstractLogRecord replayRecord : replayRecords) {
                    sendRecord(subscription, replayRecord, "replay");
                }
            }
        } catch (Exception ex) {
            if (!isDisconnectedClientException(ex)) {
                log.error("日志SSE ready发送非预期异常，移除订阅", ex);
            }
            failSubscription(subscription, ex);
            return emitter;
        }
        subscriptions.add(subscription);
        return emitter;
    }

    public void publish(AbstractLogRecord record) {
        for (LogSubscription subscription : subscriptions) {
            if (!subscription.accepts(record.getLogType())) {
                continue;
            }
            try {
                sendRecord(subscription, record, record.getLogType().name().toLowerCase());
            } catch (Exception ex) {
                if (!isDisconnectedClientException(ex)) {
                    log.error("日志SSE推送非预期异常，移除订阅", ex);
                }
                failSubscription(subscription, ex);
            }
        }
    }

    /**
     * 定时心跳：每 25s 向所有订阅 emitter 发送保活注释，防止 Nginx 60s 空闲超时断开连接；
     * 发送失败时复用 failSubscription 移除已断开的订阅。
     */
    @Scheduled(fixedDelay = HEARTBEAT_INTERVAL_MS)
    public void heartbeat() {
        for (LogSubscription subscription : subscriptions) {
            try {
                subscription.getEmitter().send(SseEmitter.event().comment("keep-alive"));
            } catch (Exception ex) {
                if (isDisconnectedClientException(ex)) {
                    log.debug("日志SSE心跳发送失败，移除订阅: {}", ex.getMessage());
                    failSubscription(subscription, ex);
                } else {
                    log.warn("日志SSE心跳非预期异常", ex);
                }
            }
        }
    }

    public int getActiveCount() {
        return subscriptions.size();
    }

    private void sendRecord(LogSubscription subscription, AbstractLogRecord record, String eventName) throws IOException {
        subscription.getEmitter().send(SseEmitter.event()
            .id(String.valueOf(record.getEventId()))
            .name(eventName)
            .reconnectTime(properties.getSseRetryMs())
            .data(record));
        logReplayService.saveCheckpoint(subscription.getSubscriberKey(), record.getLogType(), record.getEventId());
    }

    private void failSubscription(LogSubscription subscription, Exception ex) {
        try {
            subscription.getEmitter().completeWithError(ex);
        } catch (Exception ignored) {
            // Ignore secondary completion failures for already closed SSE emitters.
        }
        removeSubscription(subscription);
    }

    private boolean isDisconnectedClientException(Exception ex) {
        return ex instanceof IOException
            || ex instanceof IllegalStateException
            || ex instanceof AsyncRequestNotUsableException;
    }

    private void removeSubscription(LogSubscription subscription) {
        subscriptions.remove(subscription);
        logReplayService.flushPendingCheckpoints(subscription.getSubscriberKey());
    }
}
