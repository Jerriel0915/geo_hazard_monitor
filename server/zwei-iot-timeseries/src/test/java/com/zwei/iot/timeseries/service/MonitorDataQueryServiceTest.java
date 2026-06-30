package com.zwei.iot.timeseries.service;

import com.zwei.iot.device.domain.DeviceSensor;
import com.zwei.iot.device.domain.SensorAttribute;
import com.zwei.iot.device.service.IDeviceSensorService;
import com.zwei.iot.hazardpoint.domain.HazardPoint;
import com.zwei.iot.hazardpoint.domain.dto.BoundDeviceVO;
import com.zwei.iot.hazardpoint.mapper.DeviceHazardPointMapper;
import com.zwei.iot.hazardpoint.mapper.HazardPointMapper;
import com.zwei.common.exception.ServiceException;
import com.zwei.iot.timeseries.config.MonitorQueryProperties;
import com.zwei.iot.timeseries.domain.ChartDataVO;
import com.zwei.iot.timeseries.domain.IotdbQueryRow;
import com.zwei.iot.timeseries.domain.ValueType;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;

import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
@DisplayName("MonitorDataQueryService 图表自动降采样")
class MonitorDataQueryServiceTest {

    @Mock private DeviceHazardPointMapper deviceHazardPointMapper;
    @Mock private HazardPointMapper hazardPointMapper;
    @Mock private IDeviceSensorService deviceSensorService;
    @Mock private IotdbTimeSeriesService iotdbService;

    private MonitorDataQueryService service;
    private final MonitorQueryProperties queryProps = new MonitorQueryProperties();

    @BeforeEach
    void setUp() {
        service = new MonitorDataQueryService(
                deviceHazardPointMapper, hazardPointMapper,
                deviceSensorService, iotdbService, queryProps);

        HazardPoint hp = new HazardPoint();
        hp.setId(1L);
        hp.setName("测试隐患点");
        when(hazardPointMapper.selectHazardPointById(1L)).thenReturn(hp);

        BoundDeviceVO bound = new BoundDeviceVO();
        bound.setDeviceId(1L);
        bound.setDeviceName("测试设备");
        when(deviceHazardPointMapper.selectBoundDevicesByHazardPointId(1L))
                .thenReturn(List.of(bound));

        DeviceSensor sensor = new DeviceSensor();
        sensor.setId(1L);
        sensor.setDeviceId(1L);
        sensor.setSensorCode("rain_01");
        sensor.setSensorName("雨量计");
        SensorAttribute attr = new SensorAttribute();
        attr.setAttrCode("rainfall");
        attr.setAttrName("雨量");
        attr.setUnit("mm");
        sensor.setAttrList(List.of(attr));
        when(deviceSensorService.selectSensorListByDeviceId(1L))
                .thenReturn(List.of(sensor));
        when(deviceSensorService.selectSensorListByDeviceIds(anyList()))
                .thenReturn(List.of(sensor));

        IotdbQueryRow sampleRow = IotdbQueryRow.builder()
                .time(1700000000000L).value(12.5).quality(0).build();
        when(iotdbService.queryRangeWithLimit(anyLong(), anyString(), anyString(),
                anyLong(), anyLong(), anyInt()))
                .thenReturn(List.of(sampleRow));
        when(iotdbService.queryRangeDownsampled(anyLong(), anyString(), anyString(),
                anyLong(), anyLong(), anyString()))
                .thenReturn(List.of(sampleRow));
        when(iotdbService.queryRange(anyLong(), anyString(), anyString(),
                anyLong(), anyLong(), any(ValueType.class)))
                .thenReturn(List.of(sampleRow));
    }

    @Test
    @DisplayName("小范围(1分钟,默认valueType) → raw+LIMIT 路径,sampled=false")
    void chart_smallRange_rawWithLimit() {
        List<ChartDataVO> result = service.chart(
                1L, null, null, "rainfall",
                null, "2024-01-01 00:00:00", "2024-01-01 00:01:00", null);

        assertThat(result).hasSize(1);
        assertThat(result.get(0).sampled()).isFalse();
        assertThat(result.get(0).downsampleInterval()).isNull();
        verify(iotdbService).queryRangeWithLimit(
                eq(1L), eq("rain_01"), eq("rainfall"),
                anyLong(), anyLong(), eq(4000));
        verify(iotdbService, never()).queryRangeDownsampled(
                any(), anyString(), anyString(), any(), any(), anyString());
    }

    @Test
    @DisplayName("大范围 + granularity=auto → 自动降采样,sampled=true")
    void chart_largeRange_autoDownsample() {
        List<ChartDataVO> result = service.chart(
                1L, null, null, "rainfall",
                null, "2023-01-01 00:00:00", "2024-01-01 00:00:00", "auto");

        assertThat(result).hasSize(1);
        assertThat(result.get(0).sampled()).isTrue();
        assertThat(result.get(0).downsampleInterval()).isNotNull();
        verify(iotdbService).queryRangeDownsampled(
                eq(1L), eq("rain_01"), eq("rainfall"),
                anyLong(), anyLong(), anyString());
        verify(iotdbService, never()).queryRangeWithLimit(
                any(), anyString(), anyString(), any(), any(), anyInt());
    }

    @Test
    @DisplayName("大范围 + granularity=null → raw 数据，不自动降采样")
    void chart_largeRange_nullGranularity_usesRaw() {
        List<ChartDataVO> result = service.chart(
                1L, null, null, "rainfall",
                null, "2023-01-01 00:00:00", "2024-01-01 00:00:00", null);

        assertThat(result).hasSize(1);
        assertThat(result.get(0).sampled()).isFalse();
        assertThat(result.get(0).downsampleInterval()).isNull();
        verify(iotdbService, never()).queryRangeDownsampled(
                any(), anyString(), anyString(), any(), any(), anyString());
    }

    @Test
    @DisplayName("显式valueType=hour → 尊重用户选择,走聚合路径,不做自动决策")
    void chart_explicitAggregated_respectsUserChoice() {
        List<ChartDataVO> result = service.chart(
                1L, null, null, "rainfall",
                "hour", "2023-01-01 00:00:00", "2024-01-01 00:00:00", null);

        assertThat(result).hasSize(1);
        verify(iotdbService).queryRange(
                eq(1L), eq("rain_01"), eq("rainfall"),
                anyLong(), anyLong(), eq(ValueType.HOUR));
        verify(iotdbService, never()).queryRangeDownsampled(
                any(), anyString(), anyString(), any(), any(), anyString());
        verify(iotdbService, never()).queryRangeWithLimit(
                any(), anyString(), anyString(), any(), any(), anyInt());
    }

    @Test
    @DisplayName("granularity=5m → 强制指定粒度降采样")
    void chart_userDownsample_forceGranularity() {
        List<ChartDataVO> result = service.chart(
                1L, null, null, "rainfall",
                null, "2024-01-01 00:00:00", "2024-01-01 00:01:00", "5m");

        assertThat(result).hasSize(1);
        assertThat(result.get(0).sampled()).isTrue();
        assertThat(result.get(0).downsampleInterval()).isEqualTo("5m");
        verify(iotdbService).queryRangeDownsampled(
                eq(1L), eq("rain_01"), eq("rainfall"),
                anyLong(), anyLong(), eq("5m"));
    }

    @Test
    @DisplayName("granularity=raw → 禁用降采样,强制 raw 路径")
    void chart_userDownsample_raw_disablesDownsample() {
        List<ChartDataVO> result = service.chart(
                1L, null, null, "rainfall",
                null, "2023-01-01 00:00:00", "2024-01-01 00:00:00", "raw");

        assertThat(result).hasSize(1);
        assertThat(result.get(0).sampled()).isFalse();
        verify(iotdbService, atLeastOnce()).queryRangeWithLimit(
                eq(1L), eq("rain_01"), eq("rainfall"),
                anyLong(), anyLong(), eq(4000));
        verify(iotdbService, never()).queryRangeDownsampled(
                any(), anyString(), anyString(), any(), any(), anyString());
    }

    // ==================== P1: page() 多测点分页 ====================

    @Test
    @DisplayName("page 单测点 cursor=null → 走 queryRangePaged offset 路径")
    void page_singleDevice_offsetPath_usesPaged() {
        when(iotdbService.queryRangePaged(any(), anyString(), anyString(),
                any(), any(), anyInt(), anyInt()))
                .thenReturn(List.of(IotdbQueryRow.builder().time(1700000000000L).value(12.5).quality(0).build()));
        when(iotdbService.countRange(any(), anyString(), anyString(), any(), any()))
                .thenReturn(100L);

        Map<String, Object> result = service.page(
                1L, null, null, "rainfall", null,
                null, null, 1, 10, null);

        assertThat(result).containsKeys("total", "rows", "pageNum", "pageSize");
        assertThat(result.get("total")).isEqualTo(100L);
        verify(iotdbService).queryRangePaged(any(), anyString(), anyString(),
                any(), any(), eq(10), eq(0));
        verify(iotdbService, never()).queryRangeCursor(any(), anyString(), anyString(),
                any(), any(), any(), anyInt());
    }

    @Test
    @DisplayName("page 单测点 cursor 不为 null → 走 queryRangeCursor 游标路径,返回新 cursor")
    void page_singleDevice_cursorPath_usesCursor() {
        when(iotdbService.queryRangeCursor(any(), anyString(), anyString(),
                any(), any(), any(), anyInt()))
                .thenReturn(List.of(
                        IotdbQueryRow.builder().time(1700000003000L).value(15.0).quality(0).build(),
                        IotdbQueryRow.builder().time(1700000001000L).value(12.5).quality(0).build()
                ));
        when(iotdbService.countRange(any(), anyString(), anyString(), any(), any()))
                .thenReturn(100L);

        Map<String, Object> result = service.page(
                1L, null, null, "rainfall", null,
                null, null, 1, 10, 1700000005000L);

        assertThat(result).containsKeys("total", "rows", "cursor");
        assertThat(result.get("cursor")).isEqualTo(1700000001000L);
        assertThat((List<?>) result.get("rows")).hasSize(2);
        verify(iotdbService).queryRangeCursor(any(), anyString(), anyString(),
                any(), any(), eq(1700000005000L), eq(10));
        verify(iotdbService, never()).queryRangePaged(any(), anyString(), anyString(),
                any(), any(), anyInt(), anyInt());
    }

    @Test
    @DisplayName("page 多测点 cursor 不为 null → 每个测点调 queryRangeCursor,合并排序后返回 cursor")
    void page_multiDevice_cursorPath_mergesAndReturnsCursor() {
        // 模拟两个测点
        BoundDeviceVO bound2 = new BoundDeviceVO();
        bound2.setDeviceId(2L);
        bound2.setDeviceName("设备2");
        when(deviceHazardPointMapper.selectBoundDevicesByHazardPointId(1L))
                .thenReturn(List.of(
                        createBoundDevice(1L, "设备1"),
                        createBoundDevice(2L, "设备2")
                ));
        DeviceSensor sensor1 = createSensor(1L, 1L, "rain_01", "雨量计", "rainfall", "雨量", "mm");
        DeviceSensor sensor2 = createSensor(2L, 2L, "temp_01", "温度计", "temperature", "温度", "℃");
        when(deviceSensorService.selectSensorListByDeviceId(1L)).thenReturn(List.of(sensor1));
        when(deviceSensorService.selectSensorListByDeviceId(2L)).thenReturn(List.of(sensor2));

        when(iotdbService.queryRangeCursor(eq(1L), eq("rain_01"), eq("rainfall"),
                any(), any(), eq(1700000005000L), eq(10)))
                .thenReturn(List.of(
                        IotdbQueryRow.builder().time(1700000004000L).value(15.0).quality(0).build()
                ));
        when(iotdbService.queryRangeCursor(eq(2L), eq("temp_01"), eq("temperature"),
                any(), any(), eq(1700000005000L), eq(10)))
                .thenReturn(List.of(
                        IotdbQueryRow.builder().time(1700000003000L).value(25.0).quality(0).build()
                ));
        when(iotdbService.countRange(any(), anyString(), anyString(), any(), any()))
                .thenReturn(100L);

        Map<String, Object> result = service.page(
                1L, null, null, null, null,
                null, null, 1, 10, 1700000005000L);

        assertThat(result).containsKeys("cursor");
        // 游标应为两行中最早的时间 (降序排列后最后一行)
        assertThat(result.get("cursor")).isEqualTo(1700000003000L);
        List<?> rows = (List<?>) result.get("rows");
        assertThat(rows).hasSize(2);
        verify(iotdbService, atLeastOnce()).queryRangeCursor(any(), anyString(), anyString(),
                any(), any(), any(), anyInt());
        verify(iotdbService, never()).queryRangePaged(any(), anyString(), anyString(),
                any(), any(), anyInt(), anyInt());
    }

    @Test
    @DisplayName("page 多测点 offset 超限 → 抛 ServiceException")
    void page_multiDevice_offsetExceedsLimit_throws() {
        // 模拟两个测点
        BoundDeviceVO bound2 = new BoundDeviceVO();
        bound2.setDeviceId(2L);
        bound2.setDeviceName("设备2");
        when(deviceHazardPointMapper.selectBoundDevicesByHazardPointId(1L))
                .thenReturn(List.of(
                        createBoundDevice(1L, "设备1"),
                        createBoundDevice(2L, "设备2")
                ));
        DeviceSensor sensor1 = createSensor(1L, 1L, "rain_01", "雨量计", "rainfall", "雨量", "mm");
        DeviceSensor sensor2 = createSensor(2L, 2L, "temp_01", "温度计", "temperature", "温度", "℃");
        when(deviceSensorService.selectSensorListByDeviceId(1L)).thenReturn(List.of(sensor1));
        when(deviceSensorService.selectSensorListByDeviceId(2L)).thenReturn(List.of(sensor2));

        // pageNum=1000 * pageSize=10 = 10000 > maxMergeRows=5000
        assertThatThrownBy(() -> service.page(
                1L, null, null, null, null,
                null, null, 1000, 10, null))
                .isInstanceOf(ServiceException.class)
                .hasMessageContaining("查询结果过多");
    }

    @Test
    @DisplayName("page 多测点 offset 未超限 → 走旧路径正常返回")
    void page_multiDevice_offsetNotExceeded_works() {
        BoundDeviceVO bound2 = new BoundDeviceVO();
        bound2.setDeviceId(2L);
        bound2.setDeviceName("设备2");
        when(deviceHazardPointMapper.selectBoundDevicesByHazardPointId(1L))
                .thenReturn(List.of(
                        createBoundDevice(1L, "设备1"),
                        createBoundDevice(2L, "设备2")
                ));
        DeviceSensor sensor1 = createSensor(1L, 1L, "rain_01", "雨量计", "rainfall", "雨量", "mm");
        DeviceSensor sensor2 = createSensor(2L, 2L, "temp_01", "温度计", "temperature", "温度", "℃");
        when(deviceSensorService.selectSensorListByDeviceId(1L)).thenReturn(List.of(sensor1));
        when(deviceSensorService.selectSensorListByDeviceId(2L)).thenReturn(List.of(sensor2));

        when(iotdbService.queryRangePaged(any(), anyString(), anyString(),
                any(), any(), anyInt(), anyInt()))
                .thenReturn(List.of(
                        IotdbQueryRow.builder().time(1700000002000L).value(15.0).quality(0).build()
                ));
        when(iotdbService.countRange(any(), anyString(), anyString(), any(), any()))
                .thenReturn(50L);

        Map<String, Object> result = service.page(
                1L, null, null, null, null,
                null, null, 1, 10, null);

        assertThat(result.get("total")).isEqualTo(100L);
        assertThat((List<?>) result.get("rows")).hasSize(2);
    }

    // ── helper methods ──

    private BoundDeviceVO createBoundDevice(Long id, String name) {
        BoundDeviceVO b = new BoundDeviceVO();
        b.setDeviceId(id);
        b.setDeviceName(name);
        return b;
    }

    private DeviceSensor createSensor(Long id, Long deviceId, String sensorCode, String sensorName,
                                       String attrCode, String attrName, String unit) {
        DeviceSensor sensor = new DeviceSensor();
        sensor.setId(id);
        sensor.setDeviceId(deviceId);
        sensor.setSensorCode(sensorCode);
        sensor.setSensorName(sensorName);
        SensorAttribute attr = new SensorAttribute();
        attr.setAttrCode(attrCode);
        attr.setAttrName(attrName);
        attr.setUnit(unit);
        sensor.setAttrList(List.of(attr));
        return sensor;
    }
}
