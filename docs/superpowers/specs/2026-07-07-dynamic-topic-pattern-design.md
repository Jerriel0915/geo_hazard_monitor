# Dynamic Topic Pattern Registry — Design Spec

> Date: 2026-07-07 | Status: Approved | Module: zwei-iot-broker, zwei-iot-parser, zwei-iot-device

## Problem

MQTT topic prefix filtering is hardcoded in 4 files across 2 modules:

| File | Hardcoded Check |
|------|-----------------|
| `MqttServerMessageListener.java:86` | `topic.startsWith("sys/v1/") \|\| topic.startsWith("gb/v1/")` |
| `MqttDeviceAuthService.java:55,59` | `SYS_TOPIC_PATTERN` / `GB_TOPIC_PATTERN` regex |
| `MqttServerSubscribeValidator.java:43,45,81` | `TOPIC_PREFIX = "sys/v1/"` |
| `MonitorTopicParser.java:23-24` | `^(sys\|gb)/v1/...` regex |

When a new vendor protocol (e.g., `hj/v1/...`) is added via a `DataParseStrategy` with `source_type='hj'`, the broker still rejects messages because it doesn't recognize the prefix. Code changes are needed in all four locations.

## Constraint

Path structure `/v1/{deviceCode}/{sensorCode}/updata` is fixed; only the first path segment (`sourceType`) varies.

## Solution Overview

Introduce a cross-module **`ITopicPatternService`** that derives the active topic prefix set from `DataParseStrategy.sourceType` at runtime. All four hardcoded locations delegate to this service. Changes take effect via a manual reload API (no restart needed).

## Design Decisions

- **Reload, not real-time**: prefix list is cached in-memory (AtomicReference). Refreshed automatically on strategy CRUD operations in `DataParseStrategyService`, plus a manual `POST /api/v1/iot/parser/strategy/topic-patterns/reload` endpoint for operational scenarios.
- **Interface in device module**: follows established convention (`IDeviceSensorQueryService`, `IProductTslService` are already defined in `zwei-iot-device` and consumed by other modules).
- **Implementation in parser module**: closest to the data source (strategies table).
- **Pattern derived from sourceType**: since path structure is fixed, the combined regex is `^(sys|gb|hj|...)/v1/([A-Za-z0-9_-]+)/([A-Za-z0-9_-]+)/updata$`, built from the active sourceType set.
- **`DataParseStrategy.topic` field is unused**: the existing display-only `topic` column is not repurposed — all routing continues through `sourceType`.

## Architecture

```
iot_data_parse_strategy (MySQL)
  SELECT DISTINCT source_type WHERE status=1 AND del_flag=0
         │
         ▼
  TopicPatternServiceImpl (parser 模块)
    AtomicReference<CachedPatterns>
         │  implements
         ▼
  ITopicPatternService (device 模块 — 接口定义)
         │  @Autowired into broker module
         ▼
  ├── MqttServerMessageListener    (替换 startsWith)
  ├── MqttServerSubscribeValidator (替换 TOPIC_PREFIX/TOPIC_PATTERN)
  ├── MqttDeviceAuthService        (替换 SYS/GB_TOPIC_PATTERN)
  └── MonitorTopicParser           (替换硬编码正则)
```

## Files

### New Files

| File | Module | Purpose |
|------|--------|---------|
| `ITopicPatternService.java` | zwei-iot-device | Interface: `matches()`, `resolveSourceType()`, `getActiveSourceTypes()`, `reload()` |
| `TopicPatternServiceImpl.java` | zwei-iot-parser | Implementation: loads from DB, builds combined regex, AtomicReference cache |
| `CachedPatterns.java` (record) | zwei-iot-parser | Inner/package record: `Set<String> sourceTypes`, `Pattern compiledPattern` |

### Modified Files

| File | Module | Change |
|------|--------|--------|
| `DataParseController.java` | zwei-iot-parser | Add `POST /topic-patterns/reload` endpoint |
| `DataParseStrategyService.java` | zwei-iot-parser | Auto-call `topicPatternService.reload()` after create/update/delete/toggle/copy |
| `MqttServerMessageListener.java` | zwei-iot-broker | L86: `topicPatternService.matches(topic)` replaces hardcoded prefixes |
| `MqttDeviceAuthService.java` | zwei-iot-broker | Inject `ITopicPatternService`, delete `SYS_TOPIC_PATTERN`/`GB_TOPIC_PATTERN`, refactor `parsePublishTarget()` |
| `MqttServerSubscribeValidator.java` | zwei-iot-broker | Inject `ITopicPatternService`, delete `TOPIC_PREFIX`/`TOPIC_PATTERN` constants |
| `MonitorTopicParser.java` | zwei-iot-parser | Inject `ITopicPatternService`, delete hardcoded regex |
| `BrokerTestConfiguration.java` | zwei-iot-broker | Add `ITopicPatternService` mock bean |
| `MqttServerMessageListenerTest.java` | zwei-iot-broker | Update topic filter test cases |
| `MqttServerSubscribeValidatorTest.java` | zwei-iot-broker | Update topic validation test cases |

## Interface Contract

```java
public interface ITopicPatternService {
    /** Check if topic matches any registered protocol prefix */
    boolean matches(String topic);

    /** Extract sourceType from topic; returns null if no pattern matches */
    String resolveSourceType(String topic);

    /** Current active sourceType set (read-only, for display/monitoring) */
    Set<String> getActiveSourceTypes();

    /** Force reload from database */
    void reload();
}
```

## Topic Pattern Derivation

Given active sourceTypes = `{"sys", "gb", "hj"}`:

- Combined regex: `^(sys|gb|hj)/v1/([A-Za-z0-9_-]{1,64})/([A-Za-z0-9_-]{1,100})/updata$`
- `matches(topic)`: test against compiled pattern → boolean
- `resolveSourceType(topic)`: match + extract group(1) → "sys" / "gb" / "hj" / null

Path structure (`/v1/{deviceCode}/{sensorCode}/updata`) and segment constraints (`[A-Za-z0-9_-]`, 1-64/1-100 chars) are preserved from the existing hardcoded patterns.

## Reload API

```
POST /api/v1/iot/parser/topic-patterns/reload
→ topicPatternService.reload()
→ Response: { "code": 200, "data": ["sys", "gb", "hj"] }
```

**Startup**: `@PostConstruct` on `TopicPatternServiceImpl` calls `reload()` automatically.

## Migration Compatibility

Existing strategies have `source_type = 'sys'` and `source_type = 'gb'` (preset). After deployment, `reload()` picks up both — no behavior change for existing devices.

## Test Plan

- [ ] `TopicPatternServiceImpl.reload()` loads distinct sourceTypes from DB correctly
- [ ] `matches()` returns true for `sys/v1/DEV001/S01/updata`, false for `unknown/v1/DEV001/S01/updata`
- [ ] `resolveSourceType()` extracts correct sourceType, returns null for unknown
- [ ] `MqttServerMessageListener` rejects unknown prefix topics with `MqttMessageRejectEvent` (stage=TOPIC)
- [ ] `MqttServerMessageListener` accepts known prefix topics and proceeds to ingest
- [ ] `MqttServerSubscribeValidator` subscribes: allows known prefixes, rejects unknown
- [ ] `MqttDeviceAuthService.hasPublishPermission()` works with dynamic patterns
- [ ] `MonitorTopicParser.parse()` works with single prefix (sys/gb) and multi-prefix
- [ ] Reload API returns updated list after strategy create/enable
- [ ] Reload API returns updated list after strategy disable
- [ ] Startup auto-load: existing sys/gb strategies => both prefixes active
- [ ] Empty DB (no enabled strategies): matches() returns false, reload API returns empty list
