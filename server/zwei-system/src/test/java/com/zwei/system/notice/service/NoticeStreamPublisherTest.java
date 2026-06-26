package com.zwei.system.notice.service;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

/**
 * NoticeStreamPublisher 单元测试 — 重点验证心跳移除断开连接的能力。
 */
class NoticeStreamPublisherTest {

    private NoticeStreamPublisher publisher;

    @BeforeEach
    void setUp() {
        publisher = new NoticeStreamPublisher();
    }

    @Test
    void subscribe_returnsEmitterAndRegistered() {
        SseEmitter emitter = publisher.subscribe();
        assertNotNull(emitter);
        assertEquals(1, publisher.getActiveCount());
    }

    @Test
    void heartbeat_removesEmitterThatThrowsIOException() {
        SseEmitter emitter = publisher.subscribe();
        assertEquals(1, publisher.getActiveCount());
        // 手动 complete emitter 模拟断开，后续 send 会抛 IllegalStateException
        emitter.complete();
        publisher.heartbeat();
        assertEquals(0, publisher.getActiveCount());
    }

    @Test
    void heartbeat_multipleEmitters_removesOnlyDisconnected() {
        SseEmitter alive = publisher.subscribe();
        SseEmitter dead = publisher.subscribe();
        dead.complete();
        publisher.heartbeat();
        // 只有断开的 dead 被移除，alive 仍保留
        assertEquals(1, publisher.getActiveCount());
    }
}
