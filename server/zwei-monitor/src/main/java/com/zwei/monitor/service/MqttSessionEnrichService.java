package com.zwei.monitor.service;

import com.zwei.iot.device.domain.dto.DeviceBriefDTO;
import com.zwei.iot.device.service.IDeviceQueryService;
import com.zwei.monitor.domain.MqttClientInfo;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.*;
import java.util.stream.Collectors;

/**
 * MQTT 客户端信息富化服务。
 * <p>
 * 将 mica-mqtt 返回的原始客户端列表通过 IDeviceQueryService 接口与本地业务数据关联，
 * 补全设备名称、隐患点名称、设备运行状态等信息。
 * <p>
 * 通过 Service 接口而非 Mapper 访问 IoT 模块数据，避免跨模块数据访问层耦合。
 */
@Slf4j
@Service
public class MqttSessionEnrichService {

    private final IDeviceQueryService deviceQueryService;

    public MqttSessionEnrichService(IDeviceQueryService deviceQueryService) {
        this.deviceQueryService = deviceQueryService;
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
     * <p>
     * 通过 IDeviceQueryService 批量查询设备简要信息，一次调用完成所有设备关联查询，
     * 避免 N+1 查询问题。
     */
    public List<MqttClientInfo> enrichBatch(List<MqttClientInfo> clients) {
        if (clients == null || clients.isEmpty()) {
            return clients != null ? clients : Collections.emptyList();
        }

        // 批量查询设备简要信息（一次调用代替原来的多次循环查询）
        Set<String> usernames = clients.stream()
                .map(MqttClientInfo::getUsername)
                .filter(Objects::nonNull)
                .collect(Collectors.toSet());
        if (usernames.isEmpty()) {
            return clients;
        }
        Map<String, DeviceBriefDTO> deviceMap = deviceQueryService.getDeviceBriefsByAuthUsernames(usernames);

        // 回填扩展字段
        for (MqttClientInfo client : clients) {
            DeviceBriefDTO device = deviceMap.get(client.getUsername());
            if (device != null) {
                client.setDeviceId(device.getId());
                client.setDeviceName(device.getName());
                client.setDeviceCode(device.getCode());
                client.setLastAuthIp(device.getLastAuthIp());
                client.setLastAuthTime(device.getLastAuthTime());
                client.setHazardPointName(device.getHazardPointName());
            }
        }
        return clients;
    }
}
