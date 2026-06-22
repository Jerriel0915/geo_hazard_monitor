package com.zwei.iot.device.service;

import com.zwei.iot.device.domain.dto.DeviceBasicInfo;
import com.zwei.iot.device.domain.dto.DeviceBriefDTO;

import java.util.Map;
import java.util.Set;

/**
 * 设备简要信息查询服务接口。
 * <p>
 * 为外部模块（如 monitor）提供设备元数据的轻量查询，
 * 返回精简 DTO 而非完整 Domain 实体，隐藏内部数据结构和敏感字段。
 */
public interface IDeviceQueryService {

    /**
     * 按 MQTT 认证用户名查询设备简要信息（含隐患点名称）。
     */
    DeviceBriefDTO getDeviceBriefByAuthUsername(String authUsername);

    /**
     * 批量查询，返回 username → DeviceBriefDTO 映射。
     * <p>
     * 一次调用完成：查 Device → 查 DeviceHazardPoint 关联 → 查 HazardPoint 名称，
     * 避免调用方的 N+1 查询问题。
     */
    Map<String, DeviceBriefDTO> getDeviceBriefsByAuthUsernames(Set<String> usernames);

    /**
     * 按设备 ID 查询基础信息 (供告警引擎解析 device 维度 subject)。
     *
     * @param deviceId 设备主键
     * @return 基础信息; null 表示设备不存在或已逻辑删除
     */
    DeviceBasicInfo getBasicInfoById(Long deviceId);
}
