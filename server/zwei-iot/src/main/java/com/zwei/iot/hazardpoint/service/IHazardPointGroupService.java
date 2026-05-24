package com.zwei.iot.hazardpoint.service;

import com.zwei.iot.hazardpoint.domain.HazardPointGroup;

import java.util.List;

public interface IHazardPointGroupService
{
    List<HazardPointGroup> selectHazardPointGroupList(HazardPointGroup group);
    List<HazardPointGroup> selectHazardPointGroupAll();
    HazardPointGroup selectHazardPointGroupById(Long id);
    int insertHazardPointGroup(HazardPointGroup group);
    int updateHazardPointGroup(HazardPointGroup group);
    int deleteHazardPointGroupById(Long id);
    int deleteHazardPointGroupByIds(Long[] ids);
    boolean checkGroupCodeUnique(HazardPointGroup group);
}
