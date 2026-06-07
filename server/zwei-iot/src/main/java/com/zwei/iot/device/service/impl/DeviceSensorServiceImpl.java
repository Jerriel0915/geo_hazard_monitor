package com.zwei.iot.device.service.impl;

import com.zwei.common.exception.ServiceException;
import com.zwei.iot.device.domain.Device;
import com.zwei.iot.device.domain.DeviceSensor;
import com.zwei.iot.device.domain.SensorAttribute;
import com.zwei.iot.device.mapper.DeviceMapper;
import com.zwei.iot.device.mapper.DeviceSensorMapper;
import com.zwei.iot.device.mapper.SensorAttributeMapper;
import com.zwei.iot.device.service.IDeviceSensorService;
import com.zwei.iot.monitor.domain.MonitorType;
import com.zwei.iot.monitor.service.IMonitorTypeService;
import com.zwei.iot.timeseries.service.IotdbTimeSeriesService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.*;

/**
 * 传感器Service实现
 *
 * @author zwei
 */
@Service
public class DeviceSensorServiceImpl implements IDeviceSensorService {
    private static final int SENSOR_MONITOR_DEVICE_TYPE = 2;

    private final DeviceMapper deviceMapper;
    private final DeviceSensorMapper sensorMapper;
    private final SensorAttributeMapper attributeMapper;
    private final IMonitorTypeService monitorTypeService;
    private final IotdbTimeSeriesService iotdbTimeSeriesService;

    @Autowired
    public DeviceSensorServiceImpl(DeviceMapper deviceMapper,
                                   DeviceSensorMapper sensorMapper,
                                   SensorAttributeMapper attributeMapper,
                                   IMonitorTypeService monitorTypeService,
                                   IotdbTimeSeriesService iotdbTimeSeriesService) {
        this.deviceMapper = deviceMapper;
        this.sensorMapper = sensorMapper;
        this.attributeMapper = attributeMapper;
        this.monitorTypeService = monitorTypeService;
        this.iotdbTimeSeriesService = iotdbTimeSeriesService;
    }

    /**
     * 根据设备ID查询传感器列表
     */
    @Override
    public List<DeviceSensor> selectSensorListByDeviceId(Long deviceId) {
        List<DeviceSensor> sensors = sensorMapper.selectSensorListByDeviceId(deviceId);
        for (DeviceSensor sensor : sensors) {
            List<SensorAttribute> attrs = attributeMapper.selectAttributeListBySensorId(sensor.getId());
            sensor.setAttrList(attrs);
        }
        return sensors;
    }

    /**
     * 根据ID查询传感器详情
     */
    @Override
    public DeviceSensor selectSensorById(Long id) {
        DeviceSensor sensor = sensorMapper.selectSensorById(id);
        if (sensor != null) {
            List<SensorAttribute> attrs = attributeMapper.selectAttributeListBySensorId(id);
            sensor.setAttrList(attrs);
        }
        return sensor;
    }

    /**
     * 新增传感器
     */
    @Override
    @Transactional
    public Long insertSensor(DeviceSensor sensor, List<SensorAttribute> attrList) {
        Device device = requireDevice(sensor.getDeviceId());
        if (!checkSensorCodeUnique(sensor.getSensorCode(), 0L)) {
            throw new ServiceException("传感器编码已存在");
        }
        fillDeviceFields(sensor, device);
        fillMonitorTypeFields(sensor, requireSensorMonitorType(sensor.getMonitorTypeId()));
        validateAttributeList(attrList);

        sensorMapper.insertSensor(sensor);
        for (SensorAttribute attr : attrList) {
            attr.setSensorId(sensor.getId());
            attr.setCreateBy(sensor.getCreateBy());
            attributeMapper.insertAttribute(attr);
        }
        // 注册时预创建 IoTDB 时序 schema，将 DDL 从写入热路径提前到注册冷路径
        iotdbTimeSeriesService.createSensorSchema(sensor.getDeviceId(), sensor.getSensorNo(),
                attrList.stream().map(SensorAttribute::getAttrCode).toList());
        return sensor.getId();
    }

    /**
     * 修改传感器
     */
    @Override
    @Transactional
    public int updateSensor(DeviceSensor sensor, List<SensorAttribute> attrList) {
        DeviceSensor existing = sensorMapper.selectSensorById(sensor.getId());
        if (existing == null) {
            throw new ServiceException("传感器不存在");
        }

        fillDeviceFields(sensor, requireDevice(existing.getDeviceId()));
        Long monitorTypeId = sensor.getMonitorTypeId() != null ? sensor.getMonitorTypeId() : existing.getMonitorTypeId();
        fillMonitorTypeFields(sensor, requireSensorMonitorType(monitorTypeId));
        validateAttributeList(attrList);

        int rows = sensorMapper.updateSensor(sensor);
        if (rows <= 0) {
            return rows;
        }

        List<SensorAttribute> existingAttrs = attributeMapper.selectAttributeListBySensorId(sensor.getId());
        Map<Long, SensorAttribute> existingAttrMap = new HashMap<>();
        for (SensorAttribute existingAttr : existingAttrs) {
            existingAttrMap.put(existingAttr.getId(), existingAttr);
        }

        Set<Long> retainedIds = new HashSet<>();
        for (SensorAttribute attr : attrList) {
            if (attr.getId() == null) {
                attr.setSensorId(sensor.getId());
                attr.setCreateBy(sensor.getUpdateBy());
                attributeMapper.insertAttribute(attr);
                continue;
            }

            SensorAttribute currentAttr = existingAttrMap.get(attr.getId());
            if (currentAttr == null) {
                throw new ServiceException("属性不存在或不属于当前传感器");
            }

            retainedIds.add(attr.getId());
            attr.setSensorId(sensor.getId());
            attr.setUpdateBy(sensor.getUpdateBy());
            attributeMapper.updateAttribute(attr);
        }

        for (SensorAttribute existingAttr : existingAttrs) {
            if (!retainedIds.contains(existingAttr.getId())) {
                attributeMapper.deleteAttributeById(existingAttr.getId());
            }
        }
        return rows;
    }

    /**
     * 删除传感器（逻辑删除）
     */
    @Override
    @Transactional
    public int deleteSensorById(Long id) {
        attributeMapper.deleteAttributeBySensorId(id);
        return sensorMapper.deleteSensorById(id);
    }

    /**
     * 校验传感器编码是否唯一
     */
    @Override
    public boolean checkSensorCodeUnique(String sensorCode, Long id) {
        DeviceSensor result = sensorMapper.checkSensorCodeUnique(sensorCode, id);
        return result == null;
    }

    /**
     * 根据传感器条件查询传感器列表
     */
    @Override
    public List<DeviceSensor> selectSensorList(DeviceSensor sensor) {
        return sensorMapper.selectSensorList(sensor);
    }

    private Device requireDevice(Long deviceId) {
        if (deviceId == null) {
            throw new ServiceException("设备ID不能为空");
        }
        Device device = deviceMapper.selectDeviceById(deviceId);
        if (device == null) {
            throw new ServiceException("设备不存在");
        }
        return device;
    }

    private MonitorType requireSensorMonitorType(Long monitorTypeId) {
        if (monitorTypeId == null) {
            throw new ServiceException("监测类型ID不能为空");
        }
        MonitorType monitorType = monitorTypeService.selectMonitorTypeById(monitorTypeId);
        if (monitorType == null) {
            throw new ServiceException("监测类型不存在");
        }
        if (!Objects.equals(monitorType.getDeviceType(), SENSOR_MONITOR_DEVICE_TYPE)) {
            throw new ServiceException("仅允许选择设备类型为传感器的监测类型");
        }
        return monitorType;
    }

    private void fillDeviceFields(DeviceSensor sensor, Device device) {
        sensor.setDeviceId(device.getId());
        sensor.setDeviceCode(device.getCode());
        if (sensor.getSensorNo() == null || sensor.getSensorNo().isBlank()) {
            sensor.setSensorNo(sensor.getSensorCode());
        }
    }

    private void fillMonitorTypeFields(DeviceSensor sensor, MonitorType monitorType) {
        sensor.setMonitorTypeId(monitorType.getId());
        sensor.setMonitorTypeCode(monitorType.getCode());
        sensor.setMonitorTypeName(monitorType.getName());
    }

    private void validateAttributeList(List<SensorAttribute> attrList) {
        if (attrList == null || attrList.isEmpty()) {
            throw new ServiceException("属性列表不能为空");
        }

        Set<String> attrCodeSet = new HashSet<>();
        Set<Long> attrIdSet = new HashSet<>();
        for (SensorAttribute attr : attrList) {
            if (attr == null) {
                throw new ServiceException("属性列表存在空数据");
            }
            if (!attrCodeSet.add(attr.getAttrCode())) {
                throw new ServiceException("属性编码不能重复");
            }
            if (attr.getId() != null && !attrIdSet.add(attr.getId())) {
                throw new ServiceException("属性ID不能重复");
            }
            if (attr.getRangeMin() != null && attr.getRangeMax() != null
                    && attr.getRangeMin().compareTo(attr.getRangeMax()) > 0) {
                throw new ServiceException("属性最小值不能大于最大值");
            }
        }
    }

    @Override
    public void updateLastReportTime(Long sensorId, String lastReportTime) {
        sensorMapper.updateLastReportTime(sensorId, lastReportTime);
    }
}
