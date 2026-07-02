package com.zwei.iot.parser.engine;

import com.zwei.common.domain.ParsedMessage;
import com.zwei.common.domain.PropertyValue;
import com.zwei.iot.parser.config.ParserProperties;
import com.zwei.iot.parser.domain.DataParseLog;
import com.zwei.iot.parser.domain.DataParseStrategy;
import com.zwei.iot.parser.service.DataParseLogService;
import groovy.lang.Binding;
import groovy.lang.GroovyClassLoader;
import groovy.lang.GroovyShell;
import groovy.lang.Script;
import jakarta.annotation.PostConstruct;
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
import java.util.concurrent.atomic.AtomicInteger;

/**
 * Groovy 解析脚本执行引擎。
 *
 * <h3>缓存策略</h3>
 * <p>编译后的脚本 {@link Class} 按 {@code strategyId} 缓存在 {@link #scriptClassCache} 中，
 * 避免每条消息重复编译（B2 修复）。每次执行用 {@code clazz.getDeclaredConstructor().newInstance()}
 * 创建线程局部的 {@link Script} 实例并注入独立 {@link Binding}，保证并发安全。
 *
 * <h3>缓存淘汰</h3>
 * <p>策略更新/删除/启停时由 {@link com.zwei.iot.parser.service.DataParseStrategyService}
 * 调用 {@link #evictCache(Long)} 清除旧脚本类，避免编辑后旧脚本继续执行（B1 修复）。
 *
 * <h3>线程池</h3>
 * <p>解析任务在可配置线程池（{@code iot.parser.groovy-pool-size}，默认 4）上并发执行，
 * 摆脱原单线程串行瓶颈（B5 修复）。沙箱隔离不受并发影响——每个 Script 实例独立。
 */
@Component
public class GroovyScriptEngine {

    private static final Logger log = LoggerFactory.getLogger(GroovyScriptEngine.class);

    /** 脚本编译类缓存：strategyId -> 编译后的 Script Class */
    private final Map<Long, Class<? extends Script>> scriptClassCache = new ConcurrentHashMap<>();
    /** 计算属性脚本编译类缓存：sha256(scriptCode) -> 编译后的 Script Class */
    private final Map<String, Class<? extends Script>> computedClassCache = new ConcurrentHashMap<>();
    /** 共享安全类加载器（带沙箱配置），用于 parseClass */
    private final GroovyClassLoader secureClassLoader;
    private volatile ExecutorService executor;
    private final AtomicInteger threadCounter = new AtomicInteger();

    private static final int DEFAULT_POOL_SIZE = 4;
    private static final int TIMEOUT_SECONDS = 30;

    @Resource
    private BuiltInFunctions builtInFunctions;
    @Resource
    private DataParseLogService logService;
    @Resource
    private ParserProperties parserProperties;

    public GroovyScriptEngine() {
        this.secureClassLoader = new GroovyClassLoader(getClass().getClassLoader(), createSecureConfig());
        this.executor = Executors.newFixedThreadPool(DEFAULT_POOL_SIZE, this::buildDaemonThread);
    }

    @PostConstruct
    public void initPool() {
        int size = (parserProperties != null ? parserProperties.getGroovyPoolSize() : DEFAULT_POOL_SIZE);
        if (size <= 0) size = DEFAULT_POOL_SIZE;
        if (size != DEFAULT_POOL_SIZE) {
            ExecutorService old = this.executor;
            this.executor = Executors.newFixedThreadPool(size, this::buildDaemonThread);
            old.shutdownNow();
        }
    }

    private Thread buildDaemonThread(Runnable r) {
        Thread t = new Thread(r, "parser-groovy-" + threadCounter.incrementAndGet());
        t.setDaemon(true);
        return t;
    }

    @PreDestroy
    public void destroy() {
        scriptClassCache.clear();
        executor.shutdownNow();
        try {
            executor.awaitTermination(5, TimeUnit.SECONDS);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
    }

    /**
     * Execute a parse script and return a ParsedMessage.
     * Returns null on failure (script unsafe, timeout, or exception).
     */
    public ParsedMessage execute(DataParseStrategy strategy, String topic, byte[] message) {
        long startTime = System.currentTimeMillis();
        Future<ParsedMessage> future = executor.submit(() -> {
            try {
                Class<? extends Script> clazz = getOrCreateScriptClass(strategy);
                Binding binding = new Binding();
                binding.setVariable("builtin", builtInFunctions);
                Script script = clazz.getDeclaredConstructor().newInstance();
                script.setBinding(binding);

                @SuppressWarnings("unchecked")
                Map<String, Object> result = (Map<String, Object>) script.invokeMethod(
                        "parse", new Object[]{topic, message});

                String payloadStr = new String(message == null ? new byte[0] : message, StandardCharsets.UTF_8);
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
                logService.info(strategy.getId(), topic, "Parse OK, took " + execTime + "ms",
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
     * <p><b>注意</b>: {@code extraBindings} 在 {@code builtin} 之后注入, 若 key 与 {@code builtin}
     * 冲突将以 extraBindings 为准 (调用方负责避免冲突)。
     *
     * <p><b>注意</b>: 计算属性脚本每次都新建 {@link GroovyShell} 解析, 未走脚本类缓存——
     * 计算属性由 {@code ComputedScriptAssembler} 拼装, 内容随算法库变更频繁, 且单设备触发频率
     * 远低于主解析链路, 不做缓存以避免缓存失效复杂度。
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
                String cacheKey = sha256(scriptCode);
                Class<? extends Script> clazz = computedClassCache.computeIfAbsent(
                        cacheKey, k -> {
                            synchronized (secureClassLoader) {
                                GroovyShell shell = new GroovyShell(createSecureConfig());
                                return shell.parse(scriptCode).getClass();
                            }
                        });
                Script script = clazz.getDeclaredConstructor().newInstance();
                Binding binding = new Binding();
                binding.setVariable("builtin", builtInFunctions);
                if (extraBindings != null) {
                    extraBindings.forEach(binding::setVariable);
                }
                script.setBinding(binding);
                Object result = script.invokeMethod(
                        "compute", new Object[]{curData, prevData});
                return result instanceof Map ? (Map<String, Object>) result : Map.of();
            } catch (Exception e) {
                log.warn("Computed script execution failed: {} | scriptHead=[{}]",
                        e.getClass().getSimpleName(),
                        scriptCode != null ? scriptCode.substring(0, Math.min(scriptCode.length(), 200)) : "null", e);
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
            return Map.of("success", false, "error", e.getMessage() == null ? e.getClass().getSimpleName() : e.getMessage());
        }
    }

    /**
     * 淘汰指定策略的脚本编译缓存。
     *
     * <p>策略更新/删除/启停后由 Service 调用, 确保下次执行重新编译新脚本 (B1 修复)。
     */
    public void evictCache(Long strategyId) {
        if (strategyId != null) {
            scriptClassCache.remove(strategyId);
        }
        computedClassCache.clear();
    }

    private Class<? extends Script> getOrCreateScriptClass(DataParseStrategy strategy) {
        return scriptClassCache.computeIfAbsent(strategy.getId(), id -> {
            // GroovyClassLoader.parseClass 非线程安全, 加锁串行编译
            synchronized (secureClassLoader) {
                return compileScript(strategy.getScriptCode());
            }
        });
    }

    @SuppressWarnings("unchecked")
    private Class<? extends Script> compileScript(String scriptCode) {
        Class<?> clazz = secureClassLoader.parseClass(scriptCode);
        if (!Script.class.isAssignableFrom(clazz)) {
            throw new RuntimeException("Parsed class does not extend groovy.lang.Script: " + clazz.getName());
        }
        return (Class<? extends Script>) clazz;
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
