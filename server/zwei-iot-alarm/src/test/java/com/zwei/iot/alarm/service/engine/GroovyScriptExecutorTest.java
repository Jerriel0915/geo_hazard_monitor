package com.zwei.iot.alarm.service.engine;

import com.zwei.iot.alarm.config.AlarmProperties;
import com.zwei.iot.timeseries.compute.ScriptCacheOps;
import com.zwei.iot.timeseries.compute.ScriptSensorQuery;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;
import static org.mockito.Mockito.eq;

class GroovyScriptExecutorTest {

    private GroovyScriptExecutor executor;

    @BeforeEach
    void setUp() {
        AlarmProperties props = new AlarmProperties();
        props.setGroovyTimeoutSeconds(10);
        executor = new GroovyScriptExecutor(props);
    }

    @Test
    void execute_legacyStillWorks() {
        Integer result = executor.execute("return 3", Map.of());
        assertEquals(3, result);
    }

    @Test
    void executeWithTools_nullTools_equivalentToLegacy() {
        Integer result = executor.executeWithTools("return 2", Map.of(), null);
        assertEquals(2, result);
    }

    @Test
    void executeWithTools_cacheAccessible() {
        ScriptCacheOps cache = mock(ScriptCacheOps.class);
        when(cache.getString("rainfall_key", null)).thenReturn("45.0");

        Map<String, Object> tools = Map.of("cache", cache);
        Integer result = executor.executeWithTools(
                "return Double.parseDouble(cache.getString('rainfall_key', null)) > 10 ? 3 : 0",
                Map.of(), tools);

        assertEquals(3, result);
        verify(cache).getString("rainfall_key", null);
    }

    @Test
    void executeWithTools_sensorAccessible() {
        ScriptSensorQuery sensor = mock(ScriptSensorQuery.class);
        when(sensor.query(anyString(), anyString(), anyLong(), anyString())).thenReturn(null);

        Map<String, Object> tools = Map.of("sensor", sensor);
        Integer result = executor.executeWithTools(
                "def snapshot = sensor.query('DEV001', 'RAIN-001', System.currentTimeMillis(), 'rainfall')\n" +
                "return snapshot == null ? 0 : 1",
                Map.of(), tools);

        assertEquals(0, result);
        verify(sensor).query(eq("DEV001"), eq("RAIN-001"), anyLong(), eq("rainfall"));
    }

    @Test
    void executeWithTools_scriptThrows_returnsNull() {
        ScriptCacheOps cache = mock(ScriptCacheOps.class);
        when(cache.getString("missing", null)).thenThrow(new RuntimeException("Redis down"));

        Map<String, Object> tools = Map.of("cache", cache);
        Integer result = executor.executeWithTools(
                "return cache.getString('missing', null) == null ? 0 : 1",
                Map.of(), tools);

        assertNull(result);
    }
}
