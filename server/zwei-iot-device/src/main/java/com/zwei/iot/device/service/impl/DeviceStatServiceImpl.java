package com.zwei.iot.device.service.impl;

import com.zwei.iot.device.mapper.DeviceMapper;
import com.zwei.iot.device.mapper.DeviceOnlineStatusMapper;
import com.zwei.iot.device.mapper.DeviceSensorMapper;
import com.zwei.iot.device.service.IDeviceHazardRelationService;
import com.zwei.iot.device.service.IDeviceStatService;
import com.zwei.iot.device.service.IVideoDeviceStatService;
import com.zwei.iot.monitor.mapper.MonitorTypeMapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Map;

/**
 * 设备统计查询服务实现。
 * <p>
 * 封装各 Mapper 的统计查询方法，为 monitor 模块提供统一的统计接口。
 * 纯委托层，不包含额外业务逻辑。
 */
@Service
public class DeviceStatServiceImpl implements IDeviceStatService {

    private final DeviceMapper deviceMapper;
    private final DeviceSensorMapper deviceSensorMapper;
    private final IDeviceHazardRelationService hazardRelationService;
    private final MonitorTypeMapper monitorTypeMapper;
    private final IVideoDeviceStatService videoDeviceStatService;
    private final DeviceOnlineStatusMapper onlineStatusMapper;

    @Autowired
    public DeviceStatServiceImpl(DeviceMapper deviceMapper,
                                 DeviceSensorMapper deviceSensorMapper,
                                 IDeviceHazardRelationService hazardRelationService,
                                 MonitorTypeMapper monitorTypeMapper,
                                 IVideoDeviceStatService videoDeviceStatService,
                                 DeviceOnlineStatusMapper onlineStatusMapper) {
        this.deviceMapper = deviceMapper;
        this.deviceSensorMapper = deviceSensorMapper;
        this.hazardRelationService = hazardRelationService;
        this.monitorTypeMapper = monitorTypeMapper;
        this.videoDeviceStatService = videoDeviceStatService;
        this.onlineStatusMapper = onlineStatusMapper;
    }

    @Override
    public int countAllDevices() {
        return deviceMapper.countAll();
    }

    @Override
    public List<Map<String, Object>> countDevicesByStatus() {
        return deviceMapper.countByStatus();
    }

    @Override
    public List<Map<String, Object>> countDevicesByRunStatus() {
        return deviceMapper.countByRunStatus();
    }

    @Override
    public List<Map<String, Object>> countDevicesByMonitorType() {
        return deviceMapper.countByMonitorType();
    }

    @Override
    public int countAllSensors() {
        return deviceSensorMapper.countAll();
    }

    @Override
    public List<Map<String, Object>> countSensorsByStatus() {
        return deviceSensorMapper.countByStatus();
    }

    @Override
    public List<Map<String, Object>> countSensorsByMonitorType() {
        return deviceSensorMapper.countByMonitorType();
    }

    @Override
    public int countAllHazardPoints() {
        return hazardRelationService.countAllHazardPoints();
    }

    @Override
    public List<Map<String, Object>> countHazardPointsByStatus() {
        return hazardRelationService.countHazardPointsByStatus();
    }

    @Override
    public List<Map<String, Object>> countHazardPointsByMonth(int months) {
        return hazardRelationService.countHazardPointsByMonth(months);
    }

    @Override
    public int countAllMonitorTypes() {
        return monitorTypeMapper.countAll();
    }

    @Override
    public int countAllVideoDevices() {
        return videoDeviceStatService.countAll();
    }

    @Override
    public List<Map<String, Object>> countVideoDevicesByStatus() {
        return videoDeviceStatService.countByStatus();
    }

    @Override
    public int countDevicesComplete() {
        return deviceMapper.countComplete();
    }

    @Override
    public int countDevicesNormal() {
        return deviceMapper.countNormal();
    }

    @Override
    public int countOnlineDevices() {
        return onlineStatusMapper.countOnline();
    }

    @Override
    public int countActiveDevicesInWindow(int windowMinutes) {
        return onlineStatusMapper.countActiveInWindow(windowMinutes);
    }

    @Override
    public int countActiveSensorsInWindow(int windowMinutes) {
        return deviceSensorMapper.countActiveInWindow(windowMinutes);
    }

    @Override
    public int countSensorsByDeviceOnline() {
        return deviceSensorMapper.countByDeviceOnline();
    }
}
