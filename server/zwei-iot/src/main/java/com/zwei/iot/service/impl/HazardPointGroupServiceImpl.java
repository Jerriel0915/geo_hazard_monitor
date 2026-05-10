package com.zwei.iot.service.impl;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import com.zwei.iot.mapper.HazardPointGroupMapper;
import com.zwei.iot.service.IHazardPointGroupService;
import com.zwei.iot.domain.HazardPointGroup;

/**
 * 隐患点分组管理
 *
 * @author zwei
 */
@Service
public class HazardPointGroupServiceImpl implements IHazardPointGroupService
{
    @Autowired
    private HazardPointGroupMapper mapper;

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
        return mapper.selectHazardPointGroupById(id);
    }

    @Override
    public int insertHazardPointGroup(HazardPointGroup group)
    {
        return mapper.insertHazardPointGroup(group);
    }

    @Override
    public int updateHazardPointGroup(HazardPointGroup group)
    {
        return mapper.updateHazardPointGroup(group);
    }

    @Override
    public int deleteHazardPointGroupById(Long id)
    {
        return mapper.deleteHazardPointGroupById(id);
    }

    @Override
    public int deleteHazardPointGroupByIds(Long[] ids)
    {
        return mapper.deleteHazardPointGroupByIds(ids);
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
