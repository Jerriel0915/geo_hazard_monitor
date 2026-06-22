package com.zwei.iot.timeseries.compute;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.zwei.common.domain.ParsedMessageSnapshot;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;

import java.time.Duration;

/**
 * 按 deviceId + sensorCode 缓存最近一条 ParsedMessage 精简快照, 用作下次脚本执行的 prevData。
 *
 * <p>Redis Key: {@code monitor:last:{deviceId}:{sensorCode}}, TTL 7 天。
 * 任何失败仅 warn 日志, 不抛异常, 不影响主链路。
 */
@Service
public class LastMessageStore {

    private static final Logger log = LoggerFactory.getLogger(LastMessageStore.class);
    private static final String KEY_PREFIX = "monitor:last:";
    private static final Duration TTL = Duration.ofDays(7);

    private final StringRedisTemplate redis;
    private final ObjectMapper objectMapper;

    public LastMessageStore(StringRedisTemplate redis) {
        this.redis = redis;
        this.objectMapper = new ObjectMapper();
    }

    /** 取上一条精简消息; null 表示首次上报 / 已过期 / Redis 异常。 */
    public ParsedMessageSnapshot get(Long deviceId, String sensorCode) {
        try {
            String json = redis.opsForValue().get(buildKey(deviceId, sensorCode));
            if (json == null) return null;
            return objectMapper.readValue(json, ParsedMessageSnapshot.class);
        } catch (Exception e) {
            log.warn("LastMessageStore.get failed: deviceId={}, sensorCode={}",
                    deviceId, sensorCode, e);
            return null;
        }
    }

    /** 写当前条作为下次的 prevData; 失败仅 warn。 */
    public void put(Long deviceId, String sensorCode, ParsedMessageSnapshot snapshot) {
        try {
            String json = objectMapper.writeValueAsString(snapshot);
            redis.opsForValue().set(buildKey(deviceId, sensorCode), json, TTL);
        } catch (Exception e) {
            log.warn("LastMessageStore.put failed: deviceId={}, sensorCode={}",
                    deviceId, sensorCode, e);
        }
    }

    private String buildKey(Long deviceId, String sensorCode) {
        return KEY_PREFIX + deviceId + ":" + sensorCode;
    }
}
