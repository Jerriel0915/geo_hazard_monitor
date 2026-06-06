package com.zwei.iot.broker;

import com.zwei.iot.broker.component.MqttAuthFailureGuard;
import com.zwei.iot.broker.component.MqttDeviceSessionRegistry;
import com.zwei.iot.broker.component.MqttServerPublishPermission;
import com.zwei.iot.broker.config.MqttAuthCenterProperties;
import com.zwei.iot.broker.exception.MqttExceptionReporter;
import com.zwei.iot.broker.handler.MqttServerAuthHandler;
import com.zwei.iot.broker.service.MqttConnectStatusListener;
import com.zwei.iot.broker.service.MqttDeviceAuthService;
import com.zwei.iot.device.domain.Device;
import com.zwei.iot.device.service.IDeviceAuthQueryService;
import com.zwei.iot.device.service.DeviceAuthLogService;
import net.dreamlu.mica.net.core.ChannelContext;
import net.dreamlu.mica.net.core.Node;
import org.dromara.mica.mqtt.codec.MqttQoS;
import org.dromara.mica.mqtt.core.server.MqttServer;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.test.context.junit.jupiter.SpringJUnitConfig;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.*;

@SpringJUnitConfig(MqttAuthCenterIntegrationTest.TestConfig.class)
@DisplayName("MQTT 鉴权中心集成测试")
class MqttAuthCenterIntegrationTest {

    @Autowired
    private MqttServerAuthHandler authHandler;

    @Autowired
    private MqttServerPublishPermission publishPermission;

    @Autowired
    private MqttConnectStatusListener connectStatusListener;

    @Autowired
    private IDeviceAuthQueryService deviceAuthQueryService;

    private final ChannelContext channelContext = mock(ChannelContext.class);

    private static final String DEVICE_CODE = "DEV001";

    /**
     * 为集成场景准备一台默认在线可接入的设备。
     */
    @BeforeEach
    void setUp() {
        Device device = new Device();
        device.setId(101L);
        device.setCode(DEVICE_CODE);
        device.setAuthUsername("A7K9P2");
        device.setAuthPassword("m4T9x2Q8");
        device.setAuthStatus(1);
        device.setProtocolType("MQTT");
        when(deviceAuthQueryService.findByAuthUsername("A7K9P2")).thenReturn(device);
        when(channelContext.getClientNode()).thenReturn(new Node("127.0.0.1", 1883));
    }

    @Test
    @DisplayName("Spring 上下文内应完成鉴权、发布准入和离线清理闭环")
    void authPublishAndOffline_shouldWorkEndToEnd() {
        boolean authed = authHandler.authenticate(channelContext, "client-1", "client-1", "A7K9P2", "m4T9x2Q8");
        boolean publishAllowed = publishPermission.hasPermission(channelContext, "client-1", "sys/v1/" + DEVICE_CODE + "/S01/updata", MqttQoS.QOS1, false);

        connectStatusListener.offline(channelContext, "client-1", "A7K9P2", "test");

        boolean publishAfterOffline = publishPermission.hasPermission(channelContext, "client-1", "sys/v1/" + DEVICE_CODE + "/S01/updata", MqttQoS.QOS1, false);

        assertTrue(authed);
        assertTrue(publishAllowed);
        assertFalse(publishAfterOffline);

        ArgumentCaptor<Device> captor = ArgumentCaptor.forClass(Device.class);
        verify(deviceAuthQueryService, atLeast(2)).updateDevice(captor.capture());
        List<Device> updates = captor.getAllValues();
        assertTrue(updates.stream().anyMatch(device -> Integer.valueOf(1).equals(device.getRunStatus())));
        assertTrue(updates.stream().anyMatch(device -> Integer.valueOf(2).equals(device.getRunStatus())));
    }

    /**
     * 构建鉴权中心集成测试上下文。
     * <p>
     * 显式装配最小 Spring Bean 集，验证各组件之间的装配关系和调用链闭环。
     */
    @Configuration
    static class TestConfig {
        @Bean
        MqttAuthCenterProperties mqttAuthCenterProperties() {
            return new MqttAuthCenterProperties();
        }

        @Bean
        IDeviceAuthQueryService deviceAuthQueryService() {
            return mock(IDeviceAuthQueryService.class);
        }

        @Bean
        DeviceAuthLogService deviceAuthLogService() {
            return mock(DeviceAuthLogService.class);
        }

        @Bean
        MqttServer mqttServer() {
            return mock(MqttServer.class);
        }

        @Bean
        MqttDeviceSessionRegistry mqttDeviceSessionRegistry() {
            return new MqttDeviceSessionRegistry();
        }

        @Bean
        MqttAuthFailureGuard mqttAuthFailureGuard(MqttAuthCenterProperties properties) {
            return new MqttAuthFailureGuard(properties);
        }

        @Bean
        MqttExceptionReporter mqttExceptionReporter() {
            return new MqttExceptionReporter();
        }

        @Bean
        MqttDeviceAuthService mqttDeviceAuthService(IDeviceAuthQueryService deviceAuthQueryService,
                                                    DeviceAuthLogService deviceAuthLogService,
                                                    MqttDeviceSessionRegistry registry,
                                                    MqttAuthFailureGuard failureGuard,
                                                    MqttAuthCenterProperties properties,
                                                    MqttServer mqttServer,
                                                    MqttExceptionReporter mqttExceptionReporter) {
            return new MqttDeviceAuthService(
                    deviceAuthQueryService,
                    deviceAuthLogService,
                    registry,
                    failureGuard,
                    properties,
                    new org.springframework.beans.factory.support.StaticListableBeanFactory() {{
                        addBean("mqttServer", mqttServer);
                    }}.getBeanProvider(MqttServer.class),
                    mqttExceptionReporter,
                    mock(ApplicationEventPublisher.class)
            );
        }

        @Bean
        MqttServerAuthHandler mqttServerAuthHandler(MqttDeviceAuthService authService,
                                                    MqttExceptionReporter mqttExceptionReporter) {
            return new MqttServerAuthHandler(authService, mqttExceptionReporter);
        }

        @Bean
        MqttServerPublishPermission mqttServerPublishPermission(MqttDeviceAuthService authService,
                                                                MqttExceptionReporter mqttExceptionReporter) {
            return new MqttServerPublishPermission(authService, mqttExceptionReporter);
        }

        @Bean
        MqttConnectStatusListener mqttConnectStatusListener(MqttDeviceAuthService authService) {
            return new MqttConnectStatusListener(authService, mock(ApplicationEventPublisher.class));
        }
    }
}
