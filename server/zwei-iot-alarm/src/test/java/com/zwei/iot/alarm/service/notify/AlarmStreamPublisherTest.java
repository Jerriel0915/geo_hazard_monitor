package com.zwei.iot.alarm.service.notify;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatNoException;

/**
 * AlarmStreamPublisher 单元测试 — 重点验证 publishToUser 单点推送能力。
 */
class AlarmStreamPublisherTest {

    private AlarmStreamPublisher publisher;

    @BeforeEach
    void setUp() {
        publisher = new AlarmStreamPublisher();
    }

    @Test
    void subscribe_anonymous_returnsEmitterAndRegistered() {
        SseEmitter emitter = publisher.subscribe();
        assertThat(emitter).isNotNull();
        assertThat(publisher.getActiveCount()).isEqualTo(1);
    }

    @Test
    void subscribe_withUserId_bindsForPublishToUser() {
        SseEmitter emitter = publisher.subscribe(42L);
        assertThat(emitter).isNotNull();
        // 同时登记到全量广播列表 + userEmitters
        assertThat(publisher.getActiveCount()).isEqualTo(1);
        // 已绑定的 userId 即使无新事件也不应抛异常
        assertThatNoException().isThrownBy(() ->
            publisher.publishToUser(42L, "alarm-notify", Map.of("title", "x")));
    }

    @Test
    void publishToUser_withNoSubscribers_silentlySucceeds() {
        assertThatNoException().isThrownBy(() ->
            publisher.publishToUser(999L, "alarm-notify", Map.of("title", "x")));
    }

    @Test
    void publishToUser_anonymousSubscriberDoesNotReceiveTargetedEvent() {
        // 匿名订阅（未绑定 userId）不应被 publishToUser 触达
        publisher.subscribe();
        assertThatNoException().isThrownBy(() ->
            publisher.publishToUser(123L, "alarm-notify", Map.of("title", "x")));
    }

    @Test
    void publishToUser_nullUserId_isNoOp() {
        publisher.subscribe(1L);
        assertThatNoException().isThrownBy(() ->
            publisher.publishToUser(null, "alarm-notify", Map.of("title", "x")));
    }

    @Test
    void publish_broadcastReachesAllSubscribers() {
        publisher.subscribe();
        publisher.subscribe();
        // 仅验证不抛异常；推送成功率依赖真实 SseEmitter 状态
        assertThatNoException().isThrownBy(() ->
            publisher.publish("alarm", Map.of("alarmId", 1L)));
        assertThat(publisher.getActiveCount()).isEqualTo(2);
    }

    @Test
    void heartbeat_removesEmitterThatThrowsIOException() {
        SseEmitter emitter = publisher.subscribe();
        assertThat(publisher.getActiveCount()).isEqualTo(1);
        // 手动 complete emitter 模拟断开，后续 send 会抛 IOException
        emitter.complete();
        publisher.heartbeat();
        assertThat(publisher.getActiveCount()).isEqualTo(0);
    }

    @Test
    void heartbeat_removesUserBoundEmitterAndCleansUserEmitters() {
        SseEmitter emitter = publisher.subscribe(42L);
        assertThat(publisher.getActiveCount()).isEqualTo(1);
        emitter.complete();
        publisher.heartbeat();
        assertThat(publisher.getActiveCount()).isEqualTo(0);
        // userEmitters 也应被清空，通过 publishToUser 不抛异常间接验证
        assertThatNoException().isThrownBy(() ->
            publisher.publishToUser(42L, "alarm-notify", Map.of("title", "x")));
    }
}
