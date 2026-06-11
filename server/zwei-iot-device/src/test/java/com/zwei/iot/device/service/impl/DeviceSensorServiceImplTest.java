package com.zwei.iot.device.service.impl;

import com.zwei.common.exception.ServiceException;
import com.zwei.iot.device.domain.Device;
import com.zwei.iot.device.domain.DeviceSensor;
import com.zwei.iot.device.domain.SensorAttribute;
import com.zwei.iot.device.mapper.DeviceMapper;
import com.zwei.iot.device.mapper.DeviceSensorMapper;
import com.zwei.iot.device.mapper.SensorAttributeMapper;
import com.zwei.iot.device.service.IProductTslService;
import com.zwei.iot.device.service.ITimeSeriesSchemaService;
import com.zwei.iot.monitor.domain.MonitorContent;
import com.zwei.iot.monitor.domain.MonitorType;
import com.zwei.iot.monitor.service.IMonitorContentService;
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
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

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

    @Mock
    private IMonitorContentService monitorContentService;

    @Mock
    private ITimeSeriesSchemaService timeSeriesSchemaService;

    @Mock
    private IProductTslService productTslService;

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
    @DisplayName("新增传感器时任意 deviceType 的监测类型均可使用（已移除硬编码约束）")
    void insertSensor_shouldAcceptAnyMonitorTypeRegardlessOfDeviceType() {
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
        when(monitorTypeService.selectMonitorTypeById(4L)).thenReturn(monitorType);

        Long result = service.insertSensor(sensor, List.of(attr));

        assertEquals(sensor.getId(), result);
        assertEquals("JCLX004", sensor.getMonitorTypeCode());
        verify(sensorMapper).insertSensor(sensor);
        verify(attributeMapper).insertAttribute(any(SensorAttribute.class));
    }

    @Test
    @DisplayName("修改传感器时应增量更新属性，必须包含所有已有属性 ID")
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
        when(monitorTypeService.selectMonitorTypeById(4L)).thenReturn(monitorType);
        when(sensorMapper.updateSensor(any(DeviceSensor.class))).thenReturn(1);

        SensorAttribute existingAttr = new SensorAttribute();
        existingAttr.setId(101L);
        existingAttr.setSensorId(1L);
        existingAttr.setAttrCode("water_level");
        existingAttr.setAttrName("水位");

        SensorAttribute keptAttr = new SensorAttribute();
        keptAttr.setId(102L);
        keptAttr.setSensorId(1L);
        keptAttr.setAttrCode("water_temp");
        keptAttr.setAttrName("水温");

        when(attributeMapper.selectAttributeListBySensorId(1L)).thenReturn(List.of(existingAttr, keptAttr));

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

        // 必须包含所有已有属性（101 + 102），否则抛异常
        int rows = service.updateSensor(incoming, List.of(updatedAttr, keptAttr, newAttr));

        assertEquals(1, rows);
        verify(attributeMapper).updateAttribute(updatedAttr);
        verify(attributeMapper).insertAttribute(newAttr);
        verify(attributeMapper, never()).deleteAttributeById(any());
    }

    @Test
    @DisplayName("修改传感器时缺少已有属性 ID 应抛出异常")
    void updateSensor_shouldThrowWhenExistingAttrIdsMissing() {
        DeviceSensor incoming = new DeviceSensor();
        incoming.setId(1L);
        incoming.setSensorName("测试");
        incoming.setStatus(1);

        DeviceSensor existing = new DeviceSensor();
        existing.setId(1L);
        existing.setDeviceId(10L);
        existing.setMonitorTypeId(4L);
        when(sensorMapper.selectSensorById(1L)).thenReturn(existing);
        Device dev = new Device(); dev.setId(10L); dev.setCode("D01");
        when(deviceMapper.selectDeviceById(10L)).thenReturn(dev);
        when(sensorMapper.updateSensor(any(DeviceSensor.class))).thenReturn(1);
        MonitorType mt = new MonitorType(); mt.setId(4L); mt.setCode("T1"); mt.setName("T1");
        when(monitorTypeService.selectMonitorTypeById(4L)).thenReturn(mt);

        SensorAttribute attr1 = new SensorAttribute();
        attr1.setId(201L);
        attr1.setSensorId(1L);
        attr1.setAttrCode("a1");
        attr1.setAttrName("A1");
        when(attributeMapper.selectAttributeListBySensorId(1L)).thenReturn(List.of(attr1));

        // 只传了一个新属性，没有包含已有的 attr1(id=201)
        SensorAttribute newAttr = new SensorAttribute();
        newAttr.setAttrCode("a2");
        newAttr.setAttrName("A2");

        ServiceException ex = assertThrows(ServiceException.class,
                () -> service.updateSensor(incoming, List.of(newAttr)));
        assertTrue(ex.getMessage().contains("缺少属性 ID"));
    }

    @Test
    @DisplayName("显式删除传感器属性应校验归属关系")
    void deleteSensorAttribute_shouldValidateOwnership() {
        SensorAttribute attr = new SensorAttribute();
        attr.setId(301L);
        attr.setSensorId(10L);
        when(attributeMapper.selectAttributeById(301L)).thenReturn(attr);

        service.deleteSensorAttribute(10L, 301L);
        verify(attributeMapper).deleteAttributeById(301L);
    }

    @Test
    @DisplayName("删除不属于当前传感器的属性应抛出异常")
    void deleteSensorAttribute_shouldThrowWhenNotOwned() {
        SensorAttribute attr = new SensorAttribute();
        attr.setId(301L);
        attr.setSensorId(99L);
        when(attributeMapper.selectAttributeById(301L)).thenReturn(attr);

        assertThrows(ServiceException.class,
                () -> service.deleteSensorAttribute(10L, 301L));
        verify(attributeMapper, never()).deleteAttributeById(any());
    }
}
