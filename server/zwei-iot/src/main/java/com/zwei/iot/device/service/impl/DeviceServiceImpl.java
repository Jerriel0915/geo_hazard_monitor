package com.zwei.iot.device.service.impl;

import com.zwei.iot.device.domain.Device;
import com.zwei.iot.device.domain.DeviceSensor;
import com.zwei.iot.device.domain.SensorAttribute;
import com.zwei.iot.device.mapper.DeviceMapper;
import com.zwei.iot.device.mapper.DeviceSensorMapper;
import com.zwei.iot.device.mapper.SensorAttributeMapper;
import com.zwei.iot.device.service.IDeviceService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

/**
 * 设备Service实现
 *
 * @author zwei
 */
@Service
public class DeviceServiceImpl implements IDeviceService {
    private final DeviceMapper deviceMapper;
    private final DeviceSensorMapper sensorMapper;
    private final SensorAttributeMapper attributeMapper;

    @Autowired
    public DeviceServiceImpl(DeviceMapper deviceMapper, DeviceSensorMapper sensorMapper,
                             SensorAttributeMapper attributeMapper) {
        this.deviceMapper = deviceMapper;
        this.sensorMapper = sensorMapper;
        this.attributeMapper = attributeMapper;
    }

    /**
     * 分页查询设备列表
     */
    @Override
    public List<Device> selectDevicePage(Device device, int pageNum, int pageSize) {
        return deviceMapper.selectDeviceList(device);
    }

    /**
     * 查询所有设备列表
     */
    @Override
    public List<Device> selectDeviceAll() {
        return deviceMapper.selectDeviceAll();
    }

    /**
     * 根据ID查询设备详情
     */
    @Override
    public Device selectDeviceById(Long id) {
        Device device = deviceMapper.selectDeviceById(id);
        if (device != null) {
            // 查询设备下的传感器列表
            List<DeviceSensor> sensors = sensorMapper.selectSensorListByDeviceId(id);
            for (DeviceSensor sensor : sensors) {
                // 查询传感器的属性列表
                List<SensorAttribute> attrs = attributeMapper.selectAttributeListBySensorId(sensor.getId());
                sensor.setAttrList(attrs);
            }
            device.setSensors(sensors);
        }
        return device;
    }

    /**
     * 新增设备
     */
    @Override
    public int insertDevice(Device device) {
        return deviceMapper.insertDevice(device);
    }

    /**
     * 修改设备
     */
    @Override
    public int updateDevice(Device device) {
        return deviceMapper.updateDevice(device);
    }

    /**
     * 删除设备（逻辑删除）
     */
    @Override
    public int deleteDeviceById(Long id) {
        // 删除设备下的传感器
        sensorMapper.deleteSensorByDeviceId(id);
        // 删除设备
        return deviceMapper.deleteDeviceById(id);
    }

    /**
     * 批量删除设备（逻辑删除）
     */
    @Override
    public int deleteDeviceByIds(Long[] ids) {
        for (Long id : ids) {
            // 删除设备下的传感器
            sensorMapper.deleteSensorByDeviceId(id);
        }
        return deviceMapper.deleteDeviceByIds(ids);
    }

    /**
     * 复制设备
     */
    @Override
    @Transactional
    public Long copyDevice(Long id) {
        Device original = deviceMapper.selectDeviceById(id);
        if (original == null) {
            return null;
        }
        // 创建新设备
        Device copy = new Device();
        copy.setCode(original.getCode() + "_copy");
        copy.setName(original.getName() + "_副本");
        copy.setIcon(original.getIcon());
        copy.setIconPath(original.getIconPath());
        copy.setStatus(original.getStatus());
        copy.setCreateBy(original.getCreateBy());
        deviceMapper.insertDevice(copy);

        // 复制传感器
        List<DeviceSensor> sensors = sensorMapper.selectSensorListByDeviceId(id);
        for (DeviceSensor originalSensor : sensors) {
            DeviceSensor newSensor = new DeviceSensor();
            newSensor.setDeviceId(copy.getId());
            newSensor.setDeviceCode(copy.getCode());
            newSensor.setSensorCode(originalSensor.getSensorCode() + "_copy");
            newSensor.setSensorName(originalSensor.getSensorName());
            newSensor.setMonitorTypeId(originalSensor.getMonitorTypeId());
            newSensor.setMonitorTypeCode(originalSensor.getMonitorTypeCode());
            newSensor.setMonitorTypeName(originalSensor.getMonitorTypeName());
            newSensor.setStatus(originalSensor.getStatus());
            newSensor.setCreateBy(original.getCreateBy());
            sensorMapper.insertSensor(newSensor);

            // 复制属性
            List<SensorAttribute> attrs = attributeMapper.selectAttributeListBySensorId(originalSensor.getId());
            if (!attrs.isEmpty()) {
                for (SensorAttribute attr : attrs) {
                    attr.setSensorId(newSensor.getId());
                }
                attributeMapper.batchInsertAttribute(attrs);
            }
        }
        return copy.getId();
    }

    /**
     * 校验设备编码是否唯一
     */
    @Override
    public boolean checkDeviceCodeUnique(Device device) {
        Device result = deviceMapper.checkDeviceCodeUnique(device.getCode(), device.getId());
        return result == null;
    }

    /**
     * 获取设备传感器列表
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
}