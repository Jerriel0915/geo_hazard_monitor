package com.zwei.iot.alarm.service.engine;

import com.alibaba.fastjson2.JSON;
import com.zwei.iot.alarm.domain.AlarmCriteria;
import com.zwei.iot.alarm.mapper.AlarmCriteriaMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;

/**
 * 告警判据缓存服务 — Redis 一级缓存 + 本地 Caffeine 风格 Map 二级缓存。
 * <p>
 * 判据修改/删除/启停时主动失效；评估引擎每次从缓存读取避免 DB 查询。
 *
 * @author zwei
 */
@Service
public class CriteriaCacheService {

    private static final Logger log = LoggerFactory.getLogger(CriteriaCacheService.class);

    private static final String REDIS_KEY = "alarm:criteria:enabled";
    private static final Duration TTL = Duration.ofMinutes(5);

    private final AlarmCriteriaMapper criteriaMapper;
    private final RedisTemplate<Object, Object> redisTemplate;

    /**
     * 本地二级缓存 (减少 Redis 网络 IO)
     */
    private final Map<String, List<AlarmCriteria>> localCache = new ConcurrentHashMap<>();

    public CriteriaCacheService(AlarmCriteriaMapper criteriaMapper, RedisTemplate<Object, Object> redisTemplate) {
        this.criteriaMapper = criteriaMapper;
        this.redisTemplate = redisTemplate;
    }

    /**
     * 获取所有启用的判据列表（缓存优先）。
     */
    public List<AlarmCriteria> getEnabledCriteria() {
        // L1: 本地缓存
        List<AlarmCriteria> local = localCache.get(REDIS_KEY);
        if (local != null) return local;

        // L2: Redis
        try {
            Object cached = redisTemplate.opsForValue().get(REDIS_KEY);
            if (cached != null) {
                List<AlarmCriteria> list = JSON.parseArray(String.valueOf(cached), AlarmCriteria.class);
                localCache.put(REDIS_KEY, list);
                return list;
            }
        } catch (Exception e) {
            log.debug("Redis 判据缓存读取失败: {}", e.getMessage());
        }

        // L3: MySQL
        List<AlarmCriteria> list = criteriaMapper.selectAllEnabled();
        refreshCache(list);
        return list;
    }

    /**
     * 按 monitorContentId 过滤。
     */
    public List<AlarmCriteria> getByMonitorContentId(Long contentId) {
        List<AlarmCriteria> all = getEnabledCriteria();
        List<AlarmCriteria> filtered = all.stream()
                .filter(c -> contentId.equals(c.getMonitorContentId()))
                .collect(Collectors.toList());
        log.debug("[Alarm][Cache] 按 monitorContentId 过滤 contentId={} total={} matched={}",
                contentId, all.size(), filtered.size());
        return filtered;
    }

    /**
     * 按 hazardPointId 过滤。
     */
    public List<AlarmCriteria> getByHazardPointId(Long hpId) {
        List<AlarmCriteria> all = getEnabledCriteria();
        List<AlarmCriteria> filtered = all.stream()
                .filter(c -> hpId.equals(c.getHazardPointId()))
                .collect(Collectors.toList());
        log.debug("[Alarm][Cache] 按 hazardPointId 过滤 hpId={} total={} matched={}",
                hpId, all.size(), filtered.size());
        return filtered;
    }

    /**
     * 按 monitorTypeId 获取兜底判据（hazard_point_id IS NULL）。
     */
    public List<AlarmCriteria> getByMonitorTypeId(Long typeId) {
        List<AlarmCriteria> all = getEnabledCriteria();
        List<AlarmCriteria> filtered = all.stream()
                .filter(c -> typeId.equals(c.getMonitorTypeId()) && c.getHazardPointId() == null)
                .collect(Collectors.toList());
        log.debug("[Alarm][Cache] 按 monitorTypeId 过滤兜底 typeId={} total={} matched={}",
                typeId, all.size(), filtered.size());
        return filtered;
    }

    /**
     * 判据变更后刷新缓存。
     */
    public void refresh() {
        localCache.remove(REDIS_KEY);
        try {
            redisTemplate.delete(REDIS_KEY);
        } catch (Exception ignored) {
        }
        List<AlarmCriteria> list = criteriaMapper.selectAllEnabled();
        refreshCache(list);
        log.info("判据缓存已刷新, count={}", list.size());
    }

    private void refreshCache(List<AlarmCriteria> list) {
        localCache.put(REDIS_KEY, list);
        try {
            redisTemplate.opsForValue().set(REDIS_KEY, JSON.toJSONString(list), TTL);
        } catch (Exception e) {
            log.debug("Redis 判据缓存写入失败: {}", e.getMessage());
        }
    }
}
