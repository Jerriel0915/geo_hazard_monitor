package com.zwei.iot.timeseries.compute;

import com.zwei.iot.monitor.domain.MonitorContent;
import com.zwei.iot.monitor.service.IMonitorContentService;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Service;

import java.util.List;

/**
 * 按监测类型 ID 加载并缓存计算属性列表。
 *
 * <p>缓存 name = "computedAttrs", 由 {@code MonitorContentServiceImpl} 的
 * {@code @CacheEvict(allEntries=true)} 在 insert/update/delete 时联动失效。
 */
@Service
public class ComputedAttributeRegistry {

    private final IMonitorContentService monitorContentService;

    public ComputedAttributeRegistry(IMonitorContentService monitorContentService) {
        this.monitorContentService = monitorContentService;
    }

    /**
     * 取指定监测类型下的计算属性列表(按 sort_order 升序)。
     * 空列表表示该类型无计算属性, 调用方可走 fast path。
     */
    @Cacheable(value = "computedAttrs", key = "#monitorTypeId")
    public List<ComputedAttribute> getByMonitorTypeId(Long monitorTypeId) {
        List<MonitorContent> raw = monitorContentService.selectComputedByTypeId(monitorTypeId);
        if (raw == null || raw.isEmpty()) return List.of();
        return raw.stream().map(ComputedAttribute::from).toList();
    }
}
