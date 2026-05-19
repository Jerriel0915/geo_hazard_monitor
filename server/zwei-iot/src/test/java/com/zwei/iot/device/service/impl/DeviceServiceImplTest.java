package com.zwei.iot.device.service.impl;

import com.zwei.iot.device.domain.Device;
import com.zwei.iot.device.domain.DeviceSensor;
import com.zwei.iot.device.domain.SensorAttribute;
import com.zwei.iot.device.mapper.DeviceMapper;
import com.zwei.iot.device.mapper.DeviceSensorMapper;
import com.zwei.iot.device.mapper.SensorAttributeMapper;
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
 * DeviceServiceImpl单元测试
 * <p>
 * 测试设备服务实现类的各项功能，包括：
 * - 分页查询设备列表
 * - 全量查询
 * - 按ID查询（带传感器信息）
 * - 新增
 * - 修改
 * - 删除（单个和批量）
 * - 编码唯一性校验
 * - 复制设备
 * - 获取设备传感器列表
 *
 * @author zwei
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("DeviceServiceImpl Tests")
class DeviceServiceImplTest {

    @Mock
    private DeviceMapper deviceMapper;

    @Mock
    private DeviceSensorMapper sensorMapper;

    @Mock
    private SensorAttributeMapper attributeMapper;

    private DeviceServiceImpl deviceService;

    @BeforeEach
    void setUp() {
        deviceService = new DeviceServiceImpl(deviceMapper, sensorMapper, attributeMapper);
    }

    // ==================== selectDevicePage Tests ====================

    @Nested
    @DisplayName("selectDevicePage")
    class SelectDevicePage {

        @Test
        @DisplayName("returns list from mapper")
        void returnsListFromMapper() {
            Device expected = new Device();
            expected.setId(1L);
            expected.setCode("DEVICE001");
            expected.setName("水位雷达");
            when(deviceMapper.selectDeviceList(any(Device.class))).thenReturn(List.of(expected));

            List<Device> result = deviceService.selectDevicePage(expected, 1, 10);

            assertThat(result).hasSize(1);
            assertThat(result.get(0).getCode()).isEqualTo("DEVICE001");
            verify(deviceMapper).selectDeviceList(expected);
        }

        @Test
        @DisplayName("returns empty list when no results")
        void returnsEmptyListWhenNoResults() {
            Device query = new Device();
            when(deviceMapper.selectDeviceList(any(Device.class))).thenReturn(List.of());

            List<Device> result = deviceService.selectDevicePage(query, 1, 10);

            assertThat(result).isEmpty();
        }

        @Test
        @DisplayName("returns multiple results")
        void returnsMultipleResults() {
            Device d1 = new Device();
            d1.setId(1L);
            d1.setCode("DEVICE001");
            Device d2 = new Device();
            d2.setId(2L);
            d2.setCode("DEVICE002");
            when(deviceMapper.selectDeviceList(any(Device.class))).thenReturn(Arrays.asList(d1, d2));

            List<Device> result = deviceService.selectDevicePage(new Device(), 1, 10);

            assertThat(result).hasSize(2);
        }
    }

    // ==================== selectDeviceAll Tests ====================

    @Nested
    @DisplayName("selectDeviceAll")
    class SelectDeviceAll {

        @Test
        @DisplayName("returns all devices")
        void returnsAllDevices() {
            Device d1 = new Device();
            d1.setId(1L);
            d1.setCode("DEVICE001");
            Device d2 = new Device();
            d2.setId(2L);
            d2.setCode("DEVICE002");
            when(deviceMapper.selectDeviceAll()).thenReturn(Arrays.asList(d1, d2));

            List<Device> result = deviceService.selectDeviceAll();

            assertThat(result).hasSize(2);
            verify(deviceMapper).selectDeviceAll();
        }

        @Test
        @DisplayName("returns empty list when no devices")
        void returnsEmptyListWhenNoDevices() {
            when(deviceMapper.selectDeviceAll()).thenReturn(List.of());

            List<Device> result = deviceService.selectDeviceAll();

            assertThat(result).isEmpty();
        }
    }

    // ==================== selectDeviceById Tests ====================

    @Nested
    @DisplayName("selectDeviceById")
    class SelectDeviceById {

        @Test
        @DisplayName("returns device with sensors when found")
        void returnsDeviceWithSensorsWhenFound() {
            Device expected = new Device();
            expected.setId(1L);
            expected.setCode("DEVICE001");
            expected.setName("水位雷达");

            DeviceSensor sensor = new DeviceSensor();
            sensor.setId(1L);
            sensor.setSensorCode("SENSOR001");
            sensor.setSensorName("水位传感器");

            SensorAttribute attr = new SensorAttribute();
            attr.setId(1L);
            attr.setAttrCode("water_level");
            attr.setAttrName("水位");
            attr.setUnit("m");
            attr.setRangeMin(BigDecimal.ZERO);
            attr.setRangeMax(new BigDecimal("100"));

            when(deviceMapper.selectDeviceById(1L)).thenReturn(expected);
            when(sensorMapper.selectSensorListByDeviceId(1L)).thenReturn(List.of(sensor));
            when(attributeMapper.selectAttributeListBySensorId(1L)).thenReturn(List.of(attr));

            Device result = deviceService.selectDeviceById(1L);

            assertThat(result.getId()).isEqualTo(1L);
            assertThat(result.getSensors()).isNotNull();
        }

        @Test
        @DisplayName("returns null when device not found")
        void returnsNullWhenDeviceNotFound() {
            when(deviceMapper.selectDeviceById(999L)).thenReturn(null);

            Device result = deviceService.selectDeviceById(999L);

            assertThat(result).isNull();
            verify(sensorMapper, never()).selectSensorListByDeviceId(any());
        }

        @Test
        @DisplayName("returns device without sensors when no sensors exist")
        void returnsDeviceWithoutSensorsWhenNoSensorsExist() {
            Device expected = new Device();
            expected.setId(1L);
            expected.setCode("DEVICE001");
            when(deviceMapper.selectDeviceById(1L)).thenReturn(expected);
            when(sensorMapper.selectSensorListByDeviceId(1L)).thenReturn(List.of());

            Device result = deviceService.selectDeviceById(1L);

            assertThat(result).isNotNull();
            assertThat(result.getSensors()).isNotNull();
        }
    }

    // ==================== insertDevice Tests ====================

    @Nested
    @DisplayName("insertDevice")
    class InsertDevice {

        @Test
        @DisplayName("inserts successfully with all fields")
        void insertsSuccessfullyWithAllFields() {
            Device device = new Device();
            device.setCode("DEVICE001");
            device.setName("水位雷达");
            device.setStatus(1);
            when(deviceMapper.insertDevice(device)).thenReturn(1);

            int result = deviceService.insertDevice(device);

            assertThat(result).isEqualTo(1);
            verify(deviceMapper).insertDevice(device);
        }

        @Test
        @DisplayName("returns 1 when insert succeeds")
        void returnsOneWhenInsertSucceeds() {
            Device device = new Device();
            device.setCode("TEST001");
            when(deviceMapper.insertDevice(device)).thenReturn(1);

            int result = deviceService.insertDevice(device);

            assertThat(result).isEqualTo(1);
        }

        @Test
        @DisplayName("returns 0 when insert fails")
        void returnsZeroWhenInsertFails() {
            Device device = new Device();
            device.setCode("FAILED");
            when(deviceMapper.insertDevice(device)).thenReturn(0);

            int result = deviceService.insertDevice(device);

            assertThat(result).isEqualTo(0);
        }
    }

    // ==================== updateDevice Tests ====================

    @Nested
    @DisplayName("updateDevice")
    class UpdateDevice {

        @Test
        @DisplayName("delegates to mapper with updated fields")
        void delegatesToMapperWithUpdatedFields() {
            Device device = new Device();
            device.setId(1L);
            device.setName("更新后的设备名称");
            when(deviceMapper.updateDevice(device)).thenReturn(1);

            int result = deviceService.updateDevice(device);

            assertThat(result).isEqualTo(1);
            verify(deviceMapper).updateDevice(device);
        }

        @Test
        @DisplayName("returns 0 when update affects no rows")
        void returnsZeroWhenUpdateAffectsNoRows() {
            Device device = new Device();
            device.setId(999L);
            when(deviceMapper.updateDevice(device)).thenReturn(0);

            int result = deviceService.updateDevice(device);

            assertThat(result).isEqualTo(0);
        }
    }

    // ==================== deleteDeviceById Tests ====================

    @Nested
    @DisplayName("deleteDeviceById")
    class DeleteDeviceById {

        @Test
        @DisplayName("deletes device and its sensors")
        void deletesDeviceAndItsSensors() {
            when(sensorMapper.deleteSensorByDeviceId(1L)).thenReturn(1);
            when(deviceMapper.deleteDeviceById(1L)).thenReturn(1);

            int result = deviceService.deleteDeviceById(1L);

            assertThat(result).isEqualTo(1);
            verify(sensorMapper).deleteSensorByDeviceId(1L);
            verify(deviceMapper).deleteDeviceById(1L);
        }

        @Test
        @DisplayName("returns 0 when device does not exist")
        void returnsZeroWhenDeviceDoesNotExist() {
            when(sensorMapper.deleteSensorByDeviceId(999L)).thenReturn(0);
            when(deviceMapper.deleteDeviceById(999L)).thenReturn(0);

            int result = deviceService.deleteDeviceById(999L);

            assertThat(result).isEqualTo(0);
        }
    }

    // ==================== deleteDeviceByIds Tests ====================

    @Nested
    @DisplayName("deleteDeviceByIds")
    class DeleteDeviceByIds {

        @Test
        @DisplayName("deletes multiple devices and their sensors")
        void deletesMultipleDevicesAndTheirSensors() {
            Long[] ids = {1L, 2L, 3L};
            when(sensorMapper.deleteSensorByDeviceId(1L)).thenReturn(1);
            when(sensorMapper.deleteSensorByDeviceId(2L)).thenReturn(1);
            when(sensorMapper.deleteSensorByDeviceId(3L)).thenReturn(1);
            when(deviceMapper.deleteDeviceByIds(ids)).thenReturn(3);

            int result = deviceService.deleteDeviceByIds(ids);

            assertThat(result).isEqualTo(3);
            verify(sensorMapper).deleteSensorByDeviceId(1L);
            verify(sensorMapper).deleteSensorByDeviceId(2L);
            verify(sensorMapper).deleteSensorByDeviceId(3L);
            verify(deviceMapper).deleteDeviceByIds(ids);
        }

        @Test
        @DisplayName("returns count of deleted rows")
        void returnsCountOfDeletedRows() {
            Long[] ids = {1L, 2L};
            when(sensorMapper.deleteSensorByDeviceId(any())).thenReturn(1);
            when(deviceMapper.deleteDeviceByIds(ids)).thenReturn(2);

            int result = deviceService.deleteDeviceByIds(ids);

            assertThat(result).isEqualTo(2);
        }
    }

    // ==================== checkDeviceCodeUnique Tests ====================

    @Nested
    @DisplayName("checkDeviceCodeUnique")
    class CheckDeviceCodeUnique {

        @Test
        @DisplayName("returns true when code is new (no existing record)")
        void returnsTrueWhenCodeIsNew() {
            Device device = new Device();
            device.setCode("NEW001");
            when(deviceMapper.checkDeviceCodeUnique("NEW001", null)).thenReturn(null);

            boolean result = deviceService.checkDeviceCodeUnique(device);

            assertThat(result).isTrue();
        }

        @Test
        @DisplayName("returns false when code belongs to different device")
        void returnsFalseWhenCodeBelongsToDifferentDevice() {
            Device device = new Device();
            device.setId(1L);
            device.setCode("DEVICE001");
            Device existing = new Device();
            existing.setId(2L);
            existing.setCode("DEVICE001");
            when(deviceMapper.checkDeviceCodeUnique("DEVICE001", 1L)).thenReturn(existing);

            boolean result = deviceService.checkDeviceCodeUnique(device);

            assertThat(result).isFalse();
        }

        @Test
        @DisplayName("returns true when device id is null (new insert)")
        void returnsTrueWhenDeviceIdIsNull() {
            Device device = new Device();
            device.setId(null);
            device.setCode("DEVICE001");
            when(deviceMapper.checkDeviceCodeUnique("DEVICE001", null)).thenReturn(null);

            boolean result = deviceService.checkDeviceCodeUnique(device);

            assertThat(result).isTrue();
        }
    }

    // ==================== copyDevice Tests ====================

    @Nested
    @DisplayName("copyDevice")
    class CopyDevice {

        @Test
        @DisplayName("returns null when original device not found")
        void returnsNullWhenOriginalDeviceNotFound() {
            when(deviceMapper.selectDeviceById(999L)).thenReturn(null);

            Long result = deviceService.copyDevice(999L);

            assertThat(result).isNull();
        }

        @Test
        @DisplayName("copies device and sensors")
        void copiesDeviceAndSensors() {
            Device original = new Device();
            original.setId(1L);
            original.setCode("DEVICE001");
            original.setName("水位雷达");
            original.setIcon("sensor1");
            original.setIconPath("/jc-icon/green/sensor1_green.png");
            original.setStatus(1);
            original.setCreateBy("admin");

            DeviceSensor originalSensor = new DeviceSensor();
            originalSensor.setId(1L);
            originalSensor.setDeviceId(1L);
            originalSensor.setSensorCode("SENSOR001");
            originalSensor.setSensorName("水位传感器");
            originalSensor.setMonitorTypeId(4L);
            originalSensor.setMonitorTypeCode("JCLX004");
            originalSensor.setMonitorTypeName("水位监测");
            originalSensor.setStatus(1);
            originalSensor.setCreateBy("admin");

            SensorAttribute originalAttr = new SensorAttribute();
            originalAttr.setId(1L);
            originalAttr.setSensorId(1L);
            originalAttr.setAttrCode("water_level");
            originalAttr.setAttrName("水位");
            originalAttr.setIndicatorType("sw");
            originalAttr.setUnit("m");
            originalAttr.setRangeMin(BigDecimal.ZERO);
            originalAttr.setRangeMax(new BigDecimal("100"));

            Device copiedDevice = new Device();
            copiedDevice.setId(2L);
            copiedDevice.setCode("DEVICE001_copy");
            copiedDevice.setName("水位雷达_副本");

            when(deviceMapper.selectDeviceById(1L)).thenReturn(original);
            doAnswer(invocation -> {
                Device d = invocation.getArgument(0);
                d.setId(2L);
                return 1;
            }).when(deviceMapper).insertDevice(any(Device.class));
            when(sensorMapper.selectSensorListByDeviceId(1L)).thenReturn(List.of(originalSensor));
            when(sensorMapper.insertSensor(any(DeviceSensor.class))).thenReturn(1);
            when(attributeMapper.selectAttributeListBySensorId(1L)).thenReturn(List.of(originalAttr));
            when(attributeMapper.batchInsertAttribute(any())).thenReturn(1);

            Long result = deviceService.copyDevice(1L);

            assertThat(result).isNotNull();
            assertThat(result).isEqualTo(2L);
            verify(deviceMapper).insertDevice(any(Device.class));
            verify(sensorMapper).insertSensor(any(DeviceSensor.class));
            verify(attributeMapper).batchInsertAttribute(any());
        }
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
            sensor1.setSensorCode("SENSOR001");
            sensor1.setSensorName("水位传感器");
            sensor1.setMonitorTypeId(4L);

            SensorAttribute attr1 = new SensorAttribute();
            attr1.setId(1L);
            attr1.setAttrCode("water_level");
            attr1.setAttrName("水位");

            when(sensorMapper.selectSensorListByDeviceId(1L)).thenReturn(List.of(sensor1));
            when(attributeMapper.selectAttributeListBySensorId(1L)).thenReturn(List.of(attr1));

            List<DeviceSensor> result = deviceService.selectSensorListByDeviceId(1L);

            assertThat(result).hasSize(1);
            assertThat(result.get(0).getAttrList()).hasSize(1);
        }

        @Test
        @DisplayName("returns empty list when no sensors")
        void returnsEmptyListWhenNoSensors() {
            when(sensorMapper.selectSensorListByDeviceId(1L)).thenReturn(List.of());

            List<DeviceSensor> result = deviceService.selectSensorListByDeviceId(1L);

            assertThat(result).isEmpty();
        }
    }
}