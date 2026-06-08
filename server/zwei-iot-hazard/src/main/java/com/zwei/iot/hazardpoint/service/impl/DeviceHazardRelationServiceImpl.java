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

/**
 * 设备-隐患点关联关系跨模块服务实现。
 *
 * <p>实现 zwei-iot-device 中定义的 {@link IDeviceHazardRelationService} 接口，
 * 使 device 模块可以查询设备所属隐患点、刷新 device_count 等，
 * 而不直接依赖 hazard 模块的 Mapper。
 *
 * <p>所有方法均为薄封装委托，实际数据访问由 {@link DeviceHazardPointMapper} 和
 * {@link HazardPointMapper} 完成。
 */
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
