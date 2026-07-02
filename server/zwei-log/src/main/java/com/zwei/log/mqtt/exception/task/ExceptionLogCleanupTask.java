package com.zwei.log.mqtt.exception.task;

import com.zwei.common.constant.CacheConstants;
import com.zwei.common.core.redis.RedisCache;
import com.zwei.log.infrastructure.persistence.mysql.ExceptionLogMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.Date;

/**
 * 异常报文定时清理任务。
 * <p>
 * 由 Quartz (sys_job invoke_target = {@code exceptionLogCleanupTask.cleanExpiredLogs()}) 触发，
 * 按 {@code mqtt.exception.retention-days} 配置（默认 60 天）清理 mqtt_exception_log 过期记录。
 * 读取 Redis 缓存的 sys_config 值，与 {@code LogCleanupTask} 采用相同的配置驱动模式。
 *
 * @author zwei
 */
@Component("exceptionLogCleanupTask")
public class ExceptionLogCleanupTask {

    private static final Logger log = LoggerFactory.getLogger(ExceptionLogCleanupTask.class);

    private static final String CONFIG_KEY_ENABLED = "mqtt.exception.cleanup.enabled";
    private static final String CONFIG_KEY_RETENTION_DAYS = "mqtt.exception.retention-days";

    private static final int DEFAULT_RETENTION_DAYS = 60;
    private static final int DEFAULT_BATCH_SIZE = 1000;

    private final ExceptionLogMapper exceptionLogMapper;
    private final RedisCache redisCache;

    public ExceptionLogCleanupTask(ExceptionLogMapper exceptionLogMapper, RedisCache redisCache) {
        this.exceptionLogMapper = exceptionLogMapper;
        this.redisCache = redisCache;
    }

    public void cleanExpiredLogs() {
        if (!isCleanupEnabled()) {
            log.info("异常报文清理任务已通过系统配置禁用，跳过本次执行");
            return;
        }
        int retentionDays = getRetentionDays();
        Date cutoffTime = toDate(LocalDateTime.now().minusDays(retentionDays));
        int batchSize = DEFAULT_BATCH_SIZE;

        int deleted = deleteInBatches(limit -> exceptionLogMapper.deleteBefore(cutoffTime, limit), batchSize);
        log.info("异常报文清理完成: deleted={}, retentionDays={}, batchSize={}", deleted, retentionDays, batchSize);
    }

    private boolean isCleanupEnabled() {
        String val = redisCache.getCacheObject(CacheConstants.SYS_CONFIG_KEY + CONFIG_KEY_ENABLED);
        if (val == null) return true;
        return !"false".equalsIgnoreCase(val);
    }

    private int getRetentionDays() {
        String val = redisCache.getCacheObject(CacheConstants.SYS_CONFIG_KEY + CONFIG_KEY_RETENTION_DAYS);
        if (val != null) {
            try {
                return Math.max(1, Integer.parseInt(val));
            } catch (NumberFormatException ignored) {
            }
        }
        return DEFAULT_RETENTION_DAYS;
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
