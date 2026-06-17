package com.zwei.iot.timeseries.compute;

import com.zwei.common.event.MonitorContentChangedEvent;
import com.zwei.iot.monitor.domain.MonitorContent;
import com.zwei.iot.monitor.service.IMonitorContentService;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * 按监测类型 ID 加载并缓存计算属性列表。
 *
 * <p>使用内存 {@link ConcurrentHashMap} 缓存而非 Redis，避免 FastJSON2 对 POJO 反序列化兼容问题。
 * 通过 {@link EventListener} 监听 {@link MonitorContentChangedEvent} 自动失效缓存。
 */
@Service
public class ComputedAttributeRegistry {

    private final IMonitorContentService monitorContentService;
    private final Map<Long, List<ComputedAttribute>> cache = new ConcurrentHashMap<>();

    public ComputedAttributeRegistry(IMonitorContentService monitorContentService) {
        this.monitorContentService = monitorContentService;
    }

    /** 清空全部缓存，由 {@link #onContentChanged} 事件监听自动触发。 */
    public void evictAll() {
        cache.clear();
    }

    @EventListener
    public void onContentChanged(MonitorContentChangedEvent event) {
        evictAll();
    }

    /**
     * 取指定监测类型下的计算属性列表(按 sort_order 升序)。
     * 空列表表示该类型无计算属性, 调用方可走 fast path。
     */
    public List<ComputedAttribute> getByMonitorTypeId(Long monitorTypeId) {
        return cache.computeIfAbsent(monitorTypeId, id -> {
            List<MonitorContent> raw = monitorContentService.selectComputedByTypeId(id);
            if (raw == null || raw.isEmpty()) return List.of();
            return raw.stream().map(ComputedAttribute::from).toList();
        });
    }
}
