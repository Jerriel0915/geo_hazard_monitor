package com.zwei.iot.device.service;

import com.zwei.iot.device.domain.DeviceOnlineEventLog;

import java.util.List;

/**
 * 设备上下线事件日志查询服务。
 */
public interface IDeviceOnlineEventLogService {

    /**
     * 查询指定设备最近的上下线事件。
     *
     * @param deviceId 设备ID
     * @param limit    最大返回条数
     * @return 事件日志列表
     */
    List<DeviceOnlineEventLog> selectByDeviceId(Long deviceId, int limit);
}
