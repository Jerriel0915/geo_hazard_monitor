package com.zwei.iot.device.service.impl;

import com.zwei.iot.device.domain.Device;
import com.zwei.iot.device.domain.dto.DeviceBriefDTO;
import com.zwei.iot.device.mapper.DeviceMapper;
import com.zwei.iot.device.service.IDeviceQueryService;
import com.zwei.iot.hazardpoint.domain.HazardPoint;
import com.zwei.iot.hazardpoint.mapper.DeviceHazardPointMapper;
import com.zwei.iot.hazardpoint.mapper.HazardPointMapper;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.*;
import java.util.stream.Collectors;

/**
 * 设备简要信息查询服务实现。
 * <p>
 * 批量查询设备 + 隐患点关联，一次性组装完整 DTO。
 */
@Slf4j
@Service
public class DeviceQueryServiceImpl implements IDeviceQueryService {

    private final DeviceMapper deviceMapper;
    private final DeviceHazardPointMapper deviceHazardPointMapper;
    private final HazardPointMapper hazardPointMapper;

    @Autowired
    public DeviceQueryServiceImpl(DeviceMapper deviceMapper,
                                  DeviceHazardPointMapper deviceHazardPointMapper,
                                  HazardPointMapper hazardPointMapper) {
        this.deviceMapper = deviceMapper;
        this.deviceHazardPointMapper = deviceHazardPointMapper;
        this.hazardPointMapper = hazardPointMapper;
    }

    @Override
    public DeviceBriefDTO getDeviceBriefByAuthUsername(String authUsername) {
        Device device = deviceMapper.selectDeviceByAuthUsername(authUsername);
        if (device == null) {
            return null;
        }
        return toBriefDTO(device, resolveHazardPointName(device.getId()));
    }

    @Override
    public Map<String, DeviceBriefDTO> getDeviceBriefsByAuthUsernames(Set<String> usernames) {
        if (usernames == null || usernames.isEmpty()) {
            return Collections.emptyMap();
        }
        Map<String, DeviceBriefDTO> result = new HashMap<>();
        List<Device> devices = new ArrayList<>();
        for (String username : usernames) {
            try {
                Device device = deviceMapper.selectDeviceByAuthUsername(username);
                if (device != null) {
                    devices.add(device);
                }
            } catch (Exception e) {
                log.debug("查询设备失败 username={}: {}", username, e.getMessage());
            }
        }
        if (devices.isEmpty()) {
            return result;
        }
        // 批量解析隐患点名称
        List<Long> deviceIds = devices.stream().map(Device::getId).distinct().collect(Collectors.toList());
        Map<Long, String> hpNameMap = resolveHazardPointNamesBatch(deviceIds);
        for (Device device : devices) {
            result.put(device.getAuthUsername(), toBriefDTO(device, hpNameMap.get(device.getId())));
        }
        return result;
    }

    private String resolveHazardPointName(Long deviceId) {
        List<Long> hpIds = deviceHazardPointMapper
                .selectHazardPointIdsByDeviceIds(Collections.singletonList(deviceId));
        if (hpIds != null && !hpIds.isEmpty()) {
            HazardPoint hp = hazardPointMapper.selectHazardPointById(hpIds.get(0));
            return hp != null ? hp.getName() : null;
        }
        return null;
    }

    private Map<Long, String> resolveHazardPointNamesBatch(List<Long> deviceIds) {
        if (deviceIds.isEmpty()) {
            return Collections.emptyMap();
        }
        Map<Long, String> result = new HashMap<>();
        for (Long deviceId : deviceIds) {
            try {
                List<Long> hpIds = deviceHazardPointMapper
                        .selectHazardPointIdsByDeviceIds(Collections.singletonList(deviceId));
                if (hpIds != null && !hpIds.isEmpty()) {
                    HazardPoint hp = hazardPointMapper.selectHazardPointById(hpIds.get(0));
                    if (hp != null) {
                        result.put(deviceId, hp.getName());
                    }
                }
            } catch (Exception e) {
                log.debug("查询隐患点关联失败 deviceId={}: {}", deviceId, e.getMessage());
            }
        }
        return result;
    }

    private DeviceBriefDTO toBriefDTO(Device device, String hazardPointName) {
        return new DeviceBriefDTO(
                device.getId(),
                device.getName(),
                device.getCode(),
                device.getRunStatus(),
                device.getLastAuthIp(),
                device.getLastAuthTime(),
                hazardPointName
        );
    }
}
