package com.zwei.iot.device.service;

import java.util.List;
import java.util.Map;

/**
 * 视频设备统计查询服务接口（由 video 模块实现）。
 */
public interface IVideoDeviceStatService {
    int countAll();
    List<Map<String, Object>> countByStatus();
}
