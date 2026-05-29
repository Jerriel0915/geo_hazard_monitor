package com.zwei.iot.device.service.impl;

import com.zwei.iot.device.domain.Device;
import com.zwei.iot.device.domain.DeviceRegistrationLog;
import com.zwei.iot.device.domain.DeviceSensor;
import com.zwei.iot.device.domain.dto.DeviceRegisterMonitorTypeRequest;
import com.zwei.iot.device.domain.dto.DeviceRegisterRequest;
import com.zwei.iot.device.domain.vo.DeviceRegistryResult;
import com.zwei.iot.device.mapper.DeviceMapper;
import com.zwei.iot.device.mapper.DeviceRegistrationLogMapper;
import com.zwei.iot.device.mapper.DeviceSensorMapper;
import com.zwei.iot.device.mapper.SensorAttributeMapper;
import com.zwei.iot.device.service.DeviceRegistrationLogService;
import com.zwei.iot.device.support.DeviceAuthAccountGenerator;
import com.zwei.iot.monitor.domain.MonitorContent;
import com.zwei.iot.monitor.domain.MonitorType;
import com.zwei.iot.monitor.service.IMonitorContentService;
import com.zwei.iot.monitor.service.IMonitorTypeService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import java.math.BigDecimal;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
@DisplayName("DeviceRegistryServiceImpl 单元测试")
class DeviceRegistryServiceImplTest {

    @Mock
    private DeviceMapper deviceMapper;

    @Mock
    private DeviceSensorMapper sensorMapper;

    @Mock
    private SensorAttributeMapper attributeMapper;

    @Mock
    private DeviceRegistrationLogMapper registrationLogMapper;

    @Mock
    private DeviceRegistrationLogService registrationLogService;

    @Mock
    private IMonitorTypeService monitorTypeService;

    @Mock
    private IMonitorContentService monitorContentService;

    @Mock
    private DeviceAuthAccountGenerator accountGenerator;

    @InjectMocks
    private DeviceRegistryServiceImpl service;

    @BeforeEach
    void setUp() {
        ReflectionTestUtils.setField(service, "configuredRegisterCodes", "ABCDEF123456");
    }

    @Test
    @DisplayName("注册新设备时应创建设备、传感器和注册日志")
    void register_shouldCreateDeviceSensorsAndLog() {
        DeviceRegisterRequest request = buildRequest();

        when(registrationLogService.selectByRequestId("REQ-001")).thenReturn(null);
        when(deviceMapper.selectDeviceBySn("SN001")).thenReturn(null);
        when(deviceMapper.selectDeviceByCode("DEV-SN001")).thenReturn(null);
        when(sensorMapper.selectSensorByCode("DEV-SN001_1")).thenReturn(null);
        when(accountGenerator.generateUsername()).thenReturn("A1B2C3");
        when(accountGenerator.generatePassword()).thenReturn("Ab12Cd34");

        MonitorType monitorType = new MonitorType();
        monitorType.setId(9L);
        monitorType.setCode("L1_LF");
        monitorType.setName("裂缝监测");
        when(monitorTypeService.selectMonitorTypeByCode("L1_LF")).thenReturn(monitorType);

        MonitorContent content = new MonitorContent();
        content.setCode("displacement");
        content.setName("位移");
        content.setIndicatorType("wy");
        content.setUnit("mm");
        content.setRangeMin(BigDecimal.ZERO);
        content.setRangeMax(new BigDecimal("100"));
        when(monitorContentService.selectMonitorContentAll(9L)).thenReturn(List.of(content));

        doAnswer(invocation -> {
            Device device = invocation.getArgument(0);
            device.setId(101L);
            return 1;
        }).when(deviceMapper).insertDevice(any(Device.class));
        doAnswer(invocation -> {
            DeviceSensor sensor = invocation.getArgument(0);
            sensor.setId(1001L);
            return 1;
        }).when(sensorMapper).insertSensor(any(DeviceSensor.class));

        Device stored = new Device();
        stored.setId(101L);
        stored.setAuthUsername("A1B2C3");
        stored.setAuthPassword("Ab12Cd34");
        when(deviceMapper.selectDeviceById(101L)).thenReturn(stored);

        DeviceRegistryResult result = service.register(request);

        assertTrue(result.created());
        assertEquals(101L, result.device().getId());
        verify(deviceMapper).insertDevice(any(Device.class));
        verify(sensorMapper).insertSensor(any(DeviceSensor.class));
        verify(attributeMapper).insertAttribute(any());
        verify(registrationLogMapper).insert(any(DeviceRegistrationLog.class));
        verify(registrationLogService, never()).save(any(DeviceRegistrationLog.class));
    }

    @Test
    @DisplayName("重复requestId命中成功日志时应直接返回已有设备")
    void register_shouldReturnExistingResultWhenRequestIdExists() {
        DeviceRegistrationLog existingLog = new DeviceRegistrationLog();
        existingLog.setRequestId("REQ-001");
        existingLog.setResultStatus("SUCCESS");
        existingLog.setDeviceId(101L);
        when(registrationLogService.selectByRequestId("REQ-001")).thenReturn(existingLog);

        Device stored = new Device();
        stored.setId(101L);
        stored.setAuthUsername("A1B2C3");
        stored.setAuthPassword("Ab12Cd34");
        when(deviceMapper.selectDeviceById(101L)).thenReturn(stored);

        DeviceRegistryResult result = service.register(buildRequest());

        assertFalse(result.created());
        assertEquals(101L, result.device().getId());
        verify(deviceMapper, never()).insertDevice(any(Device.class));
    }

    private DeviceRegisterRequest buildRequest() {
        DeviceRegisterMonitorTypeRequest monitorType = new DeviceRegisterMonitorTypeRequest();
        monitorType.setType("L1_LF");
        monitorType.setSid("1");

        DeviceRegisterRequest request = new DeviceRegisterRequest();
        request.setRequestId("REQ-001");
        request.setRegisterCode("ABCDEF123456");
        request.setVendorName("测试厂商");
        request.setSn("SN001");
        request.setDeviceName("裂缝计");
        request.setDeviceType("0");
        request.setNetwork("0");
        request.setProtocol("0");
        request.setMonitorTypes(List.of(monitorType));
        return request;
    }
}
