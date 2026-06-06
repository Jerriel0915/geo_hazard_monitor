package com.zwei.iot.hazardpoint.mapper;

import com.zwei.iot.hazardpoint.domain.HazardPointGroup;
import org.apache.ibatis.annotations.Mapper;

import java.util.List;
import java.util.Map;

@Mapper
public interface HazardPointGroupMapper
{
    List<HazardPointGroup> selectHazardPointGroupList(HazardPointGroup group);
    List<HazardPointGroup> selectHazardPointGroupAll();
    HazardPointGroup selectHazardPointGroupById(Long id);
    HazardPointGroup selectHazardPointGroupByCode(String code);
    int insertHazardPointGroup(HazardPointGroup group);
    int updateHazardPointGroup(HazardPointGroup group);
    int deleteHazardPointGroupById(Long id);
    int deleteHazardPointGroupByIds(Long[] ids);
    HazardPointGroup checkGroupCodeUnique(String code);
    List<Map<String, Object>> countHazardPointsByGroupIds(List<Long> groupIds);
    int countHazardPointsByGroupId(Long groupId);
}
