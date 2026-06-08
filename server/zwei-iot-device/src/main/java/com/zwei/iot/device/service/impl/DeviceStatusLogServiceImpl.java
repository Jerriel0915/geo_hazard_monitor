package com.zwei.iot.device.service.impl;

import com.zwei.iot.device.domain.DeviceStatusLog;
import com.zwei.iot.device.mapper.DeviceStatusLogMapper;
import com.zwei.iot.device.service.IDeviceStatusLogService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;

@Service
public class DeviceStatusLogServiceImpl implements IDeviceStatusLogService {

    private final DeviceStatusLogMapper mapper;

    @Autowired
    public DeviceStatusLogServiceImpl(DeviceStatusLogMapper mapper) {
        this.mapper = mapper;
    }

    @Override
    public void saveMaintenanceLog(Long deviceId, String deviceCode, Integer oldStatus, Integer newStatus,
                                   String statusText, String operatorName, String operatorPhone,
                                   String operationDate, String description, String createBy) {
        DeviceStatusLog log = new DeviceStatusLog();
        log.setDeviceId(deviceId);
        log.setDeviceCode(deviceCode);
        log.setOldStatus(oldStatus);
        log.setNewStatus(newStatus);
        log.setStatusText(statusText);
        log.setOperatorName(operatorName);
        log.setOperatorPhone(operatorPhone);
        log.setOperationDate(operationDate != null ? LocalDateTime.parse(operationDate, DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss")) : LocalDateTime.now());
        log.setDescription(description);
        log.setCreateBy(createBy);
        mapper.insert(log);
    }

    @Override
    public List<DeviceStatusLog> getLogsByDeviceId(Long deviceId) {
        return mapper.selectByDeviceId(deviceId);
    }
}
