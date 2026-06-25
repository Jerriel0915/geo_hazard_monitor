package com.zwei.iot.timeseries.compute;

import com.zwei.iot.timeseries.domain.SensorSnapshot;
import com.zwei.iot.timeseries.util.SensorDataQueryUtil;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.MockedStatic;

import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mockStatic;

@DisplayName("ScriptSensorQuery (SensorDataQueryUtil 实例外壳)")
class ScriptSensorQueryTest {

    @Test
    @DisplayName("query 委托 SensorDataQueryUtil.query")
    void queryDelegates() {
        SensorSnapshot snap = new SensorSnapshot(1700000000000L, Map.of("rain", 25.5));
        try (MockedStatic<SensorDataQueryUtil> mocked = mockStatic(SensorDataQueryUtil.class)) {
            mocked.when(() -> SensorDataQueryUtil.query(1L, "WY_1", 1700000000000L, "rain"))
                  .thenReturn(snap);

            SensorSnapshot out = new ScriptSensorQuery().query(1L, "WY_1", 1700000000000L, "rain");

            assertThat(out).isSameAs(snap);
            mocked.verify(() -> SensorDataQueryUtil.query(1L, "WY_1", 1700000000000L, "rain"));
        }
    }

    @Test
    @DisplayName("query 返回 null (无数据): 透传 null, 不抛")
    void queryNullPropagated() {
        try (MockedStatic<SensorDataQueryUtil> mocked = mockStatic(SensorDataQueryUtil.class)) {
            mocked.when(() -> SensorDataQueryUtil.query(1L, "WY_1", 0L, "rain"))
                  .thenReturn(null);

            assertThat(new ScriptSensorQuery().query(1L, "WY_1", 0L, "rain")).isNull();
        }
    }

    @Test
    @DisplayName("query 抛异常: wrapper 吞噬返回 null (主链路保护)")
    void queryExceptionSwallowed() {
        try (MockedStatic<SensorDataQueryUtil> mocked = mockStatic(SensorDataQueryUtil.class)) {
            mocked.when(() -> SensorDataQueryUtil.query(1L, "WY_1", 0L, "rain"))
                  .thenThrow(new RuntimeException("IoTDB down"));

            assertThat(new ScriptSensorQuery().query(1L, "WY_1", 0L, "rain")).isNull();
        }
    }
}
