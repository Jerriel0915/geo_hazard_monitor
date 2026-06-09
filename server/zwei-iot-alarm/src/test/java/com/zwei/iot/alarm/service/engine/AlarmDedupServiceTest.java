package com.zwei.iot.alarm.service.engine;

import com.zwei.iot.alarm.config.AlarmProperties;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.core.ValueOperations;

import java.time.Duration;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

/**
 * AlarmDedupService 单元测试 — 覆盖持久计数、静默周期、边界场景。
 */
@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
@DisplayName("AlarmDedupService")
class AlarmDedupServiceTest {

    @Mock
    private RedisTemplate<Object, Object> redisTemplate;

    @Mock
    private ValueOperations<Object, Object> valueOps;

    private AlarmProperties properties;
    private AlarmDedupService dedupService;

    @BeforeEach
    void setUp() {
        lenient().when(redisTemplate.opsForValue()).thenReturn(valueOps);
        properties = new AlarmProperties();
        properties.setPreTriggerTtlSeconds(600);
        dedupService = new AlarmDedupService(redisTemplate, properties);
    }

    // ──────────── persistCount=1 (immediate trigger) ────────────

    @Nested
    @DisplayName("persistCount=1 立即触发")
    class ImmediateTrigger {

        @Test
        @DisplayName("首次调用直接返回 true")
        void firstCallReturnsTrue() {
            // persistCount=1, silencePeriod=0 → no Redis reads, direct trigger
            boolean result = dedupService.shouldTriggerAlarm(1L, 100L, 1, 1, 0);
            assertThat(result).isTrue();
            verify(valueOps).set(contains("last-trigger"), anyString(), any(Duration.class));
        }

        @Test
        @DisplayName("静默期内第二次调用返回 false")
        void silencePeriodBlocksRepeat() {
            long recentTs = System.currentTimeMillis() - 30_000L; // 30 sec ago, within 1 min silence
            when(valueOps.get(contains("last-trigger"))).thenReturn(String.valueOf(recentTs));

            boolean result = dedupService.shouldTriggerAlarm(1L, 100L, 1, 1, 1);
            assertThat(result).isFalse();
        }

        @Test
        @DisplayName("静默期过后重新触发")
        void silencePeriodExpired() {
            long oldTs = System.currentTimeMillis() - 120_000L; // 2 min ago, exceeds 1 min silence
            when(valueOps.get(contains("last-trigger"))).thenReturn(String.valueOf(oldTs));

            boolean result = dedupService.shouldTriggerAlarm(1L, 100L, 1, 1, 1);
            assertThat(result).isTrue();
            verify(valueOps).set(contains("last-trigger"), anyString(), any(Duration.class));
        }
    }

    // ──────────── persistCount > 1 (accumulated trigger) ────────────

    @Nested
    @DisplayName("persistCount>1 累加触发")
    class AccumulatedTrigger {

        @Test
        @DisplayName("persistCount=3, 第1次返回false, 第3次返回true")
        void thirdCallTriggers() {
            when(valueOps.increment(anyString())).thenReturn(1L, 2L, 3L);
            when(redisTemplate.expire(anyString(), any(Duration.class))).thenReturn(true);
            when(redisTemplate.delete(anyString())).thenReturn(true);

            assertThat(dedupService.shouldTriggerAlarm(1L, 100L, 1, 3, 0)).isFalse();
            assertThat(dedupService.shouldTriggerAlarm(1L, 100L, 1, 3, 0)).isFalse();
            assertThat(dedupService.shouldTriggerAlarm(1L, 100L, 1, 3, 0)).isTrue();
            verify(redisTemplate, atLeastOnce()).delete(anyString());
        }

        @Test
        @DisplayName("persistCount=3 但第3次处于静默期 → 返回 false")
        void thirdCallBlockedBySilence() {
            long recentTs = System.currentTimeMillis() - 30_000L;
            when(valueOps.increment(anyString())).thenReturn(3L);
            when(redisTemplate.expire(anyString(), any(Duration.class))).thenReturn(true);
            // silence check: last-trigger key exists with recent timestamp
            when(valueOps.get(contains("last-trigger"))).thenReturn(String.valueOf(recentTs));

            boolean result = dedupService.shouldTriggerAlarm(1L, 100L, 1, 3, 60);
            assertThat(result).isFalse();
        }
    }

    // ──────────── boundary cases ────────────

    @Nested
    @DisplayName("边界场景")
    class Boundary {

        @Test
        @DisplayName("silencePeriod=0 无静默限制")
        void zeroSilenceNoLimit() {
            boolean result = dedupService.shouldTriggerAlarm(1L, 100L, 1, 1, 0);
            assertThat(result).isTrue();
        }

        @Test
        @DisplayName("不同告警等级独立计数")
        void differentLevelsIndependentKeys() {
            when(valueOps.increment(anyString())).thenReturn(3L, 1L);
            when(redisTemplate.expire(anyString(), any(Duration.class))).thenReturn(true);
            when(redisTemplate.delete(anyString())).thenReturn(true);

            // level=1 达到 persistCount=3
            assertThat(dedupService.shouldTriggerAlarm(1L, 100L, 1, 3, 0)).isTrue();
            // level=2 仅第1次, persistCount=3 不触发
            assertThat(dedupService.shouldTriggerAlarm(1L, 100L, 2, 3, 0)).isFalse();

            verify(valueOps).increment(contains("pre-trigger:1:100:1"));
            verify(valueOps).increment(contains("pre-trigger:1:100:2"));
        }

        @Test
        @DisplayName("不同判据ID使用不同 key")
        void differentCriteriaKeys() {
            when(valueOps.increment(anyString())).thenReturn(1L, 3L);
            when(redisTemplate.expire(anyString(), any(Duration.class))).thenReturn(true);
            when(redisTemplate.delete(anyString())).thenReturn(true);

            dedupService.shouldTriggerAlarm(1L, 100L, 1, 3, 0);
            dedupService.shouldTriggerAlarm(2L, 100L, 1, 3, 0);

            verify(valueOps).increment(contains("pre-trigger:1:100"));
            verify(valueOps).increment(contains("pre-trigger:2:100"));
        }

        @Test
        @DisplayName("clearPreTrigger 删除 Redis key")
        void clearPreTriggerDeletesKey() {
            when(redisTemplate.delete(anyString())).thenReturn(true);

            dedupService.clearPreTrigger(1L, 100L, 1);

            verify(redisTemplate).delete(contains("pre-trigger"));
        }
    }
}
