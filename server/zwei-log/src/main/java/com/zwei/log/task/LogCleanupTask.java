package com.zwei.log.task;

import com.zwei.common.constant.CacheConstants;
import com.zwei.common.core.redis.RedisCache;
import com.zwei.log.infrastructure.config.LogModuleProperties;
import com.zwei.log.infrastructure.persistence.mysql.AuthLogMapper;
import com.zwei.log.infrastructure.persistence.mysql.LogStreamCheckpointMapper;
import com.zwei.log.infrastructure.persistence.mysql.OperationLogMapper;
import com.zwei.log.infrastructure.persistence.mysql.RuntimeLogMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.Date;

/**
 * 日志清理任务
 *
 * @author zwei
 */
@Component("logCleanupTask")
public class LogCleanupTask {

    private static final Logger log = LoggerFactory.getLogger(LogCleanupTask.class);

    private static final String CONFIG_KEY_ENABLED = "log.cleanup.enabled";
    private static final String CONFIG_KEY_RETENTION_DAYS = "log.cleanup.retention-days";

    private final OperationLogMapper operationLogMapper;
    private final AuthLogMapper authLogMapper;
    private final RuntimeLogMapper runtimeLogMapper;
    private final LogStreamCheckpointMapper checkpointMapper;
    private final LogModuleProperties properties;
    private final RedisCache redisCache;

    public LogCleanupTask(OperationLogMapper operationLogMapper,
        AuthLogMapper authLogMapper,
        RuntimeLogMapper runtimeLogMapper,
        LogStreamCheckpointMapper checkpointMapper,
                          LogModuleProperties properties,
                          RedisCache redisCache) {
        this.operationLogMapper = operationLogMapper;
        this.authLogMapper = authLogMapper;
        this.runtimeLogMapper = runtimeLogMapper;
        this.checkpointMapper = checkpointMapper;
        this.properties = properties;
        this.redisCache = redisCache;
    }

    public void cleanExpiredLogs() {
        if (!isCleanupEnabled()) {
            log.info("日志清理任务已通过系统配置禁用，跳过本次执行");
            return;
        }
        int batchSize = properties.getCleanupBatchSize();
        int retentionDays = getRetentionDays();
        Date logCutoffTime = toDate(LocalDateTime.now().minusDays(retentionDays));
        Date checkpointCutoffTime = toDate(LocalDateTime.now().minusDays(properties.getCleanupCheckpointRetentionDays()));

        int operationDeleted = deleteInBatches(limit -> operationLogMapper.deleteBefore(logCutoffTime, limit), batchSize);
        int authDeleted = deleteInBatches(limit -> authLogMapper.deleteBefore(logCutoffTime, limit), batchSize);
        int runtimeDeleted = deleteInBatches(limit -> runtimeLogMapper.deleteBefore(logCutoffTime, limit), batchSize);
        int checkpointDeleted = deleteInBatches(limit -> checkpointMapper.deleteBefore(checkpointCutoffTime, limit), batchSize);

        log.info("日志清理完成: operation={}, auth={}, runtime={}, checkpoint={}, retentionDays={}, checkpointRetentionDays={}, batchSize={}",
                operationDeleted, authDeleted, runtimeDeleted, checkpointDeleted, retentionDays,
            properties.getCleanupCheckpointRetentionDays(), batchSize);
    }

    private boolean isCleanupEnabled() {
        String val = redisCache.getCacheObject(CacheConstants.SYS_CONFIG_KEY + CONFIG_KEY_ENABLED);
        if (val == null) return true; // 配置不存在时默认启用
        return !"false".equalsIgnoreCase(val);
    }

    private int getRetentionDays() {
        String val = redisCache.getCacheObject(CacheConstants.SYS_CONFIG_KEY + CONFIG_KEY_RETENTION_DAYS);
        if (val != null) {
            try {
                return Integer.parseInt(val);
            } catch (NumberFormatException ignored) {
            }
        }
        return properties.getCleanupRetentionDays();
    }

    private int deleteInBatches(DeleteExecutor executor, int batchSize) {
        int total = 0;
        while (true) {
            int deleted = executor.delete(batchSize);
            total += deleted;
            if (deleted < batchSize) {
                return total;
            }
        }
    }

    private Date toDate(LocalDateTime dateTime) {
        return Date.from(dateTime.atZone(ZoneId.systemDefault()).toInstant());
    }

    @FunctionalInterface
    private interface DeleteExecutor {
        int delete(int limit);
    }
}
