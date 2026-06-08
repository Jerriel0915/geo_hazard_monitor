package com.zwei.iot.device.service;

import com.zwei.iot.device.domain.DeviceStatusLog;

import java.util.List;

public interface IDeviceStatusLogService {
    void saveMaintenanceLog(Long deviceId, String deviceCode, Integer oldStatus, Integer newStatus,
                            String statusText, String operatorName, String operatorPhone,
                            String operationDate, String description, String createBy);

    List<DeviceStatusLog> getLogsByDeviceId(Long deviceId);
}
