package com.zwei.iot.service.impl;

import com.zwei.iot.domain.DeviceSensor;
import com.zwei.iot.domain.SensorAttribute;
import com.zwei.iot.mapper.DeviceSensorMapper;
import com.zwei.iot.mapper.SensorAttributeMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.util.Arrays;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

/**
 * DeviceSensorServiceImpl单元测试
 * <p>
 * 测试传感器服务实现类的各项功能，包括：
 * - 按设备ID查询传感器列表（带属性）
 * - 按ID查询传感器详情（带属性）
 * - 新增传感器（支持属性列表）
 * - 修改传感器（替换属性）
 * - 删除传感器（级联删除属性）
 * - 编码唯一性校验
 *
 * @author zwei
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("DeviceSensorServiceImpl Tests")
class DeviceSensorServiceImplTest {

    @Mock
    private DeviceSensorMapper sensorMapper;

    @Mock
    private SensorAttributeMapper attributeMapper;

    private DeviceSensorServiceImpl sensorService;

    @BeforeEach
    void setUp() {
        sensorService = new DeviceSensorServiceImpl(sensorMapper, attributeMapper);
    }

    // ==================== selectSensorListByDeviceId Tests ====================

    @Nested
    @DisplayName("selectSensorListByDeviceId")
    class SelectSensorListByDeviceId {

        @Test
        @DisplayName("returns sensors with attributes")
        void returnsSensorsWithAttributes() {
            DeviceSensor sensor1 = new DeviceSensor();
            sensor1.setId(1L);
            sensor1.setDeviceId(100L);
            sensor1.setSensorCode("SENSOR001");
            sensor1.setSensorName("水位传感器");

            SensorAttribute attr1 = new SensorAttribute();
            attr1.setId(1L);
            attr1.setAttrCode("water_level");
            attr1.setAttrName("水位");

            when(sensorMapper.selectSensorListByDeviceId(100L)).thenReturn(List.of(sensor1));
            when(attributeMapper.selectAttributeListBySensorId(1L)).thenReturn(List.of(attr1));

            List<DeviceSensor> result = sensorService.selectSensorListByDeviceId(100L);

            assertThat(result).hasSize(1);
            assertThat(result.get(0).getAttrList()).hasSize(1);
            assertThat(result.get(0).getAttrList().get(0).getAttrCode()).isEqualTo("water_level");
        }

        @Test
        @DisplayName("returns empty list when no sensors")
        void returnsEmptyListWhenNoSensors() {
            when(sensorMapper.selectSensorListByDeviceId(999L)).thenReturn(List.of());

            List<DeviceSensor> result = sensorService.selectSensorListByDeviceId(999L);

            assertThat(result).isEmpty();
        }

        @Test
        @DisplayName("returns multiple sensors with their attributes")
        void returnsMultipleSensorsWithAttributes() {
            DeviceSensor sensor1 = new DeviceSensor();
            sensor1.setId(1L);
            sensor1.setSensorCode("SENSOR001");

            DeviceSensor sensor2 = new DeviceSensor();
            sensor2.setId(2L);
            sensor2.setSensorCode("SENSOR002");

            when(sensorMapper.selectSensorListByDeviceId(100L)).thenReturn(Arrays.asList(sensor1, sensor2));
            when(attributeMapper.selectAttributeListBySensorId(1L)).thenReturn(List.of());
            when(attributeMapper.selectAttributeListBySensorId(2L)).thenReturn(List.of());

            List<DeviceSensor> result = sensorService.selectSensorListByDeviceId(100L);

            assertThat(result).hasSize(2);
        }
    }

    // ==================== selectSensorById Tests ====================

    @Nested
    @DisplayName("selectSensorById")
    class SelectSensorById {

        @Test
        @DisplayName("returns sensor with attributes when found")
        void returnsSensorWithAttributesWhenFound() {
            DeviceSensor expected = new DeviceSensor();
            expected.setId(1L);
            expected.setSensorCode("SENSOR001");
            expected.setSensorName("水位传感器");

            SensorAttribute attr = new SensorAttribute();
            attr.setId(1L);
            attr.setAttrCode("water_level");
            attr.setAttrName("水位");
            attr.setUnit("m");
            attr.setRangeMin(BigDecimal.ZERO);
            attr.setRangeMax(new BigDecimal("100"));

            when(sensorMapper.selectSensorById(1L)).thenReturn(expected);
            when(attributeMapper.selectAttributeListBySensorId(1L)).thenReturn(List.of(attr));

            DeviceSensor result = sensorService.selectSensorById(1L);

            assertThat(result).isNotNull();
            assertThat(result.getId()).isEqualTo(1L);
            assertThat(result.getAttrList()).hasSize(1);
            assertThat(result.getAttrList().get(0).getAttrCode()).isEqualTo("water_level");
        }

        @Test
        @DisplayName("returns null when sensor not found")
        void returnsNullWhenSensorNotFound() {
            when(sensorMapper.selectSensorById(999L)).thenReturn(null);

            DeviceSensor result = sensorService.selectSensorById(999L);

            assertThat(result).isNull();
            verify(attributeMapper, never()).selectAttributeListBySensorId(any());
        }
    }

    // ==================== insertSensor Tests ====================

    @Nested
    @DisplayName("insertSensor")
    class InsertSensor {

        @Test
        @DisplayName("inserts sensor successfully")
        void insertsSensorSuccessfully() {
            DeviceSensor sensor = new DeviceSensor();
            sensor.setDeviceId(100L);
            sensor.setSensorCode("SENSOR001");
            sensor.setSensorName("水位传感器");
            sensor.setMonitorTypeId(4L);
            sensor.setMonitorTypeCode("JCLX004");
            sensor.setMonitorTypeName("水位监测");
            sensor.setStatus(1);

            doAnswer(invocation -> {
                DeviceSensor s = invocation.getArgument(0);
                s.setId(1L);
                return 1;
            }).when(sensorMapper).insertSensor(any(DeviceSensor.class));

            Long result = sensorService.insertSensor(sensor, null);

            assertThat(result).isEqualTo(1L);
            verify(sensorMapper).insertSensor(sensor);
        }

        @Test
        @DisplayName("inserts sensor with attributes")
        void insertsSensorWithAttributes() {
            DeviceSensor sensor = new DeviceSensor();
            sensor.setDeviceId(100L);
            sensor.setSensorCode("SENSOR001");
            sensor.setSensorName("水位传感器");

            SensorAttribute attr = new SensorAttribute();
            attr.setAttrCode("water_level");
            attr.setAttrName("水位");
            attr.setIndicatorType("sw");
            attr.setIndicatorTypeName("水位");
            attr.setInitialValue(new BigDecimal("0"));
            attr.setUnit("m");
            attr.setRangeMin(BigDecimal.ZERO);
            attr.setRangeMax(new BigDecimal("100"));

            doAnswer(invocation -> {
                DeviceSensor s = invocation.getArgument(0);
                s.setId(1L);
                return 1;
            }).when(sensorMapper).insertSensor(any(DeviceSensor.class));
            when(attributeMapper.insertAttribute(any(SensorAttribute.class))).thenReturn(1);

            Long result = sensorService.insertSensor(sensor, List.of(attr));

            assertThat(result).isEqualTo(1L);
            verify(sensorMapper).insertSensor(sensor);
            verify(attributeMapper).insertAttribute(any(SensorAttribute.class));
        }

        @Test
        @DisplayName("inserts sensor with multiple attributes")
        void insertsSensorWithMultipleAttributes() {
            DeviceSensor sensor = new DeviceSensor();
            sensor.setDeviceId(100L);
            sensor.setSensorCode("SENSOR001");
            sensor.setSensorName("多属性传感器");

            SensorAttribute attr1 = new SensorAttribute();
            attr1.setAttrCode("attr1");
            attr1.setAttrName("属性1");

            SensorAttribute attr2 = new SensorAttribute();
            attr2.setAttrCode("attr2");
            attr2.setAttrName("属性2");

            doAnswer(invocation -> {
                DeviceSensor s = invocation.getArgument(0);
                s.setId(1L);
                return 1;
            }).when(sensorMapper).insertSensor(any(DeviceSensor.class));
            when(attributeMapper.insertAttribute(any(SensorAttribute.class))).thenReturn(1);

            Long result = sensorService.insertSensor(sensor, Arrays.asList(attr1, attr2));

            assertThat(result).isEqualTo(1L);
            verify(attributeMapper, times(2)).insertAttribute(any(SensorAttribute.class));
        }

        @Test
        @DisplayName("returns sensor id after insert")
        void returnsSensorIdAfterInsert() {
            DeviceSensor sensor = new DeviceSensor();
            sensor.setDeviceId(100L);
            sensor.setSensorCode("SENSOR001");

            doAnswer(invocation -> {
                DeviceSensor s = invocation.getArgument(0);
                s.setId(99L);
                return 1;
            }).when(sensorMapper).insertSensor(any(DeviceSensor.class));

            Long result = sensorService.insertSensor(sensor, null);

            assertThat(result).isEqualTo(99L);
        }
    }

    // ==================== updateSensor Tests ====================

    @Nested
    @DisplayName("updateSensor")
    class UpdateSensor {

        @Test
        @DisplayName("updates sensor and replaces attributes")
        void updatesSensorAndReplacesAttributes() {
            DeviceSensor sensor = new DeviceSensor();
            sensor.setId(1L);
            sensor.setSensorName("更新后的传感器");

            SensorAttribute attr = new SensorAttribute();
            attr.setAttrCode("new_attr");
            attr.setAttrName("新属性");

            when(sensorMapper.updateSensor(sensor)).thenReturn(1);
            when(attributeMapper.deleteAttributeBySensorId(1L)).thenReturn(1);
            when(attributeMapper.insertAttribute(any(SensorAttribute.class))).thenReturn(1);

            int result = sensorService.updateSensor(sensor, List.of(attr));

            assertThat(result).isEqualTo(1);
            verify(sensorMapper).updateSensor(sensor);
            verify(attributeMapper).deleteAttributeBySensorId(1L);
            verify(attributeMapper).insertAttribute(any(SensorAttribute.class));
        }

        @Test
        @DisplayName("updates sensor and deletes all old attributes when no new attributes")
        void updatesSensorAndDeletesAllOldAttributesWhenNoNewAttributes() {
            DeviceSensor sensor = new DeviceSensor();
            sensor.setId(1L);
            sensor.setSensorName("更新后的传感器");

            when(sensorMapper.updateSensor(sensor)).thenReturn(1);
            when(attributeMapper.deleteAttributeBySensorId(1L)).thenReturn(3);

            int result = sensorService.updateSensor(sensor, null);

            assertThat(result).isEqualTo(1);
            verify(attributeMapper).deleteAttributeBySensorId(1L);
            verify(attributeMapper, never()).insertAttribute(any());
        }

        @Test
        @DisplayName("returns 0 when update affects no rows")
        void returnsZeroWhenUpdateAffectsNoRows() {
            DeviceSensor sensor = new DeviceSensor();
            sensor.setId(999L);
            when(sensorMapper.updateSensor(sensor)).thenReturn(0);

            int result = sensorService.updateSensor(sensor, null);

            assertThat(result).isEqualTo(0);
        }
    }

    // ==================== deleteSensorById Tests ====================

    @Nested
    @DisplayName("deleteSensorById")
    class DeleteSensorById {

        @Test
        @DisplayName("deletes attributes then sensor")
        void deletesAttributesThenSensor() {
            when(attributeMapper.deleteAttributeBySensorId(1L)).thenReturn(2);
            when(sensorMapper.deleteSensorById(1L)).thenReturn(1);

            int result = sensorService.deleteSensorById(1L);

            assertThat(result).isEqualTo(1);
            verify(attributeMapper).deleteAttributeBySensorId(1L);
            verify(sensorMapper).deleteSensorById(1L);
        }

        @Test
        @DisplayName("returns 0 when sensor does not exist")
        void returnsZeroWhenSensorDoesNotExist() {
            when(attributeMapper.deleteAttributeBySensorId(999L)).thenReturn(0);
            when(sensorMapper.deleteSensorById(999L)).thenReturn(0);

            int result = sensorService.deleteSensorById(999L);

            assertThat(result).isEqualTo(0);
        }

        @Test
        @DisplayName("deletes even when no attributes exist")
        void deletesEvenWhenNoAttributesExist() {
            when(attributeMapper.deleteAttributeBySensorId(1L)).thenReturn(0);
            when(sensorMapper.deleteSensorById(1L)).thenReturn(1);

            int result = sensorService.deleteSensorById(1L);

            assertThat(result).isEqualTo(1);
        }
    }

    // ==================== checkSensorCodeUnique Tests ====================

    @Nested
    @DisplayName("checkSensorCodeUnique")
    class CheckSensorCodeUnique {

        @Test
        @DisplayName("returns true when code is unique")
        void returnsTrueWhenCodeIsUnique() {
            when(sensorMapper.checkSensorCodeUnique("NEW001", 0L)).thenReturn(null);

            boolean result = sensorService.checkSensorCodeUnique("NEW001", 0L);

            assertThat(result).isTrue();
        }

        @Test
        @DisplayName("returns false when code already exists")
        void returnsFalseWhenCodeAlreadyExists() {
            DeviceSensor existing = new DeviceSensor();
            existing.setId(1L);
            existing.setSensorCode("EXISTING001");
            when(sensorMapper.checkSensorCodeUnique("EXISTING001", 0L)).thenReturn(existing);

            boolean result = sensorService.checkSensorCodeUnique("EXISTING001", 0L);

            assertThat(result).isFalse();
        }

        @Test
        @DisplayName("returns true when same id passed (for update validation)")
        void returnsTrueWhenSameIdPassed() {
            when(sensorMapper.checkSensorCodeUnique("SENSOR001", 1L)).thenReturn(null);

            boolean result = sensorService.checkSensorCodeUnique("SENSOR001", 1L);

            assertThat(result).isTrue();
        }
    }
}