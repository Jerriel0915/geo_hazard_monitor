package com.zwei.iot.timeseries.service;

import com.zwei.common.exception.ServiceException;
import com.zwei.iot.device.domain.DeviceSensor;
import com.zwei.iot.device.domain.SensorAttribute;
import com.zwei.iot.device.service.IDeviceSensorService;
import com.zwei.iot.timeseries.domain.*;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.Map;

import static com.zwei.iot.timeseries.domain.TimeWindowSpec.WindowGranularity.HOUR;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
@DisplayName("MonitorDataAggregationService")
class MonitorDataAggregationServiceTest {

    @Mock private IotdbTimeSeriesService iotdbService;
    @Mock private IDeviceSensorService deviceSensorService;

    private MonitorDataAggregationService service;

    @BeforeEach
    void setUp() {
        service = new MonitorDataAggregationService(iotdbService, deviceSensorService);
    }

    private DeviceSensor fakeSensor(List<SensorAttribute> attrs) {
        DeviceSensor sensor = new DeviceSensor();
        sensor.setId(1L);
        sensor.setDeviceId(1L);
        sensor.setSensorCode("rain_01");
        sensor.setSensorName("雨量计");
        sensor.setAttrList(attrs);
        return sensor;
    }

    private SensorAttribute attr(String code, String name, String unit) {
        SensorAttribute a = new SensorAttribute();
        a.setAttrCode(code);
        a.setAttrName(name);
        a.setUnit(unit);
        return a;
    }

    @Test
    @DisplayName("aggregateAllAttrs — 串行遍历 sensor 下所有 attrCode,逐个调 iotdbService")
    void aggregateAllAttrs_serialPerAttr() {
        when(deviceSensorService.selectSensorByDeviceIdAndCode(1L, "rain_01"))
                .thenReturn(fakeSensor(List.of(
                        attr("rainfall", "雨量", "mm"),
                        attr("battery", "电池电压", "V")
                )));
        when(iotdbService.queryAggregate(eq(1L), eq("rain_01"), any(), any(), any(), any(), any()))
                .thenReturn(List.of(new AggregationResultVO(
                        1L, "rain_01", "rainfall", "雨量", "mm", 0L, Map.of("AVG", 12.5))));

        SensorAggregationVO vo = service.aggregateAllAttrs(
                1L, "rain_01",
                new TimeWindowSpec(0L, 3600_000L, HOUR),
                List.of(new ExpressionSpec.FunctionCall(AggregationFunction.AVG)),
                null, null);

        assertThat(vo.sensorCode()).isEqualTo("rain_01");
        assertThat(vo.results()).hasSize(2);
    }

    @Test
    @DisplayName("aggregateAllAttrs — sensor 不存在抛 ServiceException")
    void aggregateAllAttrs_sensorNotFound() {
        when(deviceSensorService.selectSensorByDeviceIdAndCode(1L, "missing")).thenReturn(null);

        assertThatThrownBy(() -> service.aggregateAllAttrs(
                1L, "missing",
                new TimeWindowSpec(0L, 3600_000L, HOUR),
                List.of(new ExpressionSpec.FunctionCall(AggregationFunction.AVG)),
                null, null))
                .isInstanceOf(ServiceException.class)
                .hasMessageContaining("传感器不存在");
    }

    @Test
    @DisplayName("aggregateAllAttrs — attrList 为空抛 ServiceException")
    void aggregateAllAttrs_emptyAttrs() {
        when(deviceSensorService.selectSensorByDeviceIdAndCode(1L, "rain_01"))
                .thenReturn(fakeSensor(List.of()));

        assertThatThrownBy(() -> service.aggregateAllAttrs(
                1L, "rain_01",
                new TimeWindowSpec(0L, 3600_000L, HOUR),
                List.of(new ExpressionSpec.FunctionCall(AggregationFunction.AVG)),
                null, null))
                .isInstanceOf(ServiceException.class)
                .hasMessageContaining("无监测指标");
    }

    @Test
    @DisplayName("delta — 等价于传 LAST_VALUE - FIRST_VALUE")
    void delta_invokesAggregateWithDeltaExpr() {
        when(deviceSensorService.selectSensorByDeviceIdAndCode(1L, "rain_01"))
                .thenReturn(fakeSensor(List.of(attr("rainfall", "雨量", "mm"))));
        when(iotdbService.queryAggregate(eq(1L), eq("rain_01"), eq("rainfall"), any(), any(), any(), any()))
                .thenReturn(List.of(new AggregationResultVO(
                        1L, "rain_01", "rainfall", "雨量", "mm", 0L, Map.of("DELTA", 0.7))));

        SensorAggregationVO vo = service.delta(1L, "rain_01", new TimeWindowSpec(0L, 3600_000L, HOUR));

        assertThat(vo.results().get(0).metrics()).containsEntry("DELTA", 0.7);
    }
}
