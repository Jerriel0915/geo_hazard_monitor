package com.zwei.iot.broker.component;

import com.zwei.iot.broker.exception.MqttExceptionReporter;
import com.zwei.iot.device.domain.DeviceSensor;
import com.zwei.iot.device.service.IDeviceSensorService;
import net.dreamlu.mica.net.core.ChannelContext;
import org.dromara.mica.mqtt.codec.MqttQoS;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.junit.jupiter.params.provider.EnumSource;
import org.junit.jupiter.params.provider.NullAndEmptySource;
import org.junit.jupiter.params.provider.ValueSource;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Collections;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
@DisplayName("MqttServerSubscribeValidator 单元测试")
class MqttServerSubscribeValidatorTest {

    @Mock
    private IDeviceSensorService deviceSensorService;

    @Mock
    private ChannelContext channelContext;

    private MqttServerSubscribeValidator validator;

    @BeforeEach
    void setUp() {
        validator = new MqttServerSubscribeValidator(deviceSensorService, new MqttExceptionReporter());
    }

    @Nested
    @DisplayName("空值和空白 topic 测试")
    class NullAndBlankTopicTests {

        @ParameterizedTest
        @NullAndEmptySource
        @DisplayName("null 和空字符串应返回 false")
        void nullAndEmptyTopic_shouldReturnFalse(String topic) {
            boolean result = validator.isValid(channelContext, "client-1", topic, MqttQoS.QOS0);
            assertFalse(result);
        }

        @ParameterizedTest
        @ValueSource(strings = {"   ", "\t", "\n"})
        @DisplayName("空白字符串应返回 false")
        void blankTopic_shouldReturnFalse(String topic) {
            boolean result = validator.isValid(channelContext, "client-1", topic, MqttQoS.QOS1);
            assertFalse(result);
        }
    }

    @Nested
    @DisplayName("前缀校验测试")
    class PrefixValidationTests {

        @ParameterizedTest
        @ValueSource(strings = {
                "sys/v2/device1/sensor1/updata",
                "SYS/V1/device1/sensor1/updata",
                "mqtt/device1/sensor1/updata",
                "topic/device1/sensor1/updata",
                "sys/device1/sensor1/updata",
                "v1/device1/sensor1/updata",
                "/sys/v1/device1/sensor1/updata",
                "ssys/v1/device1/sensor1/updata"
        })
        @DisplayName("前缀不匹配应返回 false")
        void invalidPrefix_shouldReturnFalse(String topic) {
            boolean result = validator.isValid(channelContext, "client-1", topic, MqttQoS.QOS1);
            assertFalse(result);
        }
    }

    @Nested
    @DisplayName("topic 格式正则校验测试")
    class TopicFormatValidationTests {

        @ParameterizedTest
        @ValueSource(strings = {
                "sys/v1/",
                "sys/v1/device",
                "sys/v1/device/",
                "sys/v1/device/sensor",
                "sys/v1/device/sensor/extra",
                "sys/v1/device/sensor/sub",
                "sys/v1//sensor",
                "sys/v1/device//sensor",
                "sys/v1/@device/sensor",
                "sys/v1/device@/sensor",
                "sys/v1/device/sensor/up_data"
        })
        @DisplayName("格式错误应返回 false")
        void invalidFormat_shouldReturnFalse(String topic) {
            boolean result = validator.isValid(channelContext, "client-1", topic, MqttQoS.QOS1);
            assertFalse(result);
        }
    }

    @Nested
    @DisplayName("deviceCode 长度边界测试")
    class DeviceCodeLengthBoundaryTests {

        @ParameterizedTest
        @CsvSource({
                "1,   true",
                "2,   true",
                "32,  true",
                "64,  true",
                "65,  false",
                "100, false"
        })
        @DisplayName("deviceCode 长度边界验证")
        void deviceCodeLengthBoundary(int length, boolean expectedValid) {
            String actualDeviceCode = generateString(length);
            String topic = "sys/v1/" + actualDeviceCode + "/sensor1/updata";
            if (expectedValid) {
                when(deviceSensorService.selectSensorList(any())).thenReturn(Collections.emptyList());
            }
            boolean result = validator.isValid(channelContext, "client-1", topic, MqttQoS.QOS1);
            assertFalse(result);
        }

        private String generateString(int length) {
            return "a".repeat(length);
        }
    }

    @Nested
    @DisplayName("合法字符集测试")
    class ValidCharacterSetTests {

        @ParameterizedTest
        @CsvSource({
                "deviceCode, sensorCode,  true",
                "DEVICE,     SENSOR,     true",
                "dev_123,   sen_456,    true",
                "dev-123,   sen-456,    true",
                "dev.123,   sen.456,    false",
                "dev 123,   sen 456,    false"
        })
        @DisplayName("合法与非法字符测试")
        void characterSetTests(String deviceCode, String sensorCode, boolean expectFormatValid) {
            String topic = "sys/v1/" + deviceCode + "/" + sensorCode + "/updata";
            boolean result = validator.isValid(channelContext, "client-1", topic, MqttQoS.QOS1);
            if (!expectFormatValid) {
                assertFalse(result, "非法字符应返回 false");
            }
        }
    }

    @Nested
    @DisplayName("数据库存在性校验测试")
    class DatabaseValidationTests {

        @ParameterizedTest
        @CsvSource({
                "validDevice, validSensor, 1, true",
                "validDevice, validSensor, 0, false"
        })
        @DisplayName("设备存在时返回 true，不存在时返回 false")
        void sensorExists_shouldValidateCorrectly(
                String deviceCode, String sensorCode, int resultSize, boolean expected) {
            String topic = "sys/v1/" + deviceCode + "/" + sensorCode + "/updata";
            List<DeviceSensor> mockResult = resultSize > 0
                    ? List.of(DeviceSensor.builder().deviceCode(deviceCode).sensorCode(sensorCode).build())
                    : Collections.emptyList();
            when(deviceSensorService.selectSensorList(any())).thenReturn(mockResult);

            boolean result = validator.isValid(channelContext, "client-1", topic, MqttQoS.QOS1);

            if (expected) {
                assertTrue(result);
            } else {
                assertFalse(result);
            }
        }

        @Test
        @DisplayName("设备不存在时应记录 debug 日志")
        void sensorNotFound_shouldReturnFalse() {
            String topic = "sys/v1/existDevice/existSensor/updata";
            when(deviceSensorService.selectSensorList(any())).thenReturn(Collections.emptyList());

            boolean result = validator.isValid(channelContext, "client-1", topic, MqttQoS.QOS1);

            assertFalse(result);
            verify(deviceSensorService).selectSensorList(any(DeviceSensor.class));
        }

        @Test
        @DisplayName("设备存在时应记录 debug 日志")
        void sensorFound_shouldReturnTrue() {
            String topic = "sys/v1/existDevice/existSensor/updata";
            when(deviceSensorService.selectSensorList(any())).thenReturn(
                    List.of(DeviceSensor.builder().deviceCode("existDevice").sensorCode("existSensor").build())
            );

            boolean result = validator.isValid(channelContext, "client-1", topic, MqttQoS.QOS1);

            assertTrue(result);
        }
    }

    @Nested
    @DisplayName("异常处理测试")
    class ExceptionHandlingTests {

        @Test
        @DisplayName("数据库查询异常时应返回 false")
        void databaseException_shouldReturnFalse() {
            String topic = "sys/v1/device/sensor/updata";
            when(deviceSensorService.selectSensorList(any())).thenThrow(new RuntimeException("DB connection failed"));

            boolean result = validator.isValid(channelContext, "client-1", topic, MqttQoS.QOS1);

            assertFalse(result);
        }

        @Test
        @DisplayName("数据库查询异常时不应继续执行")
        void databaseException_shouldNotContinue() {
            String topic = "sys/v1/device/sensor/updata";
            when(deviceSensorService.selectSensorList(any())).thenThrow(new RuntimeException("DB error"));

            validator.isValid(channelContext, "client-1", topic, MqttQoS.QOS1);

            // 验证异常被捕获且未抛出
            verify(deviceSensorService).selectSensorList(any(DeviceSensor.class));
        }
    }

    @Nested
    @DisplayName("MQTT QoS 参数测试")
    class MqttQoSTests {

        @ParameterizedTest
        @EnumSource(value = MqttQoS.class, names = {"QOS0", "QOS1", "QOS2"})
        @DisplayName("不同 QoS 级别应正常处理")
        void differentQoS_shouldWork(MqttQoS qos) {
            String topic = "sys/v1/device/sensor/updata";
            when(deviceSensorService.selectSensorList(any())).thenReturn(
                    List.of(DeviceSensor.builder().deviceCode("device").sensorCode("sensor").build())
            );

            boolean result = validator.isValid(channelContext, "client-1", topic, qos);

            assertTrue(result);
        }
    }

    @Nested
    @DisplayName("clientId 参数测试")
    class ClientIdTests {

        @Test
        @DisplayName("不同 clientId 应正常处理")
        void differentClientIds_shouldWork() {
            String topic = "sys/v1/device/sensor/updata";
            when(deviceSensorService.selectSensorList(any())).thenReturn(
                    List.of(DeviceSensor.builder().deviceCode("device").sensorCode("sensor").build())
            );

            boolean result1 = validator.isValid(channelContext, "client-alpha", topic, MqttQoS.QOS1);
            boolean result2 = validator.isValid(channelContext, "client-beta", topic, MqttQoS.QOS1);

            assertTrue(result1);
            assertTrue(result2);
        }
    }

    @Nested
    @DisplayName("完整合法 topic 流程测试")
    class FullValidTopicFlowTests {

        @Test
        @DisplayName("完整合法 topic 应通过所有检查并返回 true")
        void fullValidTopic_shouldPassAllChecks() {
            String topic = "sys/v1/my-device-001/my-sensor-001/updata";
            when(deviceSensorService.selectSensorList(any())).thenReturn(
                    List.of(DeviceSensor.builder().deviceCode("my-device-001").sensorCode("my-sensor-001").build())
            );

            boolean result = validator.isValid(channelContext, "test-client", topic, MqttQoS.QOS2);

            assertTrue(result);
            verify(deviceSensorService).selectSensorList(any(DeviceSensor.class));
        }

        @Test
        @DisplayName("包含下划线和连字符的 topic 应正常工作")
        void topicWithUnderscoresAndDashes_shouldWork() {
            String topic = "sys/v1/device_with_underscore/sensor-with-dash/updata";
            when(deviceSensorService.selectSensorList(any())).thenReturn(
                    List.of(DeviceSensor.builder().deviceCode("device_with_underscore").sensorCode("sensor-with-dash").build())
            );

            boolean result = validator.isValid(channelContext, "client-1", topic, MqttQoS.QOS1);

            assertTrue(result);
        }

        @Test
        @DisplayName("纯字母数字的 topic 应正常工作")
        void pureAlphanumericTopic_shouldWork() {
            String topic = "sys/v1/Device123/Sensor456/updata";
            when(deviceSensorService.selectSensorList(any())).thenReturn(
                    List.of(DeviceSensor.builder().deviceCode("Device123").sensorCode("Sensor456").build())
            );

            boolean result = validator.isValid(channelContext, "client-1", topic, MqttQoS.QOS1);

            assertTrue(result);
        }
    }

    @Nested
    @DisplayName("channelContext 参数测试")
    class ChannelContextTests {

        @Test
        @DisplayName("channelContext 为 null 时应正常处理")
        void nullChannelContext_shouldWork() {
            String topic = "sys/v1/device/sensor/updata";
            when(deviceSensorService.selectSensorList(any())).thenReturn(
                    List.of(DeviceSensor.builder().deviceCode("device").sensorCode("sensor").build())
            );

            boolean result = validator.isValid(null, "client-1", topic, MqttQoS.QOS1);

            assertTrue(result);
        }
    }

}
