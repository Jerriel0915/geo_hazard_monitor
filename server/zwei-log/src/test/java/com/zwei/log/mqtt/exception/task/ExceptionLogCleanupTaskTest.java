package com.zwei.log.mqtt.exception.task;

import com.zwei.common.constant.CacheConstants;
import com.zwei.common.core.redis.RedisCache;
import com.zwei.log.infrastructure.persistence.mysql.ExceptionLogMapper;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.*;

class ExceptionLogCleanupTaskTest {

    @Test
    void shouldDeleteInBatchesWhenEnabled() {
        ExceptionLogMapper mapper = Mockito.mock(ExceptionLogMapper.class);
        RedisCache redisCache = Mockito.mock(RedisCache.class);
        // enabled=true, retention=60
        Mockito.when(redisCache.getCacheObject(CacheConstants.SYS_CONFIG_KEY + "mqtt.exception.cleanup.enabled"))
                .thenReturn("true");
        Mockito.when(redisCache.getCacheObject(CacheConstants.SYS_CONFIG_KEY + "mqtt.exception.retention-days"))
                .thenReturn("60");
        // 第一批删满 1000，第二批删 20 → 共两批
        Mockito.when(mapper.deleteBefore(any(), eq(1000))).thenReturn(1000, 20);

        ExceptionLogCleanupTask task = new ExceptionLogCleanupTask(mapper, redisCache);
        task.cleanExpiredLogs();

        Mockito.verify(mapper, Mockito.times(2)).deleteBefore(any(), eq(1000));
    }

    @Test
    void shouldSkipWhenDisabled() {
        ExceptionLogMapper mapper = Mockito.mock(ExceptionLogMapper.class);
        RedisCache redisCache = Mockito.mock(RedisCache.class);
        Mockito.when(redisCache.getCacheObject(CacheConstants.SYS_CONFIG_KEY + "mqtt.exception.cleanup.enabled"))
                .thenReturn("false");

        ExceptionLogCleanupTask task = new ExceptionLogCleanupTask(mapper, redisCache);
        task.cleanExpiredLogs();

        Mockito.verify(mapper, Mockito.never()).deleteBefore(any(), anyInt());
    }

    @Test
    void shouldFallbackToDefaultRetentionWhenConfigMissing() {
        ExceptionLogMapper mapper = Mockito.mock(ExceptionLogMapper.class);
        RedisCache redisCache = Mockito.mock(RedisCache.class);
        // config 全部缺失 → 默认 60 天、默认启用
        Mockito.when(redisCache.getCacheObject(anyString())).thenReturn(null);
        Mockito.when(mapper.deleteBefore(any(), eq(1000))).thenReturn(0);

        ExceptionLogCleanupTask task = new ExceptionLogCleanupTask(mapper, redisCache);
        task.cleanExpiredLogs();

        // 默认启用，应执行删除
        Mockito.verify(mapper, Mockito.times(1)).deleteBefore(any(), eq(1000));
    }
}
