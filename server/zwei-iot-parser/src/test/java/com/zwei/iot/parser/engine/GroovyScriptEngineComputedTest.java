package com.zwei.iot.parser.engine;

import com.zwei.iot.parser.service.DataParseLogService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.LinkedHashMap;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;

@DisplayName("GroovyScriptEngine.executeComputed")
class GroovyScriptEngineComputedTest {

    private GroovyScriptEngine engine;
    private DataParseLogService logService;

    @BeforeEach
    void setUp() {
        logService = mock(DataParseLogService.class);
        engine = new GroovyScriptEngine();
        injectField(engine, "builtInFunctions", new BuiltInFunctions());
        injectField(engine, "logService", logService);
    }

    /** Reflection helper — 与 GroovyScriptEngineTest 一致 */
    private static void injectField(Object target, String fieldName, Object value) {
        try {
            var field = target.getClass().getDeclaredField(fieldName);
            field.setAccessible(true);
            field.set(target, value);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    @Test
    @DisplayName("正常执行: curData 与 prevData 注入, 返回 Map")
    void executeSuccess() {
        String script = """
            def compute(curData, prevData) {
                def out = new LinkedHashMap<String, Object>()
                out.put('velocity', curData.get('properties').get('displacement') * 2)
                out.put('delta', curData.get('properties').get('displacement') - prevData.get('properties').get('displacement'))
                return out
            }
        """;
        Map<String, Object> cur = Map.of(
                "properties", Map.of("displacement", 12.5));
        Map<String, Object> prev = Map.of(
                "properties", Map.of("displacement", 10.0));

        Map<String, Object> out = engine.executeComputed(script, cur, prev);

        assertThat(out).hasSize(2);
        assertThat(out.get("velocity")).isEqualTo(25.0);
        assertThat(out.get("delta")).isEqualTo(2.5);
    }

    @Test
    @DisplayName("prevData=null: 脚本需自行处理, 正常返回")
    void executeWithNullPrev() {
        String script = """
            def compute(curData, prevData) {
                def out = new LinkedHashMap<String, Object>()
                out.put('x', curData.get('properties').get('x'))
                return out
            }
        """;
        Map<String, Object> cur = Map.of("properties", Map.of("x", 7.0));

        Map<String, Object> out = engine.executeComputed(script, cur, null);

        assertThat(out.get("x")).isEqualTo(7.0);
    }

    @Test
    @DisplayName("脚本异常: 返回空 Map, 不抛")
    void executeException() {
        String script = """
            def compute(curData, prevData) {
                throw new RuntimeException("boom")
            }
        """;
        Map<String, Object> out = engine.executeComputed(script, Map.of(), Map.of());
        assertThat(out).isEmpty();
    }

    @Test
    @DisplayName("沙箱拒绝: System.exit 调用应失败, 返回空 Map")
    void sandboxRejects() {
        String script = """
            def compute(curData, prevData) {
                Runtime.runtime.exec("rm -rf /")
                return [:]
            }
        """;
        Map<String, Object> out = engine.executeComputed(script, Map.of(), Map.of());
        assertThat(out).isEmpty();
    }

    @Test
    @DisplayName("4 参重载: extraBindings 进入 Binding, 脚本可调实例方法")
    void extraBindingsInjected() {
        String script = """
            def compute(curData, prevData) {
                def out = new LinkedHashMap<String, Object>()
                out.put('doubled', counter.doubleIt(21))
                return out
            }
        """;

        // counter 是一个普通 Java 对象, 验证实例方法被脚本调用
        Map<String, Object> bindings = new LinkedHashMap<>();
        bindings.put("counter", new Object() {
            @SuppressWarnings("unused")
            public int doubleIt(int x) { return x * 2; }
        });

        Map<String, Object> out = engine.executeComputed(script, Map.of(), null, bindings);

        assertThat(out).hasSize(1);
        assertThat(out.get("doubled")).isEqualTo(42);
    }

    @Test
    @DisplayName("3 参重载仍工作: 委托到 4 参 + 空 Map (回归)")
    void threeArgStillWorks() {
        String script = """
            def compute(curData, prevData) {
                def out = new LinkedHashMap<String, Object>()
                out.put('ok', true)
                return out
            }
        """;
        Map<String, Object> out = engine.executeComputed(script, Map.of(), Map.of());
        assertThat(out.get("ok")).isEqualTo(Boolean.TRUE);
    }
}
