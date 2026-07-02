package com.zwei.iot.broker.component;

import com.zwei.common.utils.StringUtils;
import com.zwei.iot.broker.config.MqttAuthCenterProperties;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Component;

import java.util.concurrent.TimeUnit;

/**
 * 设备鉴权失败保护器。
 * <p>
 * 基于 Redis 统计设备账号的连续失败次数，并在达到阈值后执行临时封禁，
 * 防止错误密码高频重试持续占用 Broker 与数据库资源。
 * <p>
 * Redis 实现保证多实例部署下失败计数跨实例共享。
 * <p>
 * <b>容错策略</b>：Redis 不可用时采用 fail-open（放行），避免 Redis 故障导致全部设备无法鉴权。
 * {@link #recordFailure} 在 Redis INCR 返回 {@code null} 时记录 warn 日志后跳过计数。
 */
@Slf4j
@Component
public class MqttAuthFailureGuard {

    private static final String KEY_PREFIX_FAIL = "mqtt:auth:fail:";
    private static final String KEY_PREFIX_BAN = "mqtt:auth:ban:";

    private final MqttAuthCenterProperties properties;
    private final StringRedisTemplate redisTemplate;

    @Autowired
    public MqttAuthFailureGuard(MqttAuthCenterProperties properties,
                                StringRedisTemplate redisTemplate) {
        this.properties = properties;
        this.redisTemplate = redisTemplate;
    }

    /**
     * 判断当前账号是否仍在封禁期内。
     *
     * @param username 设备认证账号
     * @return {@code true} 表示账号仍被禁止接入
     */
    public boolean isBlocked(String username) {
        if (StringUtils.isBlank(username)) {
            return false;
        }
        return Boolean.TRUE.equals(redisTemplate.hasKey(KEY_PREFIX_BAN + username));
    }

    /**
     * 获取当前账号剩余封禁时长。
     *
     * @param username 设备认证账号
     * @return 剩余秒数；未封禁时返回 0
     */
    public long getRemainingBlockSeconds(String username) {
        if (StringUtils.isBlank(username)) {
            return 0L;
        }
        Long ttl = redisTemplate.getExpire(KEY_PREFIX_BAN + username, TimeUnit.SECONDS);
        return ttl == null || ttl <= 0 ? 0L : ttl;
    }

    /**
     * 记录一次鉴权失败。
     * <p>
     * 使用 Redis INCR 原子递增失败计数，首次失败设置过期时间。
     * 达到阈值后设置封禁标记并清除失败计数。
     *
     * @param username 设备认证账号
     */
    public void recordFailure(String username) {
        if (StringUtils.isBlank(username)) {
            return;
        }
        String countKey = KEY_PREFIX_FAIL + username;
        String banKey = KEY_PREFIX_BAN + username;
        Long count = redisTemplate.opsForValue().increment(countKey);
        if (count == null) {
            // fail-open: Redis 不可用时放行，不阻塞设备鉴权主链路
            log.warn("Redis INCR 返回 null，跳过失败计数（fail-open）。username={}", username);
            return;
        }
        if (count == 1L) {
            redisTemplate.expire(countKey, properties.getBanDurationSeconds(), TimeUnit.SECONDS);
        }
        if (count >= properties.getFailureThreshold()) {
            redisTemplate.opsForValue().set(banKey, "1",
                    properties.getBanDurationSeconds(), TimeUnit.SECONDS);
            redisTemplate.delete(countKey);
        }
    }

    /**
     * 鉴权成功后清除失败记录与封禁状态。
     *
     * @param username 设备认证账号
     */
    public void reset(String username) {
        if (StringUtils.isBlank(username)) {
            return;
        }
        redisTemplate.delete(KEY_PREFIX_FAIL + username);
        redisTemplate.delete(KEY_PREFIX_BAN + username);
    }

    // clear() 已移除 — Redis 实现不支持 JVM 内存级别的全量清除。
    // 如需清除封禁，请通过 Redis CLI 执行: KEYS mqtt:auth:* | xargs redis-cli DEL
}
