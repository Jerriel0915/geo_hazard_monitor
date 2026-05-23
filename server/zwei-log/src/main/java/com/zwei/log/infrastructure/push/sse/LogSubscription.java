package com.zwei.log.infrastructure.push.sse;

import java.util.Set;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;
import com.zwei.log.domain.enums.LogType;

/**
 * SSE订阅
 *
 * @author zwei
 */
public class LogSubscription {

    private final SseEmitter emitter;
    private final Set<LogType> logTypes;
    private final String subscriberKey;

    public LogSubscription(SseEmitter emitter, Set<LogType> logTypes, String subscriberKey) {
        this.emitter = emitter;
        this.logTypes = logTypes;
        this.subscriberKey = subscriberKey;
    }

    public SseEmitter getEmitter() {
        return emitter;
    }

    public String getSubscriberKey() {
        return subscriberKey;
    }

    public boolean accepts(LogType logType) {
        return logTypes == null || logTypes.isEmpty() || logTypes.contains(logType);
    }
}
