package com.zwei.log.infrastructure.push.sse;

import java.lang.reflect.Field;
import java.util.Collections;
import java.util.List;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;
import com.zwei.log.application.service.LogReplayService;
import com.zwei.log.domain.model.LogRuntimeRecord;
import com.zwei.log.infrastructure.config.LogModuleProperties;

class LogStreamPublisherTest {

    @Test
    void shouldRemoveSubscriptionWhenEmitterSendFailsWithDisconnectedClient() throws Exception {
        LogModuleProperties properties = new LogModuleProperties();
        LogReplayService replayService = Mockito.mock(LogReplayService.class);
        LogStreamPublisher publisher = new LogStreamPublisher(properties, replayService);
        SseEmitter emitter = Mockito.mock(SseEmitter.class);
        Mockito.doThrow(new IllegalStateException("Failed to send"))
            .when(emitter)
            .send(Mockito.any(SseEmitter.SseEventBuilder.class));

        addSubscription(publisher, new LogSubscription(emitter, Collections.emptySet(), "user:test"));

        LogRuntimeRecord record = new LogRuntimeRecord();
        record.setEventId(1001L);

        publisher.publish(record);

        Assertions.assertEquals(0, publisher.getActiveCount());
        Mockito.verify(emitter).completeWithError(Mockito.any(IllegalStateException.class));
        Mockito.verify(replayService).flushPendingCheckpoints("user:test");
        Mockito.verify(replayService, Mockito.never()).saveCheckpoint(Mockito.anyString(), Mockito.any(), Mockito.anyLong());
    }

    @Test
    void heartbeat_removesDisconnectedSubscription() throws Exception {
        LogModuleProperties properties = new LogModuleProperties();
        LogReplayService replayService = Mockito.mock(LogReplayService.class);
        LogStreamPublisher publisher = new LogStreamPublisher(properties, replayService);
        SseEmitter emitter = Mockito.mock(SseEmitter.class);
        Mockito.doThrow(new IllegalStateException("Failed to send"))
            .when(emitter)
            .send(Mockito.any(SseEmitter.SseEventBuilder.class));

        addSubscription(publisher, new LogSubscription(emitter, Collections.emptySet(), "user:test"));
        Assertions.assertEquals(1, publisher.getActiveCount());

        publisher.heartbeat();

        Assertions.assertEquals(0, publisher.getActiveCount());
        Mockito.verify(emitter).completeWithError(Mockito.any(IllegalStateException.class));
        Mockito.verify(replayService).flushPendingCheckpoints("user:test");
    }

    @SuppressWarnings("unchecked")
    private void addSubscription(LogStreamPublisher publisher, LogSubscription subscription) throws Exception {
        Field field = LogStreamPublisher.class.getDeclaredField("subscriptions");
        field.setAccessible(true);
        List<LogSubscription> subscriptions = (List<LogSubscription>) field.get(publisher);
        subscriptions.add(subscription);
    }
}
