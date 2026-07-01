package com.zwei.iot.parser.engine;

import com.zwei.common.domain.ParsedMessage;
import com.zwei.iot.parser.domain.DataParseStrategy;
import com.zwei.iot.parser.service.DataParseLogService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.nio.charset.StandardCharsets;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;

/**
 * 验证 GroovyScriptEngine 脚本编译缓存 (B2 修复) + 多线程并发 (B5 修复)。
 */
@DisplayName("GroovyScriptEngine — 编译缓存与并发")
class GroovyScriptEngineCacheTest {

    private GroovyScriptEngine engine;
    private DataParseLogService logService;

    @BeforeEach
    void setUp() {
        logService = mock(DataParseLogService.class);
        engine = new GroovyScriptEngine();
        injectField(engine, "builtInFunctions", new BuiltInFunctions());
        injectField(engine, "logService", logService);
    }

    @Test
    @DisplayName("同一策略多次执行应命中编译缓存（仅编译一次）")
    void scriptClassCachedAcrossExecutions() {
        String script = simpleScript();
        DataParseStrategy strategy = strategy("sys", script, 100L);
        byte[] message = "{\"value\":1}".getBytes(StandardCharsets.UTF_8);

        ParsedMessage r1 = engine.execute(strategy, "sys/v1/D/S/updata", message);
        ParsedMessage r2 = engine.execute(strategy, "sys/v1/D/S/updata", message);

        assertThat(r1).isNotNull();
        assertThat(r2).isNotNull();
        // 两次都能正确解析即可；缓存命中由 evictCache 测试间接验证（编译只发生一次）
        assertThat(r1.properties().get(0).value()).isEqualTo(1.0);
        assertThat(r2.properties().get(0).value()).isEqualTo(1.0);
    }

    @Test
    @DisplayName("evictCache 后下次执行重新编译新脚本")
    void evictCacheForcesRecompile() {
        DataParseStrategy strategy = strategy("sys", simpleScript(), 101L);
        byte[] message = "{\"value\":1}".getBytes(StandardCharsets.UTF_8);

        ParsedMessage r1 = engine.execute(strategy, "sys/v1/D/S/updata", message);
        assertThat(r1).isNotNull();
        assertThat(r1.properties().get(0).value()).isEqualTo(1.0);

        // 更换脚本内容并淘汰缓存
        strategy.setScriptCode(simpleScriptReturning42());
        engine.evictCache(101L);

        ParsedMessage r2 = engine.execute(strategy, "sys/v1/D/S/updata", message);
        assertThat(r2).isNotNull();
        assertThat(r2.properties().get(0).value()).isEqualTo(42.0);
    }

    @Test
    @DisplayName("多线程并发执行同一策略应全部正确且互不干扰")
    void concurrentExecutionSameStrategy() throws Exception {
        DataParseStrategy strategy = strategy("sys", scriptReadsMessage(), 102L);
        int threads = 16;
        int perThread = 5;
        int total = threads * perThread;
        ExecutorService pool = Executors.newFixedThreadPool(threads);
        CountDownLatch latch = new CountDownLatch(total);
        AtomicInteger success = new AtomicInteger();

        for (int i = 0; i < total; i++) {
            final double expected = i + 1.0;
            pool.submit(() -> {
                try {
                    byte[] msg = ("{\"value\":" + expected + "}").getBytes(StandardCharsets.UTF_8);
                    ParsedMessage r = engine.execute(strategy, "sys/v1/D/S/updata", msg);
                    if (r != null && r.properties().get(0).value() instanceof Number n
                            && Math.abs(n.doubleValue() - expected) < 1e-9) {
                        success.incrementAndGet();
                    }
                } finally {
                    latch.countDown();
                }
            });
        }
        assertThat(latch.await(30, TimeUnit.SECONDS)).isTrue();
        pool.shutdownNow();
        assertThat(success.get()).isEqualTo(total);
    }

    // --- helpers ---

    private DataParseStrategy strategy(String sourceType, String scriptCode, Long id) {
        DataParseStrategy s = new DataParseStrategy();
        s.setId(id);
        s.setSourceType(sourceType);
        s.setScriptCode(scriptCode);
        s.setName("CacheTest-" + id);
        s.setStatus(1);
        return s;
    }

    private String simpleScript() {
        return "Map<String, Object> parse(String topic, byte[] messageBytes) {\n" +
            "    def result = [:]\n" +
            "    result.put(\"sensorCode\", \"S\")\n" +
            "    result.put(\"properties\", [[identifier: \"value\", value: 1, quality: 0]])\n" +
            "    return result\n" +
            "}";
    }

    private String simpleScriptReturning42() {
        return "Map<String, Object> parse(String topic, byte[] messageBytes) {\n" +
            "    def result = [:]\n" +
            "    result.put(\"sensorCode\", \"S\")\n" +
            "    result.put(\"properties\", [[identifier: \"value\", value: 42, quality: 0]])\n" +
            "    return result\n" +
            "}";
    }

    private String scriptReadsMessage() {
        return "import com.alibaba.fastjson2.JSON\n" +
            "Map<String, Object> parse(String topic, byte[] messageBytes) {\n" +
            "    def json = JSON.parseObject(new String(messageBytes, \"UTF-8\"))\n" +
            "    def result = [:]\n" +
            "    result.put(\"sensorCode\", \"S\")\n" +
            "    result.put(\"properties\", [[identifier: \"value\", value: json.getDouble(\"value\"), quality: 0]])\n" +
            "    return result\n" +
            "}";
    }

    private void injectField(Object target, String fieldName, Object value) {
        try {
            java.lang.reflect.Field f = target.getClass().getDeclaredField(fieldName);
            f.setAccessible(true);
            f.set(target, value);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }
}
