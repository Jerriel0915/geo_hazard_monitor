package com.zwei.iot.warmup;

import com.zwei.iot.config.CacheWarmupTask;
import com.zwei.iot.service.IHazardPointGroupService;
import com.zwei.iot.service.IotCacheService;

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