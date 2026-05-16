package com.zwei.iot.warmup;

import com.zwei.iot.config.CacheWarmupTask;
import com.zwei.iot.service.IMonitorContentService;
import com.zwei.iot.service.IotCacheService;

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