package com.zwei.log.infrastructure.push.sse;

import java.io.IOException;
import java.util.Collections;
import java.util.List;
import java.util.Set;
import java.util.concurrent.CopyOnWriteArrayList;
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
                throw rethrowSendException(ex);
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
                    throw rethrowSendException(ex);
                }
                failSubscription(subscription, ex);
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

    private RuntimeException rethrowSendException(Exception ex) {
        return ex instanceof RuntimeException runtimeException
            ? runtimeException
            : new IllegalStateException("Failed to publish SSE event", ex);
    }

    private void removeSubscription(LogSubscription subscription) {
        subscriptions.remove(subscription);
        logReplayService.flushPendingCheckpoints(subscription.getSubscriberKey());
    }
}
