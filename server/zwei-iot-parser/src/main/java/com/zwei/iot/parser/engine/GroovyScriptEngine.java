package com.zwei.iot.parser.engine;

import com.zwei.common.domain.ParsedMessage;
import com.zwei.common.domain.PropertyValue;
import com.zwei.iot.parser.domain.DataParseLog;
import com.zwei.iot.parser.domain.DataParseStrategy;
import com.zwei.iot.parser.service.DataParseLogService;
import groovy.lang.Binding;
import groovy.lang.GroovyShell;
import groovy.lang.Script;
import jakarta.annotation.PreDestroy;
import jakarta.annotation.Resource;
import org.codehaus.groovy.control.CompilerConfiguration;
import org.codehaus.groovy.control.customizers.SecureASTCustomizer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import java.io.PrintWriter;
import java.io.StringWriter;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.*;

@Component
public class GroovyScriptEngine {

    private static final Logger log = LoggerFactory.getLogger(GroovyScriptEngine.class);

    private final Map<Long, GroovyShell> shellCache = new ConcurrentHashMap<>();
    private final ExecutorService executor = Executors.newSingleThreadExecutor(r -> {
        Thread t = new Thread(r, "parser-groovy");
        t.setDaemon(true);
        return t;
    });

    private static final int TIMEOUT_SECONDS = 30;

    @Resource
    private BuiltInFunctions builtInFunctions;
    @Resource
    private DataParseLogService logService;

    @PreDestroy
    public void destroy() {
        shellCache.clear();
        executor.shutdownNow();
    }

    /**
     * Execute a parse script and return a ParsedMessage.
     * Returns null on failure (script unsafe, timeout, or exception).
     */
    public ParsedMessage execute(DataParseStrategy strategy, String topic, byte[] message) {
        long startTime = System.currentTimeMillis();
        Future<ParsedMessage> future = executor.submit(() -> {
            try {
                GroovyShell shell = getOrCreateShell(strategy);
                Binding binding = new Binding();
                binding.setVariable("builtin", builtInFunctions);
                Script script = shell.parse(strategy.getScriptCode());
                script.setBinding(binding);

                @SuppressWarnings("unchecked")
                Map<String, Object> result = (Map<String, Object>) script.invokeMethod(
                        "parse", new Object[]{topic, message});

                String payloadStr = new String(message, StandardCharsets.UTF_8);
                String hash = sha256(payloadStr);

                ParsedMessage parsed = new ParsedMessage(
                    resolveDeviceCode(result, topic),
                        resolveSensorCode(result, topic),
                    strategy.getSourceType(),
                    resolveDataTime(result),
                    System.currentTimeMillis(),
                    hash,
                    resolveProperties(result)
                );

                long execTime = System.currentTimeMillis() - startTime;
                logService.info(strategy.getId(), "Parse OK, took " + execTime + "ms",
                        payloadStr.length() > 500 ? payloadStr.substring(0, 500) : payloadStr);
                return parsed;
            } catch (Exception e) {
                log.error("Groovy script execution failed: strategyId={}, topic={}", strategy.getId(), topic, e);
                DataParseLog parseLog = DataParseLog.builder()
                        .strategyId(strategy.getId()).logLevel("ERROR")
                        .message("Execution error: " + e.getMessage())
                        .topic(topic)
                        .executionTime((int) (System.currentTimeMillis() - startTime))
                        .errorStack(getStackTrace(e)).build();
                logService.save(parseLog);
                return null;
            }
        });

        try {
            return future.get(TIMEOUT_SECONDS, TimeUnit.SECONDS);
        } catch (TimeoutException e) {
            log.warn("Groovy script timed out ({}s): strategyId={}", TIMEOUT_SECONDS, strategy.getId());
            future.cancel(true);
            return null;
        } catch (Exception e) {
            log.error("Groovy script interrupted: strategyId={}", strategy.getId(), e);
            return null;
        }
    }

    /**
     * 执行合并后的计算属性脚本 (3 参向后兼容版本)。
     *
     * <p>委托到 4 参重载, extraBindings 传空 Map。
     *
     * @see #executeComputed(String, Map, Map, Map)
     */
    public Map<String, Object> executeComputed(String scriptCode,
                                                Map<String, Object> curData,
                                                Map<String, Object> prevData) {
        return executeComputed(scriptCode, curData, prevData, Map.of());
    }

    /**
     * 执行合并后的计算属性脚本, 支持通过 extraBindings 注入额外变量到 Groovy Binding。
     *
     * <p>与 {@link #execute} 共享沙箱配置 ({@link #createSecureConfig()}) 和 executor,
     * 但调用约定不同: 脚本必须定义 {@code compute(curData, prevData)} 主入口,
     * 返回 {@code Map<String, Object>}(attrCode -> value)。
     *
     * <p>典型用法: 调用方传入 {@code Map.of("cache", cacheOps, "sensor", sensorQuery)},
     * 脚本里以 {@code cache.getInt('k')} / {@code sensor.query(...)} 形式访问。
     *
     * <p>失败永远返回空 Map, 不抛异常 (主链路数据接入可用性优先)。
     *
     * @param scriptCode    ComputedScriptAssembler.assemble() 产物
     * @param curData       当前精简消息 Map
     * @param prevData      上一条精简消息 Map, 首次上报时为 null
     * @param extraBindings 额外 Binding 变量 (可为 null / 空)
     * @return 计算结果 Map; 失败时为空 Map
     */
    @SuppressWarnings("unchecked")
    public Map<String, Object> executeComputed(String scriptCode,
                                                Map<String, Object> curData,
                                                Map<String, Object> prevData,
                                                Map<String, Object> extraBindings) {
        Future<Map<String, Object>> future = executor.submit(() -> {
            try {
                GroovyShell shell = new GroovyShell(createSecureConfig());
                Binding binding = new Binding();
                binding.setVariable("builtin", builtInFunctions);
                if (extraBindings != null) {
                    extraBindings.forEach(binding::setVariable);
                }
                Script script = shell.parse(scriptCode);
                script.setBinding(binding);
                Object result = script.invokeMethod(
                        "compute", new Object[]{curData, prevData});
                return result instanceof Map ? (Map<String, Object>) result : Map.of();
            } catch (Exception e) {
                log.warn("Computed script execution failed", e);
                return Map.of();
            }
        });
        try {
            return future.get(TIMEOUT_SECONDS, TimeUnit.SECONDS);
        } catch (TimeoutException e) {
            log.warn("Computed script timed out ({}s)", TIMEOUT_SECONDS);
            future.cancel(true);
            return Map.of();
        } catch (Exception e) {
            log.warn("Computed script interrupted", e);
            return Map.of();
        }
    }

    /** Evaluate a script without persisting anything — for the test API. */
    @SuppressWarnings("unchecked")
    public Map<String, Object> testScript(String scriptCode, String topic, String testData) {
        long startTime = System.currentTimeMillis();
        try {
            GroovyShell shell = new GroovyShell(createSecureConfig());
            Binding binding = new Binding();
            binding.setVariable("builtin", builtInFunctions);
            Script script = shell.parse(scriptCode);
            script.setBinding(binding);
            byte[] messageBytes = testData.getBytes(StandardCharsets.UTF_8);
            Map<String, Object> result = (Map<String, Object>) script.invokeMethod(
                    "parse", new Object[]{topic, messageBytes});
            return Map.of(
                "success", true,
                "executionTime", System.currentTimeMillis() - startTime,
                "parsedMessage", buildTestMessage(result)
            );
        } catch (Exception e) {
            return Map.of("success", false, "error", e.getMessage());
        }
    }

    /** Evict cached shell when strategy is updated. */
    public void evictCache(Long strategyId) {
        shellCache.remove(strategyId);
    }

    private GroovyShell getOrCreateShell(DataParseStrategy strategy) {
        return shellCache.computeIfAbsent(strategy.getId(), id -> {
            GroovyShell shell = new GroovyShell(createSecureConfig());
            shell.parse(strategy.getScriptCode());
            return shell;
        });
    }

    private CompilerConfiguration createSecureConfig() {
        CompilerConfiguration config = new CompilerConfiguration();
        SecureASTCustomizer secure = new SecureASTCustomizer();
        // Block all wildcard imports
        secure.setDisallowedStarImports(List.of("*"));
        // Block dangerous imports explicitly
        secure.setDisallowedImports(List.of(
            "java.lang.System",
            "java.lang.Runtime",
            "java.lang.ProcessBuilder",
            "java.lang.Thread",
            "java.lang.Class",
            "java.lang.ClassLoader",
            "java.io.File",
            "java.io.FileInputStream",
            "java.io.FileOutputStream",
            "java.io.RandomAccessFile",
            "java.lang.reflect.*",
            "java.lang.invoke.*",
            "java.net.*",
            "java.io.*",
            "java.nio.*",
            "javax.script.*",
            "groovy.lang.*",
            "org.codehaus.groovy.*"
        ));
        // Block dangerous static imports
        secure.setDisallowedStaticImports(List.of("*"));
        // Block method calls on dangerous receiver types (AST-level, cannot be bypassed by string tricks)
        secure.setDisallowedReceivers(List.of(
            System.class.getName(),
            Runtime.class.getName(),
            ProcessBuilder.class.getName(),
            Class.class.getName(),
            Thread.class.getName(),
            java.io.File.class.getName(),
            "groovy.lang.GroovyShell",
            "groovy.lang.GroovyClassLoader",
            "groovy.lang.Script",
            "groovy.lang.Closure",
            "org.codehaus.groovy.runtime.InvokerHelper"
        ));
        config.addCompilationCustomizers(secure);
        return config;
    }

    /**
     * Resolve deviceCode: script return value first, fall back to topic extraction.
     * Topic format: sys|gb/v1/{deviceCode}/{sensorCode}/updata
     */
    private String resolveDeviceCode(Map<String, Object> result, String topic) {
        Object dc = result.get("deviceCode");
        if (dc != null) {
            String code = dc.toString().trim();
            if (!code.isEmpty()) return code;
        }
        // Fallback: extract from topic
        if (topic != null) {
            String[] parts = topic.split("/");
            if (parts.length >= 3 && !parts[2].isEmpty()) {
                return parts[2];
            }
        }
        return "";
    }

    /**
     * Resolve sensorCode: script return value first, fall back to topic extraction.
     * Topic format: sys|gb/v1/{deviceCode}/{sensorCode}/updata
     */
    private String resolveSensorCode(Map<String, Object> result, String topic) {
        Object sc = result.get("sensorCode");
        if (sc != null) {
            String code = sc.toString().trim();
            if (!code.isEmpty()) return code;
        }
        // Fallback: extract from topic
        if (topic != null) {
            String[] parts = topic.split("/");
            if (parts.length >= 4 && !parts[3].isEmpty()) {
                return parts[3];
            }
        }
        return "";
    }

    private long resolveDataTime(Map<String, Object> result) {
        Object dt = result.get("dataTime");
        if (dt instanceof Number) return ((Number) dt).longValue();
        return System.currentTimeMillis();
    }

    @SuppressWarnings("unchecked")
    private List<PropertyValue> resolveProperties(Map<String, Object> result) {
        Object props = result.get("properties");
        if (!(props instanceof List)) return List.of();
        List<PropertyValue> list = new ArrayList<>();
        for (Object item : (List<?>) props) {
            if (item instanceof Map) {
                Map<String, Object> m = (Map<String, Object>) item;
                list.add(new PropertyValue(
                    str(m, "identifier", ""),
                    str(m, "name", ""),
                    str(m, "unit", ""),
                    toDouble(m.get("value")),
                    toInt(m.get("quality"), 0)
                ));
            }
        }
        return list;
    }

    private String str(Map<String, Object> m, String key, String def) {
        Object v = m.get(key);
        return v != null ? v.toString() : def;
    }

    private Double toDouble(Object v) {
        if (v == null) return null;
        if (v instanceof Number) return ((Number) v).doubleValue();
        try { return Double.parseDouble(v.toString().trim()); }
        catch (Exception e) { return null; }
    }

    private Integer toInt(Object v, int def) {
        if (v == null) return def;
        if (v instanceof Number) return ((Number) v).intValue();
        try { return Integer.parseInt(v.toString().trim()); }
        catch (Exception e) { return def; }
    }

    private Map<String, Object> buildTestMessage(Map<String, Object> result) {
        return Map.of(
            "deviceCode", str(result, "deviceCode", ""),
                "sensorCode", resolveSensorCode(result, null),
            "dataTime", resolveDataTime(result),
            "properties", resolveProperties(result)
        );
    }

    private String sha256(String input) {
        try {
            MessageDigest md = MessageDigest.getInstance("SHA-256");
            byte[] digest = md.digest(input.getBytes(StandardCharsets.UTF_8));
            StringBuilder sb = new StringBuilder();
            for (byte b : digest) sb.append(String.format("%02x", b));
            return sb.toString();
        } catch (Exception e) { return ""; }
    }

    private String getStackTrace(Exception e) {
        StringWriter sw = new StringWriter();
        e.printStackTrace(new PrintWriter(sw));
        return sw.toString();
    }
}
