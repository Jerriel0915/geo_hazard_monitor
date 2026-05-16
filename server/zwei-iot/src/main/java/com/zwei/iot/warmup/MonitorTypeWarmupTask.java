package com.zwei.iot.warmup;

import com.zwei.iot.config.CacheWarmupTask;
import com.zwei.iot.service.IMonitorTypeService;
import com.zwei.iot.service.IotCacheService;

/**
 * 监测类型缓存预热任务
 *
 * @author zwei
 */
public class MonitorTypeWarmupTask implements CacheWarmupTask {
    private final IMonitorTypeService monitorTypeService;
    private final IotCacheService cacheService;

    public MonitorTypeWarmupTask(IMonitorTypeService monitorTypeService, IotCacheService cacheService) {
        this.monitorTypeService = monitorTypeService;
        this.cacheService = cacheService;
    }

    @Override
    public String getName() {
        return "MonitorTypeWarmupTask";
    }

    @Override
    public int getOrder() {
        return 1;
    }

    @Override
    public void warmup() {
        var allTypes = monitorTypeService.selectMonitorTypeAll();
        if (allTypes != null && !allTypes.isEmpty()) {
            cacheService.cacheMonitorTypeList(allTypes);
        }
    }
}