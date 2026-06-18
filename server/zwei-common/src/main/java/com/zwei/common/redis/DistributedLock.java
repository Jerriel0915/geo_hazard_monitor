package com.zwei.common.redis;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Component;

import java.time.Duration;
import java.util.UUID;

/**
 * 极简 Redis 分布式锁:SETNX EX + token 校验解锁。
 * <p>
 * 仅用于本模块单实例或偶尔多实例部署的并发兜底;高并发场景请用 Redisson。
 * <p>
 * 语义:
 * <ul>
 *   <li>{@link #tryLock(String, Duration)} 获取锁,不重试。返回 {@link LockToken#acquired()}=false 表示未抢到。</li>
 *   <li>{@link #unlock(String, LockToken)} 释放锁,仅当 Redis 中保存的 token 与一致时才删除,
 *       防止 TTL 过期后误删他人持有的锁。</li>
 * </ul>
 */
@Component
public class DistributedLock {

    private static final Logger log = LoggerFactory.getLogger(DistributedLock.class);

    private final StringRedisTemplate redis;

    public DistributedLock(StringRedisTemplate redis) {
        this.redis = redis;
    }

    /**
     * 尝试获取锁,不重试。
     *
     * @param key 锁键
     * @param ttl 锁存活时间 (防死锁)
     * @return LockToken; {@link LockToken#acquired()} 为 false 表示未获取
     */
    public LockToken tryLock(String key, Duration ttl) {
        String token = UUID.randomUUID().toString();
        Boolean ok = redis.opsForValue().setIfAbsent(key, token, ttl);
        if (Boolean.TRUE.equals(ok)) {
            return new LockToken(true, token);
        }
        return LockToken.notAcquired();
    }

    /**
     * 释放锁:仅当 Redis 中保存的值与本 token 匹配时才删除(防止误删别人的锁)。
     * <p>
     * 若 token 是 {@link LockToken#notAcquired()} 则直接 no-op。
     */
    public void unlock(String key, LockToken token) {
        if (!token.acquired()) {
            return;
        }
        String current = redis.opsForValue().get(key);
        if (token.value().equals(current)) {
            Boolean deleted = redis.delete(key);
            log.debug("[lock] unlock key={} deleted={}", key, deleted);
        } else {
            log.warn("[lock] unlock skipped, token mismatch key={} (ttl expired?)", key);
        }
    }

    /**
     * 锁凭证。
     *
     * @param acquired 是否成功获取
     * @param value    写入 Redis 的随机 token; 未获取时为 null
     */
    public record LockToken(boolean acquired, String value) {
        public static LockToken notAcquired() {
            return new LockToken(false, null);
        }
    }
}
