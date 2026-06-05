package com.zwei.iot.broker.handler;

import com.zwei.iot.broker.component.MqttAuthFailureGuard;
import com.zwei.iot.broker.component.MqttDeviceSessionRegistry;
import com.zwei.iot.broker.config.MqttAuthCenterProperties;
import com.zwei.iot.broker.exception.MqttExceptionReporter;
import com.zwei.iot.broker.service.MqttDeviceAuthService;
import com.zwei.iot.device.domain.Device;
import com.zwei.iot.device.mapper.DeviceMapper;
import com.zwei.iot.device.service.DeviceAuthLogService;
import net.dreamlu.mica.net.core.ChannelContext;
import net.dreamlu.mica.net.core.Node;
import org.dromara.mica.mqtt.core.server.MqttServer;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.beans.factory.support.StaticListableBeanFactory;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("MqttServerAuthHandler 单元测试")
class MqttServerAuthHandlerTest {

    @Mock
    private DeviceMapper deviceMapper;

    @Mock
    private DeviceAuthLogService deviceAuthLogService;

    @Mock
    private MqttServer mqttServer;

    @Mock
    private ChannelContext channelContext;

    private MqttServerAuthHandler authHandler;

    /**
     * 组装鉴权中心的最小依赖，验证 Handler 对核心服务的委派行为。
     *
     */
    @BeforeEach
    void setUp() {
        MqttAuthCenterProperties properties = new MqttAuthCenterProperties();
        properties.setEnforceMqttProtocol(true);
        MqttDeviceSessionRegistry registry = new MqttDeviceSessionRegistry();
        MqttAuthFailureGuard failureGuard = new MqttAuthFailureGuard(properties);
        MqttExceptionReporter mqttExceptionReporter = new MqttExceptionReporter();
        StaticListableBeanFactory beanFactory = new StaticListableBeanFactory();
        beanFactory.addBean("mqttServer", mqttServer);
        ObjectProvider<MqttServer> mqttServerProvider = beanFactory.getBeanProvider(MqttServer.class);
        MqttDeviceAuthService authService = new MqttDeviceAuthService(
                deviceMapper,
                deviceAuthLogService,
                registry,
                failureGuard,
                properties,
                mqttServerProvider,
                mqttExceptionReporter,
                mock(ApplicationEventPublisher.class)
        );
        authHandler = new MqttServerAuthHandler(authService, mqttExceptionReporter);
        when(channelContext.getClientNode()).thenReturn(new Node("127.0.0.1", 1883));
    }

    @Test
    @DisplayName("正确用户名密码应通过鉴权并写入上下文")
    void authenticate_shouldPassWhenCredentialIsCorrect() {
        Device device = buildDevice();
        when(deviceMapper.selectDeviceByAuthUsername("A7K9P2")).thenReturn(device);

        boolean result = authHandler.authenticate(channelContext, "client-1", "client-1", "A7K9P2", "m4T9x2Q8");

        assertTrue(result);
        verify(deviceMapper).updateDevice(any(Device.class));
        verify(deviceAuthLogService).save(any());
        verify(channelContext).setUserId("101");
        verify(channelContext).setToken("A7K9P2");
        verify(channelContext).setBsId("client-1");
    }

    @Test
    @DisplayName("相同设备重复接入时应踢掉旧连接")
    void authenticate_shouldDisconnectPreviousClientWhenDeviceReconnects() {
        Device device = buildDevice();
        when(deviceMapper.selectDeviceByAuthUsername("A7K9P2")).thenReturn(device);
        when(mqttServer.disconnect("client-old")).thenReturn(true);

        assertTrue(authHandler.authenticate(channelContext, "client-old", "client-old", "A7K9P2", "m4T9x2Q8"));
        assertTrue(authHandler.authenticate(channelContext, "client-new", "client-new", "A7K9P2", "m4T9x2Q8"));

        verify(mqttServer).disconnect("client-old");
        verify(deviceAuthLogService, times(2)).save(any());
    }

    @Test
    @DisplayName("连续失败达到阈值后应进入临时封禁")
    void authenticate_shouldBlockWhenFailuresReachThreshold() {
        Device device = buildDevice();
        when(deviceMapper.selectDeviceByAuthUsername("A7K9P2")).thenReturn(device);

        for (int i = 0; i < 5; i++) {
            boolean failed = authHandler.authenticate(channelContext, "client-" + i, "client-" + i, "A7K9P2", "badPwd01");
            assertFalse(failed);
        }

        boolean blocked = authHandler.authenticate(channelContext, "client-blocked", "client-blocked", "A7K9P2", "m4T9x2Q8");

        assertFalse(blocked);
        verify(deviceAuthLogService, atLeastOnce()).save(any());
        verify(deviceMapper, times(6)).selectDeviceByAuthUsername(eq("A7K9P2"));
    }

    @Test
    @DisplayName("非 MQTT 协议设备应拒绝接入")
    void authenticate_shouldRejectWhenProtocolIsNotMqtt() {
        Device device = buildDevice();
        device.setProtocolType("HTTP");
        when(deviceMapper.selectDeviceByAuthUsername("A7K9P2")).thenReturn(device);

        boolean result = authHandler.authenticate(channelContext, "client-1", "client-1", "A7K9P2", "m4T9x2Q8");

        assertFalse(result);
        verify(deviceMapper, times(1)).selectDeviceByAuthUsername("A7K9P2");
    }

    /**
     * 构造一个默认可接入的 MQTT 设备，用作各场景测试基线。
     *
     * @return 可通过鉴权的设备实体
     */
    private Device buildDevice() {
        Device device = new Device();
        device.setId(101L);
        device.setAuthUsername("A7K9P2");
        device.setAuthPassword("m4T9x2Q8");
        device.setAuthStatus(1);
        device.setProtocolType("MQTT");
        return device;
    }
}
