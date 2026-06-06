package com.zwei.iot.timeseries.parser;

import com.zwei.common.exception.ServiceException;
import com.zwei.iot.device.domain.SensorAttribute;
import com.zwei.iot.device.domain.SensorMetadata;
import com.zwei.iot.timeseries.domain.StandardMeasurementPoint;
import com.zwei.iot.timeseries.support.MonitorTopic;
import org.junit.jupiter.api.Test;

import java.nio.charset.StandardCharsets;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

/**
 * 通用报文解析器单元测试。
 *
 * <p>新增通用报文解析测试，用于验证单值与对象多值场景的标准化结果。</p>
 */
class SysMonitorPayloadParserTest {
    private final SysMonitorPayloadParser parser = new SysMonitorPayloadParser();

    /**
     * 验证标准单值报文可被正确解析。
     */
    @Test
    void shouldParseStandardSingleValuePayload() {
        MonitorTopic topic = new MonitorTopic("sys", "DEV001", "1");
        SensorMetadata metadata = SensorMetadata.builder()
                .deviceId(101L)
                .sensorId(1001L)
                .attributes(List.of(
                        SensorAttribute.builder().id(1L).attrCode("value").attrName("裂缝值").unit("mm").build()
                ))
                .build();
        String payload = """
                {
                  "version": "1.0",
                  "deviceId": "101",
                  "sensorNo": "1",
                  "timestamp": 1716979200000,
                  "data": {
                    "time": 1716979200000,
                    "value": 12.5
                  }
                }
                """;

        List<StandardMeasurementPoint> points = parser.parse(topic, payload.getBytes(StandardCharsets.UTF_8), metadata);

        assertEquals(1, points.size());
        assertEquals("value", points.get(0).attrCode());
        assertEquals(12.5D, points.get(0).value());
        assertEquals(1716979200000L, points.get(0).dataTime());
    }

    /**
     * 验证对象多值报文可被正确拆分为多个指标点。
     */
    @Test
    void shouldParseStandardObjectValuePayload() {
        MonitorTopic topic = new MonitorTopic("sys", "DEV001", "1");
        SensorMetadata metadata = SensorMetadata.builder()
                .deviceId(101L)
                .sensorId(1001L)
                .attributes(List.of(
                        SensorAttribute.builder().id(1L).attrCode("gpsTotalX").attrName("X").unit("mm").build(),
                        SensorAttribute.builder().id(2L).attrCode("gpsTotalY").attrName("Y").unit("mm").build()
                ))
                .build();
        String payload = """
                {
                  "version": "1.0",
                  "deviceId": "101",
                  "sensorNo": "1",
                  "timestamp": 1716979200000,
                  "data": {
                    "time": 1716979200000,
                    "value": {
                      "gpsTotalX": 1.2,
                      "gpsTotalY": 2.3
                    }
                  }
                }
                """;

        List<StandardMeasurementPoint> points = parser.parse(topic, payload.getBytes(StandardCharsets.UTF_8), metadata);

        assertEquals(2, points.size());
        assertEquals("gpsTotalX", points.get(0).attrCode());
        assertEquals(1.2D, points.get(0).value());
        assertEquals("gpsTotalY", points.get(1).attrCode());
        assertEquals(2.3D, points.get(1).value());
    }

    /**
     * 验证 CSV 多值报文在传感器无属性定义时抛出明确错误。
     */
    @Test
    void shouldThrowWhenCsvPayloadWithoutAttributes() {
        MonitorTopic topic = new MonitorTopic("sys", "DEV001", "1");
        SensorMetadata metadata = SensorMetadata.builder()
                .deviceId(101L)
                .sensorId(1001L)
                .attributes(List.of())
                .build();
        String payload = """
                {
                  "version": "1.0",
                  "sensorNo": "1",
                  "timestamp": 1716979200000,
                  "data": {
                    "time": 1716979200000,
                    "value": "25.5, 50"
                  }
                }
                """;

        ServiceException ex = assertThrows(ServiceException.class, () ->
                parser.parse(topic, payload.getBytes(StandardCharsets.UTF_8), metadata));
        assertEquals("传感器未配置属性，无法解析 CSV 多值数据。请为传感器添加属性定义，或使用单值格式", ex.getMessage());
    }
}
