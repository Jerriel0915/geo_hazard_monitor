package com.zwei.iot.device.service;

import java.util.List;
import java.util.Map;

/**
 * 设备/传感器/隐患点统计查询服务接口。
 * <p>
 * 为 monitor 模块提供数据聚合查询能力，隐藏内部 Mapper 细节和表结构。
 * 所有方法返回 Map 格式的统计结果，调用方无需了解底层数据模型。
 */
public interface IDeviceStatService {

    // ==================== 设备统计 ====================

    int countAllDevices();

    List<Map<String, Object>> countDevicesByStatus();

    List<Map<String, Object>> countDevicesByRunStatus();

    List<Map<String, Object>> countDevicesByMonitorType();

    // ==================== 传感器统计 ====================

    int countAllSensors();

    List<Map<String, Object>> countSensorsByStatus();

    List<Map<String, Object>> countSensorsByMonitorType();

    // ==================== 隐患点统计 ====================

    int countAllHazardPoints();

    List<Map<String, Object>> countHazardPointsByStatus();

    List<Map<String, Object>> countHazardPointsByMonth(int months);

    // ==================== 监测类型/视频设备 ====================

    int countAllMonitorTypes();

    int countAllVideoDevices();

    List<Map<String, Object>> countVideoDevicesByStatus();
}
