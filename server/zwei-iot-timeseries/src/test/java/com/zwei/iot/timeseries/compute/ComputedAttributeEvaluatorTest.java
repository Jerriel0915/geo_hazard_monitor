package com.zwei.iot.timeseries.compute;

import com.zwei.common.domain.ParsedMessage;
import com.zwei.common.domain.PropertyValue;
import com.zwei.iot.device.domain.SensorAttribute;
import com.zwei.iot.device.domain.SensorMetadata;
import com.zwei.iot.device.service.IDeviceSensorQueryService;
import com.zwei.iot.parser.engine.GroovyScriptEngine;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@DisplayName("ComputedAttributeEvaluator")
class ComputedAttributeEvaluatorTest {

    private IDeviceSensorQueryService sensorQuery;
    private ComputedAttributeRegistry registry;
    private ComputedScriptAssembler assembler;
    private LastMessageStore lastMessageStore;
    private GroovyScriptEngine scriptEngine;
    private ComputedAttributeEvaluator evaluator;

    @BeforeEach
    void setUp() {
        sensorQuery = mock(IDeviceSensorQueryService.class);
        registry = mock(ComputedAttributeRegistry.class);
        assembler = new ComputedScriptAssembler();
        lastMessageStore = mock(LastMessageStore.class);
        scriptEngine = mock(GroovyScriptEngine.class);

        evaluator = new ComputedAttributeEvaluator(
                sensorQuery, registry, assembler, lastMessageStore, scriptEngine);
    }

    private ParsedMessage msg(double value) {
        return new ParsedMessage(
                "D1", "S1", "sys", 1700000000000L, 1700000000000L, "hash",
                List.of(new PropertyValue("displacement", "位移", "mm", value, 0)));
    }

    private void stubSensor() {
        when(sensorQuery.requireSensorMetadata(1L, "S1"))
                .thenReturn(SensorMetadata.builder()
                        .deviceId(1L).sensorId(10L).monitorTypeId(100L)
                        .attributes(List.of()).build());
    }

    @Test
    @DisplayName("fast path: 无计算属性返回空 list, 不调 scriptEngine")
    void noComputedAttrs() {
        stubSensor();
        when(registry.getByMonitorTypeId(100L)).thenReturn(List.of());

        List<PropertyValue> out = evaluator.evaluate(1L, "S1", msg(12.0));

        assertThat(out).isEmpty();
        verifyNoInteractions(scriptEngine);
        verify(lastMessageStore, never()).put(any(), any(), any());
    }

    @Test
    @DisplayName("monitorTypeId 缺失: 返回空 list, 不调 registry")
    void noMonitorTypeId() {
        when(sensorQuery.requireSensorMetadata(1L, "S1"))
                .thenReturn(SensorMetadata.builder()
                        .deviceId(1L).sensorId(10L).monitorTypeId(null)
                        .attributes(List.of()).build());

        assertThat(evaluator.evaluate(1L, "S1", msg(12.0))).isEmpty();
        verifyNoInteractions(registry);
    }

    @Test
    @DisplayName("首次上报(prevData=null): 脚本可执行, 结果合并 properties")
    void firstReport() {
        stubSensor();
        when(registry.getByMonitorTypeId(100L)).thenReturn(List.of(
                new ComputedAttribute(1L, 100L, "velocity", "速率", "mm/s",
                        "return curData.properties.displacement * 2", 1)));
        when(lastMessageStore.get(1L, "S1")).thenReturn(null);
        when(scriptEngine.executeComputed(anyString(), any(), isNull()))
                .thenReturn(Map.of("velocity", 24.0));

        List<PropertyValue> out = evaluator.evaluate(1L, "S1", msg(12.0));

        assertThat(out).hasSize(1);
        assertThat(out.get(0).identifier()).isEqualTo("velocity");
        assertThat(out.get(0).value()).isEqualTo(24.0);
        verify(lastMessageStore).put(eq(1L), eq("S1"), argThat(s ->
                s.properties().containsKey("velocity") &&
                s.properties().containsKey("displacement")));
    }

    @Test
    @DisplayName("全部脚本失败(返回空 Map): 不写回 lastMessageStore, 返回空 list")
    void allScriptsFail() {
        stubSensor();
        when(registry.getByMonitorTypeId(100L)).thenReturn(List.of(
                new ComputedAttribute(1L, 100L, "velocity", "速率", "mm/s",
                        "return 1/0", 1)));
        when(lastMessageStore.get(1L, "S1")).thenReturn(null);
        when(scriptEngine.executeComputed(anyString(), any(), any()))
                .thenReturn(Map.of());

        List<PropertyValue> out = evaluator.evaluate(1L, "S1", msg(12.0));

        assertThat(out).isEmpty();
        // 即使全部失败, prevData 也按规格修复后逻辑应更新(否则下次仍是旧值)
        // — 这里规格修复点是"只要进入求值阶段就更新", 故仍调用 put
        verify(lastMessageStore).put(eq(1L), eq("S1"), any());
    }

    @Test
    @DisplayName("sensorQuery 抛异常: 返回空 list, 不向上抛")
    void sensorQueryThrows() {
        when(sensorQuery.requireSensorMetadata(1L, "S1"))
                .thenThrow(new RuntimeException("db down"));

        assertThat(evaluator.evaluate(1L, "S1", msg(12.0))).isEmpty();
    }

    @Test
    @DisplayName("非数值结果(字符串/布尔)也被保留")
    void nonNumericResult() {
        stubSensor();
        when(registry.getByMonitorTypeId(100L)).thenReturn(List.of(
                new ComputedAttribute(1L, 100L, "good", "好", "", "return 1.0", 1),
                new ComputedAttribute(2L, 100L, "bad", "坏", "",
                        "return 'not a number'", 2)));
        when(lastMessageStore.get(1L, "S1")).thenReturn(null);
        when(scriptEngine.executeComputed(anyString(), any(), isNull()))
                .thenReturn(Map.of("good", 1.0, "bad", "not a number"));

        List<PropertyValue> out = evaluator.evaluate(1L, "S1", msg(12.0));

        assertThat(out).hasSize(2);
        assertThat(out.get(0).identifier()).isEqualTo("good");
        assertThat(out.get(0).value()).isEqualTo(1.0);
        assertThat(out.get(1).identifier()).isEqualTo("bad");
        assertThat(out.get(1).value()).isEqualTo("not a number");
    }

    @Test
    @DisplayName("部分属性结果缺失(attrCode 不在 results map 中): 跳过该属性")
    void missingResultKey() {
        stubSensor();
        when(registry.getByMonitorTypeId(100L)).thenReturn(List.of(
                new ComputedAttribute(1L, 100L, "a", "A", "", "return 1", 1),
                new ComputedAttribute(2L, 100L, "b", "B", "", "return 2", 2)));
        when(lastMessageStore.get(1L, "S1")).thenReturn(null);
        when(scriptEngine.executeComputed(anyString(), any(), isNull()))
                .thenReturn(Map.of("a", 1.0));  // b 缺失(脚本内异常)

        List<PropertyValue> out = evaluator.evaluate(1L, "S1", msg(12.0));

        assertThat(out).hasSize(1);
        assertThat(out.get(0).identifier()).isEqualTo("a");
    }

    @Test
    @DisplayName("prevData 命中: 透传给 scriptEngine")
    void prevDataPassed() {
        stubSensor();
        ParsedMessageSnapshot prevSnap = new ParsedMessageSnapshot(
                "D1", "S1", 1700000000000L, Map.of("displacement", 10.0));
        when(registry.getByMonitorTypeId(100L)).thenReturn(List.of(
                new ComputedAttribute(1L, 100L, "delta", "差分", "",
                        "return curData.properties.displacement - prevData.properties.displacement", 1)));
        when(lastMessageStore.get(1L, "S1")).thenReturn(prevSnap);
        when(scriptEngine.executeComputed(anyString(), any(), any()))
                .thenReturn(Map.of("delta", 2.0));

        List<PropertyValue> out = evaluator.evaluate(1L, "S1", msg(12.0));

        assertThat(out).hasSize(1);
        assertThat(out.get(0).value()).isEqualTo(2.0);
    }
}
