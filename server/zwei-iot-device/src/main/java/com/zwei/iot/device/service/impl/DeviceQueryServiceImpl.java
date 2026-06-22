package com.zwei.iot.device.service.impl;

import com.zwei.iot.device.domain.Device;
import com.zwei.iot.device.domain.DeviceOnlineStatus;
import com.zwei.iot.device.domain.dto.DeviceBasicInfo;
import com.zwei.iot.device.domain.dto.DeviceBriefDTO;
import com.zwei.iot.device.mapper.DeviceMapper;
import com.zwei.iot.device.service.DeviceOnlineStatusService;
import com.zwei.iot.device.service.IDeviceHazardRelationService;
import com.zwei.iot.device.service.IDeviceQueryService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import java.text.SimpleDateFormat;
import java.util.*;

@Slf4j
@Service
public class DeviceQueryServiceImpl implements IDeviceQueryService {
    private final DeviceMapper deviceMapper;
    private final IDeviceHazardRelationService hazardRelationService;
    private final DeviceOnlineStatusService deviceOnlineStatusService;

    private static final SimpleDateFormat DATE_FMT = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");

    @Autowired
    public DeviceQueryServiceImpl(DeviceMapper deviceMapper,
                                  IDeviceHazardRelationService hazardRelationService,
                                  DeviceOnlineStatusService deviceOnlineStatusService) {
        this.deviceMapper = deviceMapper;
        this.hazardRelationService = hazardRelationService;
        this.deviceOnlineStatusService = deviceOnlineStatusService;
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

    @Override
    public DeviceBasicInfo getBasicInfoById(Long deviceId) {
        if (deviceId == null) return null;
        Device device = deviceMapper.selectDeviceById(deviceId);
        if (device == null) return null;

        boolean online = false;
        long lastReportAt = 0L;
        try {
            DeviceOnlineStatus onlineStatus = deviceOnlineStatusService.getByDeviceId(deviceId);
            if (onlineStatus != null) {
                online = Integer.valueOf(1).equals(onlineStatus.getStatus());
                lastReportAt = parseTimeToEpochSeconds(onlineStatus.getLastReportAt());
            }
        } catch (Exception e) {
            log.debug("查询设备在线状态失败 deviceId={}: {}", deviceId, e.getMessage());
        }

        // device_online_status 无 lastReportAt 时降级到 device.lastReportTime
        if (lastReportAt == 0L) {
            lastReportAt = parseTimeToEpochSeconds(device.getLastReportTime());
        }

        int status = device.getStatus() != null ? device.getStatus() : 1;
        return new DeviceBasicInfo(online, lastReportAt, status);
    }

    /**
     * 将 "yyyy-MM-dd HH:mm:ss" 字符串解析为 epoch seconds; 解析失败或 null 返回 0。
     */
    private static synchronized long parseTimeToEpochSeconds(String timeStr) {
        if (timeStr == null || timeStr.isBlank()) return 0L;
        try {
            return DATE_FMT.parse(timeStr).getTime() / 1000;
        } catch (Exception ignored) {
            return 0L;
        }
    }
}
