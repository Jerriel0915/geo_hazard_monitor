package com.zwei.common.redis;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.ValueOperations;

import java.time.Duration;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

@DisplayName("DistributedLock (Redis SETNX)")
class DistributedLockTest {

    private StringRedisTemplate redis;
    private ValueOperations<String, String> valueOps;
    private DistributedLock lock;

    @BeforeEach
    void setUp() {
        redis = mock(StringRedisTemplate.class);
        valueOps = mock(ValueOperations.class);
        when(redis.opsForValue()).thenReturn(valueOps);
        lock = new DistributedLock(redis);
    }

    @Test
    @DisplayName("首次获取锁返回 acquired=true 并写入随机 token")
    void firstAcquireSucceeds() {
        when(valueOps.setIfAbsent(eq("k1"), any(String.class), eq(Duration.ofSeconds(30))))
            .thenReturn(true);

        DistributedLock.LockToken token = lock.tryLock("k1", Duration.ofSeconds(30));

        assertThat(token.acquired()).isTrue();
        assertThat(token.value()).isNotBlank();
        verify(valueOps).setIfAbsent(eq("k1"), any(String.class), eq(Duration.ofSeconds(30)));
    }

    @Test
    @DisplayName("锁已被占用返回 acquired=false")
    void secondAcquireFails() {
        when(valueOps.setIfAbsent(eq("k1"), any(), any(Duration.class))).thenReturn(false);

        DistributedLock.LockToken token = lock.tryLock("k1", Duration.ofSeconds(30));

        assertThat(token.acquired()).isFalse();
        assertThat(token.value()).isNull();
    }

    @Test
    @DisplayName("释放锁时校验 token 一致才删除")
    void unlockWithMatchingToken() {
        when(valueOps.setIfAbsent(eq("k1"), any(), any(Duration.class))).thenReturn(true);
        when(valueOps.get("k1")).thenAnswer(inv -> tokenValue);
        when(redis.delete("k1")).thenReturn(true);

        DistributedLock.LockToken token = lock.tryLock("k1", Duration.ofSeconds(30));
        tokenValue = token.value(); // 模拟 Redis 中仍持有同一 token

        lock.unlock("k1", token);

        verify(redis, atMostOnce()).delete("k1");
    }

    private String tokenValue;

    @Test
    @DisplayName("token 不匹配时不删除 (TTL 已过期被他人覆盖)")
    void unlockWithMismatchedTokenSkipsDelete() {
        when(valueOps.setIfAbsent(eq("k1"), any(), any(Duration.class))).thenReturn(true);
        when(valueOps.get("k1")).thenReturn("other-owner-token");

        DistributedLock.LockToken token = lock.tryLock("k1", Duration.ofSeconds(30));

        lock.unlock("k1", token);

        verify(redis, never()).delete("k1");
    }

    @Test
    @DisplayName("未获取锁时调用 unlock 不抛异常且不触碰 Redis")
    void unlockOnFailedAcquireIsNoop() {
        DistributedLock.LockToken token = DistributedLock.LockToken.notAcquired();
        lock.unlock("k1", token); // 不应抛异常
        verifyNoInteractions(redis);
    }
}
