package com.zwei.iot.device.service.impl;

import com.zwei.common.exception.ServiceException;
import com.zwei.iot.device.domain.Device;
import com.zwei.iot.device.domain.DeviceSensor;
import com.zwei.iot.device.domain.SensorAttribute;
import com.zwei.iot.device.domain.dto.DeviceCopyRequest;
import com.zwei.iot.device.mapper.DeviceMapper;
import com.zwei.iot.device.mapper.DeviceSensorMapper;
import com.zwei.iot.device.mapper.ProductMapper;
import com.zwei.iot.device.mapper.SensorAttributeMapper;
import com.zwei.iot.device.service.DeviceAuthLogService;
import com.zwei.iot.device.service.IDeviceHazardRelationService;
import com.zwei.iot.device.service.IDeviceSessionService;
import com.zwei.iot.device.service.IDeviceStatusLogService;
import com.zwei.iot.device.service.IProductTslService;
import com.zwei.iot.device.support.DeviceAuthAccountGenerator;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.beans.factory.ObjectProvider;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("DeviceServiceImpl 单元测试")
class DeviceServiceImplTest {

    @Mock
    private DeviceMapper deviceMapper;

    @Mock
    private DeviceSensorMapper sensorMapper;

    @Mock
    private SensorAttributeMapper attributeMapper;

    @Mock
    private IDeviceHazardRelationService hazardRelationService;

    @Mock
    private DeviceAuthAccountGenerator accountGenerator;

    @Mock
    private DeviceAuthLogService deviceAuthLogService;

    @Mock
    private ObjectProvider<IDeviceSessionService> deviceSessionServiceProvider;

    @Mock
    private IDeviceSessionService deviceSessionService;

    @Mock
    private IDeviceStatusLogService deviceStatusLogService;

    @Mock
    private IProductTslService productTslService;

    @Mock
    private ProductMapper productMapper;

    @InjectMocks
    private DeviceServiceImpl service;

    @Test
    @DisplayName("切换设备账号状态时应更新账号状态")
    void changeDeviceAuthStatus_shouldUpdateStatus() {
        Device current = new Device();
        current.setId(1L);
        current.setAuthStatus(1);
        current.setAuthUsername("A1B2C3");

        Device updated = new Device();
        updated.setId(1L);
        updated.setAuthStatus(2);
        updated.setAuthUsername("A1B2C3");

        when(deviceMapper.selectDeviceById(1L)).thenReturn(current, updated);

        Device result = service.changeDeviceAuthStatus(1L, 2, "admin", "现场停用", "127.0.0.1");

        assertEquals(2, result.getAuthStatus());
        verify(deviceMapper).updateDevice(any(Device.class));
        verify(deviceAuthLogService).save(any());
    }

    @Test
    @DisplayName("切换到相同设备账号状态时不应重复更新")
    void changeDeviceAuthStatus_shouldSkipWhenStatusUnchanged() {
        Device current = new Device();
        current.setId(1L);
        current.setAuthStatus(1);
        current.setAuthUsername("A1B2C3");

        when(deviceMapper.selectDeviceById(1L)).thenReturn(current);

        Device result = service.changeDeviceAuthStatus(1L, 1, "admin", "无需变更", "127.0.0.1");

        assertEquals(1, result.getAuthStatus());
        verify(deviceMapper, never()).updateDevice(any(Device.class));
        verify(deviceAuthLogService).save(any());
    }

    @Test
    @DisplayName("查看设备账号时应写入审计日志")
    void getDeviceAuthAccount_shouldWriteAuditLog() {
        Device device = new Device();
        device.setId(1L);
        device.setAuthUsername("A1B2C3");

        when(deviceMapper.selectDeviceById(1L)).thenReturn(device);

        Device result = service.getDeviceAuthAccount(1L, "admin", "127.0.0.1");

        assertEquals(1L, result.getId());
        verify(deviceAuthLogService).save(any());
    }

    @Test
    @DisplayName("重置设备密码时应更新密码并写入审计日志")
    void resetDeviceAuthPassword_shouldWriteAuditLog() {
        Device current = new Device();
        current.setId(1L);
        current.setAuthUsername("A1B2C3");
        current.setAuthPassword("OldPass1");

        Device latest = new Device();
        latest.setId(1L);
        latest.setAuthUsername("A1B2C3");
        latest.setAuthPassword("NewPass1");

        when(accountGenerator.generatePassword()).thenReturn("NewPass1");
        when(deviceMapper.selectDeviceById(1L)).thenReturn(current, latest);

        Device result = service.resetDeviceAuthPassword(1L, "admin", "现场更换设备", true, "127.0.0.1");

        assertEquals("NewPass1", result.getAuthPassword());
        verify(deviceMapper).updateDevice(any(Device.class));
        verify(deviceAuthLogService).save(any());
    }

    @Test
    @DisplayName("forceOffline=true 时应调用 MQTT 断连")
    void resetDeviceAuthPassword_forceOfflineTrue_shouldDisconnect() {
        Device current = new Device();
        current.setId(1L);
        current.setAuthUsername("A1B2C3");
        current.setAuthPassword("OldPass1");

        Device latest = new Device();
        latest.setId(1L);
        latest.setAuthUsername("A1B2C3");
        latest.setAuthPassword("NewPass1");

        when(accountGenerator.generatePassword()).thenReturn("NewPass1");
        when(deviceMapper.selectDeviceById(1L)).thenReturn(current, latest);
        when(deviceSessionServiceProvider.getIfAvailable()).thenReturn(deviceSessionService);

        service.resetDeviceAuthPassword(1L, "admin", "现场更换设备", true, "127.0.0.1");

        verify(deviceSessionService).disconnectDevice(1L);
    }

    @Test
    @DisplayName("forceOffline=false 时不应调用 MQTT 断连")
    void resetDeviceAuthPassword_forceOfflineFalse_shouldNotDisconnect() {
        Device current = new Device();
        current.setId(1L);
        current.setAuthUsername("A1B2C3");
        current.setAuthPassword("OldPass1");

        Device latest = new Device();
        latest.setId(1L);
        latest.setAuthUsername("A1B2C3");
        latest.setAuthPassword("NewPass1");

        when(accountGenerator.generatePassword()).thenReturn("NewPass1");
        when(deviceMapper.selectDeviceById(1L)).thenReturn(current, latest);

        service.resetDeviceAuthPassword(1L, "admin", "现场更换设备", false, "127.0.0.1");

        verify(deviceSessionService, never()).disconnectDevice(any());
    }

    @Test
    @DisplayName("IDeviceSessionService 不可用时不应抛异常")
    void resetDeviceAuthPassword_serviceUnavailable_shouldNotThrow() {
        Device current = new Device();
        current.setId(1L);
        current.setAuthUsername("A1B2C3");
        current.setAuthPassword("OldPass1");

        Device latest = new Device();
        latest.setId(1L);
        latest.setAuthUsername("A1B2C3");
        latest.setAuthPassword("NewPass1");

        when(accountGenerator.generatePassword()).thenReturn("NewPass1");
        when(deviceMapper.selectDeviceById(1L)).thenReturn(current, latest);
        when(deviceSessionServiceProvider.getIfAvailable()).thenReturn(null);

        Device result = service.resetDeviceAuthPassword(1L, "admin", "现场更换设备", true, "127.0.0.1");

        assertEquals("NewPass1", result.getAuthPassword());
    }

    @Test
    @DisplayName("报修后停用应成功 (status 1→3)")
    void maintenanceDevice_normalToDisabled_shouldSucceed() {
        Device current = new Device();
        current.setId(1L);
        current.setCode("dev-001");
        current.setStatus(1);

        when(deviceMapper.selectDeviceById(1L)).thenReturn(current);

        String result = service.maintenanceDevice(1L, 3, "Test", "13800000000",
                "2026-06-15 10:00:00", "现场停用", "admin");

        assertEquals("停用", result);
        verify(deviceMapper).updateDevice(any(Device.class));
        verify(deviceStatusLogService).saveMaintenanceLog(
                eq(1L), eq("dev-001"), eq(1), eq(3), eq("停用"),
                eq("Test"), eq("13800000000"), any(), eq("现场停用"), eq("admin"));
    }

    @Test
    @DisplayName("维修后停用应成功 (status 2→3)")
    void maintenanceDevice_maintenanceToDisabled_shouldSucceed() {
        Device current = new Device();
        current.setId(2L);
        current.setCode("dev-002");
        current.setStatus(2);

        when(deviceMapper.selectDeviceById(2L)).thenReturn(current);

        String result = service.maintenanceDevice(2L, 3, "Test", "13800000000",
                "2026-06-15 10:00:00", "维修失败停用", "admin");

        assertEquals("停用", result);
        verify(deviceMapper).updateDevice(any(Device.class));
        verify(deviceStatusLogService).saveMaintenanceLog(
                eq(2L), eq("dev-002"), eq(2), eq(3), eq("停用"),
                eq("Test"), eq("13800000000"), any(), eq("维修失败停用"), eq("admin"));
    }

    @Test
    @DisplayName("停用后停用应抛 ServiceException 提示「仅正常或维修状态的设备可以停用」")
    void maintenanceDevice_disabledToDisabled_shouldThrow() {
        Device current = new Device();
        current.setId(3L);
        current.setCode("dev-003");
        current.setStatus(3);

        when(deviceMapper.selectDeviceById(3L)).thenReturn(current);

        ServiceException ex = assertThrows(ServiceException.class,
                () -> service.maintenanceDevice(3L, 3, "Test", "13800000000",
                        "2026-06-15 10:00:00", "重复停用", "admin"));

        assertEquals("仅正常或维修状态的设备可以停用", ex.getMessage());
    }

    // ── copyDevice 测试 ──

    @Test
    @DisplayName("copySensors=true 时应复制传感器及属性")
    void copyDevice_withSensors_shouldCopySensors() {
        Device original = new Device();
        original.setId(1L);
        original.setCode("DEV001");
        original.setName("测试设备");
        original.setCreateBy("admin");

        DeviceSensor sensor = DeviceSensor.builder()
                .id(10L)
                .sensorCode("SENSOR_01")
                .sensorName("传感器1")
                .monitorTypeId(1L)
                .build();

        SensorAttribute attr = new SensorAttribute();
        attr.setId(100L);
        attr.setAttrCode("temp");

        when(deviceMapper.selectDeviceById(1L)).thenReturn(original);
        when(deviceMapper.checkDeviceCodeUnique(anyString(), any())).thenReturn(null);
        when(accountGenerator.generateUsername()).thenReturn("ABC123");
        when(accountGenerator.generatePassword()).thenReturn("Pass1234");
        when(sensorMapper.selectSensorListByDeviceId(1L)).thenReturn(List.of(sensor));
        when(attributeMapper.selectAttributeListBySensorId(10L)).thenReturn(List.of(attr));

        DeviceCopyRequest request = new DeviceCopyRequest();
        request.setCode("DEV002");
        request.setName("复制设备");
        request.setCopySensors(true);

        service.copyDevice(1L, request);

        verify(sensorMapper).insertSensor(any(DeviceSensor.class));
        verify(attributeMapper).batchInsertAttribute(any());
        verify(productTslService).regenerate(any());
    }

    @Test
    @DisplayName("copySensors=false 时不应复制传感器")
    void copyDevice_withoutSensors_shouldSkipSensors() {
        Device original = new Device();
        original.setId(1L);
        original.setCode("DEV001");
        original.setName("测试设备");
        original.setCreateBy("admin");

        when(deviceMapper.selectDeviceById(1L)).thenReturn(original);
        when(deviceMapper.checkDeviceCodeUnique(anyString(), any())).thenReturn(null);
        when(accountGenerator.generateUsername()).thenReturn("ABC123");
        when(accountGenerator.generatePassword()).thenReturn("Pass1234");

        DeviceCopyRequest request = new DeviceCopyRequest();
        request.setCode("DEV002");
        request.setName("仅设备");
        request.setCopySensors(false);

        service.copyDevice(1L, request);

        // 不应查询或复制传感器
        verify(sensorMapper, never()).selectSensorListByDeviceId(any());
        verify(sensorMapper, never()).insertSensor(any());
        verify(attributeMapper, never()).batchInsertAttribute(any());
        verify(productTslService).regenerate(any());
    }

    @Test
    @DisplayName("copySensors 未设置时默认复制传感器（向后兼容）")
    void copyDevice_defaultCopySensors_shouldCopySensors() {
        Device original = new Device();
        original.setId(1L);
        original.setCode("DEV001");
        original.setName("测试设备");
        original.setCreateBy("admin");

        DeviceSensor sensor = DeviceSensor.builder()
                .id(10L)
                .sensorCode("SENSOR_01")
                .sensorName("传感器1")
                .monitorTypeId(1L)
                .build();

        when(deviceMapper.selectDeviceById(1L)).thenReturn(original);
        when(deviceMapper.checkDeviceCodeUnique(anyString(), any())).thenReturn(null);
        when(accountGenerator.generateUsername()).thenReturn("ABC123");
        when(accountGenerator.generatePassword()).thenReturn("Pass1234");
        when(sensorMapper.selectSensorListByDeviceId(1L)).thenReturn(List.of(sensor));

        DeviceCopyRequest request = new DeviceCopyRequest();
        request.setCode("DEV002");
        request.setName("默认复制");
        // 不设置 copySensors — 应默认 true

        service.copyDevice(1L, request);

        verify(sensorMapper).selectSensorListByDeviceId(1L);
        verify(sensorMapper).insertSensor(any(DeviceSensor.class));
    }
}
