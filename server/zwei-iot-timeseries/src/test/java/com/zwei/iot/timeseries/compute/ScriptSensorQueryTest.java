package com.zwei.iot.timeseries.compute;

import com.zwei.iot.device.domain.Device;
import com.zwei.iot.device.service.IDeviceService;
import com.zwei.iot.device.service.IDeviceHazardRelationService;
import com.zwei.iot.device.service.IDeviceSensorService;
import com.zwei.iot.timeseries.domain.SensorSnapshot;
import com.zwei.iot.timeseries.util.SensorDataQueryUtil;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.MockedStatic;

import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.mockStatic;
import static org.mockito.Mockito.when;

@DisplayName("ScriptSensorQuery (SensorDataQueryUtil 实例外壳, deviceCode 入口)")
class ScriptSensorQueryTest {

    private static final String DEVICE_CODE = "DEV-001";
    private static final long DEVICE_ID = 1L;
    private static final String SENSOR_CODE = "WY_1";

    private final IDeviceService deviceService = mock(IDeviceService.class);
    private final ScriptSensorQuery query = new ScriptSensorQuery(
            deviceService,
            mock(IDeviceHazardRelationService.class),
            mock(IDeviceSensorService.class));

    @Test
    @DisplayName("query(deviceCode, ...) 解析 deviceId 后委托 SensorDataQueryUtil.query")
    void queryDelegates() {
        Device dev = deviceWithId(DEVICE_ID);
        when(deviceService.selectDeviceByCode(DEVICE_CODE)).thenReturn(dev);
        SensorSnapshot snap = new SensorSnapshot(1700000000000L, Map.of("rain", 25.5));

        try (MockedStatic<SensorDataQueryUtil> mocked = mockStatic(SensorDataQueryUtil.class)) {
            mocked.when(() -> SensorDataQueryUtil.query(DEVICE_ID, SENSOR_CODE, 1700000000000L, "rain"))
                  .thenReturn(snap);

            SensorSnapshot out = query.query(DEVICE_CODE, SENSOR_CODE, 1700000000000L, "rain");

            assertThat(out).isSameAs(snap);
            mocked.verify(() -> SensorDataQueryUtil.query(DEVICE_ID, SENSOR_CODE, 1700000000000L, "rain"));
        }
    }

    @Test
    @DisplayName("deviceCode 未找到: 返回 null, 不调 SensorDataQueryUtil (避免无谓查询)")
    void queryDeviceCodeNotFound() {
        when(deviceService.selectDeviceByCode("UNKNOWN")).thenReturn(null);

        try (MockedStatic<SensorDataQueryUtil> mocked = mockStatic(SensorDataQueryUtil.class)) {
            assertThat(query.query("UNKNOWN", SENSOR_CODE, 0L, "rain")).isNull();
            mocked.verifyNoInteractions();
        }
    }

    @Test
    @DisplayName("query 返回 null (无数据): 透传 null, 不抛")
    void queryNullPropagated() {
        when(deviceService.selectDeviceByCode(DEVICE_CODE)).thenReturn(deviceWithId(DEVICE_ID));
        try (MockedStatic<SensorDataQueryUtil> mocked = mockStatic(SensorDataQueryUtil.class)) {
            mocked.when(() -> SensorDataQueryUtil.query(DEVICE_ID, SENSOR_CODE, 0L, "rain"))
                  .thenReturn(null);

            assertThat(query.query(DEVICE_CODE, SENSOR_CODE, 0L, "rain")).isNull();
        }
    }

    @Test
    @DisplayName("query 抛异常: wrapper 吞噬返回 null (主链路保护)")
    void queryExceptionSwallowed() {
        when(deviceService.selectDeviceByCode(DEVICE_CODE)).thenReturn(deviceWithId(DEVICE_ID));
        try (MockedStatic<SensorDataQueryUtil> mocked = mockStatic(SensorDataQueryUtil.class)) {
            mocked.when(() -> SensorDataQueryUtil.query(DEVICE_ID, SENSOR_CODE, 0L, "rain"))
                  .thenThrow(new RuntimeException("IoTDB down"));

            assertThat(query.query(DEVICE_CODE, SENSOR_CODE, 0L, "rain")).isNull();
        }
    }

    private static Device deviceWithId(long id) {
        Device dev = new Device();
        dev.setId(id);
        return dev;
    }
}
