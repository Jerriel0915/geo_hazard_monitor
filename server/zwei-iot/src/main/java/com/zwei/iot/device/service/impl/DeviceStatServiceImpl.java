package com.zwei.iot.device.service.impl;

import com.zwei.iot.device.mapper.DeviceMapper;
import com.zwei.iot.device.mapper.DeviceOnlineStatusMapper;
import com.zwei.iot.device.mapper.DeviceSensorMapper;
import com.zwei.iot.device.service.IDeviceStatService;
import com.zwei.iot.hazardpoint.mapper.HazardPointMapper;
import com.zwei.iot.monitor.mapper.MonitorTypeMapper;
import com.zwei.iot.video.mapper.VideoDeviceMapper;
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
    private final HazardPointMapper hazardPointMapper;
    private final MonitorTypeMapper monitorTypeMapper;
    private final VideoDeviceMapper videoDeviceMapper;
    private final DeviceOnlineStatusMapper onlineStatusMapper;

    @Autowired
    public DeviceStatServiceImpl(DeviceMapper deviceMapper,
                                 DeviceSensorMapper deviceSensorMapper,
                                 HazardPointMapper hazardPointMapper,
                                 MonitorTypeMapper monitorTypeMapper,
                                 VideoDeviceMapper videoDeviceMapper,
                                 DeviceOnlineStatusMapper onlineStatusMapper) {
        this.deviceMapper = deviceMapper;
        this.deviceSensorMapper = deviceSensorMapper;
        this.hazardPointMapper = hazardPointMapper;
        this.monitorTypeMapper = monitorTypeMapper;
        this.videoDeviceMapper = videoDeviceMapper;
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
        return hazardPointMapper.countAll();
    }

    @Override
    public List<Map<String, Object>> countHazardPointsByStatus() {
        return hazardPointMapper.countByStatus();
    }

    @Override
    public List<Map<String, Object>> countHazardPointsByMonth(int months) {
        return hazardPointMapper.countByMonth(months);
    }

    @Override
    public int countAllMonitorTypes() {
        return monitorTypeMapper.countAll();
    }

    @Override
    public int countAllVideoDevices() {
        return videoDeviceMapper.countAll();
    }

    @Override
    public List<Map<String, Object>> countVideoDevicesByStatus() {
        return videoDeviceMapper.countByStatus();
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
}
