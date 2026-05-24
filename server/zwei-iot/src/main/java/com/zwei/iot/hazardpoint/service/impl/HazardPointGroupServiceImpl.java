package com.zwei.iot.hazardpoint.service.impl;

import com.zwei.common.exception.ServiceException;
import com.zwei.iot.hazardpoint.domain.HazardPointGroup;
import com.zwei.iot.hazardpoint.mapper.HazardPointGroupMapper;
import com.zwei.iot.hazardpoint.service.IHazardPointGroupService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

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

    @Autowired
    public HazardPointGroupServiceImpl(HazardPointGroupMapper mapper) {
        this.mapper = mapper;
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
    public HazardPointGroup selectHazardPointGroupById(Long id) {
        return mapper.selectHazardPointGroupById(id);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public int insertHazardPointGroup(HazardPointGroup group) {
        return mapper.insertHazardPointGroup(group);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public int updateHazardPointGroup(HazardPointGroup group) {
        HazardPointGroup existing = mapper.selectHazardPointGroupById(group.getId());
        if (existing == null) {
            return 0;
        }
        mergeUpdatableFields(existing, group);
        return mapper.updateHazardPointGroup(group);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public int deleteHazardPointGroupById(Long id) {
        HazardPointGroup existing = mapper.selectHazardPointGroupById(id);
        if (existing == null) {
            throw new ServiceException("分组不存在", 404);
        }
        int pointCount = mapper.countHazardPointsByGroupId(id);
        if (pointCount > 0) {
            throw new ServiceException("该分组下存在隐患点，不允许删除", 400);
        }
        return mapper.deleteHazardPointGroupById(id);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public int deleteHazardPointGroupByIds(Long[] ids) {
        if (ids == null || ids.length == 0) {
            return 0;
        }
        for (Long id : ids) {
            int pointCount = mapper.countHazardPointsByGroupId(id);
            if (pointCount > 0) {
                throw new ServiceException("分组下存在隐患点，不允许批量删除");
            }
        }
        return mapper.deleteHazardPointGroupByIds(ids);
    }

    @Override
    public boolean checkGroupCodeUnique(HazardPointGroup group) {
        if (!StringUtils.hasText(group.getCode())) {
            return true;
        }
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

    private void mergeUpdatableFields(HazardPointGroup existing, HazardPointGroup incoming) {
        if (!StringUtils.hasText(incoming.getCode())) {
            incoming.setCode(existing.getCode());
        }
        if (!StringUtils.hasText(incoming.getName())) {
            incoming.setName(existing.getName());
        }
    }
}
