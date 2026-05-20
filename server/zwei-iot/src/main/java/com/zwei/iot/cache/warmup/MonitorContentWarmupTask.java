package com.zwei.iot.cache.warmup;

import com.zwei.iot.cache.service.CacheWarmupTask;
import com.zwei.iot.cache.service.IotCacheService;
import com.zwei.iot.monitor.service.IMonitorContentService;

/**
 * 监测内容缓存预热任务
 *
 * @author zwei
 */
public class MonitorContentWarmupTask implements CacheWarmupTask {
    private final IMonitorContentService monitorContentService;
    private final IotCacheService cacheService;

    public MonitorContentWarmupTask(IMonitorContentService monitorContentService, IotCacheService cacheService) {
        this.monitorContentService = monitorContentService;
        this.cacheService = cacheService;
    }

    @Override
    public String getName() {
        return "MonitorContentWarmupTask";
    }

    @Override
    public int getOrder() {
        return 20;
    }

    @Override
    public void warmup() {
        var allContents = monitorContentService.selectMonitorContentAll(null);
        if (allContents != null && !allContents.isEmpty()) {
            cacheService.cacheMonitorContentList(allContents);
        }
    }
}