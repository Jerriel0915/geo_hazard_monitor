package com.zwei.iot.device.job;

import com.zwei.iot.device.mapper.MonitorStatMapper;
import jakarta.annotation.PostConstruct;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

/**
 * 累计监测次数持久化同步。
 * <p>
 * 热路径: Redis INCR（MonitorIngestConsumerService 每笔数据 +1）<br>
 * 定时同步: 每 30 秒将 Redis 值刷入 MySQL monitor_stats 表<br>
 * 启动回填: Redis 空或小于 MySQL 值时，从 MySQL 恢复到 Redis
 */
@Component
public class MonitorCounterSyncJob {

    private static final Logger log = LoggerFactory.getLogger(MonitorCounterSyncJob.class);

    /** Redis 累计监测次数键 */
    static final String REDIS_KEY_TOTAL_MONITOR_COUNT = "stats:total:monitor:count";

    private final RedisTemplate<Object, Object> redisTemplate;
    private final MonitorStatMapper monitorStatMapper;

    public MonitorCounterSyncJob(RedisTemplate<Object, Object> redisTemplate,
                                  MonitorStatMapper monitorStatMapper) {
        this.redisTemplate = redisTemplate;
        this.monitorStatMapper = monitorStatMapper;
    }

    /**
     * 启动时将 MySQL 中的值回填到 Redis（Redis 丢失时自动恢复）。
     */
    @PostConstruct
    public void loadFromDb() {
        try {
            var row = monitorStatMapper.selectByKey("total_monitor_count");
            if (row == null) {
                // 表里还没有记录，用 Redis 当前值初始化
                Long redisVal = readRedis();
                if (redisVal > 0) {
                    monitorStatMapper.insert("total_monitor_count", redisVal);
                    log.info("monitor_stats 初始化: total_monitor_count = {}", redisVal);
                }
                return;
            }

            if (row.getStatValue() == null) {
                // 行存在但值为 null，修复为 Redis 当前值
                long redisVal = readRedis();
                monitorStatMapper.updateValue("total_monitor_count", redisVal);
                log.info("monitor_stats 修复 null 值: total_monitor_count = {}", redisVal);
                return;
            }

            long dbVal = row.getStatValue();
            long redisVal = readRedis();

            if (redisVal < dbVal) {
                // Redis 丢失或落后 → 从 MySQL 恢复
                redisTemplate.opsForValue().set(REDIS_KEY_TOTAL_MONITOR_COUNT, String.valueOf(dbVal));
                log.info("Redis 计数器回填: {} → {}", redisVal, dbVal);
            } else if (redisVal > dbVal) {
                // Redis 比 MySQL 大（正常情况：还没来得及同步）
                // 不做回填，等下次定时任务同步
            }
        } catch (Exception e) {
            log.warn("启动加载累计监测次数失败", e);
        }
    }

    /**
     * 每 30 秒将 Redis 计数器同步到 MySQL。
     */
    @Scheduled(fixedDelay = 30_000)
    public void syncToDb() {
        try {
            long redisVal = readRedis();
            if (redisVal > 0) {
                // 正常路径: Redis → MySQL
                monitorStatMapper.updateValue("total_monitor_count", redisVal);
            } else {
                // Redis 可能丢失了 (重启 / 淘汰)，从 MySQL 恢复
                var row = monitorStatMapper.selectByKey("total_monitor_count");
                if (row != null && row.getStatValue() != null && row.getStatValue() > 0) {
                    redisTemplate.opsForValue().set(REDIS_KEY_TOTAL_MONITOR_COUNT,
                            String.valueOf(row.getStatValue()));
                    log.info("Redis 计数器恢复: 0 → {}", row.getStatValue());
                }
            }
        } catch (Exception e) {
            log.warn("累计监测次数同步 MySQL 失败", e);
        }
    }

    private long readRedis() {
        Object val = redisTemplate.opsForValue().get(REDIS_KEY_TOTAL_MONITOR_COUNT);
        if (val instanceof Number) {
            return ((Number) val).longValue();
        }
        if (val instanceof String) {
            try {
                return Long.parseLong((String) val);
            } catch (NumberFormatException ignored) {
            }
        }
        return 0L;
    }
}
