package com.zwei.iot.service;

import java.util.List;
import com.zwei.iot.domain.HazardPointGroup;

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
