package com.zwei.iot.service.impl;

import com.zwei.iot.config.CacheWarmupTaskRegistry;
import com.zwei.iot.domain.HazardPointGroup;
import com.zwei.iot.mapper.HazardPointGroupMapper;
import com.zwei.iot.service.IHazardPointGroupService;
import com.zwei.iot.service.IotCacheService;
import com.zwei.iot.warmup.HazardPointGroupWarmupTask;
import jakarta.annotation.PostConstruct;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * 隐患点分组管理
 *
 * @author zwei
 */
@Service
public class HazardPointGroupServiceImpl implements IHazardPointGroupService
{

    private final HazardPointGroupMapper mapper;
    private final IotCacheService cacheService;
    private final CacheWarmupTaskRegistry registry;

    @Autowired
    public HazardPointGroupServiceImpl(HazardPointGroupMapper mapper, IotCacheService cacheService,
                                       CacheWarmupTaskRegistry registry) {
        this.mapper = mapper;
        this.cacheService = cacheService;
        this.registry = registry;
    }

    @PostConstruct
    public void init() {
        registry.registerTask(new HazardPointGroupWarmupTask(this, cacheService));
    }

    @Override
    public List<HazardPointGroup> selectHazardPointGroupList(HazardPointGroup group)
    {
        List<HazardPointGroup> list = mapper.selectHazardPointGroupList(group);
        enrichPointCounts(list);
        return list;
    }

    @Override
    public List<HazardPointGroup> selectHazardPointGroupAll()
    {
        List<HazardPointGroup> list = mapper.selectHazardPointGroupAll();
        enrichPointCounts(list);
        return list;
    }

    @Override
    public HazardPointGroup selectHazardPointGroupById(Long id)
    {
        // 先尝试从缓存获取
        HazardPointGroup cached = cacheService.getHazardPointGroup(id);
        if (cached != null) {
            return cached;
        }
        // 缓存未命中，查询数据库并缓存
        HazardPointGroup group = mapper.selectHazardPointGroupById(id);
        if (group != null) {
            cacheService.cacheHazardPointGroup(group);
        }
        return group;
    }

    @Override
    public int insertHazardPointGroup(HazardPointGroup group)
    {
        int result = mapper.insertHazardPointGroup(group);
        // 缓存新增的分组
        if (result > 0 && group.getId() != null) {
            cacheService.cacheHazardPointGroup(group);
        }
        return result;
    }

    @Override
    public int updateHazardPointGroup(HazardPointGroup group)
    {
        int result = mapper.updateHazardPointGroup(group);
        // 更新缓存
        if (result > 0 && group.getId() != null) {
            cacheService.evictHazardPointGroup(group.getId());
        }
        return result;
    }

    @Override
    public int deleteHazardPointGroupById(Long id)
    {
        int result = mapper.deleteHazardPointGroupById(id);
        // 删除缓存
        if (result > 0) {
            cacheService.evictHazardPointGroup(id);
        }
        return result;
    }

    @Override
    public int deleteHazardPointGroupByIds(Long[] ids)
    {
        int result = mapper.deleteHazardPointGroupByIds(ids);
        // 批量删除缓存
        if (result > 0) {
            cacheService.evictHazardPointGroupList(ids);
        }
        return result;
    }

    @Override
    public boolean checkGroupCodeUnique(HazardPointGroup group)
    {
        Long id = group.getId() == null ? 0L : group.getId();
        HazardPointGroup exist = mapper.checkGroupCodeUnique(group.getCode());
        return exist == null || exist.getId().equals(id);
    }

    /**
     * 批量查询各分组下的隐患点数量，避免N+1查询问题
     */
    private void enrichPointCounts(List<HazardPointGroup> groups)
    {
        if (groups == null || groups.isEmpty())
        {
            return;
        }
        List<Long> groupIds = groups.stream()
                .map(HazardPointGroup::getId)
                .collect(Collectors.toList());

        List<Map<String, Object>> countRows = mapper.countHazardPointsByGroupIds(groupIds);
        Map<Long, Integer> countMap = countRows.stream()
                .collect(Collectors.toMap(
                        row -> (Long) row.get("groupId"),
                        row -> ((Long) row.get("cnt")).intValue()
                ));

        for (HazardPointGroup group : groups)
        {
            group.setCount(countMap.getOrDefault(group.getId(), 0));
        }
    }
}
