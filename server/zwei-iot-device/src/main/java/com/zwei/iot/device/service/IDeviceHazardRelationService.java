package com.zwei.iot.device.service;

import com.zwei.iot.device.domain.brief.DeviceBrief;

import java.util.List;
import java.util.Map;

/**
 * 设备-隐患点关联查询服务接口（由 hazardpoint 模块实现）。
 */
public interface IDeviceHazardRelationService {

    /**
     * 隐患点简要引用（id + 名称）。
     */
    record HazardPointRef(Long id, String name) {}

    List<Long> getHazardPointIdsByDeviceIds(List<Long> deviceIds);
    void deleteBindingsByDeviceIds(List<Long> deviceIds);
    void refreshDeviceCount(Long hazardPointId);
    void refreshDeviceCountByIds(List<Long> hazardPointIds);
    String getHazardPointNameByDeviceId(Long deviceId);

    /**
     * 根据设备 ID 反查其绑定的隐患点（业务规则：1 设备 ≤ 1 HP）。
     * @return 绑定的 HP 引用（id+name），无绑定时返回 null
     */
    HazardPointRef getHazardPointByDeviceId(Long deviceId);

    /**
     * 绑定设备到隐患点（用于设备编辑页直接修改关联隐患点）。
     * @param deviceId 设备ID
     * @param hazardPointId 隐患点ID
     * @param installLongitude 安装经度（可使用设备经度）
     * @param installLatitude 安装纬度（可使用设备纬度）
     * @param operator 操作人
     */
    void bindDevice(Long deviceId, Long hazardPointId, Double installLongitude, Double installLatitude, String operator);

    int countAllHazardPoints();
    List<Map<String, Object>> countHazardPointsByStatus();
    List<Map<String, Object>> countHazardPointsByMonth(int months);

    /**
     * 列出隐患点绑定的所有设备(摘要视图, 含在线状态)。
     * 实现侧通过 device_hazard_point JOIN device LEFT JOIN device_online_status 完成。
     */
    List<DeviceBrief> getDevicesByHazardPoint(Long hazardPointId);
}
