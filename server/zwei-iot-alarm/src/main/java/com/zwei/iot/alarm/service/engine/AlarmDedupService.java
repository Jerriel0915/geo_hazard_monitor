package com.zwei.iot.alarm.service.engine;

import com.zwei.iot.alarm.config.AlarmProperties;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;

import java.time.Duration;

/**
 * 告警去重服务 — 基于 Redis 管理预触发计数和静默周期。
 *
 * <h3>去重逻辑</h3>
 * <ol>
 *   <li>persistCount=1 → 立即生成告警（无预触发计数）</li>
 *   <li>persistCount > 1 → Redis 累加计数，达到阈值才生成告警</li>
 *   <li>silence_period 内 → 不生成新告警，仅更新已有记录的 trigger_count</li>
 * </ol>
 *
 * <h3>Redis Key</h3>
 * {@code alarm:pre-trigger:{criteriaId}:{hazardPointId}:{level}}
 *
 * @author zwei
 */
@Service
public class AlarmDedupService {

    private static final String PRE_TRIGGER_KEY = "alarm:pre-trigger";
    private static final String LAST_TRIGGER_KEY = "alarm:last-trigger";

    private final RedisTemplate<Object, Object> redisTemplate;
    private final AlarmProperties properties;

    public AlarmDedupService(RedisTemplate<Object, Object> redisTemplate, AlarmProperties properties) {
        this.redisTemplate = redisTemplate;
        this.properties = properties;
    }

    /**
     * 记录一次预触发，返回是否需要生成告警。
     *
     * @param criteriaId    判据ID
     * @param hazardPointId 隐患点ID
     * @param alarmLevel    告警等级
     * @param persistCount  判据要求的持续触发次数
     * @param silencePeriod 静默周期（数据采集周期数）
     * @return true = 应生成告警, false = 计数中/静默中
     */
    public boolean shouldTriggerAlarm(Long criteriaId, Long hazardPointId,
                                      int alarmLevel, int persistCount, int silencePeriod) {
        String preKey = buildPreTriggerKey(criteriaId, hazardPointId, alarmLevel);
        String lastKey = buildLastTriggerKey(criteriaId, hazardPointId);

        // persistCount=1 时直接触发
        if (persistCount <= 1) {
            // 检查静默期
            if (isInSilencePeriod(lastKey, silencePeriod)) {
                return false;
            }
            markTriggered(lastKey);
            return true;
        }

        // 累加预触发计数
        Long currentCount = redisTemplate.opsForValue().increment(preKey);
        if (currentCount == 1) {
            // 首次计数，设置 TTL（数据周期 × persistCount × 2 的安全窗口）
            int ttl = properties.getPreTriggerTtlSeconds();
            redisTemplate.expire(preKey, Duration.ofSeconds(ttl));
        }

        if (currentCount != null && currentCount >= persistCount) {
            // 达到持续触发次数
            if (isInSilencePeriod(lastKey, silencePeriod)) {
                return false;
            }
            // 达到阈值，清理预触发计数并生成告警
            redisTemplate.delete(preKey);
            markTriggered(lastKey);
            return true;
        }

        return false;
    }

    /**
     * 清除预触发计数（判据修改或删除时调用）。
     */
    public void clearPreTrigger(Long criteriaId, Long hazardPointId, int alarmLevel) {
        redisTemplate.delete(buildPreTriggerKey(criteriaId, hazardPointId, alarmLevel));
    }

    private boolean isInSilencePeriod(String lastKey, int silencePeriod) {
        if (silencePeriod <= 0) {
            return false;
        }
        Object lastTriggerObj = redisTemplate.opsForValue().get(lastKey);
        if (lastTriggerObj == null) {
            return false;
        }
        long lastTrigger = Long.parseLong(String.valueOf(lastTriggerObj));
        long now = System.currentTimeMillis();
        // silencePeriod 单位按照数据采集周期（假定每个周期为 60 秒），转换为毫秒
        long silenceMs = (long) silencePeriod * 60_000L;
        return (now - lastTrigger) < silenceMs;
    }

    private void markTriggered(String lastKey) {
        int ttl = properties.getPreTriggerTtlSeconds();
        redisTemplate.opsForValue().set(lastKey, String.valueOf(System.currentTimeMillis()),
                Duration.ofSeconds(ttl));
    }

    private String buildPreTriggerKey(Long criteriaId, Long hazardPointId, int level) {
        return PRE_TRIGGER_KEY + ":" + criteriaId + ":" + hazardPointId + ":" + level;
    }

    private String buildLastTriggerKey(Long criteriaId, Long hazardPointId) {
        return LAST_TRIGGER_KEY + ":" + criteriaId + ":" + hazardPointId;
    }
}
