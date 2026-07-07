package com.zwei.iot.parser.engine;

import com.zwei.common.domain.ParsedMessage;
import com.zwei.common.domain.PropertyValue;
import com.zwei.iot.parser.config.ParserProperties;
import com.zwei.iot.parser.domain.DataParseLog;
import com.zwei.iot.parser.domain.DataParseStrategy;
import com.zwei.iot.parser.service.DataParseLogService;
import com.zwei.iot.parser.support.GroovyScriptValidator;
import groovy.lang.Binding;
import groovy.lang.GroovyClassLoader;
import groovy.lang.GroovyShell;
import groovy.lang.Script;
import jakarta.annotation.PostConstruct;
import jakarta.annotation.PreDestroy;
import jakarta.annotation.Resource;
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
 * Groovy 解析脚本执行引擎——IoT 数据解析链路的最后一环。
 *
 * <h3>架构定位</h3>
 * <p>位于 MQTT 消息接入流水线的末端：接收已匹配的 {@link DataParseStrategy}
 * 和原始消息字节，在沙箱内执行 Groovy 解析脚本，产出结构化的 {@link ParsedMessage}
 * 供后续入 Redis Stream 和写入 IoTDB。
 *
 * <pre>
 * MQTT raw bytes → MonitorIngestFacade → 策略匹配 → GroovyScriptEngine.execute()
 *   → Script.parse(topic, bytes) → ParsedMessage → Redis Stream → IoTDB
 * </pre>
 *
 * <h3>线程安全模型</h3>
 * <ul>
 *   <li>{@link #scriptClassCache} — {@link ConcurrentHashMap}，key 为 strategyId，
 *       同一脚本的编译 Class 被所有执行线程共享读取，编译过程通过
 *       {@code synchronized(secureClassLoader)} 串行化。</li>
 *   <li>每次执行用 {@code clazz.getDeclaredConstructor().newInstance()} 创建独立的
 *       {@link Script} 实例，注入独立的 {@link Binding}，不同消息之间无共享可变状态。</li>
 *   <li>{@link #executor} — 固定大小线程池，提交无状态任务，无需 ThreadLocal。</li>
 * </ul>
 *
 * <h3>安全防护（双层）</h3>
 * <ol>
 *   <li><b>编译期 AST 沙箱</b> — {@link GroovyScriptValidator#createSecureConfig()}
 *       拦截危险 import / receiver 调用（详见该类 Javadoc）。</li>
 *   <li><b>运行时超时终止</b> — {@link #execute} 通过 {@code Future.get(30s)} 强制
 *       超时 cancel，防止死循环耗尽线程池。</li>
 * </ol>
 *
 * <h3>两类编译缓存</h3>
 * <table>
 *   <caption>缓存对比</caption>
 *   <tr><th>缓存</th><th>Key</th><th>用途</th><th>淘汰策略</th></tr>
 *   <tr><td>{@link #scriptClassCache}</td><td>{@code strategyId}</td>
 *        <td>主解析链路——每条 MQTT 消息命中一次，缓存收益极大</td>
 *        <td>策略变更时 {@link #evictCache} 精确淘汰</td></tr>
 *   <tr><td>{@link #computedClassCache}</td><td>{@code sha256(scriptCode)}</td>
 *        <td>计算属性链路——触发频率远低于主链路，脚本由 {@code ComputedScriptAssembler} 动态拼装</td>
 *        <td>{@link #evictCache} 全量清空（简化失效判断）</td></tr>
 * </table>
 *
 * @see GroovyScriptValidator
 * @see BuiltInFunctions
 */
@Component
public class GroovyScriptEngine {

    private static final Logger log = LoggerFactory.getLogger(GroovyScriptEngine.class);

    /**
     * 主解析链路编译缓存：strategyId → 编译后的 Script Class。
     *
     * <p>Key 为策略主键（不变），Value 为编译产物（不可变），天然适合无锁读。
     * 每条消息命中时直接取 Class → newInstance → setBinding，无需重新编译。
     */
    private final Map<Long, Class<? extends Script>> scriptClassCache = new ConcurrentHashMap<>();

    /**
     * 计算属性链路编译缓存：sha256(scriptCode) → 编译后的 Script Class。
     *
     * <p>以脚本体哈希为 key，因为计算属性脚本无稳定策略 ID。
     * {@link #evictCache} 时全量清空。
     */
    private final Map<String, Class<? extends Script>> computedClassCache = new ConcurrentHashMap<>();

    /**
     * 共享安全类加载器，整个引擎生命周期内复用同一个实例。
     *
     * <p>调用 {@link GroovyClassLoader#parseClass} 时必须
     * {@code synchronized(this.secureClassLoader)}——Groovy 编译链路涉及
     * 大量非线程安全的共享中间状态。
     */
    private final GroovyClassLoader secureClassLoader;

    /** 脚本执行线程池，默认 4 线程，可通过 {@code iot.parser.groovy-pool-size} 配置 */
    private volatile ExecutorService executor;
    private final AtomicInteger threadCounter = new AtomicInteger();

    private static final int DEFAULT_POOL_SIZE = 4;
    /** 单次脚本执行超时上限（秒），超时后 Future 被 cancel(true) 强制中断 */
    private static final int TIMEOUT_SECONDS = 30;

    @Resource
    private BuiltInFunctions builtInFunctions;
    @Resource
    private DataParseLogService logService;
    @Resource
    private ParserProperties parserProperties;

    public GroovyScriptEngine() {
        this.secureClassLoader = new GroovyClassLoader(getClass().getClassLoader(),
                GroovyScriptValidator.createSecureConfig());
        this.executor = Executors.newFixedThreadPool(DEFAULT_POOL_SIZE, this::buildDaemonThread);
    }

    /**
     * 根据配置重新初始化线程池。
     *
     * <p>在 Bean 初始化完成后调用，从 {@link ParserProperties} 读取线程池大小。
     * 若与默认值不同则替换 executor。后续可通过配置中心热更新后手动触发。
     */
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

    /** 创建守护线程——不阻止 JVM 正常退出 */
    private Thread buildDaemonThread(Runnable r) {
        Thread t = new Thread(r, "parser-groovy-" + threadCounter.incrementAndGet());
        t.setDaemon(true);
        return t;
    }

    /** Bean 销毁时清缓存 + 关线程池，等待最多 5 秒让在途任务完成 */
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
     * 执行解析脚本，产出结构化 {@link ParsedMessage}。
     *
     * <h3>执行流程</h3>
     * <ol>
     *   <li>从 {@link #scriptClassCache} 获取或编译脚本 Class（编译时走沙箱校验）</li>
     *   <li>创建独立的 {@link Script} 实例 + {@link Binding} + 注入 {@code builtin}</li>
     *   <li>在线程池中异步调用 {@code parse(topic, messageBytes)}</li>
     *   <li>从返回值提取 deviceCode / sensorCode / dataTime / properties</li>
     *   <li>构造 {@link ParsedMessage} + 写解析成功日志</li>
     * </ol>
     *
     * <h3>容错</h3>
     * <ul>
     *   <li>编译失败 / 脚本抛异常 → 写 ERROR 解析日志到 {@code iot_data_parse_log}，返回 {@code null}</li>
     *   <li>执行超时（{@value #TIMEOUT_SECONDS}s）→ {@code Future.cancel(true)}，返回 {@code null}</li>
     *   <li>返回值不抛异常——调用方 {@link com.zwei.iot.timeseries.service.MonitorIngestFacade}
     *       检查 null 后发布 {@code MqttMessageRejectEvent}</li>
     * </ul>
     *
     * @param strategy 已匹配的解析策略（包含脚本体 + sourceType）
     * @param topic    MQTT 原始主题，透传给脚本
     * @param message  原始消息字节
     * @return 解析成功返回 ParsedMessage；任何失败返回 null（不抛异常）
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
     * <p>与 {@link #execute} 共享沙箱配置 ({@link com.zwei.iot.parser.support.GroovyScriptValidator#createSecureConfig()}) 和 executor,
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
                                GroovyShell shell = new GroovyShell(GroovyScriptValidator.createSecureConfig());
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

    /**
     * 在线测试 API——不写日志、不走缓存、不入库。
     *
     * <p>与主链路 {@link #execute} 的区别：
     * <ul>
     *   <li>不走 {@link #scriptClassCache}——每次新建 GroovyShell 编译，
     *       确保测试的是当前编辑的版本而非旧缓存。</li>
     *   <li>不写 {@code iot_data_parse_log}——测试执行不污染运行日志。</li>
     *   <li>不入 Redis Stream——仅返回解析结果供前端预览。</li>
     * </ul>
     *
     * <p>沙箱配置与主链路一致（复用 {@link GroovyScriptValidator#createSecureConfig()}）。
     *
     * @param scriptCode 待测试的 Groovy 脚本体
     * @param topic      模拟 topic
     * @param testData   模拟原始报文
     * @return Map with success/error + parsedMessage or error message
     */
    @SuppressWarnings("unchecked")
    public Map<String, Object> testScript(String scriptCode, String topic, String testData) {
        long startTime = System.currentTimeMillis();
        try {
            GroovyShell shell = new GroovyShell(GroovyScriptValidator.createSecureConfig());
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

    /**
     * 获取或编译脚本 Class（带缓存 + 同步编译）。
     *
     * <p>{@link GroovyClassLoader#parseClass} 内部非线程安全（涉及 AST 编译
     * 链路的共享缓存），对 {@link #secureClassLoader} 加锁保证同一时刻仅一个线程编译。
     * 读命中缓存时无需加锁——{@link ConcurrentHashMap#computeIfAbsent} 保证同一 key
     * 只编译一次，后续线程直接取到已编译 Class。
     */
    private Class<? extends Script> getOrCreateScriptClass(DataParseStrategy strategy) {
        return scriptClassCache.computeIfAbsent(strategy.getId(), id -> {
            synchronized (secureClassLoader) {
                return compileScript(strategy.getScriptCode());
            }
        });
    }

    /**
     * 编译脚本源码为 Script Class。
     *
     * <p>编译产物必须是 {@link Script} 的子类，否则说明脚本内容不是合法的 Groovy 脚本。
     * 正常写入的策略都经过了 {@link GroovyScriptValidator#validate} 预检，此处的
     * 校验为防御性兜底——防止绕过 validate 直接调用 execute 的场景。
     *
     * @throws RuntimeException 如果编译产物不继承 Script
     */
    @SuppressWarnings("unchecked")
    private Class<? extends Script> compileScript(String scriptCode) {
        Class<?> clazz = secureClassLoader.parseClass(scriptCode);
        if (!Script.class.isAssignableFrom(clazz)) {
            throw new RuntimeException("Parsed class does not extend groovy.lang.Script: " + clazz.getName());
        }
        return (Class<? extends Script>) clazz;
    }

    /**
     * 解析 deviceCode：优先取脚本返回值，回退到 topic 路径提取。
     *
     * <p>Topic 格式为 <code>{sourceType}/v1/{deviceCode}/{sensorCode}/updata</code>，
     * deviceCode 位于路径第 3 段（索引 2）。
     */
    private String resolveDeviceCode(Map<String, Object> result, String topic) {
        Object dc = result.get("deviceCode");
        if (dc != null) {
            String code = dc.toString().trim();
            if (!code.isEmpty()) return code;
        }
        // 回退：从 topic 路径提取
        if (topic != null) {
            String[] parts = topic.split("/");
            if (parts.length >= 3 && !parts[2].isEmpty()) {
                return parts[2];
            }
        }
        return "";
    }

    /**
     * 解析 sensorCode：优先取脚本返回值，回退到 topic 路径提取。
     *
     * <p>Topic 格式为 <code>{sourceType}/v1/{deviceCode}/{sensorCode}/updata</code>，
     * sensorCode 位于路径第 4 段（索引 3）。
     */
    private String resolveSensorCode(Map<String, Object> result, String topic) {
        Object sc = result.get("sensorCode");
        if (sc != null) {
            String code = sc.toString().trim();
            if (!code.isEmpty()) return code;
        }
        // 回退：从 topic 路径提取
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
