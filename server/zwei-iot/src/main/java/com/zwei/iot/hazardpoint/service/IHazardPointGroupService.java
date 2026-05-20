package com.zwei.iot.hazardpoint.service;

import com.zwei.iot.hazardpoint.domain.HazardPointGroup;

import java.util.List;

public interface IHazardPointGroupService
{
    public List<HazardPointGroup> selectHazardPointGroupList(HazardPointGroup group);
    public List<HazardPointGroup> selectHazardPointGroupAll();
    public HazardPointGroup selectHazardPointGroupById(Long id);
    public int insertHazardPointGroup(HazardPointGroup group);
    public int updateHazardPointGroup(HazardPointGroup group);
    public int deleteHazardPointGroupById(Long id);
    public int deleteHazardPointGroupByIds(Long[] ids);
    public boolean checkGroupCodeUnique(HazardPointGroup group);
}
