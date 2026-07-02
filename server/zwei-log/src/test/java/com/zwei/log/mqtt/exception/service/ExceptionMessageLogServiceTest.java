package com.zwei.log.mqtt.exception.service;

import com.zwei.common.event.MqttMessageRejectEvent;
import com.zwei.log.infrastructure.persistence.mysql.ExceptionLogMapper;
import com.zwei.log.mqtt.exception.domain.ExceptionMessageLog;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.Mockito;

import java.nio.charset.StandardCharsets;

import static org.junit.jupiter.api.Assertions.*;

class ExceptionMessageLogServiceTest {

    private static String repeat(String s, int n) {
        StringBuilder sb = new StringBuilder(n * s.length());
        for (int i = 0; i < n; i++) sb.append(s);
        return sb.toString();
    }

    private static MqttMessageRejectEvent event(String payload, String reason, String stack) {
        byte[] bytes = payload != null ? payload.getBytes(StandardCharsets.UTF_8) : null;
        return new MqttMessageRejectEvent(
                "client-001", "NZMX40", 42L, "sys/v1/DEV001/SENS01/updata",
                bytes, System.currentTimeMillis(), "PARSE", reason, stack);
    }

    @Test
    void shouldPersistRejectEventAndTruncateLongPayload() {
        ExceptionLogMapper mapper = Mockito.mock(ExceptionLogMapper.class);
        ExceptionMessageLogService service = new ExceptionMessageLogService(mapper);

        String longPayload = repeat("a", 800);
        String longReason = repeat("b", 800);
        String longStack = repeat("c", 3000);

        service.onMqttMessageReject(event(longPayload, longReason, longStack));

        ArgumentCaptor<ExceptionMessageLog> captor = ArgumentCaptor.forClass(ExceptionMessageLog.class);
        Mockito.verify(mapper, Mockito.times(1)).insert(captor.capture());
        ExceptionMessageLog saved = captor.getValue();

        assertEquals(42L, saved.getDeviceId());
        assertEquals("client-001", saved.getClientId());
        assertEquals("NZMX40", saved.getUsername());
        assertEquals("sys/v1/DEV001/SENS01/updata", saved.getTopic());
        assertEquals("PARSE", saved.getRejectStage());
        assertEquals(800, saved.getPayloadSize());
        // payload truncated to 500 + "..."
        assertNotNull(saved.getPayload());
        assertEquals(503, saved.getPayload().length());
        assertTrue(saved.getPayload().endsWith("..."));
        // reason truncated to 500 + "..."
        assertEquals(503, saved.getRejectReason().length());
        assertTrue(saved.getRejectReason().endsWith("..."));
        // stack truncated to 2000 + "..."
        assertEquals(2003, saved.getErrorStack().length());
        assertTrue(saved.getErrorStack().endsWith("..."));
    }

    @Test
    void shouldHandleNullPayload() {
        ExceptionLogMapper mapper = Mockito.mock(ExceptionLogMapper.class);
        ExceptionMessageLogService service = new ExceptionMessageLogService(mapper);

        service.onMqttMessageReject(event(null, "short reason", null));

        ArgumentCaptor<ExceptionMessageLog> captor = ArgumentCaptor.forClass(ExceptionMessageLog.class);
        Mockito.verify(mapper).insert(captor.capture());
        ExceptionMessageLog saved = captor.getValue();

        assertEquals(0, saved.getPayloadSize());
        assertNull(saved.getPayload());
        assertNull(saved.getErrorStack());
        assertEquals("short reason", saved.getRejectReason());
    }

    @Test
    void shouldNotThrowWhenPersistFails() {
        ExceptionLogMapper mapper = Mockito.mock(ExceptionLogMapper.class);
        Mockito.doThrow(new RuntimeException("DB down"))
                .when(mapper).insert(Mockito.any());
        ExceptionMessageLogService service = new ExceptionMessageLogService(mapper);

        // 不应抛出异常（仅记录日志）
        assertDoesNotThrow(() -> service.onMqttMessageReject(event("payload", "reason", "stack")));
        Mockito.verify(mapper, Mockito.times(1)).insert(Mockito.any());
    }
}
