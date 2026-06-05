package com.zwei.log.task;

import com.zwei.common.core.redis.RedisCache;
import com.zwei.log.infrastructure.config.LogModuleProperties;
import com.zwei.log.infrastructure.persistence.mysql.AuthLogMapper;
import com.zwei.log.infrastructure.persistence.mysql.LogStreamCheckpointMapper;
import com.zwei.log.infrastructure.persistence.mysql.OperationLogMapper;
import com.zwei.log.infrastructure.persistence.mysql.RuntimeLogMapper;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

class LogCleanupTaskTest {

    @Test
    void shouldDeleteLogsInBatches() {
        OperationLogMapper operationLogMapper = Mockito.mock(OperationLogMapper.class);
        AuthLogMapper authLogMapper = Mockito.mock(AuthLogMapper.class);
        RuntimeLogMapper runtimeLogMapper = Mockito.mock(RuntimeLogMapper.class);
        LogStreamCheckpointMapper checkpointMapper = Mockito.mock(LogStreamCheckpointMapper.class);
        LogModuleProperties properties = new LogModuleProperties();
        properties.setCleanupRetentionDays(30);
        properties.setCleanupCheckpointRetentionDays(30);
        properties.setCleanupBatchSize(1000);
        RedisCache redisCache = Mockito.mock(RedisCache.class);
        Mockito.when(redisCache.getCacheObject(Mockito.anyString())).thenReturn(null);
        LogCleanupTask task = new LogCleanupTask(operationLogMapper, authLogMapper, runtimeLogMapper, checkpointMapper, properties, redisCache);

        Mockito.when(operationLogMapper.deleteBefore(Mockito.any(), Mockito.eq(1000))).thenReturn(1000, 1000, 20);
        Mockito.when(authLogMapper.deleteBefore(Mockito.any(), Mockito.eq(1000))).thenReturn(15);
        Mockito.when(runtimeLogMapper.deleteBefore(Mockito.any(), Mockito.eq(1000))).thenReturn(0);
        Mockito.when(checkpointMapper.deleteBefore(Mockito.any(), Mockito.eq(1000))).thenReturn(1000, 5);

        task.cleanExpiredLogs();

        Mockito.verify(operationLogMapper, Mockito.times(3)).deleteBefore(Mockito.any(), Mockito.eq(1000));
        Mockito.verify(authLogMapper, Mockito.times(1)).deleteBefore(Mockito.any(), Mockito.eq(1000));
        Mockito.verify(runtimeLogMapper, Mockito.times(1)).deleteBefore(Mockito.any(), Mockito.eq(1000));
        Mockito.verify(checkpointMapper, Mockito.times(2)).deleteBefore(Mockito.any(), Mockito.eq(1000));
    }
}
