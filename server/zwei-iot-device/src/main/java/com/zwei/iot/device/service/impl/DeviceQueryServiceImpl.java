package com.zwei.iot.device.service.impl;

import com.zwei.iot.device.domain.Device;
import com.zwei.iot.device.domain.dto.DeviceBriefDTO;
import com.zwei.iot.device.mapper.DeviceMapper;
import com.zwei.iot.device.service.IDeviceHazardRelationService;
import com.zwei.iot.device.service.IDeviceQueryService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import java.util.*;

@Slf4j
@Service
public class DeviceQueryServiceImpl implements IDeviceQueryService {
    private final DeviceMapper deviceMapper;
    private final IDeviceHazardRelationService hazardRelationService;

    @Autowired
    public DeviceQueryServiceImpl(DeviceMapper deviceMapper,
                                  IDeviceHazardRelationService hazardRelationService) {
        this.deviceMapper = deviceMapper;
        this.hazardRelationService = hazardRelationService;
    }

    @Override
    public DeviceBriefDTO getDeviceBriefByAuthUsername(String authUsername) {
        Device device = deviceMapper.selectDeviceByAuthUsername(authUsername);
        return device == null ? null : toBriefDTO(device, hazardRelationService.getHazardPointNameByDeviceId(device.getId()));
    }

    @Override
    public Map<String, DeviceBriefDTO> getDeviceBriefsByAuthUsernames(Set<String> usernames) {
        if (usernames == null || usernames.isEmpty()) return Collections.emptyMap();
        Map<String, DeviceBriefDTO> result = new HashMap<>();
        List<Device> devices = new ArrayList<>();
        for (String username : usernames) {
            try {
                Device device = deviceMapper.selectDeviceByAuthUsername(username);
                if (device != null) devices.add(device);
            } catch (Exception e) { log.debug("查询设备失败 username={}: {}", username, e.getMessage()); }
        }
        if (devices.isEmpty()) return result;
        Map<Long, String> hpNameMap = new HashMap<>();
        for (Device device : devices) {
            try { hpNameMap.put(device.getId(), hazardRelationService.getHazardPointNameByDeviceId(device.getId())); }
            catch (Exception e) { log.debug("查询隐患点关联失败 deviceId={}", device.getId()); }
        }
        for (Device device : devices)
            result.put(device.getAuthUsername(), toBriefDTO(device, hpNameMap.get(device.getId())));
        return result;
    }

    private DeviceBriefDTO toBriefDTO(Device device, String hazardPointName) {
        return new DeviceBriefDTO(device.getId(), device.getName(), device.getCode(),
                device.getLastAuthIp(), device.getLastAuthTime(), hazardPointName);
    }
}
