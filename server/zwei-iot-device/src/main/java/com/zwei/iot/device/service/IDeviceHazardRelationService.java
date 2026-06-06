package com.zwei.iot.device.service;

import java.util.List;
import java.util.Map;

/**
 * 设备-隐患点关联查询服务接口（由 hazardpoint 模块实现）。
 */
public interface IDeviceHazardRelationService {
    List<Long> getHazardPointIdsByDeviceIds(List<Long> deviceIds);
    void deleteBindingsByDeviceIds(List<Long> deviceIds);
    void refreshDeviceCount(Long hazardPointId);
    void refreshDeviceCountByIds(List<Long> hazardPointIds);
    String getHazardPointNameByDeviceId(Long deviceId);
    int countAllHazardPoints();
    List<Map<String, Object>> countHazardPointsByStatus();
    List<Map<String, Object>> countHazardPointsByMonth(int months);
}
