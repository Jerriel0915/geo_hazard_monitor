package com.zwei.iot.device.service.impl;

import com.zwei.iot.device.domain.DeviceSensor;
import com.zwei.iot.device.domain.SensorAttribute;
import com.zwei.iot.device.mapper.DeviceSensorMapper;
import com.zwei.iot.device.mapper.SensorAttributeMapper;
import com.zwei.iot.device.service.IDeviceSensorService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

/**
 * 传感器Service实现
 *
 * @author zwei
 */
@Service
public class DeviceSensorServiceImpl implements IDeviceSensorService {
    private final DeviceSensorMapper sensorMapper;
    private final SensorAttributeMapper attributeMapper;

    @Autowired
    public DeviceSensorServiceImpl(DeviceSensorMapper sensorMapper, SensorAttributeMapper attributeMapper) {
        this.sensorMapper = sensorMapper;
        this.attributeMapper = attributeMapper;
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
        // 插入传感器
        sensorMapper.insertSensor(sensor);
        // 插入属性
        if (attrList != null && !attrList.isEmpty()) {
            for (SensorAttribute attr : attrList) {
                attr.setSensorId(sensor.getId());
                attributeMapper.insertAttribute(attr);
            }
        }
        return sensor.getId();
    }

    /**
     * 修改传感器
     */
    @Override
    @Transactional
    public int updateSensor(DeviceSensor sensor, List<SensorAttribute> attrList) {
        // 更新传感器基本信息
        int rows = sensorMapper.updateSensor(sensor);
        // 删除原有属性
        attributeMapper.deleteAttributeBySensorId(sensor.getId());
        // 插入新属性
        if (attrList != null && !attrList.isEmpty()) {
            for (SensorAttribute attr : attrList) {
                attr.setSensorId(sensor.getId());
                attributeMapper.insertAttribute(attr);
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
        // 删除属性
        attributeMapper.deleteAttributeBySensorId(id);
        // 删除传感器
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
}