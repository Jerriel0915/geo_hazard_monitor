package com.zwei.iot.service.impl;

import com.zwei.iot.domain.HazardPoint;
import com.zwei.iot.domain.HazardPointGroup;
import com.zwei.iot.domain.MonitorContent;
import com.zwei.iot.domain.MonitorType;
import com.zwei.iot.service.IotCacheService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.concurrent.TimeUnit;

/**
 * IoT缓存服务实现
 *
 * @author zwei
 */
@Service
public class IotCacheServiceImpl implements IotCacheService {
    // 缓存 key 前缀
    private static final String KEY_HAZARD_POINT = "iot:hazard:point:";
    private static final String KEY_MONITOR_TYPE = "iot:monitor:type:";
    private static final String KEY_MONITOR_CONTENT = "iot:monitor:content:";
    private static final String KEY_HAZARD_GROUP = "iot:hazard:group:";

    // 缓存 key 集合（用于清空）
    private static final String CACHE_SET_HAZARD_POINT = "iot:hazard:point:ids";
    private static final String CACHE_SET_MONITOR_TYPE = "iot:monitor:type:ids";
    private static final String CACHE_SET_MONITOR_CONTENT = "iot:monitor:content:ids";
    private static final String CACHE_SET_HAZARD_GROUP = "iot:hazard:group:ids";

    // 缓存TTL（分钟）
    private static final long DEFAULT_TTL = 30;

    private final RedisTemplate<Object, Object> redisTemplate;

    @Autowired
    public IotCacheServiceImpl(RedisTemplate<Object, Object> redisTemplate) {
        this.redisTemplate = redisTemplate;
    }

    // ==================== HazardPoint 缓存实现 ====================

    @Override
    public void cacheHazardPoint(HazardPoint point) {
        if (point == null || point.getId() == null) {
            return;
        }
        String key = KEY_HAZARD_POINT + point.getId();
        redisTemplate.opsForValue().set(key, point, DEFAULT_TTL, TimeUnit.MINUTES);
        addToIdSet(CACHE_SET_HAZARD_POINT, point.getId());
    }

    @Override
    public void cacheHazardPointList(List<HazardPoint> points) {
        if (points == null || points.isEmpty()) {
            return;
        }
        for (HazardPoint point : points) {
            if (point != null && point.getId() != null) {
                String key = KEY_HAZARD_POINT + point.getId();
                redisTemplate.opsForValue().set(key, point, DEFAULT_TTL, TimeUnit.MINUTES);
                addToIdSet(CACHE_SET_HAZARD_POINT, point.getId());
            }
        }
    }

    @Override
    public HazardPoint getHazardPoint(Long id) {
        if (id == null) {
            return null;
        }
        String key = KEY_HAZARD_POINT + id;
        return (HazardPoint) redisTemplate.opsForValue().get(key);
    }

    @Override
    public List<HazardPoint> getHazardPointList(List<Long> ids) {
        if (ids == null || ids.isEmpty()) {
            return List.of();
        }
        List<String> keyList = ids.stream()
                .map(id -> KEY_HAZARD_POINT + id)
                .toList();
        List<Object> results = redisTemplate.opsForValue().multiGet(new java.util.ArrayList<>(keyList));
        if (results == null) {
            return List.of();
        }
        return results.stream()
                .filter(obj -> obj != null)
                .map(obj -> (HazardPoint) obj)
                .toList();
    }

    @Override
    public void evictHazardPoint(Long id) {
        if (id == null) {
            return;
        }
        String key = KEY_HAZARD_POINT + id;
        redisTemplate.delete(key);
        removeFromIdSet(CACHE_SET_HAZARD_POINT, id);
    }

    @Override
    public void evictHazardPointList(Long[] ids) {
        if (ids == null || ids.length == 0) {
            return;
        }
        for (Long id : ids) {
            evictHazardPoint(id);
        }
    }

    @Override
    public void clearHazardPointCache() {
        clearCacheBySet(CACHE_SET_HAZARD_POINT, KEY_HAZARD_POINT);
    }

    // ==================== MonitorType 缓存实现 ====================

    @Override
    public void cacheMonitorType(MonitorType monitorType) {
        if (monitorType == null || monitorType.getId() == null) {
            return;
        }
        String key = KEY_MONITOR_TYPE + monitorType.getId();
        redisTemplate.opsForValue().set(key, monitorType, DEFAULT_TTL, TimeUnit.MINUTES);
        addToIdSet(CACHE_SET_MONITOR_TYPE, monitorType.getId());
    }

    @Override
    public void cacheMonitorTypeList(List<MonitorType> monitorTypes) {
        if (monitorTypes == null || monitorTypes.isEmpty()) {
            return;
        }
        for (MonitorType monitorType : monitorTypes) {
            if (monitorType != null && monitorType.getId() != null) {
                String key = KEY_MONITOR_TYPE + monitorType.getId();
                redisTemplate.opsForValue().set(key, monitorType, DEFAULT_TTL, TimeUnit.MINUTES);
                addToIdSet(CACHE_SET_MONITOR_TYPE, monitorType.getId());
            }
        }
    }

    @Override
    public MonitorType getMonitorType(Long id) {
        if (id == null) {
            return null;
        }
        String key = KEY_MONITOR_TYPE + id;
        return (MonitorType) redisTemplate.opsForValue().get(key);
    }

    @Override
    public List<MonitorType> getMonitorTypeList(List<Long> ids) {
        if (ids == null || ids.isEmpty()) {
            return List.of();
        }
        List<String> keyList = ids.stream()
                .map(id -> KEY_MONITOR_TYPE + id)
                .toList();
        List<Object> results = redisTemplate.opsForValue().multiGet(new java.util.ArrayList<>(keyList));
        if (results == null) {
            return List.of();
        }
        return results.stream()
                .filter(obj -> obj != null)
                .map(obj -> (MonitorType) obj)
                .toList();
    }

    @Override
    public void evictMonitorType(Long id) {
        if (id == null) {
            return;
        }
        String key = KEY_MONITOR_TYPE + id;
        redisTemplate.delete(key);
        removeFromIdSet(CACHE_SET_MONITOR_TYPE, id);
    }

    @Override
    public void evictMonitorTypeList(Long[] ids) {
        if (ids == null || ids.length == 0) {
            return;
        }
        for (Long id : ids) {
            evictMonitorType(id);
        }
    }

    @Override
    public void clearMonitorTypeCache() {
        clearCacheBySet(CACHE_SET_MONITOR_TYPE, KEY_MONITOR_TYPE);
    }

    // ==================== MonitorContent 缓存实现 ====================

    @Override
    public void cacheMonitorContent(MonitorContent content) {
        if (content == null || content.getId() == null) {
            return;
        }
        String key = KEY_MONITOR_CONTENT + content.getId();
        redisTemplate.opsForValue().set(key, content, DEFAULT_TTL, TimeUnit.MINUTES);
        addToIdSet(CACHE_SET_MONITOR_CONTENT, content.getId());
    }

    @Override
    public void cacheMonitorContentList(List<MonitorContent> contents) {
        if (contents == null || contents.isEmpty()) {
            return;
        }
        for (MonitorContent content : contents) {
            if (content != null && content.getId() != null) {
                String key = KEY_MONITOR_CONTENT + content.getId();
                redisTemplate.opsForValue().set(key, content, DEFAULT_TTL, TimeUnit.MINUTES);
                addToIdSet(CACHE_SET_MONITOR_CONTENT, content.getId());
            }
        }
    }

    @Override
    public MonitorContent getMonitorContent(Long id) {
        if (id == null) {
            return null;
        }
        String key = KEY_MONITOR_CONTENT + id;
        return (MonitorContent) redisTemplate.opsForValue().get(key);
    }

    @Override
    public List<MonitorContent> getMonitorContentList(List<Long> ids) {
        if (ids == null || ids.isEmpty()) {
            return List.of();
        }
        List<String> keyList = ids.stream()
                .map(id -> KEY_MONITOR_CONTENT + id)
                .toList();
        List<Object> results = redisTemplate.opsForValue().multiGet(new java.util.ArrayList<>(keyList));
        if (results == null) {
            return List.of();
        }
        return results.stream()
                .filter(obj -> obj != null)
                .map(obj -> (MonitorContent) obj)
                .toList();
    }

    @Override
    public void evictMonitorContent(Long id) {
        if (id == null) {
            return;
        }
        String key = KEY_MONITOR_CONTENT + id;
        redisTemplate.delete(key);
        removeFromIdSet(CACHE_SET_MONITOR_CONTENT, id);
    }

    @Override
    public void evictMonitorContentList(Long[] ids) {
        if (ids == null || ids.length == 0) {
            return;
        }
        for (Long id : ids) {
            evictMonitorContent(id);
        }
    }

    @Override
    public void clearMonitorContentCache() {
        clearCacheBySet(CACHE_SET_MONITOR_CONTENT, KEY_MONITOR_CONTENT);
    }

    // ==================== HazardPointGroup 缓存实现 ====================

    @Override
    public void cacheHazardPointGroup(HazardPointGroup group) {
        if (group == null || group.getId() == null) {
            return;
        }
        String key = KEY_HAZARD_GROUP + group.getId();
        redisTemplate.opsForValue().set(key, group, DEFAULT_TTL, TimeUnit.MINUTES);
        addToIdSet(CACHE_SET_HAZARD_GROUP, group.getId());
    }

    @Override
    public void cacheHazardPointGroupList(List<HazardPointGroup> groups) {
        if (groups == null || groups.isEmpty()) {
            return;
        }
        for (HazardPointGroup group : groups) {
            if (group != null && group.getId() != null) {
                String key = KEY_HAZARD_GROUP + group.getId();
                redisTemplate.opsForValue().set(key, group, DEFAULT_TTL, TimeUnit.MINUTES);
                addToIdSet(CACHE_SET_HAZARD_GROUP, group.getId());
            }
        }
    }

    @Override
    public HazardPointGroup getHazardPointGroup(Long id) {
        if (id == null) {
            return null;
        }
        String key = KEY_HAZARD_GROUP + id;
        return (HazardPointGroup) redisTemplate.opsForValue().get(key);
    }

    @Override
    public List<HazardPointGroup> getHazardPointGroupList(List<Long> ids) {
        if (ids == null || ids.isEmpty()) {
            return List.of();
        }
        List<String> keyList = ids.stream()
                .map(id -> KEY_HAZARD_GROUP + id)
                .toList();
        List<Object> results = redisTemplate.opsForValue().multiGet(new java.util.ArrayList<>(keyList));
        if (results == null) {
            return List.of();
        }
        return results.stream()
                .filter(obj -> obj != null)
                .map(obj -> (HazardPointGroup) obj)
                .toList();
    }

    @Override
    public void evictHazardPointGroup(Long id) {
        if (id == null) {
            return;
        }
        String key = KEY_HAZARD_GROUP + id;
        redisTemplate.delete(key);
        removeFromIdSet(CACHE_SET_HAZARD_GROUP, id);
    }

    @Override
    public void evictHazardPointGroupList(Long[] ids) {
        if (ids == null || ids.length == 0) {
            return;
        }
        for (Long id : ids) {
            evictHazardPointGroup(id);
        }
    }

    @Override
    public void clearHazardPointGroupCache() {
        clearCacheBySet(CACHE_SET_HAZARD_GROUP, KEY_HAZARD_GROUP);
    }

    // ==================== 全量清空 ====================

    @Override
    public void clearAllCache() {
        clearHazardPointCache();
        clearMonitorTypeCache();
        clearMonitorContentCache();
        clearHazardPointGroupCache();
    }

    // ==================== 工具方法 ====================

    /**
     * 添加ID到ID集合（用于追踪已缓存的ID）
     */
    private void addToIdSet(String setKey, Long id) {
        redisTemplate.opsForSet().add(setKey, id);
        // ID集合TTL与缓存TTL一致，确保清理时ID集合未过期
        redisTemplate.expire(setKey, DEFAULT_TTL, TimeUnit.MINUTES);
    }

    /**
     * 从ID集合中移除ID
     */
    private void removeFromIdSet(String setKey, Long id) {
        redisTemplate.opsForSet().remove(setKey, id);
    }

    /**
     * 根据ID集合清空对应前缀的缓存
     */
    private void clearCacheBySet(String setKey, String keyPrefix) {
        var ids = redisTemplate.opsForSet().members(setKey);
        if (ids != null && !ids.isEmpty()) {
            for (Object id : ids) {
                String key = keyPrefix + id;
                redisTemplate.delete(key);
            }
        }
        redisTemplate.delete(setKey);
    }
}