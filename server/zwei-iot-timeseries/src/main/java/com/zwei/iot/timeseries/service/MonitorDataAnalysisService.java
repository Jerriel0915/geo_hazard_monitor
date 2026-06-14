package com.zwei.iot.timeseries.service;

import com.zwei.common.exception.ServiceException;
import com.zwei.iot.device.domain.DeviceSensor;
import com.zwei.iot.device.service.IDeviceSensorService;
import com.zwei.iot.timeseries.domain.CompletenessReportVO;
import com.zwei.iot.timeseries.domain.TimeWindowSpec;
import com.zwei.iot.timeseries.domain.TrendReportVO;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

/**
 * 监测数据派生分析服务 — 完整度 + 趋势。
 */
@Service
public class MonitorDataAnalysisService {

    private final IotdbTimeSeriesService iotdbService;
    private final IDeviceSensorService deviceSensorService;

    @Autowired
    public MonitorDataAnalysisService(IotdbTimeSeriesService iotdbService,
                                      IDeviceSensorService deviceSensorService) {
        this.iotdbService = iotdbService;
        this.deviceSensorService = deviceSensorService;
    }

    /**
     * 数据完整度统计。
     *
     * @param deviceId           设备ID
     * @param sensorCode         传感器编号
     * @param attrCode           指标编码
     * @param window             时间窗口
     * @param expectedIntervalMs 期望采样间隔(毫秒),为 null 时由 iotdbService 用 60s 兜底
     * @throws ServiceException 传感器不存在
     */
    public CompletenessReportVO completeness(
            Long deviceId, String sensorCode, String attrCode,
            TimeWindowSpec window, Long expectedIntervalMs) {
        DeviceSensor sensor = deviceSensorService.selectSensorByDeviceIdAndCode(deviceId, sensorCode);
        if (sensor == null) {
            throw new ServiceException("传感器不存在: deviceId=" + deviceId + ", sensorCode=" + sensorCode);
        }
        return iotdbService.queryCompleteness(deviceId, sensorCode, attrCode, window, expectedIntervalMs);
    }

    /**
     * 趋势/变化率(端点斜率近似)。
     */
    public TrendReportVO trend(
            Long deviceId, String sensorCode, String attrCode, TimeWindowSpec window) {
        DeviceSensor sensor = deviceSensorService.selectSensorByDeviceIdAndCode(deviceId, sensorCode);
        if (sensor == null) {
            throw new ServiceException("传感器不存在: deviceId=" + deviceId + ", sensorCode=" + sensorCode);
        }
        return iotdbService.queryTrend(deviceId, sensorCode, attrCode, window);
    }
}
