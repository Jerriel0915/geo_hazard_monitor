package com.zwei.iot.cache.warmup;

import com.zwei.iot.cache.service.CacheWarmupTask;
import com.zwei.iot.cache.service.IotCacheService;
import com.zwei.iot.hazardpoint.domain.HazardPoint;
import com.zwei.iot.hazardpoint.service.IHazardPointService;

/**
 * 隐患点缓存预热任务
 *
 * @author zwei
 */
public class HazardPointWarmupTask implements CacheWarmupTask {
    private final IHazardPointService hazardPointService;
    private final IotCacheService cacheService;

    public HazardPointWarmupTask(IHazardPointService hazardPointService, IotCacheService cacheService) {
        this.hazardPointService = hazardPointService;
        this.cacheService = cacheService;
    }

    @Override
    public String getName() {
        return "HazardPointWarmupTask";
    }

    @Override
    public int getOrder() {
        return 10;
    }

    @Override
    public void warmup() {
        var allPoints = hazardPointService.selectHazardPointList(new HazardPoint());
        if (allPoints != null && !allPoints.isEmpty()) {
            cacheService.cacheHazardPointList(allPoints);
        }
    }
}