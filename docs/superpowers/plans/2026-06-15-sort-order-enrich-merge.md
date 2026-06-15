# sort_order 字段 + enrich/validate 合并 实现计划

- [ ] **For agentic workers:** Use superpowers:subagent-driven-development to implement task-by-task.

**Goal:** 给 `monitor_content` 表加 `sort_order` 字段，合并 `enrichProperties` 和 TSL validation 为一次 `ProductTsl` 查询。

**Architecture:** TSL properties 按 `monitor_content.sort_order` 排序后持久化，`enrichProperties` 直接用 `tsl.properties.get(N)` 做 CSV 位置映射，同时复用同一份 TSL 做值域校验，消除 `SensorMetadata` 查询。

**Tech Stack:** Java 17, MyBatis, MySQL 8.0

---

### Task 1: DDL — monitor_content 表加 sort_order 列

**Files:**
- 创建: `db/upgrade/v2.2-sort-order.sql`

- [ ] **Step 1: 编写 DDL**

```sql
-- ============================================================
-- v2.2: monitor_content 增加 sort_order 字段
-- 每个 monitor_type 内 sort_order 从 1 递增，monitor_type 之间独立
-- ============================================================

ALTER TABLE monitor_content ADD COLUMN sort_order INT NOT NULL DEFAULT 0 COMMENT '排序号(每个监测类型内从1递增)' AFTER unit;

-- 按现有 id 顺序初始化 sort_order（每个 monitor_type 内独立编号）
UPDATE monitor_content mc
SET sort_order = (
    SELECT rn FROM (
        SELECT id, ROW_NUMBER() OVER (PARTITION BY monitor_type_id ORDER BY id) AS rn
        FROM monitor_content WHERE del_flag = 0
    ) t WHERE t.id = mc.id
)
WHERE del_flag = 0;
```

- [ ] **Step 2: Commit**

```bash
git add db/upgrade/v2.2-sort-order.sql
git commit -m "feat: add sort_order column to monitor_content table"
```

---

### Task 2: MonitorContent domain — 加 sortOrder 字段

**Files:**
- 修改: `server/zwei-iot-monitor/src/main/java/com/zwei/iot/monitor/domain/MonitorContent.java`
- 修改: `server/zwei-iot-monitor/src/main/java/com/zwei/iot/monitor/domain/dto/MonitorContentCreateRequest.java`
- 修改: `server/zwei-iot-monitor/src/main/java/com/zwei/iot/monitor/domain/dto/MonitorContentUpdateRequest.java`
- 修改: `server/zwei-iot-monitor/src/main/java/com/zwei/iot/monitor/mapper/MonitorContentMapper.xml`
- 修改: `server/zwei-iot-monitor/src/main/java/com/zwei/iot/monitor/controller/MonitorContentController.java`

- [ ] **Step 1: 读现有文件并加字段**

`MonitorContent.java` — 加:
```java
private Integer sortOrder;
```

`MonitorContentCreateRequest.java` — 加:
```java
private Integer sortOrder;
```

`MonitorContentUpdateRequest.java` — 加:
```java
private Integer sortOrder;
```

- [ ] **Step 2: 更新 Controller 的 build 方法**

`MonitorContentController.buildMonitorContentForCreate()` 加:
```java
monitorContent.setSortOrder(request.getSortOrder());
```

`MonitorContentController.buildMonitorContentForUpdate()` 加:
```java
if (request.getSortOrder() != null) {
    monitorContent.setSortOrder(request.getSortOrder());
}
```

- [ ] **Step 3: 更新 MyBatis XML**

`MonitorContentMapper.xml` — 所有 SQL 语句的列列表中加 `sort_order`:
- `selectMonitorContentList` / `selectMonitorContentAll` / `selectMonitorContentById` / `selectMonitorContentByCode` → resultMap 加 `<result column="sort_order" property="sortOrder"/>`
- `insertMonitorContent` → 列列表加 `sort_order`，值加 `#{sortOrder}`
- `updateMonitorContent` → SET 子句加 `sort_order = #{sortOrder}`
- `checkMonitorContentCodeUnique` → resultMap 同上

- [ ] **Step 4: 验证编译**

```bash
cd server && mvn clean compile -pl zwei-iot-monitor -am
```

预期: BUILD SUCCESS

- [ ] **Step 5: Commit**

```bash
git add server/zwei-iot-monitor/
git commit -m "feat: add sortOrder field to MonitorContent domain, DTOs, and mapper"
```

---

### Task 3: SensorAttribute 查询 — 按 monitor_content.sort_order 排序

**Files:**
- 修改: `server/zwei-iot-device/src/main/resources/mapper/iot/device/SensorAttributeMapper.xml`

- [ ] **Step 1: 修改 selectAttributeListByDeviceId 查询**

在 `selectAttributeListByDeviceId` 的 SQL 中加 JOIN + ORDER BY:

```xml
<select id="selectAttributeListByDeviceId" resultMap="SensorAttributeResult">
    SELECT sa.*
    FROM sensor_attribute sa
    INNER JOIN device_sensor ds ON sa.sensor_id = ds.id
    LEFT JOIN monitor_content mc ON sa.monitor_content_id = mc.id
    WHERE ds.device_id = #{deviceId}
      AND ds.del_flag = 0
      AND sa.del_flag = 0
    ORDER BY mc.sort_order ASC, sa.id ASC
</select>
```

> 注: 需要先读实际 XML 确认当前 SQL 结构，按实际格式调整。`LEFT JOIN` 确保没有 monitor_content 关联的属性也不会被过滤掉。

- [ ] **Step 2: 同样修改 selectAttributeListBySensorId**

加 `ORDER BY mc.sort_order ASC, sa.id ASC`

- [ ] **Step 3: 验证编译 + 运行已有测试**

```bash
cd server && mvn test -pl zwei-iot-device -Dtest="DeviceSensorServiceImplTest,ProductTslServiceImplTest"
```

预期: 所有已有测试通过

- [ ] **Step 4: Commit**

```bash
git add server/zwei-iot-device/src/main/resources/mapper/iot/device/SensorAttributeMapper.xml
git commit -m "feat: sort sensor attributes by monitor_content.sort_order"
```

---

### Task 4: enrichProperties — 用 ProductTsl 替代 SensorMetadata

**Files:**
- 修改: `server/zwei-iot-timeseries/src/main/java/com/zwei/iot/timeseries/service/MonitorIngestFacade.java:77-106`

- [ ] **Step 1: 重写 ingest() 的 Step 4-5**

将当前分开的 enrich + validate 合并为一次 TSL 查询:

```java
// 4. Enrich + Validate: use TSL properties (sorted by sort_order) for both
try {
    var tsl = metadataService.getTsl(deviceId);
    if (tsl != null && tsl.properties() != null) {
        parsedMessage = enrichProperties(parsedMessage, tsl);
    }
} catch (Exception e) {
    log.warn("TSL lookup failed, skip enrichment and validation: deviceId={}", deviceId, e);
}
```

**enrichProperties 重写：**

```java
private ParsedMessage enrichProperties(ParsedMessage message, ProductTsl tsl) {
    List<PropertyValue> props = message.properties();
    // Fast path: no positional identifiers
    boolean hasPositional = false;
    for (var p : props) {
        if (p.identifier() != null && p.identifier().matches("value_\\d+")) {
            hasPositional = true;
            break;
        }
    }
    if (!hasPositional) return message;

    List<TslProperty> tslProps = tsl.properties();
    List<PropertyValue> enriched = new ArrayList<>();
    for (var p : props) {
        String id = p.identifier();
        if (id != null && id.matches("value_\\d+")) {
            int idx = Integer.parseInt(id.substring(6));
            if (idx >= 0 && idx < tslProps.size()) {
                var tslProp = tslProps.get(idx);
                // Enrich with real identifier + unit from TSL
                enriched.add(new PropertyValue(
                        tslProp.identifier(),
                        tslProp.name(),
                        tslProp.dataType() != null && tslProp.dataType().specs() != null
                                ? tslProp.dataType().specs().unit() : null,
                        p.value(),
                        p.quality()));
                // Validate against TSL specs
                validateValue(tslProp, p.value());
            } else {
                log.warn("Positional identifier {} out of range (TSL properties size={}), keeping as-is",
                        id, tslProps.size());
                enriched.add(p);
            }
        } else {
            enriched.add(p);
        }
    }
    return new ParsedMessage(message.deviceCode(), message.sensorCode(), message.sourceType(),
            message.dataTime(), message.receiveTime(), message.payloadHash(), enriched);
}

private void validateValue(TslProperty tslProp, Double value) {
    if (tslProp.dataType() == null || tslProp.dataType().specs() == null) return;
    var specs = tslProp.dataType().specs();
    if (specs.min() != null && value != null && value < Double.parseDouble(specs.min())) {
        log.warn("Property value below min: {}={}, min={}", tslProp.identifier(), value, specs.min());
    }
    if (specs.max() != null && value != null && value > Double.parseDouble(specs.max())) {
        log.warn("Property value exceeds max: {}={}, max={}", tslProp.identifier(), value, specs.max());
    }
}
```

- [ ] **Step 2: 清理 imports — 移除不再需要的 SensorMetadata/SensorAttribute 相关 import**

Facade 不再需要:
```java
import com.zwei.iot.device.domain.SensorAttribute; // 删除
```
新增:
```java
import com.zwei.iot.device.domain.tsl.ProductTsl;
import com.zwei.iot.device.domain.tsl.TslProperty;
```

- [ ] **Step 3: 验证编译 + 运行测试**

```bash
cd server && mvn clean test -pl zwei-iot-timeseries,zwei-iot-parser -am
```

预期: BUILD SUCCESS, 所有测试通过

- [ ] **Step 4: Commit**

```bash
git add server/zwei-iot-timeseries/src/main/java/com/zwei/iot/timeseries/service/MonitorIngestFacade.java
git commit -m "refactor: merge enrichProperties and TSL validation into single ProductTsl lookup"
```

---

### Task 5: 验证端到端数据通路

- [ ] **Step 1: 全量编译 + 测试**

```bash
cd server && mvn clean test -pl zwei-iot-parser,zwei-iot-timeseries,zwei-iot-device -am
```

预期: BUILD SUCCESS, 150+ 测试通过

- [ ] **Step 2: 验证 DB 迁移正确性**（手动）

```sql
-- 检查 sort_order 初始化
SELECT mt.name AS type_name, mc.code, mc.sort_order
FROM monitor_content mc
JOIN monitor_type mt ON mc.monitor_type_id = mt.id
WHERE mc.del_flag = 0
ORDER BY mt.id, mc.sort_order;

-- 验证 TSL properties 排序
SELECT d.id AS device_id, d.code, p.tsl_json
FROM device d
JOIN product p ON p.device_id = d.id
WHERE d.del_flag = 0;
-- 检查 JSON 中 properties 数组的顺序是否与 sort_order 一致
```

- [ ] **Step 3: 模拟数据解析**（手动，启动应用后）

```bash
# 发送 sys CSV 报文
curl -X POST http://localhost:8080/api/v1/iot/parser/strategy/test \
  -H "Authorization: Bearer <token>" \
  -H "Content-Type: application/json" \
  -d '{
    "strategyId": 1,
    "scriptCode": "<sys preset script>",
    "topic": "sys/v1/Device_AA_001/WY_1/updata",
    "testData": "{\"version\":\"1.0\",\"data\":{\"value\":\"16.7,24.1,9.7\"}}"
  }'
```

预期: `parsedMessage.properties` 中的 identifier 为真实 attrCode（如 `rainfall_hour`），而非 `value_0`

- [ ] **Step 4: Commit**

```bash
git commit -m "chore: end-to-end verification — sort_order enrichment pipeline"
```

---

## 变更影响范围

| 模块 | 文件 | 变更类型 |
|------|------|---------|
| db | `upgrade/v2.2-sort-order.sql` | 新增 |
| zwei-iot-monitor | `domain/MonitorContent.java` | 加 sortOrder 字段 |
| zwei-iot-monitor | `dto/MonitorContentCreateRequest.java` | 加 sortOrder |
| zwei-iot-monitor | `dto/MonitorContentUpdateRequest.java` | 加 sortOrder |
| zwei-iot-monitor | `mapper/MonitorContentMapper.xml` | 加 sort_order 列映射 |
| zwei-iot-monitor | `controller/MonitorContentController.java` | build 方法加 sortOrder |
| zwei-iot-device | `mapper/SensorAttributeMapper.xml` | SQL 加 JOIN monitor_content + ORDER BY sort_order |
| zwei-iot-timeseries | `service/MonitorIngestFacade.java` | enrichProperties 用 TSL 替代 SensorMetadata, 合并 validate |
