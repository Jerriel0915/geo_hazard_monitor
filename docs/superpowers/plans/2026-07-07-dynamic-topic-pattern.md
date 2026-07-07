# Dynamic Topic Pattern Registry — Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Replace 4 hardcoded MQTT topic prefix checks with a dynamic registry derived from `DataParseStrategy.sourceType`.

**Architecture:** Interface `ITopicPatternService` in device module, implementation `TopicPatternServiceImpl` in parser module (AtomicReference cache + MyBatis query), consumed by 3 broker files + MonitorTopicParser. Reload via POST API, auto-load on startup.

**Tech Stack:** Java 17, Spring Boot 4.0, MyBatis, Maven

---

### Task 1: Add `selectDistinctSourceTypes` to MyBatis mapper

**Files:**
- Modify: `server/zwei-iot-parser/src/main/java/com/zwei/iot/parser/mapper/DataParseStrategyMapper.java`
- Modify: `server/zwei-iot-parser/src/main/resources/mapper/iot/parser/DataParseStrategyMapper.xml`

- [ ] **Step 1: Add mapper method declaration**

In `DataParseStrategyMapper.java`, add after `selectEnabled()`:

```java
/** 获取所有启用的策略的协议标识符（去重），用于构建动态 topic 匹配正则 */
List<String> selectDistinctSourceTypes();
```

- [ ] **Step 2: Add XML query**

In `DataParseStrategyMapper.xml`, add before `</mapper>`:

```xml
<select id="selectDistinctSourceTypes" resultType="java.lang.String">
    SELECT DISTINCT source_type FROM iot_data_parse_strategy
    WHERE status = 1 AND del_flag = 0 AND source_type IS NOT NULL
</select>
```

- [ ] **Step 3: Verify compilation**

Run: `cd server && mvn compile -pl zwei-iot-parser -q`
Expected: BUILD SUCCESS

- [ ] **Step 4: Commit**

```bash
git add server/zwei-iot-parser/src/main/java/com/zwei/iot/parser/mapper/DataParseStrategyMapper.java server/zwei-iot-parser/src/main/resources/mapper/iot/parser/DataParseStrategyMapper.xml
git commit -m "feat: add selectDistinctSourceTypes to DataParseStrategyMapper"
```

---

### Task 2: Create `ITopicPatternService` interface in device module

**Files:**
- Create: `server/zwei-iot-device/src/main/java/com/zwei/iot/device/service/ITopicPatternService.java`

- [ ] **Step 1: Create interface file**

```java
package com.zwei.iot.device.service;

import java.util.Set;

/**
 * MQTT topic pattern registry — dynamic topic prefix validation.
 *
 * <p>Derives active topic prefixes from {@code DataParseStrategy.sourceType},
 * replacing hardcoded {@code sys/v1/} and {@code gb/v1/} checks across the broker
 * and parser modules.
 *
 * <p>Path structure <code>{sourceType}/v1/{deviceCode}/{sensorCode}/updata</code>
 * is fixed; only the first segment varies.
 */
public interface ITopicPatternService {

    /**
     * Check if a topic matches any registered protocol prefix.
     *
     * @param topic MQTT topic string
     * @return true if the topic matches a known pattern
     */
    boolean matches(String topic);

    /**
     * Extract structured components from a topic.
     *
     * @param topic MQTT topic string
     * @return parsed components, or {@code null} if no pattern matches
     */
    TopicComponents resolveTopic(String topic);

    /**
     * Current active sourceType set (read-only snapshot).
     */
    Set<String> getActiveSourceTypes();

    /**
     * Force reload the pattern registry from the database.
     */
    void reload();

    /**
     * Structured representation of a parsed MQTT monitor topic.
     */
    record TopicComponents(String sourceType, String deviceCode, String sensorCode) {
    }
}
```

- [ ] **Step 2: Verify compilation**

Run: `cd server && mvn compile -pl zwei-iot-device -q`
Expected: BUILD SUCCESS

- [ ] **Step 3: Commit**

```bash
git add server/zwei-iot-device/src/main/java/com/zwei/iot/device/service/ITopicPatternService.java
git commit -m "feat: add ITopicPatternService interface for dynamic topic matching"
```

---

### Task 3: Create `TopicPatternServiceImpl` in parser module

**Files:**
- Create: `server/zwei-iot-parser/src/main/java/com/zwei/iot/parser/service/TopicPatternServiceImpl.java`

- [ ] **Step 1: Create implementation**

```java
package com.zwei.iot.parser.service;

import com.zwei.iot.device.service.ITopicPatternService;
import com.zwei.iot.parser.mapper.DataParseStrategyMapper;
import jakarta.annotation.PostConstruct;
import jakarta.annotation.Resource;
import org.springframework.stereotype.Service;

import java.util.Collections;
import java.util.List;
import java.util.Set;
import java.util.concurrent.atomic.AtomicReference;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

@Service
public class TopicPatternServiceImpl implements ITopicPatternService {

    private static final Pattern NEVER_MATCH = Pattern.compile("(?!)");

    @Resource
    private DataParseStrategyMapper strategyMapper;

    private final AtomicReference<CachedPatterns> cache = new AtomicReference<>();

    @PostConstruct
    public void init() {
        reload();
    }

    @Override
    public boolean matches(String topic) {
        CachedPatterns p = cache.get();
        return p != null && p.pattern.matcher(topic == null ? "" : topic).matches();
    }

    @Override
    public TopicComponents resolveTopic(String topic) {
        CachedPatterns p = cache.get();
        if (p == null || p.sourceTypes.isEmpty()) {
            return null;
        }
        Matcher m = p.pattern.matcher(topic == null ? "" : topic);
        if (!m.matches()) {
            return null;
        }
        return new TopicComponents(m.group(1), m.group(2), m.group(3));
    }

    @Override
    public Set<String> getActiveSourceTypes() {
        CachedPatterns p = cache.get();
        return p == null ? Collections.emptySet() : p.sourceTypes;
    }

    @Override
    public void reload() {
        List<String> sourceTypes = strategyMapper.selectDistinctSourceTypes();
        Set<String> set = sourceTypes != null && !sourceTypes.isEmpty()
                ? Set.copyOf(sourceTypes)
                : Collections.emptySet();
        cache.set(new CachedPatterns(set));
    }

    private static class CachedPatterns {
        final Set<String> sourceTypes;
        final Pattern pattern;

        CachedPatterns(Set<String> sourceTypes) {
            this.sourceTypes = sourceTypes;
            if (sourceTypes.isEmpty()) {
                this.pattern = NEVER_MATCH;
            } else {
                String prefix = sourceTypes.stream()
                        .map(Pattern::quote)
                        .collect(Collectors.joining("|"));
                this.pattern = Pattern.compile(
                        "^(" + prefix + ")/v1/([A-Za-z0-9_-]{1,64})/([A-Za-z0-9_-]{1,100})/updata$");
            }
        }
    }
}
```

- [ ] **Step 2: Verify compilation**

Run: `cd server && mvn compile -pl zwei-iot-parser -q`
Expected: BUILD SUCCESS

- [ ] **Step 3: Commit**

```bash
git add server/zwei-iot-parser/src/main/java/com/zwei/iot/parser/service/TopicPatternServiceImpl.java
git commit -m "feat: add TopicPatternServiceImpl with AtomicReference cache"
```

---

### Task 4: Write unit test for `TopicPatternServiceImpl`

**Files:**
- Create: `server/zwei-iot-parser/src/test/java/com/zwei/iot/parser/service/TopicPatternServiceImplTest.java`

- [ ] **Step 1: Write the tests**

```java
package com.zwei.iot.parser.service;

import com.zwei.iot.device.service.ITopicPatternService.TopicComponents;
import com.zwei.iot.parser.mapper.DataParseStrategyMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class TopicPatternServiceImplTest {

    @Mock
    private DataParseStrategyMapper strategyMapper;

    private TopicPatternServiceImpl service;

    @BeforeEach
    void setUp() {
        service = new TopicPatternServiceImpl();
        // Inject mock manually (no Spring context)
        injectMapper(service, strategyMapper);
    }

    // --- reload + getActiveSourceTypes ---

    @Test
    @DisplayName("reload loads distinct sourceTypes from DB")
    void reload_loadsDistinctSourceTypes() {
        when(strategyMapper.selectDistinctSourceTypes())
                .thenReturn(List.of("sys", "gb", "sys")); // intentional duplicate

        service.reload();

        assertThat(service.getActiveSourceTypes()).containsExactlyInAnyOrder("sys", "gb");
    }

    @Test
    @DisplayName("reload with empty DB produces empty set")
    void reload_emptyDb_producesEmptySet() {
        when(strategyMapper.selectDistinctSourceTypes()).thenReturn(List.of());

        service.reload();

        assertThat(service.getActiveSourceTypes()).isEmpty();
    }

    @Test
    @DisplayName("reload with null from mapper produces empty set")
    void reload_nullFromMapper_producesEmptySet() {
        when(strategyMapper.selectDistinctSourceTypes()).thenReturn(null);

        service.reload();

        assertThat(service.getActiveSourceTypes()).isEmpty();
    }

    // --- matches ---

    @Test
    @DisplayName("matches returns true for known sys topic")
    void matches_sysTopic_returnsTrue() {
        when(strategyMapper.selectDistinctSourceTypes()).thenReturn(List.of("sys", "gb"));
        service.reload();

        assertThat(service.matches("sys/v1/DEV001/S01/updata")).isTrue();
    }

    @Test
    @DisplayName("matches returns true for known gb topic")
    void matches_gbTopic_returnsTrue() {
        when(strategyMapper.selectDistinctSourceTypes()).thenReturn(List.of("sys", "gb"));
        service.reload();

        assertThat(service.matches("gb/v1/DEV002/S02/updata")).isTrue();
    }

    @Test
    @DisplayName("matches returns false for unknown prefix")
    void matches_unknownPrefix_returnsFalse() {
        when(strategyMapper.selectDistinctSourceTypes()).thenReturn(List.of("sys"));
        service.reload();

        assertThat(service.matches("unknown/v1/DEV001/S01/updata")).isFalse();
    }

    @Test
    @DisplayName("matches returns false when no sourceTypes loaded")
    void matches_emptyRegistry_returnsFalse() {
        when(strategyMapper.selectDistinctSourceTypes()).thenReturn(List.of());
        service.reload();

        assertThat(service.matches("sys/v1/DEV001/S01/updata")).isFalse();
    }

    @Test
    @DisplayName("matches returns false for null topic")
    void matches_nullTopic_returnsFalse() {
        when(strategyMapper.selectDistinctSourceTypes()).thenReturn(List.of("sys"));
        service.reload();

        assertThat(service.matches(null)).isFalse();
    }

    @Test
    @DisplayName("matches returns true for custom sourceType with regex-safe chars")
    void matches_customSourceType_returnsTrue() {
        when(strategyMapper.selectDistinctSourceTypes()).thenReturn(List.of("hj", "sl"));
        service.reload();

        assertThat(service.matches("hj/v1/DEV001/S01/updata")).isTrue();
        assertThat(service.matches("sl/v1/DEV001/S01/updata")).isTrue();
    }

    // --- resolveTopic ---

    @Test
    @DisplayName("resolveTopic extracts all three components")
    void resolveTopic_sysTopic_extractsComponents() {
        when(strategyMapper.selectDistinctSourceTypes()).thenReturn(List.of("sys", "gb"));
        service.reload();

        TopicComponents c = service.resolveTopic("gb/v1/DEV999/S88/updata");

        assertThat(c).isNotNull();
        assertThat(c.sourceType()).isEqualTo("gb");
        assertThat(c.deviceCode()).isEqualTo("DEV999");
        assertThat(c.sensorCode()).isEqualTo("S88");
    }

    @Test
    @DisplayName("resolveTopic returns null for unknown topic")
    void resolveTopic_unknownPrefix_returnsNull() {
        when(strategyMapper.selectDistinctSourceTypes()).thenReturn(List.of("sys"));
        service.reload();

        assertThat(service.resolveTopic("xxx/v1/DEV001/S01/updata")).isNull();
    }

    @Test
    @DisplayName("resolveTopic returns null for malformed topic")
    void resolveTopic_malformedTopic_returnsNull() {
        when(strategyMapper.selectDistinctSourceTypes()).thenReturn(List.of("sys"));
        service.reload();

        assertThat(service.resolveTopic("sys/v1/DEV001")).isNull();
        assertThat(service.resolveTopic("sys/v1/DEV001/S01")).isNull();
    }

    @Test
    @DisplayName("resolveTopic returns null for null topic")
    void resolveTopic_nullTopic_returnsNull() {
        when(strategyMapper.selectDistinctSourceTypes()).thenReturn(List.of("sys"));
        service.reload();

        assertThat(service.resolveTopic(null)).isNull();
    }

    // --- helper to inject mock via reflection ---

    private static void injectMapper(TopicPatternServiceImpl svc, DataParseStrategyMapper mapper) {
        try {
            var field = TopicPatternServiceImpl.class.getDeclaredField("strategyMapper");
            field.setAccessible(true);
            field.set(svc, mapper);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }
}
```

- [ ] **Step 2: Run tests to verify they pass**

Run: `cd server && mvn test -pl zwei-iot-parser -Dtest=TopicPatternServiceImplTest -q`
Expected: Tests run: 11, Failures: 0

- [ ] **Step 3: Commit**

```bash
git add server/zwei-iot-parser/src/test/java/com/zwei/iot/parser/service/TopicPatternServiceImplTest.java
git commit -m "test: add TopicPatternServiceImpl unit tests (11 cases)"
```

---

### Task 5: Modify `MonitorTopicParser` to use dynamic pattern

**Files:**
- Modify: `server/zwei-iot-parser/src/main/java/com/zwei/iot/parser/support/MonitorTopicParser.java`

- [ ] **Step 1: Rewrite MonitorTopicParser**

```java
package com.zwei.iot.parser.support;

import com.zwei.iot.device.service.ITopicPatternService;
import com.zwei.iot.device.service.ITopicPatternService.TopicComponents;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

/**
 * MQTT 监测主题解析器。
 *
 * <p>从监测数据上报主题中提取三要素：协议类型、设备编码、传感器编号。
 * 通过 {@link ITopicPatternService} 动态匹配已注册的协议前缀。
 *
 * <h3>支持的 MQTT 主题格式</h3>
 * <pre>
 * {sourceType}/v1/{deviceCode}/{sensorCode}/updata
 * </pre>
 * <p>其中 {@code sourceType} 来自系统中已启用的解析策略的 {@code source_type} 字段。
 *
 * <p>解析失败时返回 null，由上游 {@link com.zwei.iot.timeseries.service.MonitorIngestFacade} 统一处理。
 */
@Component
public class MonitorTopicParser {

    private final ITopicPatternService topicPatternService;

    @Autowired
    public MonitorTopicParser(ITopicPatternService topicPatternService) {
        this.topicPatternService = topicPatternService;
    }

    /**
     * 解析监测数据主题。
     *
     * @param topic MQTT 主题
     * @return 成功时返回主题对象，失败时返回 {@code null}
     */
    public MonitorTopic parse(String topic) {
        TopicComponents c = topicPatternService.resolveTopic(topic);
        if (c == null) {
            return null;
        }
        return new MonitorTopic(c.sourceType(), c.deviceCode(), c.sensorCode());
    }
}
```

- [ ] **Step 2: Verify compilation**

Run: `cd server && mvn compile -pl zwei-iot-parser -q`
Expected: BUILD SUCCESS

- [ ] **Step 3: Run existing MonitorTopicParserTest**

Run: `cd server && mvn test -pl zwei-iot-parser -Dtest=MonitorTopicParserTest -q`
Expected: Tests should fail if they mock the old static Pattern — need to update tests in Task 12

- [ ] **Step 4: Commit**

```bash
git add server/zwei-iot-parser/src/main/java/com/zwei/iot/parser/support/MonitorTopicParser.java
git commit -m "refactor: MonitorTopicParser delegates to ITopicPatternService"
```

---

### Task 6: Add reload endpoint to `DataParseController`

**Files:**
- Modify: `server/zwei-iot-parser/src/main/java/com/zwei/iot/parser/controller/DataParseController.java`

- [ ] **Step 1: Add endpoint**

In `DataParseController.java`, inject `ITopicPatternService` and add the reload method.

Add import:
```java
import com.zwei.iot.device.service.ITopicPatternService;
```

Add field injection:
```java
@Resource
private ITopicPatternService topicPatternService;
```

Add endpoint after the `copy` method and before `testScript`:

```java
@PreAuthorize("@ss.hasPermi('monitor:parser:edit')")
@PostMapping("/topic-patterns/reload")
public AjaxResult reloadTopicPatterns() {
    topicPatternService.reload();
    return AjaxResult.success(topicPatternService.getActiveSourceTypes());
}
```

- [ ] **Step 2: Verify compilation**

Run: `cd server && mvn compile -pl zwei-iot-parser -q`
Expected: BUILD SUCCESS

- [ ] **Step 3: Commit**

```bash
git add server/zwei-iot-parser/src/main/java/com/zwei/iot/parser/controller/DataParseController.java
git commit -m "feat: add POST /api/v1/iot/parser/strategy/topic-patterns/reload endpoint"
```

---

### Task 7: Modify `MqttServerMessageListener` to use dynamic pattern

**Files:**
- Modify: `server/zwei-iot-broker/src/main/java/com/zwei/iot/broker/service/MqttServerMessageListener.java`

- [ ] **Step 1: Inject ITopicPatternService and replace hardcoded check**

Add import:
```java
import com.zwei.iot.device.service.ITopicPatternService;
```

Add field + constructor parameter:
```java
private final ITopicPatternService topicPatternService;

@Autowired
public MqttServerMessageListener(MonitorIngestFacade monitorIngestFacade,
                                 MqttDeviceSessionRegistry sessionRegistry,
                                 ApplicationEventPublisher eventPublisher,
                                 ITopicPatternService topicPatternService) {
    this.monitorIngestFacade = monitorIngestFacade;
    this.sessionRegistry = sessionRegistry;
    this.eventPublisher = eventPublisher;
    this.topicPatternService = topicPatternService;
}
```

Replace line 86:
```java
// Before:
if (topic == null || (!topic.startsWith("sys/v1/") && !topic.startsWith("gb/v1/"))) {
// After:
if (topic == null || !topicPatternService.matches(topic)) {
```

Also update the reject reason on the next line:
```java
publishReject(clientId, username, deviceId, topic, message, receiveTime,
        "TOPIC", "主题不匹配任何已注册的协议前缀", null);
```

- [ ] **Step 2: Verify compilation**

Run: `cd server && mvn compile -pl zwei-iot-broker -q`
Expected: BUILD SUCCESS

- [ ] **Step 3: Commit**

```bash
git add server/zwei-iot-broker/src/main/java/com/zwei/iot/broker/service/MqttServerMessageListener.java
git commit -m "refactor: replace hardcoded topic prefix check with ITopicPatternService in MessageListener"
```

---

### Task 8: Modify `MqttDeviceAuthService` to use dynamic pattern

**Files:**
- Modify: `server/zwei-iot-broker/src/main/java/com/zwei/iot/broker/service/MqttDeviceAuthService.java`

- [ ] **Step 1: Remove hardcoded constants and inject service**

Delete lines 53-59 (the `SYS_TOPIC_PATTERN` and `GB_TOPIC_PATTERN` constants):

```java
// DELETE these lines:
private static final Pattern SYS_TOPIC_PATTERN = Pattern.compile("^sys/v1/(?<deviceCode>[A-Za-z0-9_-]{1,64})/(?<sensorCode>[A-Za-z0-9_-]{1,100})/updata$");
private static final Pattern GB_TOPIC_PATTERN = Pattern.compile("^gb/v1/(?<deviceCode>[A-Za-z0-9_-]{1,64})/(?<sensorCode>[A-Za-z0-9_-]{1,100})/updata$");
```

Add import:
```java
import com.zwei.iot.device.service.ITopicPatternService;
import com.zwei.iot.device.service.ITopicPatternService.TopicComponents;
```

Add field:
```java
private final ITopicPatternService topicPatternService;
```

Update constructor to include the new parameter:

In the constructor parameter list, add after `ApplicationEventPublisher eventPublisher`:
```java
ITopicPatternService topicPatternService
```

In the constructor body, add:
```java
this.topicPatternService = topicPatternService;
```

- [ ] **Step 2: Rewrite `parsePublishTarget` method**

Replace the existing `parsePublishTarget` (lines 380-390):

```java
private PublishTarget parsePublishTarget(String topic) {
    TopicComponents c = topicPatternService.resolveTopic(topic);
    if (c == null) {
        return null;
    }
    return new PublishTarget(c.deviceCode(), c.sensorCode());
}
```

- [ ] **Step 3: Verify compilation**

Run: `cd server && mvn compile -pl zwei-iot-broker -q`
Expected: BUILD SUCCESS

- [ ] **Step 4: Commit**

```bash
git add server/zwei-iot-broker/src/main/java/com/zwei/iot/broker/service/MqttDeviceAuthService.java
git commit -m "refactor: replace hardcoded topic patterns with ITopicPatternService in MqttDeviceAuthService"
```

---

### Task 9: Modify `MqttServerSubscribeValidator` to use dynamic pattern

**Files:**
- Modify: `server/zwei-iot-broker/src/main/java/com/zwei/iot/broker/component/MqttServerSubscribeValidator.java`

- [ ] **Step 1: Remove hardcoded constants and inject service**

Delete lines 42-45:
```java
// DELETE:
private static final String TOPIC_PREFIX = "sys/v1/";
private static final Pattern TOPIC_PATTERN = Pattern.compile("^sys/v1/(?<deviceCode>[A-Za-z0-9_-]{1,64})/(?<sensorCode>[A-Za-z0-9_-]{1,100})/updata$");
```

Add import:
```java
import com.zwei.iot.device.service.ITopicPatternService;
import com.zwei.iot.device.service.ITopicPatternService.TopicComponents;
```

Remove unused import:
```java
// DELETE:
import java.util.regex.Matcher;
import java.util.regex.Pattern;
```

Add field:
```java
private final ITopicPatternService topicPatternService;
```

Update constructor signature and body to include:
```java
@Autowired
public MqttServerSubscribeValidator(IDeviceSensorService deviceSensorService,
                                    MqttExceptionReporter mqttExceptionReporter,
                                    MqttDeviceSessionRegistry sessionRegistry,
                                    ITopicPatternService topicPatternService) {
    this.deviceSensorService = deviceSensorService;
    this.mqttExceptionReporter = mqttExceptionReporter;
    this.sessionRegistry = sessionRegistry;
    this.topicPatternService = topicPatternService;
}
```

- [ ] **Step 2: Rewrite `isValid` method**

Replace the prefix + regex check (lines 72-95) with a single `resolveTopic` call:

```java
@Override
public boolean isValid(ChannelContext context, String clientId, String topicFilter, MqttQoS qoS) {
    // 空topic过滤
    if (StringUtils.isBlank(topicFilter)) {
        return mqttExceptionReporter.rejectWithDebug(new MqttBusinessException.InvalidTopic(
                mqttExceptionReporter.context(clientId, topicFilter, qoS).build(),
                "订阅主题为空"
        ));
    }

    TopicComponents c = topicPatternService.resolveTopic(topicFilter);
    if (c == null) {
        return mqttExceptionReporter.rejectWithDebug(new MqttBusinessException.InvalidTopic(
                mqttExceptionReporter.context(clientId, topicFilter, qoS).build(),
                "订阅主题格式非法或前缀不匹配"
        ));
    }

    String deviceCode = c.deviceCode();
    String sensorCode = c.sensorCode();

    // 设备归属校验
    String normalizedClientId = clientId == null ? null : clientId.trim();
    Optional<MqttDeviceSession> session = sessionRegistry.getByClientId(normalizedClientId);
    if (session.isEmpty()) {
        return mqttExceptionReporter.rejectWithDebug(new MqttBusinessException.PermissionDenied(
                mqttExceptionReporter.context(clientId, topicFilter, qoS)
                        .putAttribute("deviceCode", deviceCode)
                        .build(),
                "未建立鉴权会话，禁止订阅"
        ));
    }
    if (!Objects.equals(session.get().deviceCode(), deviceCode)) {
        return mqttExceptionReporter.rejectWithDebug(new MqttBusinessException.PermissionDenied(
                mqttExceptionReporter.context(clientId, topicFilter, qoS)
                        .putAttribute("deviceCode", deviceCode)
                        .putAttribute("sessionDeviceCode", session.get().deviceCode())
                        .build(),
                "设备与订阅主题不匹配，禁止订阅"
        ));
    }

    try {
        DeviceSensor sensor = DeviceSensor.builder()
                .deviceCode(deviceCode)
                .sensorCode(sensorCode)
                .build();
        boolean exists = StringUtils.isNotEmpty(deviceSensorService.selectSensorList(sensor));
        if (!exists) {
            return mqttExceptionReporter.rejectWithDebug(new MqttBusinessException.PermissionDenied(
                    mqttExceptionReporter.context(clientId, topicFilter, qoS)
                            .putAttribute("deviceCode", deviceCode)
                            .putAttribute("sensorCode", sensorCode)
                            .build(),
                    "测点不存在或无权限订阅"
            ));
        }
        log.debug("[MQTT] Valid topic. clientId: {}, topic: {}", clientId, topicFilter);
    } catch (Exception e) {
        return mqttExceptionReporter.rejectWithError(new MqttCommunicationException.SubscribeFailed(
                mqttExceptionReporter.context(clientId, topicFilter, qoS)
                        .putAttribute("deviceCode", deviceCode)
                        .putAttribute("sensorCode", sensorCode)
                        .build(),
                "订阅校验异常",
                e
        ), e);
    }

    return true;
}
```

- [ ] **Step 3: Verify compilation**

Run: `cd server && mvn compile -pl zwei-iot-broker -q`
Expected: BUILD SUCCESS

- [ ] **Step 4: Commit**

```bash
git add server/zwei-iot-broker/src/main/java/com/zwei/iot/broker/component/MqttServerSubscribeValidator.java
git commit -m "refactor: replace hardcoded topic prefix with ITopicPatternService in SubscribeValidator"
```

---

### Task 10: Update `MonitorTopicParserTest` to mock `ITopicPatternService`

**Files:**
- Modify: `server/zwei-iot-parser/src/test/java/com/zwei/iot/parser/support/MonitorTopicParserTest.java`

- [ ] **Step 1: Read existing test file and update**

Read the existing test file first, then rewrite it to mock `ITopicPatternService` instead of relying on the old hardcoded Pattern:

```java
package com.zwei.iot.parser.support;

import com.zwei.iot.device.service.ITopicPatternService;
import com.zwei.iot.device.service.ITopicPatternService.TopicComponents;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class MonitorTopicParserTest {

    @Mock
    private ITopicPatternService topicPatternService;

    private MonitorTopicParser parser;

    @BeforeEach
    void setUp() {
        parser = new MonitorTopicParser(topicPatternService);
    }

    @Test
    @DisplayName("parse extracts components from sys topic")
    void parse_sysTopic_returnsMonitorTopic() {
        when(topicPatternService.resolveTopic("sys/v1/DEV001/S01/updata"))
                .thenReturn(new TopicComponents("sys", "DEV001", "S01"));

        MonitorTopic result = parser.parse("sys/v1/DEV001/S01/updata");

        assertThat(result).isNotNull();
        assertThat(result.sourceType()).isEqualTo("sys");
        assertThat(result.deviceCode()).isEqualTo("DEV001");
        assertThat(result.sensorCode()).isEqualTo("S01");
    }

    @Test
    @DisplayName("parse extracts components from gb topic")
    void parse_gbTopic_returnsMonitorTopic() {
        when(topicPatternService.resolveTopic("gb/v1/ABC/S_99/updata"))
                .thenReturn(new TopicComponents("gb", "ABC", "S_99"));

        MonitorTopic result = parser.parse("gb/v1/ABC/S_99/updata");

        assertThat(result).isNotNull();
        assertThat(result.sourceType()).isEqualTo("gb");
        assertThat(result.deviceCode()).isEqualTo("ABC");
        assertThat(result.sensorCode()).isEqualTo("S_99");
    }

    @Test
    @DisplayName("parse returns null when resolveTopic returns null")
    void parse_invalidTopic_returnsNull() {
        when(topicPatternService.resolveTopic(anyString())).thenReturn(null);

        assertThat(parser.parse("invalid")).isNull();
        assertThat(parser.parse((String) null)).isNull();
    }
}
```

- [ ] **Step 2: Run test to verify**

Run: `cd server && mvn test -pl zwei-iot-parser -Dtest=MonitorTopicParserTest -q`
Expected: Tests run: 3, Failures: 0

- [ ] **Step 3: Commit**

```bash
git add server/zwei-iot-parser/src/test/java/com/zwei/iot/parser/support/MonitorTopicParserTest.java
git commit -m "test: update MonitorTopicParserTest to mock ITopicPatternService"
```

---

### Task 11: Update `MqttServerSubscribeValidatorTest` to mock `ITopicPatternService`

**Files:**
- Modify: `server/zwei-iot-broker/src/test/java/com/zwei/iot/broker/component/MqttServerSubscribeValidatorTest.java`

- [ ] **Step 1: Read existing test and add mock for ITopicPatternService**

Read the existing test file, then add `@Mock ITopicPatternService` and update the setUp to construct with 4 params. Update any test cases that set up topic validation to use `topicPatternService.resolveTopic()` instead.

Key changes:
```java
@Mock
private ITopicPatternService topicPatternService;

// In setUp:
validator = new MqttServerSubscribeValidator(deviceSensorService, mqttExceptionReporter, sessionRegistry, topicPatternService);

// In valid topic tests:
when(topicPatternService.resolveTopic("sys/v1/DEV001/S01/updata"))
        .thenReturn(new TopicComponents("sys", "DEV001", "S01"));

// In invalid topic tests:
when(topicPatternService.resolveTopic(anyString())).thenReturn(null);
```

- [ ] **Step 2: Run test to verify**

Run: `cd server && mvn test -pl zwei-iot-broker -Dtest=MqttServerSubscribeValidatorTest -q`
Expected: All existing tests pass

- [ ] **Step 3: Commit**

```bash
git add server/zwei-iot-broker/src/test/java/com/zwei/iot/broker/component/MqttServerSubscribeValidatorTest.java
git commit -m "test: update SubscribeValidatorTest to mock ITopicPatternService"
```

---

### Task 12: Full module compilation and test run

**Files:** N/A (verification only)

- [ ] **Step 1: Compile all affected modules**

Run: `cd server && mvn compile -pl zwei-iot-device,zwei-iot-parser,zwei-iot-timeseries,zwei-iot-broker -q`
Expected: BUILD SUCCESS

- [ ] **Step 2: Run all parser module tests**

Run: `cd server && mvn test -pl zwei-iot-parser -q`
Expected: All tests pass

- [ ] **Step 3: Run all broker module tests**

Run: `cd server && mvn test -pl zwei-iot-broker -q`
Expected: All tests pass

- [ ] **Step 4: Run timeseries module tests**

Run: `cd server && mvn test -pl zwei-iot-timeseries -q`
Expected: All tests pass (MonitorIngestFacadeTest uses hardcoded "sys/v1/..." topic, needs ITopicPatternService bean in test context)

- [ ] **Step 5: Commit if any test-only fixes made**

```bash
git add -A
git commit -m "test: fix test configuration for dynamic topic pattern"
```
