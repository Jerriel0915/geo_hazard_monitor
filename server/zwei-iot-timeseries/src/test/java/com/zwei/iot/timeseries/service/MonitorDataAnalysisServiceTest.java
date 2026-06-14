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

import static com.zwei.iot.timeseries.domain.TimeWindowSpec.WindowGranularity.RAW;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
@DisplayName("MonitorDataAnalysisService")
class MonitorDataAnalysisServiceTest {

    @Mock private IotdbTimeSeriesService iotdbService;
    @Mock private IDeviceSensorService deviceSensorService;

    private MonitorDataAnalysisService service;

    @BeforeEach
    void setUp() {
        service = new MonitorDataAnalysisService(iotdbService, deviceSensorService);
    }

    private DeviceSensor sensor(String code, List<SensorAttribute> attrs) {
        DeviceSensor s = new DeviceSensor();
        s.setId(1L);
        s.setDeviceId(1L);
        s.setSensorCode(code);
        s.setSensorName("测试传感器");
        s.setAttrList(attrs);
        return s;
    }

    private SensorAttribute attr(String code) {
        SensorAttribute a = new SensorAttribute();
        a.setAttrCode(code);
        a.setAttrName(code);
        return a;
    }

    @Test
    @DisplayName("completeness — 校验 sensor 存在,代理给 iotdb")
    void completeness_basic() {
        when(deviceSensorService.selectSensorByDeviceIdAndCode(1L, "rain_01"))
                .thenReturn(sensor("rain_01", List.of(attr("rainfall"))));
        when(iotdbService.queryCompleteness(eq(1L), eq("rain_01"), eq("rainfall"), any(), eq(60_000L)))
                .thenReturn(new CompletenessReportVO(1L, "rain_01", "rainfall", 60L, 50L, 50.0 / 60.0, 10.0 / 60.0, 1700000000000L));

        CompletenessReportVO vo = service.completeness(
                1L, "rain_01", "rainfall",
                new TimeWindowSpec(0L, 3_600_000L, RAW), 60_000L);

        assertThat(vo.expectedPoints()).isEqualTo(60L);
        assertThat(vo.actualPoints()).isEqualTo(50L);
    }

    @Test
    @DisplayName("completeness — expectedIntervalMs 为 null 时直接传 null 给 iotdb")
    void completeness_nullInterval() {
        when(deviceSensorService.selectSensorByDeviceIdAndCode(1L, "rain_01"))
                .thenReturn(sensor("rain_01", List.of(attr("rainfall"))));
        when(iotdbService.queryCompleteness(eq(1L), eq("rain_01"), eq("rainfall"), any(), eq(null)))
                .thenReturn(new CompletenessReportVO(1L, "rain_01", "rainfall", 60L, 0L, 0.0, 1.0, null));

        CompletenessReportVO vo = service.completeness(
                1L, "rain_01", "rainfall",
                new TimeWindowSpec(0L, 3_600_000L, RAW), null);

        assertThat(vo.completenessRate()).isEqualTo(0.0);
    }

    @Test
    @DisplayName("trend — 直接代理 iotdbService.queryTrend")
    void trend_proxyToIotdb() {
        when(deviceSensorService.selectSensorByDeviceIdAndCode(1L, "rain_01"))
                .thenReturn(sensor("rain_01", List.of(attr("rainfall"))));
        TrendReportVO expected = new TrendReportVO(1L, "rain_01", "rainfall",
                0L, 3_600_000L, 1.0e-7, 0.36, 8.64, 0.0, 1.0, "rising");
        when(iotdbService.queryTrend(eq(1L), eq("rain_01"), eq("rainfall"), any())).thenReturn(expected);

        TrendReportVO vo = service.trend(1L, "rain_01", "rainfall",
                new TimeWindowSpec(0L, 3_600_000L, RAW));

        assertThat(vo).isEqualTo(expected);
    }

    @Test
    @DisplayName("completeness — sensor 不存在抛 ServiceException")
    void completeness_sensorNotFound() {
        when(deviceSensorService.selectSensorByDeviceIdAndCode(1L, "missing")).thenReturn(null);

        assertThatThrownBy(() -> service.completeness(
                1L, "missing", "rainfall",
                new TimeWindowSpec(0L, 3_600_000L, RAW), 60_000L))
                .isInstanceOf(ServiceException.class)
                .hasMessageContaining("传感器不存在");
    }
}
