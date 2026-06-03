package com.zwei.monitor.service;

import com.zwei.iot.device.domain.Device;
import com.zwei.iot.device.mapper.DeviceMapper;
import com.zwei.iot.hazardpoint.domain.HazardPoint;
import com.zwei.iot.hazardpoint.mapper.DeviceHazardPointMapper;
import com.zwei.iot.hazardpoint.mapper.HazardPointMapper;
import com.zwei.monitor.domain.MqttClientInfo;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.*;
import java.util.stream.Collectors;

/**
 * MQTT 客户端信息富化服务。
 * <p>
 * 将 mica-mqtt 返回的原始客户端列表与本地业务数据关联，
 * 补全设备名称、隐患点名称、设备运行状态等信息。
 */
@Slf4j
@Service
public class MqttSessionEnrichService {

    private final DeviceMapper deviceMapper;
    private final DeviceHazardPointMapper deviceHazardPointMapper;
    private final HazardPointMapper hazardPointMapper;

    public MqttSessionEnrichService(DeviceMapper deviceMapper,
                                    DeviceHazardPointMapper deviceHazardPointMapper,
                                    HazardPointMapper hazardPointMapper) {
        this.deviceMapper = deviceMapper;
        this.deviceHazardPointMapper = deviceHazardPointMapper;
        this.hazardPointMapper = hazardPointMapper;
    }

    /**
     * 富化单个客户端信息。
     */
    public MqttClientInfo enrich(MqttClientInfo client) {
        if (client == null) {
            return null;
        }
        List<MqttClientInfo> enriched = enrichBatch(Collections.singletonList(client));
        return enriched.isEmpty() ? client : enriched.get(0);
    }

    /**
     * 批量富化客户端信息。
     */
    public List<MqttClientInfo> enrichBatch(List<MqttClientInfo> clients) {
        if (clients == null || clients.isEmpty()) {
            return clients != null ? clients : Collections.emptyList();
        }

        // 根据 username（authUsername）批量查询设备
        Map<String, Device> deviceByUsername = resolveDevices(clients);
        if (deviceByUsername.isEmpty()) {
            return clients;
        }

        // 收集所有已知 deviceId，批量查询隐患点
        List<Long> deviceIds = deviceByUsername.values().stream()
                .map(Device::getId)
                .distinct()
                .collect(Collectors.toList());
        Map<Long, String> hpNameByDeviceId = resolveHazardPointNames(deviceIds);

        // 回填扩展字段
        for (MqttClientInfo client : clients) {
            Device device = deviceByUsername.get(client.getUsername());
            if (device != null) {
                client.setDeviceId(device.getId());
                client.setDeviceName(device.getName());
                client.setDeviceCode(device.getCode());
                client.setDeviceRunStatus(device.getRunStatus());
                client.setLastAuthIp(device.getLastAuthIp());
                client.setLastAuthTime(device.getLastAuthTime());
                client.setHazardPointName(hpNameByDeviceId.get(device.getId()));
            }
        }
        return clients;
    }

    private Map<String, Device> resolveDevices(List<MqttClientInfo> clients) {
        Set<String> usernames = clients.stream()
                .map(MqttClientInfo::getUsername)
                .filter(Objects::nonNull)
                .collect(Collectors.toSet());
        Map<String, Device> result = new HashMap<>();
        for (String username : usernames) {
            try {
                Device device = deviceMapper.selectDeviceByAuthUsername(username);
                if (device != null) {
                    result.put(username, device);
                }
            } catch (Exception e) {
                log.debug("查询设备失败 username={}: {}", username, e.getMessage());
            }
        }
        return result;
    }

    private Map<Long, String> resolveHazardPointNames(List<Long> deviceIds) {
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
}
