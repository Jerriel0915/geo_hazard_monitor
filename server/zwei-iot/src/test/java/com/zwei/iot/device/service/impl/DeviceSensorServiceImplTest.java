package com.zwei.iot.device.service.impl;

import com.zwei.common.exception.ServiceException;
import com.zwei.iot.device.domain.Device;
import com.zwei.iot.device.domain.DeviceSensor;
import com.zwei.iot.device.domain.SensorAttribute;
import com.zwei.iot.device.mapper.DeviceMapper;
import com.zwei.iot.device.mapper.DeviceSensorMapper;
import com.zwei.iot.device.mapper.SensorAttributeMapper;
import com.zwei.iot.monitor.domain.MonitorType;
import com.zwei.iot.monitor.service.IMonitorTypeService;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
@DisplayName("DeviceSensorServiceImpl 单元测试")
class DeviceSensorServiceImplTest {

    @Mock
    private DeviceMapper deviceMapper;

    @Mock
    private DeviceSensorMapper sensorMapper;

    @Mock
    private SensorAttributeMapper attributeMapper;

    @Mock
    private IMonitorTypeService monitorTypeService;

    @InjectMocks
    private DeviceSensorServiceImpl service;

    @Test
    @DisplayName("新增传感器时应校验监测类型必须为传感器类型并回填快照字段")
    void insertSensor_shouldValidateMonitorTypeAndFillSnapshotFields() {
        DeviceSensor sensor = new DeviceSensor();
        sensor.setDeviceId(10L);
        sensor.setSensorCode("SENSOR001");
        sensor.setSensorName("水位传感器");
        sensor.setMonitorTypeId(4L);
        sensor.setStatus(1);
        sensor.setCreateBy("admin");

        SensorAttribute attr = new SensorAttribute();
        attr.setAttrCode("water_level");
        attr.setAttrName("水位");
        attr.setRangeMin(BigDecimal.ZERO);
        attr.setRangeMax(new BigDecimal("100"));

        Device device = new Device();
        device.setId(10L);
        device.setCode("DEVICE001");
        when(deviceMapper.selectDeviceById(10L)).thenReturn(device);
        when(sensorMapper.checkSensorCodeUnique("SENSOR001", 0L)).thenReturn(null);

        MonitorType monitorType = new MonitorType();
        monitorType.setId(4L);
        monitorType.setCode("JCLX004");
        monitorType.setName("水位监测");
        monitorType.setDeviceType(2);
        when(monitorTypeService.selectMonitorTypeById(4L)).thenReturn(monitorType);

        Long result = service.insertSensor(sensor, List.of(attr));

        assertEquals(sensor.getId(), result);
        assertEquals("DEVICE001", sensor.getDeviceCode());
        assertEquals("JCLX004", sensor.getMonitorTypeCode());
        assertEquals("水位监测", sensor.getMonitorTypeName());
        verify(sensorMapper).insertSensor(sensor);
        verify(attributeMapper).insertAttribute(any(SensorAttribute.class));
    }

    @Test
    @DisplayName("新增传感器时监测类型不是传感器类型应抛出异常")
    void insertSensor_shouldThrowWhenMonitorTypeIsNotSensorType() {
        DeviceSensor sensor = new DeviceSensor();
        sensor.setDeviceId(10L);
        sensor.setSensorCode("SENSOR001");
        sensor.setSensorName("水位传感器");
        sensor.setMonitorTypeId(4L);
        sensor.setStatus(1);

        SensorAttribute attr = new SensorAttribute();
        attr.setAttrCode("water_level");
        attr.setAttrName("水位");

        Device device = new Device();
        device.setId(10L);
        when(deviceMapper.selectDeviceById(10L)).thenReturn(device);
        when(sensorMapper.checkSensorCodeUnique("SENSOR001", 0L)).thenReturn(null);

        MonitorType monitorType = new MonitorType();
        monitorType.setId(4L);
        monitorType.setDeviceType(1);
        when(monitorTypeService.selectMonitorTypeById(4L)).thenReturn(monitorType);

        ServiceException exception = assertThrows(ServiceException.class,
                () -> service.insertSensor(sensor, List.of(attr)));

        assertEquals("仅允许选择设备类型为传感器的监测类型", exception.getMessage());
        verify(sensorMapper, never()).insertSensor(any(DeviceSensor.class));
    }

    @Test
    @DisplayName("修改传感器时应增量更新属性并删除被移除项")
    void updateSensor_shouldUpdateAttributesIncrementally() {
        DeviceSensor incoming = new DeviceSensor();
        incoming.setId(1L);
        incoming.setSensorName("水位传感器修改");
        incoming.setStatus(1);
        incoming.setUpdateBy("admin");

        DeviceSensor existingSensor = new DeviceSensor();
        existingSensor.setId(1L);
        existingSensor.setDeviceId(10L);
        existingSensor.setMonitorTypeId(4L);
        when(sensorMapper.selectSensorById(1L)).thenReturn(existingSensor);

        Device device = new Device();
        device.setId(10L);
        device.setCode("DEVICE001");
        when(deviceMapper.selectDeviceById(10L)).thenReturn(device);

        MonitorType monitorType = new MonitorType();
        monitorType.setId(4L);
        monitorType.setCode("JCLX004");
        monitorType.setName("水位监测");
        monitorType.setDeviceType(2);
        when(monitorTypeService.selectMonitorTypeById(4L)).thenReturn(monitorType);
        when(sensorMapper.updateSensor(any(DeviceSensor.class))).thenReturn(1);

        SensorAttribute existingAttr = new SensorAttribute();
        existingAttr.setId(101L);
        existingAttr.setSensorId(1L);
        existingAttr.setAttrCode("water_level");
        existingAttr.setAttrName("水位");

        SensorAttribute removedAttr = new SensorAttribute();
        removedAttr.setId(102L);
        removedAttr.setSensorId(1L);
        removedAttr.setAttrCode("water_temp");
        removedAttr.setAttrName("水温");

        when(attributeMapper.selectAttributeListBySensorId(1L)).thenReturn(List.of(existingAttr, removedAttr));

        SensorAttribute updatedAttr = new SensorAttribute();
        updatedAttr.setId(101L);
        updatedAttr.setAttrCode("water_level");
        updatedAttr.setAttrName("水位");
        updatedAttr.setRangeMin(BigDecimal.ZERO);
        updatedAttr.setRangeMax(new BigDecimal("100"));

        SensorAttribute newAttr = new SensorAttribute();
        newAttr.setAttrCode("water_pressure");
        newAttr.setAttrName("水压");
        newAttr.setRangeMin(BigDecimal.ZERO);
        newAttr.setRangeMax(new BigDecimal("50"));

        int rows = service.updateSensor(incoming, List.of(updatedAttr, newAttr));

        assertEquals(1, rows);
        verify(attributeMapper).updateAttribute(updatedAttr);
        verify(attributeMapper).insertAttribute(newAttr);
        verify(attributeMapper).deleteAttributeById(102L);
    }
}
