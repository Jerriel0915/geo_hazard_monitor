package com.zwei.iot.timeseries.integration;

import com.zwei.common.domain.ParsedMessage;
import com.zwei.common.domain.PropertyValue;
import com.zwei.iot.device.domain.SensorMetadata;
import com.zwei.iot.device.service.IDeviceSensorQueryService;
import com.zwei.iot.monitor.domain.MonitorContent;
import com.zwei.iot.monitor.service.IMonitorContentService;
import com.zwei.iot.parser.engine.BuiltInFunctions;
import com.zwei.iot.parser.engine.GroovyScriptEngine;
import com.zwei.iot.parser.service.DataParseLogService;
import com.zwei.iot.timeseries.compute.ComputedAttributeEvaluator;
import com.zwei.iot.timeseries.compute.ComputedAttributeRegistry;
import com.zwei.iot.timeseries.compute.ComputedScriptAssembler;
import com.zwei.iot.timeseries.compute.LastMessageStore;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.ValueOperations;

import java.time.Duration;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

/**
 * 计算属性求值端到端 wiring 测试:
 * 真实 Registry + Assembler + Evaluator + ScriptEngine,
 * 只 mock 字典层(MonitorContentService) + 设备层(SensorQuery) + Redis + 日志服务。
 *
 * <p>规格偏差说明:
 * <ul>
 *   <li>Groovy 5.0.4 兼容: 用 {@code curData.get('properties').get('displacement')} 替代
 *       {@code curData.properties.displacement}(后者被 Groovy 元类拦截返回空)。</li>
 *   <li>GroovyScriptEngine 通过 {@code @Resource} 注入了两个字段 ({@code builtInFunctions}
 *       + {@code logService}), 必须同时注入, 否则 {@code executeComputed} 内部 NPE。</li>
 * </ul>
 */
@DisplayName("Computed Attribute 端到端 wiring")
class ComputedAttributeIngestIT {

    private ComputedAttributeEvaluator evaluator;

    @BeforeEach
    void setUp() {
        // 真实组件
        ComputedAttributeRegistry registry = new ComputedAttributeRegistry(mockMonitorContentService());
        ComputedScriptAssembler assembler = new ComputedScriptAssembler();
        GroovyScriptEngine scriptEngine = new GroovyScriptEngine();
        injectField(scriptEngine, "builtInFunctions", new BuiltInFunctions());
        // 规格偏差: logService 也必须注入(GroovyScriptEngine 的 @Resource 字段)
        injectField(scriptEngine, "logService", mock(DataParseLogService.class));
        // Redis mock: opsForValue().get() 返回 null(首次上报), set() no-op
        @SuppressWarnings("unchecked")
        ValueOperations<String, String> valueOps = mock(ValueOperations.class);
        when(valueOps.get(anyString())).thenReturn(null);
        StringRedisTemplate redisTemplate = mock(StringRedisTemplate.class);
        when(redisTemplate.opsForValue()).thenReturn(valueOps);
        LastMessageStore lastMessageStore = new LastMessageStore(
                redisTemplate, new com.fasterxml.jackson.databind.ObjectMapper());

        // 设备层 mock
        IDeviceSensorQueryService sensorQuery = mock(IDeviceSensorQueryService.class);
        when(sensorQuery.requireSensorMetadata(eq(1L), eq("S1")))
                .thenReturn(SensorMetadata.builder()
                        .deviceId(1L).sensorId(10L).monitorTypeId(100L)
                        .attributes(List.of()).build());

        evaluator = new ComputedAttributeEvaluator(
                sensorQuery, registry, assembler, lastMessageStore, scriptEngine);
    }

    private IMonitorContentService mockMonitorContentService() {
        IMonitorContentService svc = mock(IMonitorContentService.class);
        MonitorContent mc = new MonitorContent();
        mc.setId(1L);
        mc.setMonitorTypeId(100L);
        mc.setCode("velocity");
        mc.setName("速率");
        mc.setUnit("mm/s");
        mc.setSortOrder(1);
        // Groovy 5 兼容: 用 .get('properties').get('displacement') 替代 .properties.displacement
        mc.setCalcScript("return curData.get('properties').get('displacement') * 2");
        when(svc.selectComputedByTypeId(100L)).thenReturn(List.of(mc));
        return svc;
    }

    private static void injectField(Object target, String name, Object value) {
        try {
            var f = target.getClass().getDeclaredField(name);
            f.setAccessible(true);
            f.set(target, value);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    @Test
    @DisplayName("单计算属性: 真实脚本执行后产出到 properties")
    void singleComputedAttr() {
        ParsedMessage msg = new ParsedMessage(
                "D1", "S1", "sys", 1700000000000L, 1700000000000L, "hash",
                List.of(new PropertyValue("displacement", "位移", "mm", 12.5, 0)));

        List<PropertyValue> out = evaluator.evaluate(1L, "S1", msg);

        assertThat(out).hasSize(1);
        assertThat(out.get(0).identifier()).isEqualTo("velocity");
        assertThat(out.get(0).value()).isEqualTo(25.0);
    }
}
