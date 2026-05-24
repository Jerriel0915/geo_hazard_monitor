package com.zwei.iot.hazardpoint.service.impl;

import com.zwei.iot.hazardpoint.domain.HazardPointGroup;
import com.zwei.iot.hazardpoint.mapper.HazardPointMapper;
import com.zwei.iot.hazardpoint.mapper.HazardPointGroupMapper;
import com.zwei.iot.hazardpoint.service.IHazardPointGroupService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.cache.annotation.Caching;
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
public class HazardPointGroupServiceImpl implements IHazardPointGroupService {

    private final HazardPointGroupMapper mapper;
    private final HazardPointMapper hazardPointMapper;

    @Autowired
    public HazardPointGroupServiceImpl(HazardPointGroupMapper mapper,
                                       HazardPointMapper hazardPointMapper) {
        this.mapper = mapper;
        this.hazardPointMapper = hazardPointMapper;
    }

    @Override
    public List<HazardPointGroup> selectHazardPointGroupList(HazardPointGroup group) {
        List<HazardPointGroup> list = mapper.selectHazardPointGroupList(group);
        enrichPointCounts(list);
        return list;
    }

    @Override
    public List<HazardPointGroup> selectHazardPointGroupAll() {
        List<HazardPointGroup> list = mapper.selectHazardPointGroupAll();
        enrichPointCounts(list);
        return list;
    }

    @Override
    @Cacheable(value = "hazardPointGroup", key = "#id")
    public HazardPointGroup selectHazardPointGroupById(Long id) {
        return mapper.selectHazardPointGroupById(id);
    }

    @Override
    @Caching(evict = {
            @CacheEvict(value = "hazardPointGroup", key = "#group.id"),
            @CacheEvict(value = "hazardPointGroupList", allEntries = true)
    })
    public int insertHazardPointGroup(HazardPointGroup group) {
        return mapper.insertHazardPointGroup(group);
    }

    @Override
    @Caching(evict = {
            @CacheEvict(value = "hazardPointGroup", key = "#group.id"),
            @CacheEvict(value = "hazardPointGroupList", allEntries = true)
    })
    public int updateHazardPointGroup(HazardPointGroup group) {
        int rows = mapper.updateHazardPointGroup(group);
        if (rows > 0) {
            hazardPointMapper.updateGroupNameByGroupId(group.getId(), group.getName());
        }
        return rows;
    }

    @Override
    @Caching(evict = {
            @CacheEvict(value = "hazardPointGroup", key = "#id"),
            @CacheEvict(value = "hazardPointGroupList", allEntries = true)
    })
    public int deleteHazardPointGroupById(Long id) {
        return mapper.deleteHazardPointGroupById(id);
    }

    @Override
    @Caching(evict = {
            @CacheEvict(value = "hazardPointGroup", allEntries = true),
            @CacheEvict(value = "hazardPointGroupList", allEntries = true)
    })
    public int deleteHazardPointGroupByIds(Long[] ids) {
        return mapper.deleteHazardPointGroupByIds(ids);
    }

    @Override
    public boolean checkGroupCodeUnique(HazardPointGroup group) {
        Long id = group.getId() == null ? 0L : group.getId();
        HazardPointGroup exist = mapper.checkGroupCodeUnique(group.getCode());
        return exist == null || exist.getId().equals(id);
    }

    /**
     * 批量查询各分组下的隐患点数量，避免N+1查询问题
     */
    private void enrichPointCounts(List<HazardPointGroup> groups) {
        if (groups == null || groups.isEmpty()) {
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

        for (HazardPointGroup group : groups) {
            group.setCount(countMap.getOrDefault(group.getId(), 0));
        }
    }
}
