package com.zwei.iot.broker.component;

import com.zwei.iot.broker.config.MqttAuthCenterProperties;
import com.zwei.iot.broker.exception.MqttExceptionReporter;
import com.zwei.iot.broker.service.MqttDeviceAuthService;
import com.zwei.iot.device.domain.Device;
import com.zwei.iot.device.service.IDeviceAuthQueryService;
import com.zwei.iot.device.service.DeviceAuthLogService;
import com.zwei.iot.device.service.ITopicPatternService;
import net.dreamlu.mica.net.core.ChannelContext;
import org.dromara.mica.mqtt.codec.MqttQoS;
import org.dromara.mica.mqtt.core.server.MqttServer;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;
import org.springframework.beans.factory.support.StaticListableBeanFactory;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.data.redis.core.StringRedisTemplate;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
@DisplayName("MqttServerPublishPermission 单元测试")
class MqttServerPublishPermissionTest {

    @Mock
    private IDeviceAuthQueryService deviceAuthQueryService;

    @Mock
    private DeviceAuthLogService deviceAuthLogService;

    @Mock
    private ChannelContext channelContext;

    private MqttServerPublishPermission publishPermission;
    private MqttDeviceAuthService authService;

    private static final String DEVICE_CODE = "DEV001";

    /**
     * 先构造一个已完成 CONNECT 鉴权的会话，为后续发布准入场景提供统一起点。
     */
    @Mock
    private ITopicPatternService topicPatternService;

    @BeforeEach
    void setUp() {
        MqttAuthCenterProperties properties = new MqttAuthCenterProperties();
        MqttDeviceSessionRegistry registry = new MqttDeviceSessionRegistry();
        MqttAuthFailureGuard failureGuard = new MqttAuthFailureGuard(properties,
                mock(StringRedisTemplate.class, org.mockito.Mockito.RETURNS_DEEP_STUBS));
        MqttExceptionReporter mqttExceptionReporter = new MqttExceptionReporter();
        StaticListableBeanFactory beanFactory = new StaticListableBeanFactory();
        beanFactory.addBean("mqttServer", org.mockito.Mockito.mock(MqttServer.class));
        authService = new MqttDeviceAuthService(
                deviceAuthQueryService,
                deviceAuthLogService,
                registry,
                failureGuard,
                properties,
                beanFactory.getBeanProvider(MqttServer.class),
                mqttExceptionReporter,
                mock(ApplicationEventPublisher.class),
                topicPatternService
        );
        // stub topic pattern resolution: valid topics return components, invalid return null
        when(topicPatternService.resolveTopic("sys/v1/" + DEVICE_CODE + "/S01/updata"))
                .thenReturn(new ITopicPatternService.TopicComponents("sys", DEVICE_CODE, "S01"));
        when(topicPatternService.resolveTopic("sys/v1/OTHER/S01/updata"))
                .thenReturn(new ITopicPatternService.TopicComponents("sys", "OTHER", "S01"));
        publishPermission = new MqttServerPublishPermission(authService, mqttExceptionReporter);
        when(deviceAuthQueryService.findByAuthUsername("A7K9P2")).thenReturn(buildDevice());
        authService.authenticate(channelContext, "client-1", "client-1", "A7K9P2", "m4T9x2Q8");
    }

    @Test
    @DisplayName("已鉴权设备发布合法主题时应放行")
    void hasPermission_shouldReturnTrueWhenTopicValidAndAuthenticated() {
        boolean result = publishPermission.hasPermission(channelContext, "client-1", "sys/v1/" + DEVICE_CODE + "/S01/updata", MqttQoS.QOS1, false);

        assertTrue(result);
    }

    @Test
    @DisplayName("未认证会话发布消息时应拒绝")
    void hasPermission_shouldReturnFalseWhenSessionMissing() {
        boolean result = publishPermission.hasPermission(channelContext, "client-2", "sys/v1/" + DEVICE_CODE + "/S01/updata", MqttQoS.QOS1, false);

        assertFalse(result);
    }

    @Test
    @DisplayName("topic 中设备编码与已认证设备不一致时应拒绝")
    void hasPermission_shouldReturnFalseWhenDeviceMismatch() {
        boolean result = publishPermission.hasPermission(channelContext, "client-1", "sys/v1/OTHER/S01/updata", MqttQoS.QOS1, false);

        assertFalse(result);
    }

    @Test
    @DisplayName("topic 非 sys 或 gb 规范时应拒绝")
    void hasPermission_shouldReturnFalseWhenTopicInvalid() {
        boolean result = publishPermission.hasPermission(channelContext, "client-1", "test/topic", MqttQoS.QOS0, false);

        assertFalse(result);
    }

    /**
     * 构造一个默认可接入设备，用于发布权限测试前置鉴权。
     *
     * @return 可通过鉴权的设备实体
     */
    private Device buildDevice() {
        Device device = new Device();
        device.setId(101L);
        device.setCode(DEVICE_CODE);
        device.setAuthUsername("A7K9P2");
        device.setAuthPassword("m4T9x2Q8");
        device.setAuthStatus(1);
        device.setProtocolType("MQTT");
        return device;
    }
}
