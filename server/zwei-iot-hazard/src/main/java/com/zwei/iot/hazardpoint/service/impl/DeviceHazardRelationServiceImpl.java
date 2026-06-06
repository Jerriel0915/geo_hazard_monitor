package com.zwei.iot.hazardpoint.service.impl;

import com.zwei.iot.device.service.IDeviceHazardRelationService;
import com.zwei.iot.hazardpoint.domain.HazardPoint;
import com.zwei.iot.hazardpoint.mapper.DeviceHazardPointMapper;
import com.zwei.iot.hazardpoint.mapper.HazardPointMapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.Collections;
import java.util.List;
import java.util.Map;

@Service
public class DeviceHazardRelationServiceImpl implements IDeviceHazardRelationService {
    private final DeviceHazardPointMapper deviceHazardPointMapper;
    private final HazardPointMapper hazardPointMapper;

    @Autowired
    public DeviceHazardRelationServiceImpl(DeviceHazardPointMapper deviceHazardPointMapper,
                                           HazardPointMapper hazardPointMapper) {
        this.deviceHazardPointMapper = deviceHazardPointMapper;
        this.hazardPointMapper = hazardPointMapper;
    }

    @Override public List<Long> getHazardPointIdsByDeviceIds(List<Long> ids) { return deviceHazardPointMapper.selectHazardPointIdsByDeviceIds(ids); }
    @Override public void deleteBindingsByDeviceIds(List<Long> ids) { deviceHazardPointMapper.deleteByDeviceIds(ids); }
    @Override public void refreshDeviceCount(Long id) { hazardPointMapper.refreshDeviceCountById(id); }
    @Override public void refreshDeviceCountByIds(List<Long> ids) { for (Long id : ids) hazardPointMapper.refreshDeviceCountById(id); }

    @Override
    public String getHazardPointNameByDeviceId(Long deviceId) {
        List<Long> hpIds = deviceHazardPointMapper.selectHazardPointIdsByDeviceIds(Collections.singletonList(deviceId));
        if (hpIds != null && !hpIds.isEmpty()) {
            HazardPoint hp = hazardPointMapper.selectHazardPointById(hpIds.get(0));
            return hp != null ? hp.getName() : null;
        }
        return null;
    }

    @Override public int countAllHazardPoints() { return hazardPointMapper.countAll(); }
    @Override public List<Map<String, Object>> countHazardPointsByStatus() { return hazardPointMapper.countByStatus(); }
    @Override public List<Map<String, Object>> countHazardPointsByMonth(int months) { return hazardPointMapper.countByMonth(months); }
}
