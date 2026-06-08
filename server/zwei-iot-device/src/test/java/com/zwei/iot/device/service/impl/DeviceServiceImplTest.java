package com.zwei.iot.device.service.impl;

import com.zwei.iot.device.domain.Device;
import com.zwei.iot.device.mapper.DeviceMapper;
import com.zwei.iot.device.mapper.DeviceSensorMapper;
import com.zwei.iot.device.mapper.SensorAttributeMapper;
import com.zwei.iot.device.service.DeviceAuthLogService;
import com.zwei.iot.device.service.IDeviceHazardRelationService;
import com.zwei.iot.device.service.IDeviceSessionService;
import com.zwei.iot.device.service.IDeviceStatusLogService;
import com.zwei.iot.device.support.DeviceAuthAccountGenerator;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.beans.factory.ObjectProvider;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
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
}
