package com.zwei.iot.cache.warmup;

import com.zwei.iot.cache.service.CacheWarmupTask;
import com.zwei.iot.cache.service.IotCacheService;
import com.zwei.iot.hazardpoint.service.IHazardPointGroupService;

/**
 * 隐患点分组缓存预热任务
 *
 * @author zwei
 */
public class HazardPointGroupWarmupTask implements CacheWarmupTask {
    private final IHazardPointGroupService hazardPointGroupService;
    private final IotCacheService cacheService;

    public HazardPointGroupWarmupTask(IHazardPointGroupService hazardPointGroupService, IotCacheService cacheService) {
        this.hazardPointGroupService = hazardPointGroupService;
        this.cacheService = cacheService;
    }

    @Override
    public String getName() {
        return "HazardPointGroupWarmupTask";
    }

    @Override
    public int getOrder() {
        return 5;
    }

    @Override
    public void warmup() {
        var allGroups = hazardPointGroupService.selectHazardPointGroupAll();
        if (allGroups != null && !allGroups.isEmpty()) {
            cacheService.cacheHazardPointGroupList(allGroups);
        }
    }
}