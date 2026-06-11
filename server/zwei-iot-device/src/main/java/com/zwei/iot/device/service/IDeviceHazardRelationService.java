package com.zwei.iot.device.service;

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

    int countAllHazardPoints();
    List<Map<String, Object>> countHazardPointsByStatus();
    List<Map<String, Object>> countHazardPointsByMonth(int months);
}
